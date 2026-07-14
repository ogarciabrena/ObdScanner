package com.obd.scanner.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.obd.scanner.ObdScannerApp
import com.obd.scanner.data.sync.TripUploadWorker
import com.obd.scanner.data.sync.testSupabaseConnection
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as ObdScannerApp
    val scope = rememberCoroutineScope()

    var url by remember { mutableStateOf("") }
    var key by remember { mutableStateOf("") }
    var configured by remember { mutableStateOf(false) }
    var pendingCount by remember { mutableStateOf(0) }
    var testing by remember { mutableStateOf(false) }
    var testOk by remember { mutableStateOf<Boolean?>(null) }
    var testMessage by remember { mutableStateOf<String?>(null) }

    // --- Cuenta (Supabase Auth) ---
    val loggedEmail by app.authManager.emailFlow.collectAsState(initial = null)
    val isLoggedIn by app.authManager.isLoggedInFlow.collectAsState(initial = false)
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var authBusy by remember { mutableStateOf(false) }
    var authMessage by remember { mutableStateOf<String?>(null) }
    var authOk by remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(Unit) {
        val creds = app.syncSettings.current()
        url = creds.url
        key = creds.key
        configured = creds.isConfigured
        pendingCount = app.tripRecorder.listPendingTrips().size
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Settings",
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
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (configured) Icons.Default.Cloud else Icons.Default.CloudOff,
                        contentDescription = null,
                        tint = if (configured) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Sincronización en la nube (Supabase)",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (configured)
                        "Configurado — los viajes se suben automáticamente cuando hay internet"
                    else
                        "Sin configurar — los viajes se guardan solo en este dispositivo. " +
                        "Crea un proyecto gratis en supabase.com y pega aquí tus credenciales.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it; testOk = null; testMessage = null },
                    label = { Text("Project URL") },
                    placeholder = { Text("https://xxxx.supabase.co") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = key,
                    onValueChange = { key = it; testOk = null; testMessage = null },
                    label = { Text("Publishable (anon) key") },
                    placeholder = { Text("sb_publishable_…") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = {
                            scope.launch {
                                testing = true
                                testMessage = null
                                val (ok, msg) = testSupabaseConnection(url, key)
                                testOk = ok
                                if (ok) {
                                    app.syncSettings.save(url, key)
                                    configured = app.syncSettings.current().isConfigured
                                    testMessage = "Guardado — $msg"
                                    TripUploadWorker.enqueue(context)
                                } else {
                                    testMessage = "No guardado — $msg"
                                }
                                testing = false
                            }
                        },
                        enabled = !testing
                    ) { Text(if (testing) "Verificando..." else "Verificar y guardar") }

                    if (pendingCount > 0) {
                        Spacer(modifier = Modifier.padding(horizontal = 6.dp))
                        OutlinedButton(
                            onClick = { TripUploadWorker.enqueue(context) },
                            enabled = configured
                        ) { Text("Subir ahora ($pendingCount)") }
                    }
                }

                testMessage?.let { msg ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = (if (testOk == true) "✓ " else "✗ ") + msg,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (testOk == true) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Viajes pendientes de subir: $pendingCount",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ---- Cuenta (Supabase Auth) ----
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccountCircle, contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = "Cuenta",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                if (isLoggedIn) {
                    Text(
                        text = "Sesión iniciada como:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = loggedEmail ?: "usuario",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tus viajes se suben a tu cuenta. Cambia de cuenta para separar los datos de otro vehículo.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(onClick = { scope.launch { app.authManager.signOut() } }) {
                        Text("Cerrar sesión")
                    }
                } else {
                    Text(
                        text = "Inicia sesión para que tus viajes se guarden en tu cuenta. Cada cuenta separa los datos (ideal para varios vehículos).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it; authMessage = null },
                        label = { Text("Email") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; authMessage = null },
                        label = { Text("Contraseña") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row {
                        Button(
                            enabled = !authBusy && email.isNotBlank() && password.length >= 6,
                            onClick = {
                                scope.launch {
                                    authBusy = true; authMessage = null
                                    val r = app.authManager.signIn(email, password)
                                    authOk = r.ok; authMessage = r.message
                                    if (r.ok) TripUploadWorker.enqueue(context)
                                    authBusy = false
                                }
                            }
                        ) { Text(if (authBusy) "..." else "Iniciar sesión") }

                        Spacer(modifier = Modifier.padding(horizontal = 6.dp))

                        OutlinedButton(
                            enabled = !authBusy && email.isNotBlank() && password.length >= 6,
                            onClick = {
                                scope.launch {
                                    authBusy = true; authMessage = null
                                    val r = app.authManager.signUp(email, password)
                                    authOk = r.ok; authMessage = r.message
                                    if (r.ok) TripUploadWorker.enqueue(context)
                                    authBusy = false
                                }
                            }
                        ) { Text("Registrarse") }
                    }

                    authMessage?.let { msg ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = (if (authOk == true) "✓ " else "✗ ") + msg,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (authOk == true) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "About",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null)
                    Column(modifier = Modifier.padding(start = 12.dp)) {
                        Text(
                            text = "ObdScanner",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Version 1.0.0",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Telemetría OBD-II con detección de viajes y sync a la nube",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
