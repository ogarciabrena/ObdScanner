package com.obd.scanner.domain.model

data class Dtc(
    val code: String,
    val description: String,
    val status: DtcStatus = DtcStatus.STORED,
    val timestamp: Long = System.currentTimeMillis()
)

enum class DtcStatus {
    STORED, PENDING, PERMANENT
}

enum class DtcSystem(val prefix: Char, val description: String) {
    P('P', "Powertrain"),
    C('C', "Chassis"),
    B('B', "Body"),
    U('U', "Network")
}
