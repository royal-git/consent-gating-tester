package com.example.consentgatinglab.analytics

import timber.log.Timber

/**
 * No-op sink used when consent is denied.
 * Logs attempts but does NOT call any vendor SDKs.
 */
object NoopSink : AnalyticsSink {
    override fun logEvent(name: String, props: Map<String, Any?>) {
        Timber.w("🚫 BLOCKED: logEvent($name) - no consent")
        Timber.w("  ↳ Event would collect device data if we called AppsFlyer")
    }

    override fun setUserId(userId: String) {
        Timber.w("🚫 BLOCKED: setUserId($userId) - no consent")
    }

    override fun logRevenue(revenue: String, currency: String, props: Map<String, Any?>) {
        Timber.w("🚫 BLOCKED: logRevenue($revenue $currency) - no consent")
    }
}
