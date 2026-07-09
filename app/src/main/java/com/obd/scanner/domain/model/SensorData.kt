package com.obd.scanner.domain.model

data class SensorData(
    val pid: Int,
    val name: String,
    val value: Float,
    val unit: String,
    val formattedValue: String,
    val timestamp: Long = System.currentTimeMillis(),
    val minValue: Float? = null,
    val maxValue: Float? = null,
    val category: Category = Category.GENERIC
)
