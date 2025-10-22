package com.example.consentgatinglab.analytics

/**
 * Abstraction for analytics events.
 * Implementations: NoopSink (consent denied) or AppsFlyerSink (consent granted).
 */
interface AnalyticsSink {
    fun logEvent(name: String, props: Map<String, Any?> = emptyMap())
    fun setUserId(userId: String)
    fun logRevenue(revenue: String, currency: String, props: Map<String, Any?> = emptyMap())
}
