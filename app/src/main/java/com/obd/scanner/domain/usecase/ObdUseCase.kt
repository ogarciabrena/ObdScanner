package com.obd.scanner.domain.usecase

import com.obd.scanner.data.obd.BluetoothManager
import com.obd.scanner.data.obd.commands.ObdPids
import com.obd.scanner.domain.model.ConnectionState
import com.obd.scanner.domain.model.Dtc
import com.obd.scanner.domain.model.ObdCommand
import com.obd.scanner.domain.model.SensorData
import kotlinx.coroutines.flow.StateFlow

class ObdUseCase(private val bluetoothManager: BluetoothManager) {

    val connectionState: StateFlow<ConnectionState> = bluetoothManager.connectionState

    suspend fun getSensorData(command: ObdCommand): SensorData? {
        val elm = bluetoothManager.getElm327() ?: return null
        val result = elm.readPid(command.mode, command.pid)
        if (result.isFailure) return null

        val data = result.getOrNull() ?: return null
        if (data.isEmpty()) return null

        val rawData = if (data.size >= command.bytes) data.take(command.bytes) else data
        val value = command.formula.evaluate(rawData)

        val formatted = when {
            command.unit == "°C" || command.unit == "°" -> String.format("%.1f%s", value, command.unit)
            command.unit == "rpm" -> "%.0f".format(value)
            command.unit == "km/h" -> "%.0f".format(value)
            command.unit == "%" -> "%.1f".format(value)
            command.unit == "V" -> "%.2f".format(value)
            command.unit == "g/s" -> "%.2f".format(value)
            command.unit == "kPa" -> "%.0f".format(value)
            command.unit == "L/h" -> "%.1f".format(value)
            value >= 1000 -> "%.0f".format(value)
            value >= 100 -> "%.1f".format(value)
            else -> "%.2f".format(value)
        }

        return SensorData(
            pid = command.pid,
            name = command.name,
            value = value,
            unit = command.unit,
            formattedValue = "$formatted ${command.unit}",
            minValue = command.minValue,
            maxValue = command.maxValue,
            category = command.category
        )
    }

    suspend fun readDtc(): List<Dtc> {
        val elm = bluetoothManager.getElm327() ?: return emptyList()
        val result = elm.readDtc()
        return result.getOrNull() ?: emptyList()
    }

    suspend fun clearDtc(): Boolean {
        val elm = bluetoothManager.getElm327() ?: return false
        val result = elm.clearDtc()
        return result.isSuccess
    }
}
