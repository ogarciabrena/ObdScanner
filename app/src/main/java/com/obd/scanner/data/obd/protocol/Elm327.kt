package com.obd.scanner.data.obd.protocol

import com.obd.scanner.domain.model.Dtc
import com.obd.scanner.domain.model.ObdProtocol
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import kotlin.coroutines.coroutineContext

class Elm327(
    private val inputStream: InputStream,
    private val outputStream: OutputStream
) {
    private val reader: BufferedReader = BufferedReader(InputStreamReader(inputStream))
    private var isInitialized = false
    private var detectedProtocol: ObdProtocol = ObdProtocol.AUTO

    // Serializes request/response cycles: concurrent callers would interleave
    // commands on the single ELM327 serial channel and corrupt both responses.
    private val ioMutex = Mutex()

    suspend fun initialize(): Result<ObdProtocol> = withContext(Dispatchers.IO) {
        ioMutex.withLock {
            try {
                reset()
                setEcho(false)
                setLinefeed(true)
                setHeader(false)
                setSpaces(false)
                val proto = autoDetectProtocol()
                isInitialized = true
                detectedProtocol = proto
                timeout(200)
                Result.success(proto)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun reset() {
        sendCommand("ATZ")
        consumeUntilPrompt(2000)
    }

    suspend fun setEcho(enabled: Boolean) {
        sendCommand(if (enabled) "ATE1" else "ATE0")
        consumeUntilPrompt(500)
    }

    suspend fun setLinefeed(enabled: Boolean) {
        sendCommand(if (enabled) "ATL1" else "ATL0")
        consumeUntilPrompt(500)
    }

    suspend fun setHeader(enabled: Boolean) {
        sendCommand(if (enabled) "ATH1" else "ATH0")
        consumeUntilPrompt(500)
    }

    suspend fun setSpaces(enabled: Boolean) {
        sendCommand(if (enabled) "ATS1" else "ATS0")
        consumeUntilPrompt(500)
    }

    suspend fun timeout(ms: Int) {
        sendCommand("ATST${String.format("%02X", ms / 4)}")
        consumeUntilPrompt(500)
    }

    suspend fun setProtocol(protocol: ObdProtocol) {
        val code = when (protocol) {
            ObdProtocol.AUTO -> "0"
            ObdProtocol.SAE_J1850_PWM -> "1"
            ObdProtocol.SAE_J1850_VPW -> "2"
            ObdProtocol.ISO_9141_2 -> "3"
            ObdProtocol.ISO_14230_4_KWP -> "4"
            ObdProtocol.ISO_14230_4_KWP_FAST -> "5"
            ObdProtocol.ISO_15765_4_CAN_11 -> "6"
            ObdProtocol.ISO_15765_4_CAN_29 -> "7"
            ObdProtocol.ISO_15765_4_CAN_11_250K -> "8"
            ObdProtocol.ISO_15765_4_CAN_29_250K -> "9"
        }
        sendCommand("ATSP$code")
        consumeUntilPrompt(2000)
    }

    private suspend fun autoDetectProtocol(): ObdProtocol {
        sendCommand("ATSP0")
        consumeUntilPrompt(3000)
        tryRequest("0100")?.let { response ->
            val proto = when {
                response.contains("41 00") -> detectFromResponse(response)
                else -> ObdProtocol.ISO_15765_4_CAN_11
            }
            return proto
        }
        return ObdProtocol.ISO_15765_4_CAN_11
    }

    private fun detectFromResponse(response: String): ObdProtocol {
        return when {
            response.startsWith("7F") -> ObdProtocol.SAE_J1850_PWM
            response.contains("48 6B") -> ObdProtocol.SAE_J1850_VPW
            response.contains("7E8") -> ObdProtocol.ISO_15765_4_CAN_11
            else -> ObdProtocol.ISO_15765_4_CAN_11
        }
    }

    suspend fun sendCommand(command: String) {
        outputStream.write((command + "\r\n").toByteArray(Charsets.US_ASCII))
        outputStream.flush()
    }

    private suspend fun readLine(timeoutMs: Long = 1000): String? {
        val startTime = System.currentTimeMillis()
        while (coroutineContext.isActive) {
            if (reader.ready()) {
                return reader.readLine()?.trim()
            }
            if (System.currentTimeMillis() - startTime > timeoutMs) return null
            kotlinx.coroutines.delay(10)
        }
        return null
    }

    private suspend fun consumeUntilPrompt(timeoutMs: Long) {
        val startTime = System.currentTimeMillis()
        while (coroutineContext.isActive) {
            if (System.currentTimeMillis() - startTime > timeoutMs) return
            val line = readLine(100)
            if (line == null) {
                kotlinx.coroutines.delay(50)
                continue
            }
            if (line == ">" || line == "OK") return
        }
    }

    suspend fun readDtc(): Result<List<Dtc>> = withContext(Dispatchers.IO) {
        ioMutex.withLock {
            try {
                sendCommand("03")
                val response = readRawResponse(2000)
                val codes = parseDtc(response)
                Result.success(codes)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun clearDtc(): Result<Boolean> = withContext(Dispatchers.IO) {
        ioMutex.withLock {
            try {
                sendCommand("04")
                consumeUntilPrompt(2000)
                Result.success(true)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun readPid(mode: Int, pid: Int): Result<List<Int>> = withContext(Dispatchers.IO) {
        ioMutex.withLock {
            try {
                val cmd = String.format("%02X%02X", mode, pid)
                val response = tryRequest(cmd) ?: return@withLock Result.failure(Exception("No response"))
                val data = parsePidResponse(response, mode, pid)
                Result.success(data)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private suspend fun tryRequest(cmd: String, timeoutMs: Long = 2000): String? {
        sendCommand(cmd)
        val response = readRawResponse(timeoutMs)
        if (response.isBlank() || response.contains("NO DATA") || response.contains("?")) return null
        return response
    }

    private suspend fun readRawResponse(timeoutMs: Long): String {
        val buffer = StringBuilder()
        val startTime = System.currentTimeMillis()
        while (coroutineContext.isActive) {
            if (System.currentTimeMillis() - startTime > timeoutMs) break
            val line = readLine(100)
            if (line == null) {
                kotlinx.coroutines.delay(10)
                continue
            }
            if (line == ">" || line.startsWith(">")) break
            if (line.isNotBlank() && line != "OK") {
                buffer.append(line).append(" ")
            }
        }
        return buffer.toString().trim()
    }

    private fun parsePidResponse(data: String, mode: Int, pid: Int): List<Int> {
        val clean = data.replace("\\s+".toRegex(), " ")
        val bytes = clean.split(" ")
            .filter { it.matches(Regex("^[0-9A-Fa-f]{2}$")) }
            .map { it.toInt(16) }

        val modeIdx = bytes.indexOfFirst { it == mode + 0x40 }
        if (modeIdx < 0) return emptyList()
        val pidIdx = modeIdx + 1
        if (pidIdx >= bytes.size || bytes[pidIdx] != pid) {
            val altPidIdx = bytes.indexOf(pid)
            if (altPidIdx < 0 || altPidIdx + 1 >= bytes.size) return emptyList()
            return bytes.drop(altPidIdx + 1)
        }
        return bytes.drop(pidIdx + 1)
    }

    fun parseDtc(response: String): List<Dtc> {
        val codes = mutableListOf<Dtc>()
        val hexValues = response.split(" ")
            .filter { it.matches(Regex("^[0-9A-Fa-f]{2,4}$")) }
            .mapNotNull {
                try {
                    if (it.length == 4) listOf(it.substring(0, 2).toInt(16), it.substring(2, 4).toInt(16))
                    else listOf(it.toInt(16))
                } catch (e: Exception) { null }
            }.flatten()

        var i = 0
        while (i + 1 < hexValues.size) {
            val first = hexValues[i]
            val second = hexValues[i + 1]
            val code = when ((first and 0xC0) shr 6) {
                0 -> "P"
                1 -> "C"
                2 -> "B"
                3 -> "U"
                else -> "?"
            } + String.format("%02X%02X", first and 0x3F, second)

            if (code.length == 5 && code[0] in "PCBU") {
                codes.add(Dtc(code = code, description = DtcDatabase.getDescription(code)))
            }
            i += 2
        }
        return codes
    }

    fun close() {
        try { inputStream.close() } catch (_: Exception) {}
        try { outputStream.close() } catch (_: Exception) {}
        try { reader.close() } catch (_: Exception) {}
    }
}
