package com.example.consentgatinglab.appsflyer

import com.example.consentgatinglab.core.ConsentType

data class Effective(
    val allowAppsFlyer: Boolean
)

object AfConsentMapper {
    fun effective(
        granted: Set<ConsentType>,
        gdprSubject: Boolean,
        umpReady: Boolean,
        hasTcf: Boolean
    ): Effective {
        val underGdpr = gdprSubject
        val cmpAllows = !underGdpr || (umpReady && hasTcf)

        // AppsFlyer requires both ANALYTICS and MARKETING consent
        val allow = (ConsentType.ANALYTICS in granted) &&
                    (ConsentType.MARKETING in granted) &&
                    cmpAllows

        return Effective(allow)
    }
}
