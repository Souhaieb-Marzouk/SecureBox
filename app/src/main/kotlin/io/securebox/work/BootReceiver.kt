package io.securebox.work

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Enqueues a one-off scan after boot, only if the user kept scheduled scans enabled. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                val entry =
                    EntryPointAccessors.fromApplication(
                        context.applicationContext,
                        NetworkEntryPoint::class.java,
                    )
                val enabled = entry.settingsRepository().settings.first().scheduledScansEnabled
                if (enabled) {
                    WorkManager
                        .getInstance(context)
                        .enqueue(OneTimeWorkRequestBuilder<ScanWorker>().build())
                }
            }
            pending.finish()
        }
    }
}
