package com.obd.scanner.data.obd

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.obd.scanner.MainActivity
import com.obd.scanner.ObdScannerApp
import com.obd.scanner.data.sync.TripUploadWorker
import com.obd.scanner.domain.model.ConnectionState
import com.obd.scanner.ui.dashboard.DefaultPids
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ObdService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var tripJob: Job? = null

    private val app get() = application as ObdScannerApp

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_TRIP -> {
                startForeground(NOTIFICATION_ID, buildNotification("Conectando al adaptador OBD..."))
                startTrip(intent.getStringExtra(EXTRA_DEVICE_ADDRESS))
            }
            ACTION_STOP_TRIP -> stopTrip()
            else -> startForeground(NOTIFICATION_ID, buildNotification("Servicio OBD activo"))
        }
        return START_NOT_STICKY
    }

    private fun startTrip(deviceAddress: String?) {
        if (tripJob?.isActive == true) return
        tripJob = scope.launch {
            val bm = app.bluetoothManager

            if (bm.connectionState.value !is ConnectionState.Connected) {
                val device = try {
                    val address = deviceAddress ?: run { stopSelf(); return@launch }
                    BluetoothAdapter.getDefaultAdapter()?.getRemoteDevice(address)
                } catch (_: Exception) { null }
                if (device == null) {
                    updateNotification("No se encontró el adaptador OBD")
                    delay(3000); stopSelf(); return@launch
                }
                updateNotification("Conectando a ${bm.safeDeviceName(device)}...")
                val result = bm.connect(device)
                if (result.isFailure) {
                    updateNotification("No se pudo conectar al OBD")
                    delay(3000); stopSelf(); return@launch
                }
            }

            val connected = bm.connectionState.value as? ConnectionState.Connected
            app.tripRecorder.start(connected?.deviceName ?: "OBD")
            updateNotification("Viaje en curso — grabando telemetría")

            var lastSpeed: String? = null
            var lastRpm: String? = null
            val pidFailures = mutableMapOf<Int, Int>()

            while (isActive && bm.connectionState.value is ConnectionState.Connected) {
                for (pid in DefaultPids) {
                    if ((pidFailures[pid.pid] ?: 0) >= 3) continue
                    val data = app.obdUseCase.getSensorData(pid)
                    if (data == null) {
                        pidFailures[pid.pid] = (pidFailures[pid.pid] ?: 0) + 1
                        continue
                    }
                    pidFailures[pid.pid] = 0
                    app.tripRecorder.record(data)
                    when (pid.pid) {
                        0x0D -> lastSpeed = data.formattedValue
                        0x0C -> lastRpm = data.formattedValue
                    }
                }
                if (lastSpeed != null || lastRpm != null) {
                    updateNotification(
                        "Viaje en curso — " +
                            listOfNotNull(lastSpeed, lastRpm).joinToString("  ·  ")
                    )
                }
                delay(200)
            }

            finishTrip()
        }
    }

    private fun stopTrip() {
        tripJob?.cancel()
        tripJob = null
        finishTrip()
    }

    private fun finishTrip() {
        val summary = app.tripRecorder.stop()
        if (summary != null) {
            TripUploadWorker.enqueue(this)
            val minutes = (summary.endTime - summary.startTime) / 60000
            val manager = getSystemService(NotificationManager::class.java)
            manager.notify(
                SUMMARY_NOTIFICATION_ID,
                NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("Viaje finalizado")
                    .setContentText("$minutes min · ${summary.sampleCount} muestras guardadas")
                    .setSmallIcon(android.R.drawable.ic_menu_directions)
                    .setContentIntent(openAppIntent())
                    .setAutoCancel(true)
                    .build()
            )
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        app.tripRecorder.stop()
        scope.cancel()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Viaje OBD",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Grabación de telemetría durante el viaje"
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun openAppIntent(): PendingIntent = PendingIntent.getActivity(
        this, 0,
        Intent(this, MainActivity::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun buildNotification(text: String) = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("ObdScanner")
        .setContentText(text)
        .setSmallIcon(android.R.drawable.ic_menu_directions)
        .setOngoing(true)
        .setContentIntent(openAppIntent())
        .addAction(
            0, "Finalizar viaje",
            PendingIntent.getService(
                this, 1,
                Intent(this, ObdService::class.java).setAction(ACTION_STOP_TRIP),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
        .build()

    private fun updateNotification(text: String) {
        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, buildNotification(text))
    }

    companion object {
        const val ACTION_START_TRIP = "com.obd.scanner.action.START_TRIP"
        const val ACTION_STOP_TRIP = "com.obd.scanner.action.STOP_TRIP"
        const val EXTRA_DEVICE_ADDRESS = "device_address"
        private const val CHANNEL_ID = "obd_service"
        private const val NOTIFICATION_ID = 1001
        private const val SUMMARY_NOTIFICATION_ID = 1002
    }
}
