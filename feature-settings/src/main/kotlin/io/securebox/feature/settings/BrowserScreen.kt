package io.securebox.feature.settings

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.securebox.core.domain.model.TrackingProtectionMode
import io.securebox.core.domain.network.DnsBlockKind
import io.securebox.core.ui.icon.SecureBoxIcons
import io.securebox.core.ui.theme.SecureBoxColors
import java.io.ByteArrayInputStream
import java.util.Locale

private const val HOME_URL = "https://duckduckgo.com/"

private data class SessionCounts(
    val ads: Long = 0,
    val trackers: Long = 0,
    val annoyances: Long = 0,
    val malicious: Long = 0,
)

private enum class BlockKind {
    AD,
    TRACKER,
    ANNOYANCE,
    MALICIOUS,
}

@Composable
fun BrowserRoute(
    onBack: () -> Unit,
    viewModel: ShieldViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    BrowserScreen(
        appSettings = state.settings,
        onBack = onBack,
        onStats = viewModel::recordBrowserSession,
    )
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserScreen(
    appSettings: io.securebox.core.domain.model.AppSettings,
    onBack: () -> Unit,
    onStats: (ads: Long, trackers: Long, annoyances: Long, malicious: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val currentSettings = rememberUpdatedState(appSettings)
    val session = remember { mutableStateOf(SessionCounts()) }
    var address by rememberSaveable { mutableStateOf(HOME_URL) }
    var blockedCount by rememberSaveable { mutableIntStateOf(0) }
    var blockedUrl by remember { mutableStateOf<String?>(null) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var canGoBack by remember { mutableStateOf(false) }

    fun record(kind: BlockKind) {
        session.value =
            when (kind) {
                BlockKind.AD -> session.value.copy(ads = session.value.ads + 1)
                BlockKind.TRACKER -> session.value.copy(trackers = session.value.trackers + 1)
                BlockKind.ANNOYANCE -> session.value.copy(annoyances = session.value.annoyances + 1)
                BlockKind.MALICIOUS -> session.value.copy(malicious = session.value.malicious + 1)
            }
    }

    fun normalize(input: String): String {
        val value = input.trim()
        if (value.startsWith("http://")) return "https://${value.removePrefix("http://")}"
        if (value.startsWith("https://")) return value
        return if (value.contains(".") && !value.contains(" ")) {
            "https://$value"
        } else {
            "https://duckduckgo.com/?q=${Uri.encode(value)}"
        }
    }

    fun navigate(input: String) {
        val current = currentSettings.value
        val normalized = normalize(input)
        val target =
            if (current.trackingProtection == TrackingProtectionMode.HIGH) {
                BrowserFilters.stripTrackingParams(normalized)
            } else {
                normalized
            }
        if (current.safeBrowsing &&
            (SafeBrowsingStore.isBlocked(context, normalized) || SafeBrowsingStore.isBlocked(context, target))
        ) {
            record(BlockKind.MALICIOUS)
            blockedUrl = target
            return
        }
        blockedUrl = null
        address = target
        webView?.loadUrl(target)
        focusManager.clearFocus()
    }

    BackHandler {
        if (webView?.canGoBack() == true) {
            webView?.goBack()
        } else {
            onBack()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            val counts = session.value
            onStats(counts.ads, counts.trackers, counts.annoyances, counts.malicious)
            val high = currentSettings.value.trackingProtection == TrackingProtectionMode.HIGH
            if (high) {
                CookieManager.getInstance().removeAllCookies(null)
                CookieManager.getInstance().flush()
            }
            webView?.stopLoading()
            webView?.destroy()
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Surface(
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.80f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                IconButton(
                    onClick = { if (webView?.canGoBack() == true) webView?.goBack() else onBack() },
                ) {
                    Icon(
                        imageVector = SecureBoxIcons.ChevronRight,
                        contentDescription = stringResource(R.string.browser_back),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.rotate(180f),
                    )
                }
                IconButton(onClick = { webView?.reload() }) {
                    Icon(
                        imageVector = SecureBoxIcons.Refresh,
                        contentDescription = stringResource(R.string.browser_reload),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium,
                    placeholder = { Text(stringResource(R.string.browser_address), maxLines = 1) },
                    shape = RoundedCornerShape(18.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = { navigate(address) }),
                )
                IconButton(onClick = { navigate(address) }) {
                    Icon(
                        imageVector = SecureBoxIcons.ChevronRight,
                        contentDescription = stringResource(R.string.browser_go),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            allowFileAccess = false
                            allowContentAccess = false
                            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                            mediaPlaybackRequiresUserGesture = true
                            setSafeBrowsingEnabled(true)
                            userAgentString = "$userAgentString SecureBox/0.7"
                        }
                        CookieManager.getInstance().setAcceptCookie(true)
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, false)
                        webViewClient =
                            object : WebViewClient() {
                                override fun shouldInterceptRequest(
                                    view: WebView,
                                    request: WebResourceRequest,
                                ): WebResourceResponse? {
                                    val current = currentSettings.value
                                    val resource = request.url.toString()
                                    if (current.safeBrowsing && SafeBrowsingStore.isBlocked(ctx, resource)) {
                                        view.post {
                                            record(BlockKind.MALICIOUS)
                                            blockedCount += 1
                                        }
                                        return WebResourceResponse(
                                            "text/plain",
                                            "utf-8",
                                            ByteArrayInputStream(ByteArray(0)),
                                        )
                                    }
                                    val kind = BrowserFilters.blockKind(request.url.host)
                                    if (current.blockAdsAndTrackers && kind != null) {
                                        view.post {
                                            record(if (kind == DnsBlockKind.AD) BlockKind.AD else BlockKind.TRACKER)
                                            blockedCount += 1
                                        }
                                        return WebResourceResponse(
                                            "text/plain",
                                            "utf-8",
                                            ByteArrayInputStream(ByteArray(0)),
                                        )
                                    }
                                    return null
                                }

                                override fun shouldOverrideUrlLoading(
                                    view: WebView,
                                    request: WebResourceRequest,
                                ): Boolean {
                                    val current = currentSettings.value
                                    val original = request.url.toString()
                                    val normalized = normalize(original)
                                    val target =
                                        if (current.trackingProtection == TrackingProtectionMode.HIGH) {
                                            BrowserFilters.stripTrackingParams(normalized)
                                        } else {
                                            normalized
                                        }
                                    if (current.safeBrowsing &&
                                        (SafeBrowsingStore.isBlocked(ctx, original) ||
                                            SafeBrowsingStore.isBlocked(ctx, target))
                                    ) {
                                        view.post {
                                            record(BlockKind.MALICIOUS)
                                            blockedUrl = target
                                        }
                                        return true
                                    }
                                    if (target != original) {
                                        view.post { view.loadUrl(target) }
                                        return true
                                    }
                                    return false
                                }

                                override fun onPageStarted(
                                    view: WebView,
                                    url: String?,
                                    favicon: Bitmap?,
                                ) {
                                    url?.let { address = it }
                                    canGoBack = view.canGoBack()
                                }

                                override fun onPageFinished(
                                    view: WebView,
                                    url: String?,
                                ) {
                                    canGoBack = view.canGoBack()
                                    url?.let { address = it }
                                    if (currentSettings.value.hideAnnoyances) {
                                        view.evaluateJavascript(BrowserFilters.cosmeticScript, null)
                                        view.post { record(BlockKind.ANNOYANCE) }
                                    }
                                }
                            }
                        loadUrl(HOME_URL)
                    }.also { created ->
                        webView = created
                    }
                },
                update = { webView = it },
            )

            blockedUrl?.let { blocked ->
                Surface(
                    modifier = Modifier.fillMaxSize().padding(20.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            imageVector = SecureBoxIcons.Alert,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp),
                        )
                        Text(
                            text = stringResource(R.string.browser_safe_blocked_title),
                            style = MaterialTheme.typography.headlineMedium,
                        )
                        Text(
                            text = stringResource(R.string.browser_safe_blocked_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = blocked,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Button(
                            onClick = {
                                blockedUrl = null
                                if (webView?.canGoBack() == true) webView?.goBack() else webView?.loadUrl(HOME_URL)
                            },
                        ) {
                            Text(stringResource(R.string.browser_safe_back))
                        }
                    }
                }
            }
        }

        Surface(
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text =
                        stringResource(
                            if (appSettings.trackingProtection == TrackingProtectionMode.HIGH) {
                                R.string.browser_status_high
                            } else {
                                R.string.browser_status_standard
                            },
                        ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(R.string.browser_blocked, blockedCount),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
