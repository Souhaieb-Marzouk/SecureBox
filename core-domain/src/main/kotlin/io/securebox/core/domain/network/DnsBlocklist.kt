package io.securebox.core.domain.network

/** What a blocked DNS/browser request was classified as. */
public enum class DnsBlockKind {
    AD,
    TRACKER,
}

/**
 * Shared local host blocklist used by the private browser and the DNS Shield VPN. The DNS resolver
 * chosen in the shield also filters upstream; this list gives immediate offline protection and lets
 * SecureBox count what it blocked locally.
 */
public object DnsBlocklist {
    private val adHosts =
        setOf(
            "doubleclick.net",
            "googlesyndication.com",
            "googleadservices.com",
            "adservice.google.com",
            "pagead2.googlesyndication.com",
            "partner.googleadservices.com",
            "amazon-adsystem.com",
            "adsrvr.org",
            "adnxs.com",
            "rubiconproject.com",
            "pubmatic.com",
            "openx.net",
            "casalemedia.com",
            "criteo.com",
            "criteo.net",
            "taboola.com",
            "outbrain.com",
            "moatads.com",
            "imrworldwide.com",
            "adsafeprotected.com",
            "adcolony.com",
            "applovin.com",
            "unityads.unity3d.com",
            "vungle.com",
            "chartboost.com",
            "inmobi.com",
        )

    private val trackerHosts =
        setOf(
            "facebook.net",
            "connect.facebook.net",
            "analytics.google.com",
            "googletagmanager.com",
            "googletagservices.com",
            "scorecardresearch.com",
            "quantserve.com",
            "segment.io",
            "segment.com",
            "mixpanel.com",
            "amplitude.com",
            "hotjar.com",
            "fullstory.com",
            "appsflyer.com",
            "adjust.com",
            "branch.io",
            "app-measurement.com",
            "bugsnag.com",
            "newrelic.com",
            "sentry.io",
            "crazyegg.com",
            "mouseflow.com",
            "clicktale.net",
        )

    public fun kind(host: String?): DnsBlockKind? {
        val value = host?.lowercase()?.trim() ?: return null
        return when {
            matches(value, adHosts) -> DnsBlockKind.AD
            matches(value, trackerHosts) -> DnsBlockKind.TRACKER
            else -> null
        }
    }

    public fun isBlocked(host: String?): Boolean = kind(host) != null

    private fun matches(
        host: String,
        set: Set<String>,
    ): Boolean = set.any { blocked -> host == blocked || host.endsWith(".$blocked") }
}
