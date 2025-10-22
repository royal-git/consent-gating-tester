package com.example.consentgatinglab.analytics

import android.app.Application
import com.appsflyer.AppsFlyerLib
import timber.log.Timber

/**
 * Real AppsFlyer sink - only created when consent is granted.
 * Wraps all AppsFlyer SDK calls.
 */
class AppsFlyerSink(private val app: Application) : AnalyticsSink {

    override fun logEvent(name: String, props: Map<String, Any?>) {
        Timber.i("📤 AF logEvent: $name")
        AppsFlyerLib.getInstance().logEvent(app, name, props)
    }

    override fun setUserId(userId: String) {
        Timber.i("📤 AF setUserId: $userId")
        AppsFlyerLib.getInstance().setCustomerUserId(userId)
    }

    override fun logRevenue(revenue: String, currency: String, props: Map<String, Any?>) {
        Timber.i("📤 AF logRevenue: $revenue $currency")
        val params = props.toMutableMap().apply {
            put("af_revenue", revenue)
            put("af_currency", currency)
        }
        AppsFlyerLib.getInstance().logEvent(app, "af_purchase", params)
    }
}
