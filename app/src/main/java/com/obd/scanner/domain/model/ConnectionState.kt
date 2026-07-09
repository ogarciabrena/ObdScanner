package com.obd.scanner.domain.model

sealed interface ConnectionState {
    data object Disconnected : ConnectionState
    data object Connecting : ConnectionState
    data object Initializing : ConnectionState
    data class Connected(val deviceName: String, val protocol: ObdProtocol) : ConnectionState
    data class Error(val message: String) : ConnectionState
}

enum class ObdProtocol(val description: String) {
    AUTO("Automatic"),
    SAE_J1850_PWM("SAE J1850 PWM (41.6 kbaud)"),
    SAE_J1850_VPW("SAE J1850 VPW (10.4 kbaud)"),
    ISO_9141_2("ISO 9141-2 (5 baud init)"),
    ISO_14230_4_KWP("ISO 14230-4 KWP (5 baud init)"),
    ISO_14230_4_KWP_FAST("ISO 14230-4 KWP (fast init)"),
    ISO_15765_4_CAN_11("ISO 15765-4 CAN (11 bit ID, 500 kbaud)"),
    ISO_15765_4_CAN_29("ISO 15765-4 CAN (29 bit ID, 500 kbaud)"),
    ISO_15765_4_CAN_11_250K("ISO 15765-4 CAN (11 bit ID, 250 kbaud)"),
    ISO_15765_4_CAN_29_250K("ISO 15765-4 CAN (29 bit ID, 250 kbaud)")
}
