package io.securebox.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first

/** Daily signed-feed refresh. Honours the user's auto-update preference. */
class ThreatFeedWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val entry = EntryPointAccessors.fromApplication(applicationContext, NetworkEntryPoint::class.java)
        val settings = runCatching { entry.settingsRepository().settings.first() }.getOrNull() ?: return Result.retry()
        if (!settings.threatIntelEnabled || !settings.threatIntelAutoUpdate) return Result.success()
        runCatching { entry.threatFeedRepository().update(force = false) }
        return Result.success()
    }

    companion object {
        const val UNIQUE_WORK_NAME: String = "securebox-threat-feed"
    }
}
