package io.securebox.core.domain.security

/** Read-only view of installed apps, their permissions and their risks. */
public interface SecurityRepository {
    /** Every visible installed app with its source and requested permissions. */
    public suspend fun installedApps(): List<AppSecurityInfo>

    /**
     * Fast audit: permissions, installer source and components. Does not read APK bytes, so it is
     * safe to run for every app on a TV box.
     */
    public suspend fun auditAll(onProgress: (done: Int, total: Int) -> Unit = { _, _ -> }): List<AppRiskReport>

    /**
     * Deep scan: audit plus DEX string extraction matched against the bundled signature rules and
     * tracker list. Slower; runs on demand or on the weekly schedule.
     */
    public suspend fun scanAll(onProgress: (done: Int, total: Int) -> Unit = { _, _ -> }): ScanSummary

    public suspend fun lastScan(): ScanSummary?

    public suspend fun storedReports(): List<AppRiskReport>

    public suspend fun storeReports(
        reports: List<AppRiskReport>,
        summary: ScanSummary,
    )

    /** Signature/tracker rules currently loaded, for display in the UI. */
    public suspend fun rulesVersion(): Int
}

