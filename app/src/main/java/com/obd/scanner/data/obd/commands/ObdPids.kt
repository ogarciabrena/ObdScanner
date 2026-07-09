package com.obd.scanner.data.obd.commands

import com.obd.scanner.domain.model.Category
import com.obd.scanner.domain.model.Formula
import com.obd.scanner.domain.model.ObdCommand

object ObdPids {

    val SUPPORTED_PIDS_01_20 = ObdCommand(0, "Supported PIDs 01-20", 1, 0x00, 4, Formula.NONE, "", category = Category.DIAGNOSTIC)
    val SUPPORTED_PIDS_21_40 = ObdCommand(1, "Supported PIDs 21-40", 1, 0x20, 4, Formula.NONE, "", category = Category.DIAGNOSTIC)
    val SUPPORTED_PIDS_41_60 = ObdCommand(2, "Supported PIDs 41-60", 1, 0x40, 4, Formula.NONE, "", category = Category.DIAGNOSTIC)
    val SUPPORTED_PIDS_61_80 = ObdCommand(3, "Supported PIDs 61-80", 1, 0x60, 4, Formula.NONE, "", category = Category.DIAGNOSTIC)
    val SUPPORTED_PIDS_81_A0 = ObdCommand(4, "Supported PIDs 81-A0", 1, 0x80, 4, Formula.NONE, "", category = Category.DIAGNOSTIC)

