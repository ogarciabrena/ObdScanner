#!/usr/bin/env python3
"""
ObdScanner — puente OBD-II para Kali / Linux.

Conecta a un adaptador ELM327 por Bluetooth (RFCOMM) y hace un escaneo de
diagnóstico completo (VIN, códigos confirmados y pendientes, freeze frame y
lecturas en vivo), guardando el resultado como JSON para que una IA lo analice.

Uso:
    # 1. Empareja el adaptador una sola vez (interactivo):
    #    bluetoothctl  ->  scan on ; pair <MAC> ; trust <MAC> ; quit
    # 2. Escaneo de diagnóstico:
    sudo python3 obd_bridge.py --mac AA:BB:CC:DD:EE:FF
    # 3. Modo interactivo (mandar comandos AT/OBD a mano):
    sudo python3 obd_bridge.py --mac AA:BB:CC:DD:EE:FF --interactive

El JSON se escribe en ./obd_report.json (o --out).
"""
import argparse
import json
import os
import subprocess
import sys
import time

# ---- Fórmulas OBD-II estándar (mismas que la app Android) -------------------
def u(b, i=0):           # byte sin signo
    return b[i] if i < len(b) else 0

PIDS = {
    #  pid : (nombre, nº bytes, fórmula, unidad)
    0x04: ("Carga del motor",       1, lambda b: u(b)*100/255,             "%"),
    0x05: ("Refrigerante",          1, lambda b: u(b)-40,                  "°C"),
    0x06: ("Fuel Trim corto B1",    1, lambda b: u(b)/1.28-100,           "%"),
    0x07: ("Fuel Trim largo B1",    1, lambda b: u(b)/1.28-100,           "%"),
    0x0B: ("Presión colector",      1, lambda b: float(u(b)),              "kPa"),
    0x0C: ("RPM",                   2, lambda b: (u(b,0)*256+u(b,1))/4,    "rpm"),
    0x0D: ("Velocidad",             1, lambda b: float(u(b)),              "km/h"),
    0x0E: ("Avance de encendido",   1, lambda b: u(b)/2-64,                "°"),
    0x0F: ("Temp. admisión",        1, lambda b: u(b)-40,                  "°C"),
    0x10: ("Flujo de aire (MAF)",   2, lambda b: (u(b,0)*256+u(b,1))/100,  "g/s"),
    0x11: ("Acelerador",            1, lambda b: u(b)*100/255,             "%"),
    0x1F: ("Tiempo con motor on",   2, lambda b: float(u(b,0)*256+u(b,1)), "s"),
    0x2F: ("Nivel combustible",     1, lambda b: u(b)*100/255,             "%"),
    0x42: ("Voltaje del módulo",    2, lambda b: (u(b,0)*256+u(b,1))/1000, "V"),
    0x46: ("Temp. ambiente",        1, lambda b: u(b)-40,                  "°C"),
    0x5C: ("Temp. aceite motor",    1, lambda b: u(b)-40,                  "°C"),
    0x5E: ("Consumo combustible",   2, lambda b: (u(b,0)*256+u(b,1))/20,   "L/h"),
}

LIVE_SCAN = [0x0C, 0x0D, 0x05, 0x04, 0x06, 0x07, 0x0B, 0x11, 0x0F, 0x42, 0x5E, 0x46, 0x5C]
FREEZE_SCAN = [0x0C, 0x0D, 0x05, 0x04, 0x06, 0x07, 0x0B, 0x11]

DTC_PREFIX = {0: "P", 1: "C", 2: "B", 3: "U"}


