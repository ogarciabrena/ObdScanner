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

    // PIDs the vehicle doesn't support waste a full timeout per cycle;
    // after MAX_PID_FAILURES consecutive misses they're skipped.
    private val pidFailures = mutableMapOf<Int, Int>()

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
        pidFailures.clear()
        _state.value = _state.value.copy(isScanning = true)
        scanJob = viewModelScope.launch {
            while (isActive) {
                for (pid in _state.value.selectedPids) {
                    if ((pidFailures[pid.pid] ?: 0) >= MAX_PID_FAILURES) continue
                    val data = obdUseCase.getSensorData(pid)
                    if (data != null) {
                        pidFailures[pid.pid] = 0
                        // Publish each reading as it arrives: while a trip is
                        // recording, the shared ELM327 channel halves the scan
                        // rate and a whole-cycle batch would look frozen.
                        _state.value = _state.value.copy(
                            sensors = _state.value.sensors + (pid.pid to data)
                        )
                    } else {
                        pidFailures[pid.pid] = (pidFailures[pid.pid] ?: 0) + 1
                    }
                    delay(10)
                }
                delay(100)
            }
        }
    }

    private companion object {
        const val MAX_PID_FAILURES = 3
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