    val ENGINE_LOAD = ObdCommand(5, "Engine Load", 1, 0x04, 1, Formula.PERCENT, "%", category = Category.ENGINE)
    val SHORT_FUEL_TRIM_B1 = ObdCommand(67, "Short Fuel Trim Bank 1", 1, 0x06, 1, Formula.FUEL_TRIM, "%", minValue = -100f, maxValue = 99f, category = Category.FUEL)
    val LONG_FUEL_TRIM_B1 = ObdCommand(68, "Long Fuel Trim Bank 1", 1, 0x07, 1, Formula.FUEL_TRIM, "%", minValue = -100f, maxValue = 99f, category = Category.FUEL)
    val SHORT_FUEL_TRIM_B2 = ObdCommand(69, "Short Fuel Trim Bank 2", 1, 0x08, 1, Formula.FUEL_TRIM, "%", minValue = -100f, maxValue = 99f, category = Category.FUEL)
    val LONG_FUEL_TRIM_B2 = ObdCommand(70, "Long Fuel Trim Bank 2", 1, 0x09, 1, Formula.FUEL_TRIM, "%", minValue = -100f, maxValue = 99f, category = Category.FUEL)
    val COOLANT_TEMP = ObdCommand(6, "Coolant Temperature", 1, 0x05, 1, Formula.TEMP_C, "°C", minValue = -40f, maxValue = 215f, category = Category.TEMPERATURE)
    val FUEL_PRESSURE = ObdCommand(7, "Fuel Pressure", 1, 0x0A, 1, Formula.FUEL_PRESSURE, "kPa", category = Category.FUEL)
    val INTAKE_MAP = ObdCommand(8, "Intake Manifold Pressure", 1, 0x0B, 1, Formula.NONE, "kPa", category = Category.AIR_INTAKE)
    val ENGINE_RPM = ObdCommand(9, "Engine RPM", 1, 0x0C, 2, Formula.RPM, "rpm", minValue = 0f, maxValue = 8000f, category = Category.ENGINE)
    val VEHICLE_SPEED = ObdCommand(10, "Vehicle Speed", 1, 0x0D, 1, Formula.SPEED, "km/h", minValue = 0f, maxValue = 300f, category = Category.SPEED)
    val TIMING_ADVANCE = ObdCommand(11, "Timing Advance", 1, 0x0E, 1, Formula.TIMING, "°", category = Category.ENGINE)
    val INTAKE_AIR_TEMP = ObdCommand(12, "Intake Air Temperature", 1, 0x0F, 1, Formula.TEMP_C, "°C", minValue = -40f, maxValue = 215f, category = Category.TEMPERATURE)
    val MAF = ObdCommand(13, "Mass Air Flow", 1, 0x10, 2, Formula.MAF, "g/s", category = Category.AIR_INTAKE)
    val THROTTLE_POS = ObdCommand(14, "Throttle Position", 1, 0x11, 1, Formula.THROTTLE, "%", minValue = 0f, maxValue = 100f, category = Category.AIR_INTAKE)
    val SECONDARY_AIR_STATUS = ObdCommand(15, "Secondary Air Status", 1, 0x12, 1, Formula.NONE, "", category = Category.EMISSIONS)
    val O2_SENSORS_PRESENT = ObdCommand(16, "O2 Sensors Present", 1, 0x13, 1, Formula.NONE, "", category = Category.EMISSIONS)
    val O2_SENSOR_1 = ObdCommand(17, "O2 Sensor 1", 1, 0x14, 2, Formula.O2_VOLTAGE, "V", category = Category.EMISSIONS)
    val O2_SENSOR_2 = ObdCommand(18, "O2 Sensor 2", 1, 0x15, 2, Formula.O2_VOLTAGE, "V", category = Category.EMISSIONS)
    val O2_SENSOR_3 = ObdCommand(19, "O2 Sensor 3", 1, 0x16, 2, Formula.O2_VOLTAGE, "V", category = Category.EMISSIONS)
    val O2_SENSOR_4 = ObdCommand(20, "O2 Sensor 4", 1, 0x17, 2, Formula.O2_VOLTAGE, "V", category = Category.EMISSIONS)
    val O2_SENSOR_5 = ObdCommand(21, "O2 Sensor 5", 1, 0x18, 2, Formula.O2_VOLTAGE, "V", category = Category.EMISSIONS)
    val O2_SENSOR_6 = ObdCommand(22, "O2 Sensor 6", 1, 0x19, 2, Formula.O2_VOLTAGE, "V", category = Category.EMISSIONS)
    val O2_SENSOR_7 = ObdCommand(23, "O2 Sensor 7", 1, 0x1A, 2, Formula.O2_VOLTAGE, "V", category = Category.EMISSIONS)
    val O2_SENSOR_8 = ObdCommand(24, "O2 Sensor 8", 1, 0x1B, 2, Formula.O2_VOLTAGE, "V", category = Category.EMISSIONS)
    val OBD_STANDARD = ObdCommand(25, "OBD Standard", 1, 0x1C, 1, Formula.NONE, "", category = Category.DIAGNOSTIC)
    val O2_SENSORS_11_BIT = ObdCommand(26, "O2 Sensors (11-bit)", 1, 0x1D, 1, Formula.NONE, "", category = Category.EMISSIONS)
    val AUX_INPUT = ObdCommand(27, "Auxiliary Input", 1, 0x1E, 1, Formula.NONE, "", category = Category.ELECTRICAL)
    val RUN_TIME = ObdCommand(28, "Run Time Since Start", 1, 0x1F, 2, Formula.RUN_TIME, "s", category = Category.GENERIC)

