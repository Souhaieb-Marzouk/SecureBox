package io.securebox.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.hilt.android.EntryPointAccessors
import io.securebox.core.domain.model.CheckResult
import io.securebox.core.domain.model.UpdateDecision
import io.securebox.core.domain.update.UpdateEvaluator

/** Daily update check. Silent when offline or up to date. */
class UpdateCheckWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val entry = EntryPointAccessors.fromApplication(applicationContext, NetworkEntryPoint::class.java)
        val version = entry.appVersion()
        val result = runCatching { entry.updateRepository().fetchLatestRelease(force = false, current = version) }.getOrNull()
        if (result is CheckResult.Fetched) {
            val decision = UpdateEvaluator.evaluate(version, result.release)
            if (decision is UpdateDecision.UpdateAvailable) {
                SecurityNotifier.notifyUpdateAvailable(applicationContext, result.release.tagName)
            }
        }
        return Result.success()
    }

    companion object {
        const val UNIQUE_WORK_NAME: String = "securebox-update-check"
    }
}
