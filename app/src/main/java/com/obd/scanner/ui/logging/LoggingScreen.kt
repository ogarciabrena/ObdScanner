package com.obd.scanner.ui.logging

import android.content.Intent
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
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.obd.scanner.ObdScannerApp
import com.obd.scanner.data.obd.ObdService
import com.obd.scanner.domain.model.SensorData

@Composable
fun LoggingScreen(
    isConnected: Boolean,
    recentData: List<SensorData>
) {
    val context = LocalContext.current
    val app = context.applicationContext as ObdScannerApp

    // Real recording state from the app-level TripRecorder: survives tab
    // switches and reflects the foreground service doing the actual work.
    val isRecording by app.tripRecorder.isRecording.collectAsState()
    val sampleCount by app.tripRecorder.sampleCountFlow.collectAsState()
    var pendingCount by remember { mutableStateOf(0) }

    LaunchedEffect(isRecording) {
        pendingCount = app.tripRecorder.listPendingTrips().size
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        Text(
            text = "Grabación de viajes",
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
                    text = if (isRecording) "● Grabando viaje..." else "Listo para grabar",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isRecording) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isRecording)
                        "$sampleCount muestras de este viaje"
                    else
                        "Viajes pendientes de subir: $pendingCount",
                    style = MaterialTheme.typography.bodyMedium
                )
                if (isRecording) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "La grabación sigue en segundo plano aunque cambies de pestaña o cierres la app",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        val intent = Intent(context, ObdService::class.java).setAction(
                            if (isRecording) ObdService.ACTION_STOP_TRIP
                            else ObdService.ACTION_START_TRIP
                        )
                        context.startForegroundService(intent)
                    },
                    enabled = isConnected || isRecording,
                    colors = if (isRecording)
                        ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    else ButtonDefaults.buttonColors()
                ) {
                    Icon(
                        if (isRecording) Icons.Default.Stop else Icons.Default.FiberManualRecord,
                        contentDescription = null
                    )
                    Text(
                        if (isRecording) "Finalizar viaje" else "Iniciar viaje",
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }

                if (!isConnected && !isRecording) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Conéctate a un adaptador OBD primero",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Datos en vivo",
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
                    text = "Sin datos — conéctate al OBD",
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
