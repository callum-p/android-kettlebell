package com.kettlebell.app

import android.app.Application
import com.kettlebell.app.data.WorkoutRepository
import com.kettlebell.app.data.db.KettlebellDatabase
import com.kettlebell.app.debug.AppLogger
import com.kettlebell.app.notify.RestNotifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class KettlebellApp : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    lateinit var repository: WorkoutRepository
        private set

    override fun onCreate() {
        super.onCreate()
        AppLogger.init(this)
        RestNotifier.ensureChannel(this)
        repository = WorkoutRepository(KettlebellDatabase.get(this))
        applicationScope.launch {
            runCatching { repository.seedIfNeeded() }
                .onFailure { AppLogger.e("Seed", "Failed to seed exercise library", it) }
        }
    }
}
