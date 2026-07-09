package com.obd.scanner.ui.dtc

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.obd.scanner.data.obd.BluetoothManager
import com.obd.scanner.domain.model.Dtc
import com.obd.scanner.domain.model.DtcSystem
import com.obd.scanner.domain.usecase.ObdUseCase
import kotlinx.coroutines.launch

@Composable
fun DtcScreen(
    obdUseCase: ObdUseCase,
    isConnected: Boolean
) {
    val scope = rememberCoroutineScope()
    var dtcList by remember { mutableStateOf<List<Dtc>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    var showResult by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        Text(
            text = "Diagnostic Trouble Codes",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    scope.launch {
                        isLoading = true
                        dtcList = obdUseCase.readDtc()
                        isLoading = false
                    }
                },
                enabled = isConnected && !isLoading
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Text("Read DTCs", modifier = Modifier.padding(start = 4.dp))
            }

            Button(
                onClick = { showClearDialog = true },
                enabled = isConnected && dtcList.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.Default.Delete, contentDescription = null)
                Text("Clear", modifier = Modifier.padding(start = 4.dp))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        showResult?.let { msg ->
            Text(
                text = msg,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (isLoading) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Reading DTCs...", style = MaterialTheme.typography.bodyLarge)
            }
        } else if (dtcList.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.Build,
                    contentDescription = null,
                    modifier = Modifier.height(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (isConnected) "No trouble codes found"
                    else "Connect to an OBD adapter first",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Text(
                text = "${dtcList.size} code(s) found",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(dtcList) { dtc ->
                    DtcCard(dtc = dtc)
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear DTCs") },
            text = { Text("Are you sure you want to clear all diagnostic trouble codes?") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        val success = obdUseCase.clearDtc()
                        showResult = if (success) "DTCs cleared successfully" else "Failed to clear DTCs"
                        if (success) dtcList = emptyList()
                    }
                    showClearDialog = false
                }) {
                    Text("Clear", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun DtcCard(dtc: Dtc) {
    val system = dtc.code.firstOrNull()?.let { c ->
        DtcSystem.entries.find { it.prefix == c }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (system) {
                DtcSystem.P -> MaterialTheme.colorScheme.errorContainer
                DtcSystem.C -> MaterialTheme.colorScheme.tertiaryContainer
                DtcSystem.B -> MaterialTheme.colorScheme.secondaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(
                    text = dtc.code,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = dtc.description,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
