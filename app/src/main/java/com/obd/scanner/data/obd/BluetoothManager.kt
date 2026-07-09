package com.obd.scanner.data.obd

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import com.obd.scanner.data.obd.protocol.Elm327
import com.obd.scanner.domain.model.ConnectionState
import com.obd.scanner.domain.model.ObdProtocol
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID

class BluetoothManager {
    private val SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private var socket: BluetoothSocket? = null
    private var elm327: Elm327? = null

    private val _pairedDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val pairedDevices: StateFlow<List<BluetoothDevice>> = _pairedDevices.asStateFlow()

    private val _scanResults = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val scanResults: StateFlow<List<BluetoothDevice>> = _scanResults.asStateFlow()

    fun loadPairedDevices() {
        try {
            val adapter = BluetoothAdapter.getDefaultAdapter()
            _pairedDevices.value = adapter?.bondedDevices?.toList()?.filter {
                it.name?.contains("OBD", ignoreCase = true) == true ||
                it.name?.contains("ELM", ignoreCase = true) == true ||
                it.name?.contains("BT", ignoreCase = true) == true ||
                it.name?.contains("CAR", ignoreCase = true) == true ||
                it.name?.contains("OBDII", ignoreCase = true) == true ||
                it.name?.contains("VEEPEAK", ignoreCase = true) == true ||
                it.name?.contains("KIWI", ignoreCase = true) == true ||
                it.name?.contains("PLX", ignoreCase = true) == true ||
                it.name?.contains("SCAN", ignoreCase = true) == true
            }?.ifEmpty { adapter.bondedDevices?.toList() } ?: emptyList()
        } catch (_: SecurityException) {
            _pairedDevices.value = emptyList()
        }
    }

    fun isBluetoothEnabled(): Boolean = try {
        BluetoothAdapter.getDefaultAdapter()?.isEnabled == true
    } catch (_: SecurityException) {
        false
    }

    suspend fun connect(device: BluetoothDevice): Result<Unit> = withContext(Dispatchers.IO) {
        _connectionState.value = ConnectionState.Connecting
        try {
            connectClassic(device)
            _connectionState.value = ConnectionState.Initializing
            val initResult = elm327?.initialize()
            if (initResult?.isSuccess == true) {
                _connectionState.value = ConnectionState.Connected(
                    deviceName = safeDeviceName(device),
                    protocol = initResult.getOrNull() ?: ObdProtocol.AUTO
                )
                Result.success(Unit)
            } else {
                disconnect()
                _connectionState.value = ConnectionState.Error(
                    initResult?.exceptionOrNull()?.message ?: "Failed to initialize ELM327"
                )
                Result.failure(Exception("Initialization failed"))
            }
        } catch (e: SecurityException) {
            disconnect()
            _connectionState.value = ConnectionState.Error("Bluetooth permission not granted")
            Result.failure(e)
        } catch (e: Exception) {
            disconnect()
            _connectionState.value = ConnectionState.Error(e.message ?: "Connection failed")
            Result.failure(e)
        }
    }

    fun safeDeviceName(device: BluetoothDevice): String = try {
        device.name ?: device.address
    } catch (_: SecurityException) {
        device.address
    }

    /**
     * Cheap ELM327 clones often reject the standard secure SPP socket
     * ("read failed, socket might closed... ret: -1"), so fall back through
     * insecure SPP and then a direct RFCOMM channel-1 socket via reflection.
     */
    private suspend fun connectClassic(device: BluetoothDevice) {
        BluetoothAdapter.getDefaultAdapter()?.cancelDiscovery()

        val strategies: List<Pair<String, () -> BluetoothSocket>> = listOf(
            "secure SPP" to { device.createRfcommSocketToServiceRecord(SPP_UUID) },
            "insecure SPP" to { device.createInsecureRfcommSocketToServiceRecord(SPP_UUID) },
            "RFCOMM channel 1" to {
                device.javaClass
                    .getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                    .invoke(device, 1) as BluetoothSocket
            }
        )

        var lastError: Exception? = null
        for ((_, createSocket) in strategies) {
            val sock = try { createSocket() } catch (e: Exception) {
                lastError = e; continue
            }
            try {
                sock.connect()
                socket = sock
                elm327 = Elm327(
                    inputStream = sock.inputStream,
                    outputStream = sock.outputStream
                )
                return
            } catch (e: Exception) {
                try { sock.close() } catch (_: Exception) {}
                lastError = e
                delay(400)
            }
        }
        throw lastError ?: IOException("No se pudo abrir el socket Bluetooth")
    }

    suspend fun disconnect() {
        withContext(Dispatchers.IO) {
            try {
                elm327?.close()
                elm327 = null
                socket?.close()
            } catch (_: Exception) {}
            socket = null
            _connectionState.value = ConnectionState.Disconnected
        }
    }

    fun getElm327(): Elm327? = elm327
}
