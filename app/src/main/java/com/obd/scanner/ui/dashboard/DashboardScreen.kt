package com.obd.scanner.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.obd.scanner.domain.model.ConnectionState
import com.obd.scanner.ui.components.SensorCard

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onDisconnect: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        when (val conn = state.connectionState) {
            is ConnectionState.Connected -> {
                Text(
                    text = "Connected to ${conn.deviceName}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (state.sensors.isEmpty() && !state.isScanning) {
                    Button(onClick = { viewModel.startScanning() }) {
                        Text("Start Scanning")
                    }
                }

                if (state.sensors.isNotEmpty()) {
                    val rpm = state.sensors.values.find { it.pid == 0x0C }
                    val speed = state.sensors.values.find { it.pid == 0x0D }

                    if (rpm != null || speed != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            if (rpm != null) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = rpm.formattedValue,
                                        style = MaterialTheme.typography.displaySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(text = "RPM", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                            if (speed != null) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = speed.formattedValue,
                                        style = MaterialTheme.typography.displaySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(text = "Speed", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.sensors.values.filter { it.pid != 0x0C && it.pid != 0x0D }.toList()) { sensor ->
                            SensorCard(sensor = sensor)
                        }
                    }
                }

                if (state.isScanning) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                        Text("Scanning...", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            is ConnectionState.Connecting -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Connecting...", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }

            is ConnectionState.Initializing -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Initializing ELM327...", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }

            is ConnectionState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.DirectionsCar, contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.height(48.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = conn.message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onDisconnect,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Disconnect")
                        }
                    }
                }
            }

            is ConnectionState.Disconnected -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Not Connected",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Connect to an OBD adapter from the Connection tab",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
