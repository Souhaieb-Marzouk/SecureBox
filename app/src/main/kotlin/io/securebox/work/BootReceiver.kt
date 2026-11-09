package io.securebox.work

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.EntryPointAccessors
import io.securebox.core.data.network.SecureBoxVpnService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Enqueues a boot scan and restarts the combined VPN when the user had it enabled. */
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
                val settings = entry.settingsRepository().settings.first()
                val firewall = entry.firewallRepository().state.first()
                if (settings.dnsShieldEnabled || firewall.enabled) {
                    ContextCompat.startForegroundService(
                        context,
                        Intent(context, SecureBoxVpnService::class.java),
                    )
                }
                if (settings.scheduledScansEnabled) {
                    WorkManager
                        .getInstance(context)
                        .enqueue(OneTimeWorkRequestBuilder<ScanWorker>().build())
                }
            }
            pending.finish()
        }
    }
}
