package io.securebox.feature.settings

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Build
import android.provider.Settings
import io.securebox.core.domain.model.DnsPreset

/** A system-wide encrypted-DNS preset the user applies through Android Private DNS. */
data class DnsPresetInfo(
    val preset: DnsPreset,
    val titleRes: Int,
    val bodyRes: Int,
    val dohUrl: String,
    val dotHost: String,
)

val dnsPresetInfos: List<DnsPresetInfo> =
    listOf(
        DnsPresetInfo(
            preset = DnsPreset.ADGUARD,
            titleRes = R.string.shield_dns_adguard_title,
            bodyRes = R.string.shield_dns_adguard_body,
            dohUrl = DnsPreset.ADGUARD.dohUrl,
            dotHost = DnsPreset.ADGUARD.dotHost,
        ),
        DnsPresetInfo(
            preset = DnsPreset.CLOUDFLARE_FAMILIES,
            titleRes = R.string.shield_dns_cloudflare_title,
            bodyRes = R.string.shield_dns_cloudflare_body,
            dohUrl = DnsPreset.CLOUDFLARE_FAMILIES.dohUrl,
            dotHost = DnsPreset.CLOUDFLARE_FAMILIES.dotHost,
        ),
        DnsPresetInfo(
            preset = DnsPreset.QUAD9,
            titleRes = R.string.shield_dns_quad9_title,
            bodyRes = R.string.shield_dns_quad9_body,
            dohUrl = DnsPreset.QUAD9.dohUrl,
            dotHost = DnsPreset.QUAD9.dotHost,
        ),
    )

fun dnsPresetInfo(preset: DnsPreset): DnsPresetInfo =
    dnsPresetInfos.firstOrNull { it.preset == preset } ?: dnsPresetInfos.first()

/** The hostname currently configured in Android Private DNS, or null for Off/Automatic. */
fun currentPrivateDnsHost(context: Context): String? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return null
    return runCatching {
        val manager = context.getSystemService(ConnectivityManager::class.java) ?: return null
        val network = manager.activeNetwork ?: return null
        manager.getLinkProperties(network)?.privateDnsServerName
    }.getOrNull()
}

/** Opens the system Private DNS screen; returns false if the OEM does not expose one. */
fun openPrivateDnsSettings(context: Context): Boolean {
    val candidates =
        buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                add(Intent("android.settings.PRIVATE_DNS_SETTINGS"))
            }
            add(Intent(Settings.ACTION_WIRELESS_SETTINGS))
            add(Intent(Settings.ACTION_SETTINGS))
        }
    for (intent in candidates) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (intent.resolveActivity(context.packageManager) == null) continue
        if (runCatching { context.startActivity(intent) }.isSuccess) return true
    }
    return false
}
