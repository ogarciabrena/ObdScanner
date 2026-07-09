package com.obd.scanner.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.obd.scanner.domain.model.Category
import com.obd.scanner.domain.model.SensorData
import com.obd.scanner.ui.theme.GaugeBlue
import com.obd.scanner.ui.theme.GaugeCyan
import com.obd.scanner.ui.theme.GaugeGreen
import com.obd.scanner.ui.theme.GaugeOrange
import com.obd.scanner.ui.theme.GaugeRed
import com.obd.scanner.ui.theme.GaugeYellow

@Composable
fun SensorCard(
    sensor: SensorData,
    modifier: Modifier = Modifier
) {
    val accentColor = when (sensor.category) {
        Category.ENGINE -> GaugeOrange
        Category.TEMPERATURE -> GaugeRed
        Category.SPEED -> GaugeGreen
        Category.FUEL -> GaugeYellow
        Category.AIR_INTAKE -> GaugeBlue
        Category.ELECTRICAL -> GaugeCyan
        else -> GaugeBlue
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = sensor.formattedValue,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = sensor.name,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