class Elm327:
    def __init__(self, device):
        import serial
        self.ser = serial.Serial(device, baudrate=38400, timeout=0.1)

    def _write(self, cmd):
        self.ser.reset_input_buffer()
        self.ser.write((cmd + "\r").encode("ascii"))

    def _read_until_prompt(self, timeout=3.0):
        buf, start = b"", time.time()
        while time.time() - start < timeout:
            b = self.ser.read(1)
            if not b:
                continue
            if b == b">":
                break
            buf += b
        return buf.decode("ascii", "ignore").replace("\r", " ").replace("\n", " ").strip()

    def cmd(self, c, timeout=3.0):
        self._write(c)
        return self._read_until_prompt(timeout)

    def init(self):
        for c in ("ATZ", "ATE0", "ATL0", "ATS1", "ATH0", "ATSP0"):
            self.cmd(c, 2.0)
            time.sleep(0.1)
        # provoca la autodetección de protocolo
        self.cmd("0100", 4.0)

    @staticmethod
    def _hex_bytes(resp):
        return [int(x, 16) for x in resp.split() if len(x) == 2 and all(ch in "0123456789ABCDEFabcdef" for ch in x)]

    def read_pid(self, pid, mode=1):
        resp = self.cmd(f"{mode:02X}{pid:02X}")
        if "NO DATA" in resp or "?" in resp or not resp:
            return None
        b = self._hex_bytes(resp)
        try:
            i = b.index(0x40 + mode)
        except ValueError:
            return None
        if i + 1 < len(b) and b[i + 1] == pid:
            return b[i + 2:]
        return None

    def read_freeze_pid(self, pid):
        resp = self.cmd(f"02{pid:02X}00")
        if "NO DATA" in resp or not resp:
            return None
        b = self._hex_bytes(resp)
        try:
            i = b.index(0x42)
        except ValueError:
            return None
        return b[i + 3:]  # salta 42, pid, frame

    def read_dtcs(self, mode=3):
        resp = self.cmd(f"{mode:02X}")
        return self._parse_dtcs(resp)

    def read_vin(self):
        resp = self.cmd("0902", 4.0)
        b = self._hex_bytes(resp)
        chars = [chr(x) for x in b if 32 <= x <= 126]
        vin = "".join(chars)
        # el VIN son 17 chars alfanuméricos; recorta ruido de cabecera
        clean = "".join(c for c in vin if c.isalnum())
        return clean[-17:] if len(clean) >= 17 else (clean or None)

    @staticmethod
    def _parse_dtcs(resp):
        b = Elm327._hex_bytes(resp)
        # descarta byte de modo (43/47) y de conteo si viene
        if b and b[0] in (0x43, 0x47):
            b = b[1:]
        codes = []
        for i in range(0, len(b) - 1, 2):
            hi, lo = b[i], b[i + 1]
            if hi == 0 and lo == 0:
                continue
            code = DTC_PREFIX[(hi & 0xC0) >> 6] + f"{hi & 0x3F:02X}{lo:02X}"
            codes.append(code)
        return codes

    def close(self):
        try:
            self.ser.close()
        except Exception:
            pass


def bind_rfcomm(mac, channel=1, dev="/dev/rfcomm0"):
    if os.path.exists(dev):
        return dev
    print(f"[*] Bindeando {mac} -> {dev} (canal {channel})...")
    subprocess.run(["rfcomm", "release", dev], stderr=subprocess.DEVNULL)
    r = subprocess.run(["rfcomm", "bind", dev, mac, str(channel)])
    if r.returncode != 0:
        sys.exit("[!] Falló rfcomm bind. ¿Emparejaste el adaptador y corres con sudo?")
    time.sleep(1)
    return dev


def fmt(pid, data):
    name, nb, formula, unit = PIDS[pid]
    try:
        val = formula(data[:nb] if len(data) >= nb else data)
    except Exception:
        return None
    return {"pid": pid, "name": name, "value": round(val, 2), "unit": unit}


def full_scan(elm):
    report = {"timestamp": int(time.time() * 1000)}
    print("[*] Inicializando ELM327...")
    elm.init()

    report["vin"] = elm.read_vin()
    print(f"    VIN: {report['vin']}")

    print("[*] Leyendo códigos de falla...")
    report["stored_dtcs"] = elm.read_dtcs(3)
    report["pending_dtcs"] = elm.read_dtcs(7)
    print(f"    Confirmados: {report['stored_dtcs'] or 'ninguno'}")
    print(f"    Pendientes:  {report['pending_dtcs'] or 'ninguno'}")

    if report["stored_dtcs"]:
        print("[*] Leyendo freeze frame...")
        ff = []
        for pid in FREEZE_SCAN:
            d = elm.read_freeze_pid(pid)
            if d:
                r = fmt(pid, d)
                if r:
                    ff.append(r)
        report["freeze_frame"] = ff

    print("[*] Leyendo sensores en vivo...")
    live = []
    for pid in LIVE_SCAN:
        d = elm.read_pid(pid)
        if d:
            r = fmt(pid, d)
            if r:
                live.append(r)
                print(f"    {r['name']}: {r['value']} {r['unit']}")
    report["live"] = live
    return report


def main():
    ap = argparse.ArgumentParser(description="Puente OBD-II ELM327 para Kali")
    ap.add_argument("--mac", required=True, help="MAC del adaptador (bluetoothctl)")
    ap.add_argument("--channel", type=int, default=1)
    ap.add_argument("--dev", default="/dev/rfcomm0")
    ap.add_argument("--out", default="obd_report.json")
    ap.add_argument("--interactive", action="store_true", help="modo comando a mano")
    args = ap.parse_args()

    dev = bind_rfcomm(args.mac, args.channel, args.dev)
    elm = Elm327(dev)

    try:
        if args.interactive:
            elm.init()
            print("[*] Modo interactivo. Escribe comandos OBD/AT (ej. 010C, ATRV, 03). 'q' para salir.")
            while True:
                c = input("obd> ").strip()
                if c.lower() in ("q", "quit", "exit"):
                    break
                if c:
                    print(elm.cmd(c))
        else:
            report = full_scan(elm)
            with open(args.out, "w") as f:
                json.dump(report, f, indent=2, ensure_ascii=False)
            print(f"\n[✓] Reporte guardado en {args.out} — pásaselo a Claude para el análisis.")
    finally:
        elm.close()


if __name__ == "__main__":
    main()
