package io.securebox.feature.settings

import android.net.Uri

/**
 * Local filter rules for the private browser. Host blocking stops requests before they leave the
 * device; cosmetic filtering hides common consent/social/app-banner elements inside the page.
 *
 * This is intentionally conservative: it never blocks a whole video/CDN domain just to remove an
 * ad, and it does not pretend to filter HTTPS inside other apps.
 */
object BrowserFilters {
    private val blockedHosts =
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
            "scorecardresearch.com",
            "quantserve.com",
            "moatads.com",
            "imrworldwide.com",
            "adsafeprotected.com",
            "adcolony.com",
            "applovin.com",
            "unityads.unity3d.com",
            "vungle.com",
            "chartboost.com",
            "inmobi.com",
            "facebook.net",
            "connect.facebook.net",
            "analytics.google.com",
            "googletagmanager.com",
            "googletagservices.com",
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

    private val trackingParams =
        setOf(
            "utm_source",
            "utm_medium",
            "utm_campaign",
            "utm_term",
            "utm_content",
            "utm_id",
            "utm_name",
            "fbclid",
            "gclid",
            "dclid",
            "msclkid",
            "yclid",
            "twclid",
            "igshid",
            "mc_eid",
            "mc_cid",
            "_ga",
            "_gl",
            "wickedid",
            "oly_enc_id",
            "vero_id",
            "rb_clickid",
            "s_cid",
            "mkt_tok",
        )

    fun isBlockedHost(host: String?): Boolean {
        val value = host?.lowercase() ?: return false
        return blockedHosts.any { blocked -> value == blocked || value.endsWith(".$blocked") }
    }

    /** Removes known tracking parameters while preserving every other query parameter. */
    fun stripTrackingParams(url: String): String {
        val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return url
        if (uri.scheme != "http" && uri.scheme != "https") return url
        val names = uri.queryParameterNames.orEmpty()
        if (names.none { it.lowercase() in trackingParams }) return url

        val builder = uri.buildUpon().clearQuery()
        names
            .filterNot { it.lowercase() in trackingParams }
            .forEach { name -> builder.appendQueryParameter(name, uri.getQueryParameter(name)) }
        return builder.build().toString()
    }

    /** Hides common cookie notices, social widgets, app banners and ad slots. */
    val cosmeticScript: String =
        """
        (function () {
          if (window.__secureboxShield) return;
          window.__secureboxShield = true;
          var css = [
            '#onetrust-banner-sdk', '#onetrust-consent-sdk', '.qc-cmp2-container',
            '.cc-window', '.cookie-consent', '.cookie-banner',
            '[id*="cookie-banner"]', '[class*="cookie-banner"]',
            '.fc-consent-root', '.didomi-popup-container', '.sp-message-open',
            '.newsletter-popup', '[class*="newsletter-popup"]',
            '.social-share', '.share-buttons', '[class*="social-share"]',
            '.smartbanner', '.app-banner', '[class*="app-download"]',
            '[id*="app-banner"]', '.interstitial-ad', '.ad-overlay',
            '[class*="ad-slot"]', '[id*="google_ads"]', 'ins.adsbygoogle',
            '.advertisement', '[data-ad-slot]'
          ].join(',');
          var style = document.createElement('style');
          style.type = 'text/css';
          style.textContent = css + '{display:none !important; visibility:hidden !important;}';
          (document.head || document.documentElement).appendChild(style);
        })();
        """.trimIndent()
}
