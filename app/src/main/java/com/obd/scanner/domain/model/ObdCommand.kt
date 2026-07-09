package com.obd.scanner.domain.model

data class ObdCommand(
    val id: Int,
    val name: String,
    val mode: Int,
    val pid: Int,
    val bytes: Int,
    val formula: Formula,
    val unit: String,
    val minValue: Float? = null,
    val maxValue: Float? = null,
    val category: Category = Category.GENERIC,
    val description: String = ""
)

enum class Category {
    GENERIC, ENGINE, FUEL, TEMPERATURE, AIR_INTAKE,
    SPEED, ELECTRICAL, EMISSIONS, DIAGNOSTIC
}

enum class Formula(val evaluate: (List<Int>) -> Float) {
    NONE({ data -> data[0].toFloat() }),
    PERCENT({ data -> data[0].toFloat() * 100f / 255f }),
    TEMP_C({ data -> data[0].toFloat() - 40f }),
    RPM({ data -> ((data[0] * 256) + data[1]).toFloat() / 4f }),
    SPEED({ data -> data[0].toFloat() }),
    MAF({ data -> ((data[0] * 256) + data[1]).toFloat() / 100f }),
    THROTTLE({ data -> data[0].toFloat() * 100f / 255f }),
    FUEL_PRESSURE({ data -> data[0].toFloat() * 3f }),
    TIMING({ data -> (data[0] / 2f) - 64f }),
    AIR_FLOW({ data -> data[0].toFloat() * 100f / 255f }),
    FUEL_TRIM({ data -> (data[0] / 1.28f) - 100f }),
    VOLTAGE({ data -> data[0].toFloat() * 8f / 255f }),
    EQUIV_RATIO({ data -> ((data[0] * 256) + data[1]).toFloat() / 32768f }),
    EVAP_PERCENT({ data -> data[0].toFloat() * 100f / 255f }),
    O2_VOLTAGE({ data -> data[0].toFloat() * 8f / 255f }),
    FUEL_LEVEL({ data -> data[0].toFloat() * 100f / 255f }),
    DISTANCE({ data -> ((data[0] * 256) + data[1]).toFloat() }),
    TIME({ data -> ((data[0] * 256) + data[1]).toFloat() }),
    ABS_LOAD({ data -> ((data[0] * 256) + data[1]).toFloat() / 2.55f }),
    COMMANDED_EGR({ data -> data[0].toFloat() * 100f / 255f }),
    EGR_ERROR({ data -> (data[0] / 1.28f) - 100f }),
    FUEL_RAIL_PRESSURE({ data -> ((data[0] * 256) + data[1]).toFloat() * 10f }),
    ENGINE_OIL_TEMP({ data -> data[0].toFloat() - 40f }),
    CATALYST_TEMP({ data -> ((data[0] * 256) + data[1]).toFloat() / 10f - 40f }),
    BOOST({ data -> ((data[0] * 256) + data[1]).toFloat() / 1000f }),
    AMBIENT_TEMP({ data -> data[0].toFloat() - 40f }),
    ETHANOL({ data -> data[0].toFloat() * 100f / 255f }),
    HYBRID_BATTERY({ data -> data[0].toFloat() * 100f / 255f }),
    RUN_TIME({ data -> ((data[0] * 256) + data[1]).toFloat() }),
    FUEL_RATE({ data -> ((data[0] * 256) + data[1]).toFloat() * 0.05f }),
    ENGINE_LOAD({ data -> data[0].toFloat() * 100f / 255f }),
}
