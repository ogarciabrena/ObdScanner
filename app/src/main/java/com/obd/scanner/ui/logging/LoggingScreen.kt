package com.obd.scanner.ui.logging

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.obd.scanner.domain.model.SensorData

@Composable
fun LoggingScreen(
    isConnected: Boolean,
    recentData: List<SensorData>
) {
    var isLogging by remember { mutableStateOf(false) }
    var logCount by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        Text(
            text = "Data Logger",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isLogging) "Recording..." else "Ready",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isLogging) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "$logCount data points logged",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        isLogging = !isLogging
                        if (!isLogging && logCount > 0) {
                            /* TODO: save to CSV */
                        }
                    },
                    enabled = isConnected
                ) {
                    Icon(
                        if (isLogging) Icons.Default.Info else Icons.Default.FileDownload,
                        contentDescription = null
                    )
                    Text(
                        if (isLogging) "Stop Logging" else "Start Logging",
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Live Data",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (recentData.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "No data available",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(recentData.takeLast(50).reversed()) { data ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = data.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = data.formattedValue,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}
