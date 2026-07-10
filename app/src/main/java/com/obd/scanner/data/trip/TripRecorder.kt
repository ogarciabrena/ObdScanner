package com.obd.scanner.data.trip

import android.content.Context
import com.obd.scanner.domain.model.SensorData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter

data class TripSummary(
    val id: String,
    val deviceName: String,
    val startTime: Long,
    val endTime: Long,
    val sampleCount: Int,
    val file: File
)

/**
 * Records trip telemetry as one JSONL file per trip under filesDir/trips.
 * Files double as an offline upload queue for cloud sync: upload, then delete.
 */
class TripRecorder(context: Context) {

    private val tripsDir = File(context.filesDir, "trips").apply { mkdirs() }

    /** Completed trips waiting for cloud upload. */
    val pendingDir = File(tripsDir, "pending").apply { mkdirs() }

    init {
        // Recover trips orphaned by a process kill mid-recording: empty files
        // are unrecoverable noise; files with data are queued for upload.
        tripsDir.listFiles { f -> f.isFile && f.extension == "jsonl" }?.forEach { f ->
            if (f.length() == 0L) f.delete() else f.renameTo(File(pendingDir, f.name))
        }
    }

    private var writer: BufferedWriter? = null
    private var currentFile: File? = null
    private var startTime = 0L
    private var sampleCount = 0
    private var deviceName = ""

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _sampleCountFlow = MutableStateFlow(0)
    val sampleCountFlow: StateFlow<Int> = _sampleCountFlow.asStateFlow()

    @Synchronized
    fun start(deviceName: String) {
        if (_isRecording.value) return
        startTime = System.currentTimeMillis()
        this.deviceName = deviceName
        sampleCount = 0
        val file = File(tripsDir, "trip_$startTime.jsonl")
        currentFile = file
        writer = BufferedWriter(FileWriter(file, true))
        writer?.write("""{"type":"start","ts":$startTime,"device":${jsonString(deviceName)}}""")
        writer?.newLine()
        _sampleCountFlow.value = 0
        _isRecording.value = true
    }

    @Synchronized
    fun record(sample: SensorData) {
        val w = writer ?: return
        w.write(
            """{"type":"sample","ts":${sample.timestamp},"pid":${sample.pid},""" +
                """"name":${jsonString(sample.name)},"value":${sample.value},"unit":${jsonString(sample.unit)}}"""
        )
        w.newLine()
        sampleCount++
        _sampleCountFlow.value = sampleCount
        if (sampleCount % 50 == 0) w.flush()
    }

    @Synchronized
    fun stop(): TripSummary? {
        if (!_isRecording.value) return null
        val endTime = System.currentTimeMillis()
        writer?.write("""{"type":"end","ts":$endTime,"samples":$sampleCount}""")
        writer?.newLine()
        try { writer?.close() } catch (_: Exception) {}
        writer = null
        _isRecording.value = false
        val file = currentFile ?: return null
        currentFile = null

        val pendingFile = File(pendingDir, file.name)
        val finalFile = if (file.renameTo(pendingFile)) pendingFile else file

        return TripSummary(
            id = finalFile.nameWithoutExtension,
            deviceName = deviceName,
            startTime = startTime,
            endTime = endTime,
            sampleCount = sampleCount,
            file = finalFile
        )
    }

    fun listPendingTrips(): List<File> =
        pendingDir.listFiles { f -> f.extension == "jsonl" }?.sortedBy { it.name } ?: emptyList()

    private fun jsonString(s: String): String =
        "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
}
