package com.example.consentgatinglab.analytics

import android.app.Application
import com.appsflyer.AppsFlyerConversionListener
import com.appsflyer.AppsFlyerLib
import com.appsflyer.deeplink.DeepLinkListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber
import java.io.File
import java.util.concurrent.atomic.AtomicReference

/**
 * Consent-gated analytics controller.
 *
 * KEY PRINCIPLE: AppsFlyer SDK is NOT initialized until consent is granted.
 * When consent is revoked, we purge all cached state to prevent backfill.
 *
 * All app code calls methods on this controller, which delegates to the
 * current sink (noop or real).
 */
class AnalyticsController(
    private val app: Application,
    private val devKey: String
) {
    private val _sink = AtomicReference<AnalyticsSink>(NoopSink)
    private val _isStarted = MutableStateFlow(false)

    val sink: AnalyticsSink get() = _sink.get()
    val isStarted: StateFlow<Boolean> = _isStarted

    // Instrumentation listeners to detect data flow
    private val conversionListener = object : AppsFlyerConversionListener {
        override fun onConversionDataSuccess(data: MutableMap<String, Any>?) {
            Timber.w("🔴 AF DATA FLOW: onConversionDataSuccess")
            Timber.w("  ↳ isStarted=${_isStarted.value}, data keys: ${data?.keys}")
        }

        override fun onConversionDataFail(error: String?) {
            Timber.w("🔴 AF DATA FLOW: onConversionDataFail: $error")
        }

        override fun onAppOpenAttribution(data: MutableMap<String, String>?) {
            Timber.w("🔴 AF DATA FLOW: onAppOpenAttribution")
        }

        override fun onAttributionFailure(error: String?) {
            Timber.w("🔴 AF DATA FLOW: onAttributionFailure: $error")
        }
    }

    private val deepLinkListener = DeepLinkListener { result ->
        Timber.w("🔴 AF DATA FLOW: DeepLink ${result.status}")
    }

    /**
     * Called when consent is GRANTED.
     * Initializes AppsFlyer SDK and switches to real sink.
     */
    fun onConsentGranted() {
        if (_isStarted.value) {
            Timber.i("✅ Already started, ignoring")
            return
        }

        Timber.w("✅ CONSENT GRANTED - Initializing AppsFlyer SDK")

        try {
            val af = AppsFlyerLib.getInstance()
            af.setDebugLog(true)

            // Enable TCF data collection
            af.enableTCFDataCollection(true)

            // Register listeners for instrumentation
            af.init(devKey, conversionListener, app)
            af.subscribeForDeepLink(deepLinkListener)

            // Explicitly allow SDK to run (undo any previous stop)
            af.stop(false, app)

            // Start the SDK
            af.start(app)

            // Switch to real sink
            _sink.set(AppsFlyerSink(app))
            _isStarted.value = true

            Timber.w("✅ AppsFlyer initialized and started")
            Timber.w("  ↳ Watch for 🔴 AF DATA FLOW logs to confirm activity")
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to initialize AppsFlyer")
        }
    }

    /**
     * Called when consent is REVOKED.
     * Stops SDK, purges cached data, switches to noop sink.
     */
    fun onConsentRevoked() {
        if (!_isStarted.value) {
            Timber.i("🛑 Already stopped, ignoring")
            return
        }

        Timber.w("🛑 CONSENT REVOKED - Stopping AppsFlyer and purging state")

        try {
            // Stop SDK immediately
            AppsFlyerLib.getInstance().stop(true, app)

            // Switch to noop sink BEFORE purging
            _sink.set(NoopSink)
            _isStarted.value = false

            // Purge all cached events so they don't send on re-consent
            purgeAppsFlyerState(app)

            Timber.w("🛑 AppsFlyer stopped and state purged")
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to stop AppsFlyer cleanly")
        }
    }

    /**
     * Purge AppsFlyer's cached request queue and local storage.
     * This prevents queued events from being sent when SDK is restarted.
     */
    private fun purgeAppsFlyerState(app: Application) {
        try {
            var deletedCount = 0

            // Check cache directory
            app.cacheDir.listFiles()?.forEach { file ->
                if (file.name.contains("appsflyer", ignoreCase = true) ||
                    file.name.contains("AF_", ignoreCase = false)) {
                    if (file.deleteRecursively()) {
                        deletedCount++
                        Timber.d("Deleted cache: ${file.name}")
                    }
                }
            }

            // Check files directory
            app.filesDir.listFiles()?.forEach { file ->
                if (file.name.contains("appsflyer", ignoreCase = true) ||
                    file.name.contains("AF_", ignoreCase = false)) {
                    if (file.deleteRecursively()) {
                        deletedCount++
                        Timber.d("Deleted file: ${file.name}")
                    }
                }
            }

            // Check shared preferences
            val prefsDir = File(app.applicationInfo.dataDir, "shared_prefs")
            prefsDir.listFiles()?.forEach { file ->
                if (file.name.contains("appsflyer", ignoreCase = true)) {
                    if (file.delete()) {
                        deletedCount++
                        Timber.d("Deleted prefs: ${file.name}")
                    }
                }
            }

            Timber.w("🗑️  Purged $deletedCount AppsFlyer files")
        } catch (e: Exception) {
            Timber.e(e, "Failed to purge AppsFlyer state")
        }
    }

    // Convenience methods that delegate to sink
    fun logEvent(name: String, props: Map<String, Any?> = emptyMap()) {
        sink.logEvent(name, props)
    }

    fun setUserId(userId: String) {
        sink.setUserId(userId)
    }

    fun logRevenue(revenue: String, currency: String, props: Map<String, Any?> = emptyMap()) {
        sink.logRevenue(revenue, currency, props)
    }
}
