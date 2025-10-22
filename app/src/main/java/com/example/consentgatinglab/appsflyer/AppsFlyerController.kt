package com.example.consentgatinglab.appsflyer

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

class AppsFlyerController(
    private val app: Application,
    private val devKey: String
) {
    @Volatile
    private var bootstrapped = false

    private val _isStarted = MutableStateFlow(false)
    val isStarted: StateFlow<Boolean> = _isStarted

    // Listener to track attribution callbacks - proves AF is receiving data
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

    suspend fun bootstrap(): InitResult = withContext(Dispatchers.Default) {
        if (bootstrapped) return@withContext InitResult.Success
        return@withContext runCatching {
            val af = AppsFlyerLib.getInstance()
            af.setDebugLog(true)

            // Enable TCF data collection from CMP
            af.enableTCFDataCollection(true)
            Timber.i("AF enabled TCF data collection")

            // Register listeners BEFORE init to catch all callbacks
            Timber.i("AF registering attribution & deep link listeners")
            af.init(devKey, conversionListener, app)
            af.subscribeForDeepLink(deepLinkListener)

            bootstrapped = true
            Timber.i("AF bootstrap ok - listeners active, TCF enabled, will log all data flow")
            InitResult.Success
        }.getOrElse { e ->
            Timber.e(e, "AF bootstrap failed")
            InitResult.Failure(e)
        }
    }

    /**
     * Set AppsFlyer consent using their native API (SDK 6.17.4).
     * This tests whether AF respects their own consent framework.
     *
     * The key test: Does setConsentData() actually prevent data collection when denied?
     */
    suspend fun setConsent(
        isGdprSubject: Boolean,
        hasDataUsageConsent: Boolean,
        hasAdPersonalizationConsent: Boolean
    ): InitResult = withContext(Dispatchers.Default) {
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

    suspend fun startIfAllowed(allow: Boolean): InitResult = withContext(Dispatchers.Default) {
        if (!allow) {
            Timber.d("AF start blocked: allow=false")
            return@withContext InitResult.Success
        }
        if (_isStarted.value) {
            Timber.d("AF already started")
            return@withContext InitResult.Success
        }
        return@withContext runCatching {
            val t0 = System.nanoTime()
            AppsFlyerLib.getInstance().start(app)
            _isStarted.value = true
            val latency = (System.nanoTime() - t0) / 1_000_000
            Timber.w("✅ AF STARTED in %d ms - data flow now ALLOWED", latency)
            Timber.w("  ↳ Watch for 🔴 AF DATA FLOW logs to confirm traffic")
            InitResult.Success
        }.getOrElse { e ->
            Timber.e(e, "AF start failed")
            InitResult.Failure(e)
        }
    }

    suspend fun stop(): InitResult = withContext(Dispatchers.Default) {
        if (!_isStarted.value) {
            Timber.d("AF already stopped")
            return@withContext InitResult.Success
        }
        return@withContext runCatching {
            val t0 = System.nanoTime()
            AppsFlyerLib.getInstance().stop(true, app)
            _isStarted.value = false
            val latency = (System.nanoTime() - t0) / 1_000_000
            Timber.w("🛑 AF STOPPED in %d ms - data flow now BLOCKED", latency)
            Timber.w("  ↳ Any 🔴 AF DATA FLOW logs = TRIPWIRE violation!")
            InitResult.Success
        }.getOrElse { e ->
            Timber.e(e, "AF stop failed")
            InitResult.Failure(e)
        }
    }

    /**
     * Test methods to validate AppsFlyer's consent/stop behavior.
     * These call AppsFlyer APIs unconditionally to test whether AF itself
     * respects consent settings and stop() state.
     *
     * Watch logs for:
     * - "preparing data" = AF is processing the event
     * - "CACHE: caching request" = AF is queuing for network
     * - "QUEUE: new task added" = AF will send when network is allowed
     */
    suspend fun logTestEvent(eventName: String = "test_event"): InitResult = withContext(Dispatchers.Default) {
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

    suspend fun setTestUserId(userId: String = "test_user_${System.currentTimeMillis()}"): InitResult = withContext(Dispatchers.Default) {
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

    suspend fun logTestRevenue(): InitResult = withContext(Dispatchers.Default) {
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

    /**
     * Check if AppsFlyer has queued any requests in its cache.
     * This detects if AF is collecting/queueing data even when stopped.
     */
    suspend fun checkCachedRequests(): String = withContext(Dispatchers.Default) {
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
}
