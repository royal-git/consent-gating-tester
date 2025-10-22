package com.example.consentgatinglab.analytics

import android.app.Application
import com.appsflyer.AppsFlyerConsent
import com.appsflyer.AppsFlyerConversionListener
import com.appsflyer.AppsFlyerLib
import com.appsflyer.deeplink.DeepLinkListener
import com.appsflyer.deeplink.DeepLinkResult
import com.example.consentgatinglab.core.InitResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
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
    private val dispatcher = Dispatchers.Default
    @Volatile
    private var bootstrapped = false

    private val _sink = AtomicReference<AnalyticsSink>(NoopSink)
    private val _isStarted = MutableStateFlow(false)

    val sink: AnalyticsSink get() = _sink.get()
    val isStarted: StateFlow<Boolean> = _isStarted

    // Instrumentation listeners to detect data flow
    private val conversionListener = object : AppsFlyerConversionListener {
        override fun onConversionDataSuccess(data: MutableMap<String, Any>?) {
            val ts = System.currentTimeMillis()
            Timber.w("🔴 AF DATA FLOW: onConversionDataSuccess at $ts")
            Timber.w("  ↳ isStarted=${_isStarted.value}, data keys: ${data?.keys}")
            if (!_isStarted.value) {
                Timber.e("⚠️ TRIPWIRE: AppsFlyer received attribution data while STOPPED!")
            }
        }

        override fun onConversionDataFail(error: String?) {
            val ts = System.currentTimeMillis()
            Timber.w("🔴 AF DATA FLOW: onConversionDataFail at $ts: $error")
        }

        override fun onAppOpenAttribution(data: MutableMap<String, String>?) {
            val ts = System.currentTimeMillis()
            Timber.w("🔴 AF DATA FLOW: onAppOpenAttribution at $ts")
            Timber.w("  ↳ isStarted=${_isStarted.value}, data keys: ${data?.keys}")
            if (!_isStarted.value) {
                Timber.e("⚠️ TRIPWIRE: AppsFlyer app open attribution while STOPPED!")
            }
        }

        override fun onAttributionFailure(error: String?) {
            val ts = System.currentTimeMillis()
            Timber.w("🔴 AF DATA FLOW: onAttributionFailure at $ts: $error")
        }
    }

    // Listener to track deep link handling - proves AF is processing data
    private val deepLinkListener = DeepLinkListener { result ->
        val ts = System.currentTimeMillis()
        when (result.status) {
            DeepLinkResult.Status.FOUND -> {
                Timber.w("🔴 AF DATA FLOW: DeepLink FOUND at $ts")
                Timber.w("  ↳ isStarted=${_isStarted.value}, deepLink: ${result.deepLink}")
                if (!_isStarted.value) {
                    Timber.e("⚠️ TRIPWIRE: AppsFlyer processed deep link while STOPPED!")
                }
            }
            DeepLinkResult.Status.NOT_FOUND -> {
                Timber.w("🔴 AF DATA FLOW: DeepLink NOT_FOUND at $ts")
            }
            DeepLinkResult.Status.ERROR -> {
                Timber.w("🔴 AF DATA FLOW: DeepLink ERROR at $ts: ${result.error}")
            }
            else -> {
                Timber.w("🔴 AF DATA FLOW: DeepLink status=${result.status} at $ts")
            }
        }
    }

    suspend fun onConsentGranted() = withContext(dispatcher) {
        runCatching { performStart() }
            .onFailure { Timber.e(it, "❌ Failed to initialize AppsFlyer") }
    }

    suspend fun onConsentRevoked() = withContext(dispatcher) {
        runCatching { performStop(purge = true) }
            .onFailure { Timber.e(it, "❌ Failed to stop AppsFlyer cleanly") }
    }

    suspend fun startIfAllowed(allow: Boolean): InitResult = withContext(dispatcher) {
        if (!allow) {
            Timber.d("AF start blocked: allow=false")
            return@withContext InitResult.Success
        }

        return@withContext runCatching {
            performStart()
            InitResult.Success
        }.getOrElse { e ->
            Timber.e(e, "AF start failed")
            InitResult.Failure(e)
        }
    }

    suspend fun stop(): InitResult = withContext(dispatcher) {
        return@withContext runCatching {
            performStop(purge = false)
            InitResult.Success
        }.getOrElse { e ->
            Timber.e(e, "AF stop failed")
            InitResult.Failure(e)
        }
    }

    suspend fun setConsent(
        isGdprSubject: Boolean,
        hasDataUsageConsent: Boolean,
        hasAdPersonalizationConsent: Boolean
    ): InitResult = withContext(dispatcher) {
        return@withContext runCatching {
            val consent = AppsFlyerConsent(
                isGdprSubject,
                hasDataUsageConsent,
                hasAdPersonalizationConsent
            )

            AppsFlyerLib.getInstance().setConsentData(consent)
            Timber.w("📋 AF Consent set: GDPR=$isGdprSubject, dataUsage=$hasDataUsageConsent, adPersonalization=$hasAdPersonalizationConsent")
            Timber.w("  ↳ AppsFlyer should now respect this consent setting")
            Timber.w("  ↳ Test: log events and check for 'preparing data', 'CACHE', 'QUEUE' logs")
            InitResult.Success
        }.getOrElse { e ->
            Timber.e(e, "AF setConsent failed")
            InitResult.Failure(e)
        }
    }

    suspend fun logTestEvent(eventName: String = "test_event"): InitResult = withContext(dispatcher) {
        return@withContext runCatching {
            val ts = System.currentTimeMillis()
            Timber.w("📤 logEvent: $eventName at $ts (AF isStarted=${_isStarted.value})")

            val params = mapOf(
                "test_param" to "test_value",
                "timestamp" to ts.toString()
            )

            AppsFlyerLib.getInstance().logEvent(app, eventName, params)
            Timber.w("  ↳ Called AF logEvent - watch for 'preparing data', 'CACHE', 'QUEUE' logs")
            Timber.w("  ↳ If AF respects stop(), no caching should occur")
            InitResult.Success
        }.getOrElse { e ->
            Timber.e(e, "logTestEvent failed")
            InitResult.Failure(e)
        }
    }

    suspend fun setTestUserId(userId: String = "test_user_${System.currentTimeMillis()}"): InitResult =
        withContext(dispatcher) {
            return@withContext runCatching {
                Timber.w("📤 setCustomerUserId: $userId (AF isStarted=${_isStarted.value})")

                AppsFlyerLib.getInstance().setCustomerUserId(userId)
                Timber.w("  ↳ Called AF setCustomerUserId - check if data is prepared/cached")
                InitResult.Success
            }.getOrElse { e ->
                Timber.e(e, "setTestUserId failed")
                InitResult.Failure(e)
            }
        }

    suspend fun logTestRevenue(): InitResult = withContext(dispatcher) {
        return@withContext runCatching {
            val ts = System.currentTimeMillis()
            Timber.w("📤 logEvent: af_purchase at $ts (AF isStarted=${_isStarted.value})")

            val params = mapOf(
                "af_revenue" to "9.99",
                "af_currency" to "USD",
                "af_content_type" to "test_purchase",
                "timestamp" to ts.toString()
            )

            AppsFlyerLib.getInstance().logEvent(app, "af_purchase", params)
            Timber.w("  ↳ Called AF logEvent(af_purchase) - watch for data preparation/caching")
            InitResult.Success
        }.getOrElse { e ->
            Timber.e(e, "logTestRevenue failed")
            InitResult.Failure(e)
        }
    }

    suspend fun checkCachedRequests(): String = withContext(dispatcher) {
        try {
            val cacheDir = app.cacheDir
            val afCacheFiles = cacheDir.listFiles { file ->
                file.name.contains("appsflyer", ignoreCase = true) ||
                    file.name.contains("AF_", ignoreCase = false)
            }?.toList() ?: emptyList()

            val afFilesDir = app.filesDir
            val afStorageFiles = afFilesDir.listFiles { file ->
                file.name.contains("appsflyer", ignoreCase = true) ||
                    file.name.contains("AF_", ignoreCase = false)
            }?.toList() ?: emptyList()

            val allAfFiles = afCacheFiles + afStorageFiles

            if (allAfFiles.isEmpty()) {
                Timber.i("📦 No AppsFlyer cache files found")
                "No AF cache files"
            } else {
                val summary = allAfFiles.joinToString("\n") { file ->
                    "  • ${file.name} (${file.length()} bytes, modified ${file.lastModified()})"
                }
                Timber.w("📦 AppsFlyer cache files found:\n$summary")
                "Found ${allAfFiles.size} AF cache files:\n$summary"
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to check AF cache")
            "Error checking cache: ${e.message}"
        }
    }

    // Convenience methods that delegate to sink when running through gated APIs
    fun logEvent(name: String, props: Map<String, Any?> = emptyMap()) {
        sink.logEvent(name, props)
    }

    fun setUserId(userId: String) {
        sink.setUserId(userId)
    }

    fun logRevenue(revenue: String, currency: String, props: Map<String, Any?> = emptyMap()) {
        sink.logRevenue(revenue, currency, props)
    }

    private fun performStart() {
        if (_isStarted.value) {
            Timber.i("✅ Already started, ignoring")
            return
        }

        Timber.w("✅ CONSENT GRANTED - Initializing AppsFlyer SDK")
        bootstrapIfNeeded()

        val af = AppsFlyerLib.getInstance()

        // Explicitly allow SDK to run (undo any previous stop)
        af.stop(false, app)

        // Start the SDK
        af.start(app)

        // Switch to real sink
        _sink.set(AppsFlyerSink(app))
        _isStarted.value = true

        Timber.w("✅ AppsFlyer initialized and started")
        Timber.w("  ↳ Watch for 🔴 AF DATA FLOW logs to confirm activity")
    }

    private fun performStop(purge: Boolean) {
        if (!_isStarted.value) {
            Timber.i("🛑 Already stopped, ignoring")
            return
        }

        if (purge) {
            Timber.w("🛑 CONSENT REVOKED - Stopping AppsFlyer and purging state")
        } else {
            Timber.w("🛑 Stopping AppsFlyer (manual)")
        }

        AppsFlyerLib.getInstance().stop(true, app)

        // Switch to noop sink BEFORE purging
        _sink.set(NoopSink)
        _isStarted.value = false

        if (purge) {
            purgeAppsFlyerState(app)
            Timber.w("🛑 AppsFlyer stopped and state purged")
        } else {
            Timber.w("🛑 AppsFlyer stopped")
        }
    }

    private fun bootstrapIfNeeded() {
        if (bootstrapped) return

        val af = AppsFlyerLib.getInstance()
        af.setDebugLog(true)
        af.enableTCFDataCollection(true)
        Timber.i("AF enabled TCF data collection")

        Timber.i("AF registering attribution & deep link listeners")
        af.init(devKey, conversionListener, app)
        af.subscribeForDeepLink(deepLinkListener)

        bootstrapped = true
        Timber.i("AF bootstrap ok - listeners active, TCF enabled, will log all data flow")
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
}
