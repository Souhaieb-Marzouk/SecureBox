package io.securebox.work

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Handles the "Block internet" action on an anomaly notification. */
class AnomalyActionReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action != ACTION_BLOCK) return
        val packageName = intent.getStringExtra(EXTRA_PACKAGE) ?: return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                val entry =
                    EntryPointAccessors.fromApplication(
                        context.applicationContext,
                        NetworkEntryPoint::class.java,
                    )
                entry.firewallRepository().setBlocked(packageName, true)
            }
            pending.finish()
        }
    }

    companion object {
        const val ACTION_BLOCK: String = "io.securebox.action.BLOCK_APP"
        const val EXTRA_PACKAGE: String = "io.securebox.extra.PACKAGE"
    }
}