    val FUEL_SYSTEM_STATUS = ObdCommand(29, "Fuel System Status", 1, 0x03, 2, Formula.NONE, "", category = Category.FUEL)
    val MIL_DISTANCE = ObdCommand(30, "Distance with MIL", 1, 0x21, 2, Formula.DISTANCE, "km", category = Category.DIAGNOSTIC)
    val FUEL_RAIL_PRESSURE_VAC = ObdCommand(31, "Fuel Rail Pressure (vac)", 1, 0x22, 2, Formula.FUEL_RAIL_PRESSURE, "kPa", category = Category.FUEL)
    val FUEL_RAIL_PRESSURE_DIRECT = ObdCommand(32, "Fuel Rail Pressure (direct)", 1, 0x23, 2, Formula.FUEL_RAIL_PRESSURE, "kPa", category = Category.FUEL)
    val O2_SENSOR_1_WIDE = ObdCommand(33, "O2 Sensor 1 (Wide)", 1, 0x24, 4, Formula.EQUIV_RATIO, "ratio", category = Category.EMISSIONS)
    val O2_SENSOR_2_WIDE = ObdCommand(34, "O2 Sensor 2 (Wide)", 1, 0x25, 4, Formula.EQUIV_RATIO, "ratio", category = Category.EMISSIONS)
    val O2_SENSOR_3_WIDE = ObdCommand(35, "O2 Sensor 3 (Wide)", 1, 0x26, 4, Formula.EQUIV_RATIO, "ratio", category = Category.EMISSIONS)
    val O2_SENSOR_4_WIDE = ObdCommand(36, "O2 Sensor 4 (Wide)", 1, 0x27, 4, Formula.EQUIV_RATIO, "ratio", category = Category.EMISSIONS)
    val O2_SENSOR_5_WIDE = ObdCommand(37, "O2 Sensor 5 (Wide)", 1, 0x28, 4, Formula.EQUIV_RATIO, "ratio", category = Category.EMISSIONS)
    val O2_SENSOR_6_WIDE = ObdCommand(38, "O2 Sensor 6 (Wide)", 1, 0x29, 4, Formula.EQUIV_RATIO, "ratio", category = Category.EMISSIONS)
    val O2_SENSOR_7_WIDE = ObdCommand(39, "O2 Sensor 7 (Wide)", 1, 0x2A, 4, Formula.EQUIV_RATIO, "ratio", category = Category.EMISSIONS)
    val O2_SENSOR_8_WIDE = ObdCommand(40, "O2 Sensor 8 (Wide)", 1, 0x2B, 4, Formula.EQUIV_RATIO, "ratio", category = Category.EMISSIONS)
    val COMMANDED_EGR = ObdCommand(41, "Commanded EGR", 1, 0x2C, 1, Formula.COMMANDED_EGR, "%", category = Category.EMISSIONS)
    val EGR_ERROR = ObdCommand(42, "EGR Error", 1, 0x2D, 1, Formula.EGR_ERROR, "%", category = Category.EMISSIONS)
    val EVAP_PURGE = ObdCommand(43, "Evaporative Purge", 1, 0x2E, 1, Formula.EVAP_PERCENT, "%", category = Category.EMISSIONS)
    val FUEL_LEVEL = ObdCommand(44, "Fuel Level", 1, 0x2F, 1, Formula.FUEL_LEVEL, "%", minValue = 0f, maxValue = 100f, category = Category.FUEL)
    val WARMUPS_SINCE_CLEAR = ObdCommand(45, "Warmups Since Clear", 1, 0x30, 1, Formula.NONE, "", category = Category.DIAGNOSTIC)
    val DISTANCE_SINCE_CLEAR = ObdCommand(46, "Distance Since Clear", 1, 0x31, 2, Formula.DISTANCE, "km", category = Category.DIAGNOSTIC)
    val EVAP_VAPOR_PRESSURE = ObdCommand(47, "Evap Vapor Pressure", 1, 0x32, 2, Formula.NONE, "Pa", category = Category.EMISSIONS)
    val BAROMETRIC_PRESSURE = ObdCommand(48, "Barometric Pressure", 1, 0x33, 1, Formula.NONE, "kPa", category = Category.AIR_INTAKE)
    val O2_SENSOR_1_CURRENT = ObdCommand(49, "O2 Sensor 1 Current", 1, 0x34, 2, Formula.NONE, "mA", category = Category.EMISSIONS)
    val O2_SENSOR_2_CURRENT = ObdCommand(50, "O2 Sensor 2 Current", 1, 0x35, 2, Formula.NONE, "mA", category = Category.EMISSIONS)
    val CATALYST_TEMP_B1S1 = ObdCommand(51, "Catalyst Temp Bank 1 S1", 1, 0x3C, 2, Formula.CATALYST_TEMP, "°C", category = Category.TEMPERATURE)
    val CATALYST_TEMP_B1S2 = ObdCommand(52, "Catalyst Temp Bank 1 S2", 1, 0x3D, 2, Formula.CATALYST_TEMP, "°C", category = Category.TEMPERATURE)
    val CATALYST_TEMP_B2S1 = ObdCommand(53, "Catalyst Temp Bank 2 S1", 1, 0x3E, 2, Formula.CATALYST_TEMP, "°C", category = Category.TEMPERATURE)
    val CATALYST_TEMP_B2S2 = ObdCommand(54, "Catalyst Temp Bank 2 S2", 1, 0x3F, 2, Formula.CATALYST_TEMP, "°C", category = Category.TEMPERATURE)

