package com.example.consentgatinglab.gate

import com.example.consentgatinglab.analytics.AnalyticsController
import com.example.consentgatinglab.appsflyer.AfConsentMapper
import com.example.consentgatinglab.core.ConsentSnapshot
import com.example.consentgatinglab.core.ConsentType
import com.example.consentgatinglab.ump.UmpSnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber

class AfCoordinator(
    private val scope: CoroutineScope,
    private val consentFlow: Flow<ConsentSnapshot>,
    private val umpFlow: Flow<UmpSnapshot>,
    private val isGdprSubject: () -> Boolean,
    private val controller: AnalyticsController,
    private val requiredConsent: Set<ConsentType>
) {
    fun start() {
        scope.launch {
            combine(
                consentFlow.map { it.granted }.distinctUntilChanged(),
                umpFlow.distinctUntilChanged()
            ) { granted, ump ->
                AfConsentMapper.effective(
                    granted = granted,
                    required = requiredConsent,
                    gdprSubject = isGdprSubject(),
                    umpReady = ump.ready,
                    hasTcf = ump.hasTcf
                )
            }.collectLatest { eff ->
                Timber.i("📊 Consent decision: allowAF=${eff.allowAppsFlyer}")
                if (eff.allowAppsFlyer) {
                    controller.onConsentGranted()
                } else {
                    controller.onConsentRevoked()
                }
            }
        }
    }
}
