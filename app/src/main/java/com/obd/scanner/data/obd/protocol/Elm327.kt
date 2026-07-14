package com.obd.scanner.data.obd.protocol

import com.obd.scanner.domain.model.Dtc
import com.obd.scanner.domain.model.DtcStatus
import com.obd.scanner.domain.model.ObdProtocol
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import kotlin.coroutines.coroutineContext

class Elm327(
    private val inputStream: InputStream,
    private val outputStream: OutputStream
) {
    private val TAG = "Elm327"
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
                setLinefeed(false)
                setHeader(false)
                // Spaces ON: the PID/DTC parsers split responses on whitespace
                setSpaces(true)
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
        // Drain any stale bytes from a previous (timed-out) exchange
        while (inputStream.available() > 0) inputStream.read()
        android.util.Log.d(TAG, ">> $command")
        outputStream.write((command + "\r").toByteArray(Charsets.US_ASCII))
        outputStream.flush()
    }

    /**
     * Reads raw chars until the ELM327 prompt '>' or timeout. The prompt is NOT
     * followed by a newline, so line-based reads would block forever.
     */
    private suspend fun readUntilPrompt(timeoutMs: Long): String {
        val sb = StringBuilder()
        val startTime = System.currentTimeMillis()
        while (coroutineContext.isActive && System.currentTimeMillis() - startTime < timeoutMs) {
            if (inputStream.available() > 0) {
                val c = inputStream.read()
                if (c == -1) break
                val ch = c.toChar()
                if (ch == '>') break
                sb.append(ch)
            } else {
                delay(20)
            }
        }
        return sb.toString()
    }

    private suspend fun consumeUntilPrompt(timeoutMs: Long) {
        readUntilPrompt(timeoutMs)
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

    suspend fun readPendingDtc(): Result<List<Dtc>> = withContext(Dispatchers.IO) {
        ioMutex.withLock {
            try {
                sendCommand("07")
                val response = readRawResponse(2000)
                val codes = parseDtc(response).map { it.copy(status = DtcStatus.PENDING) }
                Result.success(codes)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /** Mode 02 freeze-frame: request "02 <PID> 00" (frame 0). */
    suspend fun readFreezeFramePid(pid: Int): Result<List<Int>> = withContext(Dispatchers.IO) {
        ioMutex.withLock {
            try {
                val cmd = String.format("02%02X00", pid)
                val response = tryRequest(cmd) ?: return@withLock Result.failure(Exception("No response"))
                Result.success(parseFreezeFrameResponse(response))
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

    /** Mode 09 PID 02: número de serie del vehículo (VIN), 17 caracteres. */
    suspend fun readVin(): Result<String?> = withContext(Dispatchers.IO) {
        ioMutex.withLock {
            try {
                sendCommand("0902")
                val resp = readRawResponse(4000)
                Result.success(parseVin(resp))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private fun parseVin(resp: String): String? {
        val b = resp.split(" ")
            .filter { it.matches(Regex("^[0-9A-Fa-f]{2}$")) }
            .map { it.toInt(16) }
        for (i in 0 until b.size - 1) {
            if (b[i] == 0x49 && b[i + 1] == 0x02) {
                var data = b.drop(i + 2)
                if (data.isNotEmpty() && data[0] <= 0x10) data = data.drop(1)  // NODI
                val text = data.filter { it in 32..126 }.map { it.toChar() }.joinToString("")
                val clean = text.filter { it.isLetterOrDigit() }
                return when {
                    clean.length >= 17 -> clean.takeLast(17)
                    clean.isNotBlank() -> clean
                    else -> null
                }
            }
        }
        return null
    }

    suspend fun readPid(mode: Int, pid: Int): Result<List<Int>> = withContext(Dispatchers.IO) {
        ioMutex.withLock {
            try {
                val cmd = String.format("%02X%02X", mode, pid)
                val response = tryRequest(cmd) ?: return@withLock Result.failure(Exception("No response"))
                val data = parsePidResponse(response, mode, pid)
                if (data.isEmpty()) {
                    android.util.Log.w(TAG, "unparsed response for $cmd: '$response'")
                }
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
        val resp = readUntilPrompt(timeoutMs)
            .replace('\r', ' ')
            .replace('\n', ' ')
            .replace("\\s+".toRegex(), " ")
            .trim()
        android.util.Log.d(TAG, "<< $resp")
        return resp
    }

    /** Mode 02 response is "42 <PID> <frame> <data...>" — drop the 3 header bytes. */
    private fun parseFreezeFrameResponse(data: String): List<Int> {
        val bytes = data.split(" ")
            .filter { it.matches(Regex("^[0-9A-Fa-f]{2}$")) }
            .map { it.toInt(16) }
        val modeIdx = bytes.indexOfFirst { it == 0x42 }
        if (modeIdx < 0) return emptyList()
        val dataStart = modeIdx + 3
        if (dataStart >= bytes.size) return emptyList()
        return bytes.drop(dataStart)
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
    }
}
