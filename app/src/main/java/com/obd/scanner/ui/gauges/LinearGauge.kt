package com.obd.scanner.ui.gauges

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.obd.scanner.ui.theme.GaugeBlue
import com.obd.scanner.ui.theme.GaugeGreen
import com.obd.scanner.ui.theme.GaugeOrange
import com.obd.scanner.ui.theme.GaugeRed
import com.obd.scanner.ui.theme.GaugeYellow

@Composable
fun LinearGauge(
    value: Float,
    minValue: Float,
    maxValue: Float,
    label: String,
    unit: String,
    formattedValue: String,
    modifier: Modifier = Modifier,
    color: Color = GaugeBlue
) {
    val fraction = ((value - minValue) / (maxValue - minValue)).coerceIn(0f, 1f)
    val animatedFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(300),
        label = "gauge"
    )

    val gaugeColor = when {
        fraction < 0.3f -> GaugeGreen
        fraction < 0.6f -> GaugeBlue
        fraction < 0.8f -> GaugeYellow
        else -> GaugeOrange
    }

    Column(
        modifier = modifier.padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = formattedValue,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = gaugeColor,
            textAlign = TextAlign.Center
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .padding(horizontal = 4.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxWidth().height(12.dp)) {
                drawRoundRect(
                    color = Color.Gray.copy(alpha = 0.3f),
                    cornerRadius = CornerRadius(6f, 6f),
                    size = Size(size.width, 12f)
                )
                drawRoundRect(
                    color = gaugeColor,
                    cornerRadius = CornerRadius(6f, 6f),
                    size = Size(size.width * animatedFraction, 12f)
                )
            }
        }

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun CircularGauge(
    value: Float,
    minValue: Float,
    maxValue: Float,
    label: String,
    unit: String,
    formattedValue: String,
    modifier: Modifier = Modifier
) {
    val fraction = ((value - minValue) / (maxValue - minValue)).coerceIn(0f, 1f)
    val animatedFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(500),
        label = "circle_gauge"
    )

    val gaugeColor = when {
        fraction < 0.3f -> GaugeGreen
        fraction < 0.6f -> GaugeBlue
        fraction < 0.8f -> GaugeYellow
        else -> GaugeOrange
    }

    Column(
        modifier = modifier.padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.height(100.dp).fillMaxWidth()) {
                val strokeWidth = 10f
                val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)

                drawArc(
                    color = Color.Gray.copy(alpha = 0.2f),
                    startAngle = 135f,
                    sweepAngle = 270f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
                drawArc(
                    color = gaugeColor,
                    startAngle = 135f,
                    sweepAngle = 270f * animatedFraction,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = formattedValue,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = gaugeColor
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
