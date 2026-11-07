package io.securebox.core.data.security

import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import io.securebox.core.domain.security.AppRiskReport
import io.securebox.core.domain.security.AppSecurityInfo
import io.securebox.core.domain.security.Finding
import io.securebox.core.domain.security.FindingCode
import io.securebox.core.domain.security.InstallSource
import io.securebox.core.domain.security.PermissionAuditor
import io.securebox.core.domain.security.RiskLevel
import io.securebox.core.domain.security.RiskScorer
import io.securebox.core.domain.security.RuleEngine
import io.securebox.core.domain.security.ScanSummary
import io.securebox.core.domain.security.SecurityRepository
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

@Singleton
class AndroidSecurityRepository
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val rulesRepository: SecurityRulesRepository,
        private val permissionAuditor: PermissionAuditor,
        private val riskScorer: RiskScorer,
        private val dataStore: DataStore<Preferences>,
    ) : SecurityRepository {
        private val packageManager: PackageManager get() = context.packageManager

        override suspend fun installedApps(): List<AppSecurityInfo> =
            withContext(Dispatchers.IO) {
                @Suppress("DEPRECATION")
                val packages = packageManager.getInstalledPackages(PackageManager.GET_PERMISSIONS)
                packages
                    .mapNotNull { pkg ->
                        val app = pkg.applicationInfo ?: return@mapNotNull null
                        val label =
                            runCatching { packageManager.getApplicationLabel(app).toString() }
                                .getOrDefault(pkg.packageName)
                        val system = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                        val installer = installerOf(pkg.packageName)
                        val source =
                            when {
                                system -> InstallSource.SYSTEM
                                installer == PLAY_STORE -> InstallSource.PLAY_STORE
                                else -> InstallSource.SIDELOADED
                            }
                        AppSecurityInfo(
                            packageName = pkg.packageName,
                            label = label,
                            uid = app.uid,
                            versionName = pkg.versionName.orEmpty(),
                            isSystem = system,
                            source = source,
                            installer = installer,
                            firstInstallTimeMillis = pkg.firstInstallTime,
                            lastUpdateTimeMillis = pkg.lastUpdateTime,
                            permissions = pkg.requestedPermissions?.toList().orEmpty(),
                        )
                    }.sortedBy { it.label.lowercase() }
            }

        override suspend fun auditAll(onProgress: (Int, Int) -> Unit): List<AppRiskReport> =
            withContext(Dispatchers.IO) {
                val apps = installedApps()
                val reports = ArrayList<AppRiskReport>(apps.size)
                apps.forEachIndexed { index, app ->
                    reports += buildReport(app, emptyList())
                    onProgress(index + 1, apps.size)
                }
                reports.sortedWith(compareByDescending<AppRiskReport> { it.level.ordinal }.thenBy { it.score })
            }

        override suspend fun scanAll(onProgress: (Int, Int) -> Unit): ScanSummary =
            withContext(Dispatchers.IO) {
                val started = System.currentTimeMillis()
                val apps = installedApps().filterNot { it.isSystem }
                val ruleSet = rulesRepository.rules()
                val engine = RuleEngine(ruleSet)
                val reports = ArrayList<AppRiskReport>(apps.size)
                var findingsCount = 0

                apps.forEachIndexed { index, app ->
                    val extra =
                        runCatching {
                            val apkPath = packageManager.getApplicationInfo(app.packageName, 0).sourceDir
                            val corpus = DexStringExtractor.extract(File(apkPath))
                            engine.scan(corpus)
                        }.getOrDefault(emptyList())

                    val report = buildReport(app, extra)
                    findingsCount += report.findings.size
                    reports += report
                    onProgress(index + 1, apps.size)
                }

                val finished = System.currentTimeMillis()
                val summary =
                    ScanSummary(
                        startedAtMillis = started,
                        finishedAtMillis = finished,
                        scannedApps = apps.size,
                        highRiskApps = reports.count { it.level.ordinal >= RiskLevel.HIGH.ordinal },
                        findingsCount = findingsCount,
                        rulesVersion = ruleSet.version,
                    )
                storeReports(reports, summary)
                summary
            }

        override suspend fun lastScan(): ScanSummary? {
            val raw = dataStore.data.first()[Keys.LAST_SCAN] ?: return null
            return runCatching { decodeSummary(JSONObject(raw)) }.getOrNull()
        }

        override suspend fun storedReports(): List<AppRiskReport> {
            val raw = dataStore.data.first()[Keys.REPORTS] ?: return emptyList()
            return runCatching {
                val array = JSONArray(raw)
                buildList {
                    for (index in 0 until array.length()) add(decodeReport(array.getJSONObject(index)))
                }
            }.getOrDefault(emptyList())
        }

        override suspend fun storeReports(
            reports: List<AppRiskReport>,
            summary: ScanSummary,
        ) {
            dataStore.edit { preferences ->
                preferences[Keys.LAST_SCAN] = encodeSummary(summary).toString()
                preferences[Keys.REPORTS] =
                    JSONArray().apply {
                        reports.sortedBy { it.score }.take(MAX_STORED_REPORTS).forEach { put(encodeReport(it)) }
                    }.toString()
            }
        }

        override suspend fun rulesVersion(): Int = rulesRepository.rules().version

        private fun buildReport(
            app: AppSecurityInfo,
            extra: List<Finding>,
        ): AppRiskReport {
            val findings =
                (
                    permissionAuditor.analyze(app.permissions) +
                        sourceFindings(app) +
                        componentFindings(app) +
                        extra
                ).distinctBy { it.code to it.detail }
            return riskScorer.report(app, findings)
        }

        private fun sourceFindings(app: AppSecurityInfo): List<Finding> =
            if (app.source == InstallSource.SIDELOADED) {
                listOf(Finding(FindingCode.SIDELOADED, RiskLevel.MEDIUM, app.installer ?: "unknown"))
            } else {
                emptyList()
            }

        private fun componentFindings(app: AppSecurityInfo): List<Finding> {
            val findings = mutableListOf<Finding>()
            val info =
                runCatching {
                    @Suppress("DEPRECATION")
                    packageManager.getPackageInfo(
                        app.packageName,
                        PackageManager.GET_ACTIVITIES or PackageManager.GET_SERVICES or PackageManager.GET_RECEIVERS,
                    )
                }.getOrNull() ?: return findings.toList()

            val exportedActivities = info.activities?.count { activity -> activity.exported } ?: 0
            val exportedServices = info.services?.count { service -> service.exported } ?: 0
            val exportedReceivers = info.receivers?.count { receiver -> receiver.exported } ?: 0
            val exported = exportedActivities + exportedServices + exportedReceivers
            if (exported > 0) {
                findings += Finding(FindingCode.EXPORTED_COMPONENT, if (exported > 3) RiskLevel.HIGH else RiskLevel.MEDIUM, exported.toString())
            }

            if (hasService(app.packageName, ACCESSIBILITY_SERVICE)) {
                findings += Finding(FindingCode.ACCESSIBILITY, RiskLevel.HIGH)
            }
            if (hasService(app.packageName, NOTIFICATION_LISTENER_SERVICE)) {
                findings += Finding(FindingCode.NOTIFICATION_LISTENER, RiskLevel.HIGH)
            }

            val devicePolicy = context.getSystemService(DevicePolicyManager::class.java)
            if (devicePolicy?.activeAdmins?.any { it.packageName == app.packageName } == true) {
                findings += Finding(FindingCode.DEVICE_ADMIN, RiskLevel.HIGH)
            }

            if (!app.isSystem && packageManager.getLaunchIntentForPackage(app.packageName) == null) {
                findings += Finding(FindingCode.HIDDEN_LAUNCHER, RiskLevel.MEDIUM)
            }
            return findings
        }

        private fun hasService(
            packageName: String,
            action: String,
        ): Boolean =
            runCatching {
                packageManager
                    .queryIntentServices(Intent(action), PackageManager.MATCH_ALL)
                    .any { it.serviceInfo?.packageName == packageName }
            }.getOrDefault(false)

        private fun installerOf(packageName: String): String? =
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    packageManager.getInstallSourceInfo(packageName).installingPackageName
                } else {
                    @Suppress("DEPRECATION")
                    packageManager.getInstallerPackageName(packageName)
                }
            }.getOrNull()

        private fun encodeReport(report: AppRiskReport): JSONObject =
            JSONObject().apply {
                put("package", report.app.packageName)
                put("label", report.app.label)
                put("uid", report.app.uid)
                put("version", report.app.versionName)
                put("system", report.app.isSystem)
                put("source", report.app.source.name)
                put("installer", report.app.installer ?: JSONObject.NULL)
                put("installed", report.app.firstInstallTimeMillis)
                put("updated", report.app.lastUpdateTimeMillis)
                put("score", report.score)
                put("level", report.level.name)
                put("permissions", JSONArray(report.app.permissions))
                put(
                    "findings",
                    JSONArray().apply {
                        report.findings.forEach { finding ->
                            put(
                                JSONObject().apply {
                                    put("code", finding.code.name)
                                    put("severity", finding.severity.name)
                                    put("detail", finding.detail ?: JSONObject.NULL)
                                },
                            )
                        }
                    },
                )
            }

        private fun decodeReport(json: JSONObject): AppRiskReport {
            val permissions = mutableListOf<String>()
            json.optJSONArray("permissions")?.let { array ->
                for (index in 0 until array.length()) permissions += array.getString(index)
            }
            val findings = mutableListOf<Finding>()
            json.optJSONArray("findings")?.let { array ->
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    findings +=
                        Finding(
                            code = runCatching { FindingCode.valueOf(item.getString("code")) }.getOrDefault(FindingCode.SIGNATURE_RULE),
                            severity = runCatching { RiskLevel.valueOf(item.getString("severity")) }.getOrDefault(RiskLevel.LOW),
                            detail = if (item.isNull("detail")) null else item.optString("detail"),
                        )
                }
            }
            val app =
                AppSecurityInfo(
                    packageName = json.getString("package"),
                    label = json.optString("label", json.getString("package")),
                    uid = json.optInt("uid", -1),
                    versionName = json.optString("version", ""),
                    isSystem = json.optBoolean("system", false),
                    source = runCatching { InstallSource.valueOf(json.optString("source")) }.getOrDefault(InstallSource.UNKNOWN),
                    installer = if (json.isNull("installer")) null else json.optString("installer"),
                    firstInstallTimeMillis = json.optLong("installed", 0L),
                    lastUpdateTimeMillis = json.optLong("updated", 0L),
                    permissions = permissions,
                )
            return AppRiskReport(
                app = app,
                score = json.optInt("score", 100),
                level = runCatching { RiskLevel.valueOf(json.optString("level")) }.getOrDefault(RiskLevel.SAFE),
                findings = findings,
            )
        }

        private fun encodeSummary(summary: ScanSummary): JSONObject =
            JSONObject().apply {
                put("started", summary.startedAtMillis)
                put("finished", summary.finishedAtMillis)
                put("scanned", summary.scannedApps)
                put("highRisk", summary.highRiskApps)
                put("findings", summary.findingsCount)
                put("rules", summary.rulesVersion)
            }

        private fun decodeSummary(json: JSONObject): ScanSummary =
            ScanSummary(
                startedAtMillis = json.optLong("started"),
                finishedAtMillis = json.optLong("finished"),
                scannedApps = json.optInt("scanned"),
                highRiskApps = json.optInt("highRisk"),
                findingsCount = json.optInt("findings"),
                rulesVersion = json.optInt("rules"),
            )

        private object Keys {
            val LAST_SCAN = stringPreferencesKey("security_last_scan")
            val REPORTS = stringPreferencesKey("security_scan_reports")
        }

        private companion object {
            const val PLAY_STORE: String = "com.android.vending"
            const val ACCESSIBILITY_SERVICE: String = "android.accessibilityservice.AccessibilityService"
            const val NOTIFICATION_LISTENER_SERVICE: String = "android.service.notification.NotificationListenerService"
            const val MAX_STORED_REPORTS: Int = 80
        }
    }
