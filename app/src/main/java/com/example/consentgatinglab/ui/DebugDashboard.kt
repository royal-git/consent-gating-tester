package com.example.consentgatinglab.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.consentgatinglab.analytics.AnalyticsController
import com.example.consentgatinglab.core.ConsentManager
import com.example.consentgatinglab.core.ConsentSnapshot
import com.example.consentgatinglab.core.ConsentType
import com.example.consentgatinglab.ump.UmpStateSource
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugDashboard(
    consent: ConsentManager,
    ump: UmpStateSource,
    controller: AnalyticsController
) {
    val scope = rememberCoroutineScope()
    val consentState by consent.flow.collectAsState(
        initial = ConsentSnapshot(null, emptySet())
    )

    var analytics by remember { mutableStateOf(false) }
    var marketing by remember { mutableStateOf(false) }
    var personalization by remember { mutableStateOf(false) }

    // Sync switches with persistent consent state
    LaunchedEffect(consentState) {
        analytics = consentState.granted.contains(ConsentType.ANALYTICS)
        marketing = consentState.granted.contains(ConsentType.MARKETING)
        personalization = consentState.granted.contains(ConsentType.PERSONALIZATION)
    }

    val umpSnap by ump.flow.collectAsState()
    var gdpr by remember { mutableStateOf(false) } // simple toggle for demo

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("ConsentGatingLab") })
        }
    ) { pad ->
        Column(
            Modifier
                .padding(pad)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("consent toggles", style = MaterialTheme.typography.titleMedium)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = analytics, onCheckedChange = { analytics = it })
                Spacer(Modifier.width(8.dp))
                Text("analytics")
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = marketing, onCheckedChange = { marketing = it })
                Spacer(Modifier.width(8.dp))
                Text("marketing")
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = personalization, onCheckedChange = { personalization = it })
                Spacer(Modifier.width(8.dp))
                Text("personalization")
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    scope.launch {
                        val set = buildSet {
                            if (analytics) add(ConsentType.ANALYTICS)
                            if (marketing) add(ConsentType.MARKETING)
                            if (personalization) add(ConsentType.PERSONALIZATION)
                        }
                        consent.update(set, null)
                    }
                }) {
                    Text("apply app consent")
                }
                Button(onClick = {
                    scope.launch {
                        consent.update(emptySet(), null)
                    }
                }) {
                    Text("clear all")
                }
            }

            HorizontalDivider()

            Text("ump (simulated)", style = MaterialTheme.typography.titleMedium)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = umpSnap.ready,
                    onCheckedChange = { ump.update(umpSnap.copy(ready = it)) }
                )
                Spacer(Modifier.width(8.dp))
                Text("ump ready")
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = umpSnap.hasTcf,
                    onCheckedChange = { ump.update(umpSnap.copy(hasTcf = it)) }
                )
                Spacer(Modifier.width(8.dp))
                Text("has TCF string")
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = gdpr, onCheckedChange = { gdpr = it })
                Spacer(Modifier.width(8.dp))
                Text("gdpr subject (app)")
            }

            HorizontalDivider()

            val isStarted by controller.isStarted.collectAsState()
            Text("appsFlyer SDK", style = MaterialTheme.typography.titleMedium)
            Text("isStarted: $isStarted", style = MaterialTheme.typography.bodyLarge)

            Text("AppsFlyer Consent (their API)", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    scope.launch {
                        // Grant all consent for GDPR user
                        controller.setConsent(
                            isGdprSubject = true,
                            hasDataUsageConsent = true,
                            hasAdPersonalizationConsent = true
                        )
                    }
                }) {
                    Text("AF: grant all")
                }
                Button(onClick = {
                    scope.launch {
                        // Deny all consent for GDPR user
                        controller.setConsent(
                            isGdprSubject = true,
                            hasDataUsageConsent = false,
                            hasAdPersonalizationConsent = false
                        )
                    }
                }) {
                    Text("AF: deny all")
                }
            }

            Spacer(Modifier.height(8.dp))
            Text("SDK Lifecycle", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    scope.launch { controller.stop() }
                }) {
                    Text("stop()")
                }
                Button(onClick = {
                    scope.launch { controller.startIfAllowed(true) }
                }) {
                    Text("start()")
                }
            }

            Spacer(Modifier.height(12.dp))
            Text("Test Scenarios", style = MaterialTheme.typography.titleMedium)
            Text("Call AF APIs and watch Logcat for 'preparing data', 'CACHE', 'QUEUE'",
                style = MaterialTheme.typography.bodySmall)

            var cacheStatus by remember { mutableStateOf("Not checked") }

            Button(
                onClick = {
                    scope.launch {
                        cacheStatus = controller.checkCachedRequests()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Check AF cache for queued requests")
            }
            Text(cacheStatus, style = MaterialTheme.typography.bodySmall)

            Spacer(Modifier.height(8.dp))
            Text("Scenario 1: AF stopped + log event", style = MaterialTheme.typography.titleSmall)
            Button(
                onClick = {
                    scope.launch {
                        controller.stop()
                        kotlinx.coroutines.delay(500)
                        controller.logTestEvent("scenario_1_stopped")
                        kotlinx.coroutines.delay(500)
                        cacheStatus = controller.checkCachedRequests()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Run: stop() → logEvent() → check cache")
            }

            Spacer(Modifier.height(8.dp))
            Text("Scenario 2: AF consent denied + log event", style = MaterialTheme.typography.titleSmall)
            Button(
                onClick = {
                    scope.launch {
                        controller.setConsent(true, false, false)
                        kotlinx.coroutines.delay(500)
                        controller.logTestEvent("scenario_2_consent_denied")
                        kotlinx.coroutines.delay(500)
                        cacheStatus = controller.checkCachedRequests()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Run: deny consent → logEvent() → check cache")
            }

            Spacer(Modifier.height(8.dp))
            Text("Scenario 3: AF consent granted + started + log event", style = MaterialTheme.typography.titleSmall)
            Button(
                onClick = {
                    scope.launch {
                        controller.setConsent(true, true, true)
                        kotlinx.coroutines.delay(500)
                        controller.startIfAllowed(true)
                        kotlinx.coroutines.delay(500)
                        controller.logTestEvent("scenario_3_granted")
                        kotlinx.coroutines.delay(500)
                        cacheStatus = controller.checkCachedRequests()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Run: grant consent → start() → logEvent()")
            }

            Spacer(Modifier.height(8.dp))
            Text("Individual API Calls", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    scope.launch { controller.logTestEvent() }
                }) {
                    Text("log event")
                }
                Button(onClick = {
                    scope.launch { controller.logTestRevenue() }
                }) {
                    Text("log revenue")
                }
            }

            Button(onClick = {
                scope.launch { controller.setTestUserId() }
            }) {
                Text("set user ID")
            }

            Spacer(Modifier.height(12.dp))
            Text("events", style = MaterialTheme.typography.titleMedium)
            Text("see Logcat (Timber) for full trace, start/stop latency, consent diffs",
                style = MaterialTheme.typography.bodySmall)
        }
    }
}
