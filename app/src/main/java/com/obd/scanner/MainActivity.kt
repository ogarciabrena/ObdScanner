package com.obd.scanner

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.coroutines.launch
import com.obd.scanner.ui.connection.ConnectionScreen
import com.obd.scanner.ui.dashboard.DashboardScreen
import com.obd.scanner.ui.dashboard.DashboardViewModel
import com.obd.scanner.ui.dtc.DtcScreen
import com.obd.scanner.ui.logging.LoggingScreen
import com.obd.scanner.ui.settings.SettingsScreen
import com.obd.scanner.ui.theme.ObdScannerTheme

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        val app = application as ObdScannerApp

        setContent {
            ObdScannerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        bluetoothManager = app.bluetoothManager,
                        obdUseCase = app.obdUseCase
                    )
                }
            }
        }
    }
}

data class NavItem(
    val label: String,
    val icon: ImageVector
)

private val navItems = listOf(
    NavItem("Dashboard", Icons.Default.Dashboard),
    NavItem("Connection", Icons.Default.Bluetooth),
    NavItem("DTCs", Icons.Default.Build),
    NavItem("Logs", Icons.Default.Storage),
    NavItem("Settings", Icons.Default.Settings),
)

@Composable
private fun MainScreen(
    bluetoothManager: com.obd.scanner.data.obd.BluetoothManager,
    obdUseCase: com.obd.scanner.domain.usecase.ObdUseCase
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val connectionState by bluetoothManager.connectionState.collectAsState()
    val isConnected = connectionState is com.obd.scanner.domain.model.ConnectionState.Connected
    val coroutineScope = rememberCoroutineScope()

    val dashboardViewModel = androidx.lifecycle.viewmodel.compose.viewModel {
        DashboardViewModel(obdUseCase, bluetoothManager)
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                navItems.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) }
                    )
                }
            }
        }
    ) { padding ->
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (selectedTab) {
                0 -> DashboardScreen(
                    viewModel = dashboardViewModel,
                    onDisconnect = {
                        coroutineScope.launch { bluetoothManager.disconnect() }
                    }
                )
                1 -> ConnectionScreen(bluetoothManager = bluetoothManager)
                2 -> DtcScreen(
                    obdUseCase = obdUseCase,
                    isConnected = isConnected
                )
                3 -> LoggingScreen(
                    isConnected = isConnected,
                    recentData = dashboardViewModel.state.value.sensors.values.toList()
                )
                else -> SettingsScreen()
            }
        }
    }
}
