package io.securebox.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import io.securebox.core.domain.network.AnomalyRepository
import io.securebox.core.domain.network.FirewallRepository

/** Hilt bridge for components (workers, receivers) that cannot use constructor injection. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface NetworkEntryPoint {
    fun anomalyRepository(): AnomalyRepository

    fun firewallRepository(): FirewallRepository
}

class AnomalyCheckWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val entry = EntryPointAccessors.fromApplication(applicationContext, NetworkEntryPoint::class.java)
        val events =
            runCatching { entry.anomalyRepository().runCheck() }
                .getOrElse { return Result.retry() }

        events.take(MAX_NOTIFICATIONS).forEach { event ->
            AnomalyNotifier.post(applicationContext, event)
        }
        return Result.success()
    }

    companion object {
        const val UNIQUE_WORK_NAME: String = "securebox-anomaly-check"
        private const val MAX_NOTIFICATIONS: Int = 3
    }
}
