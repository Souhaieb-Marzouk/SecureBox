package io.securebox.core.domain

/**
 * Local feature flags. Kept in the domain layer so feature modules can read them without depending
 * on the application module.
 */
public object FeatureFlags {
    /**
     * When true, Settings shows the local-only "Upgrade available" placeholder for a future paid
     * Threat Intelligence tier. There is no backend and no purchase flow yet.
     */
    public const val PAID_TIER_AVAILABLE: Boolean = false
}
