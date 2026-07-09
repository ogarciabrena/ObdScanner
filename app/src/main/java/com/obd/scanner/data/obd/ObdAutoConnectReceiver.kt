package com.obd.scanner.data.obd

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import com.obd.scanner.MainActivity

/**
 * Fires when any Bluetooth device establishes an ACL connection. If it looks
 * like an OBD adapter, prompts the user to start a trip via notification.
 */
class ObdAutoConnectReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != BluetoothDevice.ACTION_ACL_CONNECTED) return

        val device = IntentCompat.getParcelableExtra(
            intent, BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java
        ) ?: return

        val name = try { device.name } catch (_: SecurityException) { null } ?: return
        val looksLikeObd = OBD_NAME_HINTS.any { name.contains(it, ignoreCase = true) }
        if (!looksLikeObd) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
        ) return

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Adaptador OBD detectado",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Aviso cuando el adaptador OBD se conecta por Bluetooth" }
        )

        val startTripIntent = PendingIntent.getForegroundService(
            context, 10,
            Intent(context, ObdService::class.java)
                .setAction(ObdService.ACTION_START_TRIP)
                .putExtra(ObdService.EXTRA_DEVICE_ADDRESS, device.address),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val openAppIntent = PendingIntent.getActivity(
            context, 11,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        manager.notify(
            NOTIFICATION_ID,
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("Adaptador OBD conectado")
                .setContentText("$name detectado. ¿Iniciar viaje?")
                .setSmallIcon(android.R.drawable.ic_menu_directions)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(openAppIntent)
                .addAction(0, "Iniciar viaje", startTripIntent)
                .setAutoCancel(true)
                .build()
        )
    }

    companion object {
        private const val CHANNEL_ID = "obd_detected"
        private const val NOTIFICATION_ID = 2001
        private val OBD_NAME_HINTS = listOf(
            "OBD", "ELM", "VLINK", "VEEPEAK", "KIWI", "PLX", "SCAN", "KONNWEI", "ICAR"
        )
    }
}