    val CONTROL_MODULE_VOLTAGE = ObdCommand(55, "Control Module Voltage", 1, 0x42, 2, Formula.VOLTAGE, "V", minValue = 0f, maxValue = 16f, category = Category.ELECTRICAL)
    val ABS_ENGINE_LOAD = ObdCommand(56, "Absolute Engine Load", 1, 0x43, 2, Formula.ABS_LOAD, "%", category = Category.ENGINE)
    val COMMANDED_AIR_FUEL = ObdCommand(57, "Commanded Air-Fuel Ratio", 1, 0x44, 4, Formula.EQUIV_RATIO, "ratio", category = Category.FUEL)
    val RELATIVE_THROTTLE = ObdCommand(58, "Relative Throttle Position", 1, 0x45, 1, Formula.THROTTLE, "%", category = Category.AIR_INTAKE)
    val AMBIENT_TEMP = ObdCommand(59, "Ambient Temperature", 1, 0x46, 1, Formula.AMBIENT_TEMP, "°C", category = Category.TEMPERATURE)
    val THROTTLE_ACTUATOR = ObdCommand(60, "Throttle Actuator", 1, 0x4C, 1, Formula.THROTTLE, "%", category = Category.AIR_INTAKE)
    val RUN_TIME_MIL = ObdCommand(61, "Run Time with MIL", 1, 0x4D, 2, Formula.TIME, "min", category = Category.DIAGNOSTIC)
    val TIME_SINCE_CLEAR = ObdCommand(62, "Time Since Clear", 1, 0x4E, 2, Formula.TIME, "min", category = Category.DIAGNOSTIC)
    val FUEL_RATE = ObdCommand(63, "Fuel Rate", 1, 0x5E, 2, Formula.FUEL_RATE, "L/h", category = Category.FUEL)
    val ENGINE_OIL_TEMP = ObdCommand(64, "Engine Oil Temperature", 1, 0x5C, 1, Formula.ENGINE_OIL_TEMP, "°C", category = Category.TEMPERATURE)
    val ETHANOL_FUEL = ObdCommand(65, "Ethanol Fuel %", 1, 0x52, 1, Formula.ETHANOL, "%", category = Category.FUEL)
    val HYBRID_BATTERY_REMAINING = ObdCommand(66, "Hybrid Battery Remaining", 1, 0x5B, 1, Formula.HYBRID_BATTERY, "%", category = Category.ELECTRICAL)

    val ALL_PIDS: List<ObdCommand> = listOf(
        ENGINE_LOAD, COOLANT_TEMP, FUEL_PRESSURE, INTAKE_MAP,
        SHORT_FUEL_TRIM_B1, LONG_FUEL_TRIM_B1, SHORT_FUEL_TRIM_B2, LONG_FUEL_TRIM_B2,
        ENGINE_RPM, VEHICLE_SPEED, TIMING_ADVANCE, INTAKE_AIR_TEMP,
        MAF, THROTTLE_POS, FUEL_SYSTEM_STATUS,
        O2_SENSOR_1, O2_SENSOR_2, O2_SENSOR_3, O2_SENSOR_4,
        O2_SENSOR_5, O2_SENSOR_6, O2_SENSOR_7, O2_SENSOR_8,
        RUN_TIME, MIL_DISTANCE, FUEL_LEVEL,
        FUEL_RAIL_PRESSURE_VAC, FUEL_RAIL_PRESSURE_DIRECT,
        COMMANDED_EGR, EGR_ERROR, EVAP_PURGE,
        BAROMETRIC_PRESSURE, CATALYST_TEMP_B1S1, CATALYST_TEMP_B1S2,
        CATALYST_TEMP_B2S1, CATALYST_TEMP_B2S2,
        CONTROL_MODULE_VOLTAGE, ABS_ENGINE_LOAD, COMMANDED_AIR_FUEL,
        RELATIVE_THROTTLE, AMBIENT_TEMP, THROTTLE_ACTUATOR,
        RUN_TIME_MIL, TIME_SINCE_CLEAR,
        ENGINE_OIL_TEMP, ETHANOL_FUEL, HYBRID_BATTERY_REMAINING, FUEL_RATE
    )
}
