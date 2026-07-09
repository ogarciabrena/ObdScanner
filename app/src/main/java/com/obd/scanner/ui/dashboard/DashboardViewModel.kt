package com.obd.scanner.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.obd.scanner.data.obd.BluetoothManager
import com.obd.scanner.data.obd.commands.ObdPids
import com.obd.scanner.domain.model.ConnectionState
import com.obd.scanner.domain.model.ObdCommand
import com.obd.scanner.domain.model.SensorData
import com.obd.scanner.domain.usecase.ObdUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class DashboardState(
    val connectionState: ConnectionState = ConnectionState.Disconnected,
    val sensors: Map<Int, SensorData> = emptyMap(),
    val selectedPids: List<ObdCommand> = DefaultPids,
    val isScanning: Boolean = false
)

val DefaultPids = listOf(
    ObdPids.ENGINE_RPM,
    ObdPids.VEHICLE_SPEED,
    ObdPids.COOLANT_TEMP,
    ObdPids.ENGINE_LOAD,
    ObdPids.MAF,
    ObdPids.THROTTLE_POS,
    ObdPids.INTAKE_MAP,
    ObdPids.CONTROL_MODULE_VOLTAGE,
    ObdPids.INTAKE_AIR_TEMP,
    ObdPids.TIMING_ADVANCE,
    ObdPids.FUEL_LEVEL,
    ObdPids.FUEL_RATE,
    // Fuel trims: early indicators of mixture faults (vacuum leaks, injectors, O2 sensors)
    ObdPids.SHORT_FUEL_TRIM_B1,
    ObdPids.LONG_FUEL_TRIM_B1,
)

class DashboardViewModel(
    private val obdUseCase: ObdUseCase,
    private val bluetoothManager: BluetoothManager
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    private var scanJob: Job? = null

    init {
        viewModelScope.launch {
            obdUseCase.connectionState.collect { connState ->
                _state.value = _state.value.copy(connectionState = connState)
                if (connState is ConnectionState.Connected) {
                    startScanning()
                } else {
                    stopScanning()
                }
            }
        }
    }

    fun startScanning() {
        if (scanJob?.isActive == true) return
        _state.value = _state.value.copy(isScanning = true)
        scanJob = viewModelScope.launch {
            while (isActive) {
                val sensors = mutableMapOf<Int, SensorData>()
                for (pid in _state.value.selectedPids) {
                    val data = obdUseCase.getSensorData(pid)
                    if (data != null) {
                        sensors[pid.pid] = data
                    }
                    delay(10)
                }
                _state.value = _state.value.copy(sensors = sensors)
                delay(100)
            }
        }
    }

    fun stopScanning() {
        scanJob?.cancel()
        scanJob = null
        _state.value = _state.value.copy(isScanning = false)
    }

    fun togglePid(command: ObdCommand) {
        val current = _state.value.selectedPids.toMutableList()
        if (current.any { it.pid == command.pid }) {
            current.removeAll { it.pid == command.pid }
        } else {
            current.add(command)
        }
        _state.value = _state.value.copy(selectedPids = current)
    }

    fun disconnect() {
        stopScanning()
        viewModelScope.launch { bluetoothManager.disconnect() }
    }

    override fun onCleared() {
        super.onCleared()
        stopScanning()
    }
}
