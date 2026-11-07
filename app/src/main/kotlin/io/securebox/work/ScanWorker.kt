package io.securebox.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first

/** Weekly (and post-boot) on-device scan. Respects the user's scheduled-scans preference. */
class ScanWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val entry = EntryPointAccessors.fromApplication(applicationContext, NetworkEntryPoint::class.java)
        val settings = runCatching { entry.settingsRepository().settings.first() }.getOrNull() ?: return Result.retry()
        if (!settings.scheduledScansEnabled) return Result.success()

        val summary =
            runCatching { entry.securityRepository().scanAll() }
                .getOrElse { return Result.retry() }

        if (summary.highRiskApps > 0) {
            SecurityNotifier.notifyScanResult(applicationContext, summary.highRiskApps, summary.findingsCount)
        }
        return Result.success()
    }

    companion object {
        const val UNIQUE_WORK_NAME: String = "securebox-weekly-scan"
    }
}
