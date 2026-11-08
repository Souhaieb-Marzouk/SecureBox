package io.securebox.feature.settings

import android.net.Uri
import io.securebox.core.domain.network.DnsBlockKind
import io.securebox.core.domain.network.DnsBlocklist

/**
 * Local filter rules for the private browser. Host blocking stops requests before they leave the
 * device; cosmetic filtering hides common consent/social/app-banner elements inside the page.
 *
 * This is intentionally conservative: it never blocks a whole video/CDN domain just to remove an
 * ad, and it does not pretend to filter HTTPS inside other apps.
 */
object BrowserFilters {
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

    fun isBlockedHost(host: String?): Boolean = DnsBlocklist.isBlocked(host)

    fun blockKind(host: String?): DnsBlockKind? = DnsBlocklist.kind(host)

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
