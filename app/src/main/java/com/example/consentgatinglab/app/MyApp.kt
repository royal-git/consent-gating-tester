package com.example.consentgatinglab.app

import android.app.Application
import com.example.consentgatinglab.analytics.AnalyticsController
import com.example.consentgatinglab.consent.DefaultConsentManager
import com.example.consentgatinglab.core.SdkId
import com.example.consentgatinglab.core.SdkRegistry
import com.example.consentgatinglab.gate.AfCoordinator
import com.example.consentgatinglab.ump.UmpStateSource
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.MainScope
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class MyApp : Application() {

    @Inject
    lateinit var consent: DefaultConsentManager

    @Inject
    lateinit var ump: UmpStateSource

    @Inject
    lateinit var analytics: AnalyticsController

    @Inject
    lateinit var sdkRegistry: SdkRegistry

    override fun onCreate() {
        super.onCreate()

        // Always enable logging - this is a dev/debug tool
        Timber.plant(Timber.DebugTree())

        val gdprRegion = {
            // TODO: simple toggle via UI for demo
            false
        }

        val requiredConsent = sdkRegistry.get(SdkId.APPSFLYER)?.requiredConsent.orEmpty()

        if (requiredConsent.isEmpty()) {
            Timber.w("⚠️ No consent requirements defined for APPSFLYER in sdk_policy.json – SDK will start when UMP permits.")
        } else {
            Timber.i("SDK policy loaded for APPSFLYER: requires $requiredConsent")
        }

        AfCoordinator(
            scope = MainScope(),
            consentFlow = consent.flow,
            umpFlow = ump.flow,
            isGdprSubject = gdprRegion,
            controller = analytics,
            requiredConsent = requiredConsent
        ).start()

        // Seed: default deny all (already handled by DefaultConsentManager initial state)
    }
}
