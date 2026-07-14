# Puente OBD-II para Kali / Linux 🐧🔌

Diagnostica cualquier vehículo desde tu laptop con un adaptador ELM327 Bluetooth,
y deja que una IA analice el resultado. Sin instalar la app Android.

## Requisitos

- Adaptador ELM327 Bluetooth (vLinker, Veepeak, etc.)
- `pyserial` (`pip install pyserial` — o ya viene en Kali)
- Estar en los grupos `dialout` y `bluetooth` (Kali por defecto sí)

## Uso paso a paso

### 1. Empareja el adaptador (una sola vez)

```bash
bluetoothctl
# dentro de bluetoothctl:
scan on           # espera a ver tu adaptador (ej. Android-Vlink)
pair AA:BB:CC:DD:EE:FF
trust AA:BB:CC:DD:EE:FF
quit
```
(Sustituye la MAC por la de tu adaptador; aparece en `scan on`.)

### 2. Escaneo de diagnóstico completo

Con el adaptador en el puerto OBD y el **motor encendido**:

```bash
sudo python3 obd_bridge.py --mac AA:BB:CC:DD:EE:FF
```

Lee VIN, códigos confirmados y pendientes, freeze frame y sensores en vivo,
y guarda todo en `obd_report.json`.

### 3. Análisis con IA

Abre una sesión de tu asistente (Claude Code, opencode, etc.) en esta carpeta y
pídele: *"analiza obd_report.json y dime qué tiene el carro"*. El JSON es
autoexplicativo (nombres, valores, unidades, códigos).

### Modo interactivo (avanzado)

Para mandar comandos OBD/AT a mano:

```bash
sudo python3 obd_bridge.py --mac AA:BB:CC:DD:EE:FF --interactive
obd> 010C      # RPM
obd> ATRV      # voltaje de batería
obd> 03        # códigos de falla crudos
obd> q
```

## Qué lee (todo solo-lectura, seguro)

| Dato | Modo OBD |
|------|----------|
| VIN | 09 02 |
| Códigos confirmados | 03 |
| Códigos pendientes (antes del check-engine) | 07 |
| Freeze frame (condiciones al fallar) | 02 |
| Sensores en vivo (RPM, temp, voltaje, fuel trims, etc.) | 01 |

> Este puente **no escribe** a la ECU (salvo la autodetección de protocolo del
> propio ELM327). No hace tuning ni coding — es para diagnóstico. Úsalo solo en
> vehículos propios o con permiso del dueño.

## Diagnosticar el carro de alguien más

Flujo típico: llevas tu laptop + adaptador, lo conectas al OBD del vehículo (con
permiso del dueño), corres el escaneo y compartes `obd_report.json` con tu IA
para el diagnóstico en vivo. Si quieres guardar histórico, súbelo a tu Supabase
igual que hace la app.
