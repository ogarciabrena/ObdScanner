package com.obd.scanner

import android.app.Application
import com.obd.scanner.data.obd.BluetoothManager
import com.obd.scanner.data.sync.AuthManager
import com.obd.scanner.data.sync.SyncSettings
import com.obd.scanner.data.sync.TripUploadWorker
import com.obd.scanner.data.trip.TripRecorder
import com.obd.scanner.domain.usecase.ObdUseCase

class ObdScannerApp : Application() {
    val bluetoothManager: BluetoothManager by lazy { BluetoothManager() }
    val obdUseCase: ObdUseCase by lazy { ObdUseCase(bluetoothManager) }
    val tripRecorder: TripRecorder by lazy { TripRecorder(this) }
    val syncSettings: SyncSettings by lazy { SyncSettings(this) }
    val authManager: AuthManager by lazy { AuthManager(this, syncSettings) }

    override fun onCreate() {
        super.onCreate()
        // Retry any trips left pending from previous sessions (e.g. no internet in the car)
        if (tripRecorder.listPendingTrips().isNotEmpty()) {
            TripUploadWorker.enqueue(this)
        }
    }
}
