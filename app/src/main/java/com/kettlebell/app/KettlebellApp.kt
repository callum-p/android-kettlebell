package com.kettlebell.app

import android.app.Application
import com.kettlebell.app.data.SettingsStore
import com.kettlebell.app.data.WorkoutRepository
import com.kettlebell.app.data.db.KettlebellDatabase
import com.kettlebell.app.debug.AppLogger
import com.kettlebell.app.notify.RestNotifier
import com.kettlebell.app.sync.DriveSync
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class KettlebellApp : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Completes once any Google Drive boot-restore has finished, gating the first database read. */
    private val bootReady = CompletableDeferred<Unit>()

    lateinit var repository: WorkoutRepository
        private set

    lateinit var driveSync: DriveSync
        private set

    lateinit var settingsStore: SettingsStore
        private set

    override fun onCreate() {
        super.onCreate()
        AppLogger.init(this)
        RestNotifier.ensureChannel(this)
        driveSync = DriveSync(this)
        settingsStore = SettingsStore(this)
        repository = WorkoutRepository(KettlebellDatabase.get(this), bootReady)

        applicationScope.launch {
            try {
                if (driveSync.isConnected()) {
                    AppLogger.i("KettlebellApp", "Restoring database from Google Drive…")
                    withTimeoutOrNull(20_000) { driveSync.restore() }
                }
            } catch (t: Throwable) {
                AppLogger.e("KettlebellApp", "Boot restore failed", t)
            } finally {
                // Always release the gate so the app never hangs waiting on sync.
                bootReady.complete(Unit)
            }
            runCatching { repository.seedIfNeeded() }
                .onFailure { AppLogger.e("Seed", "Failed to seed exercise library", it) }
        }
    }
}
