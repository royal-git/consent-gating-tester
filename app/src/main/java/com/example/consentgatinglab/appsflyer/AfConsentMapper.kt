package com.example.consentgatinglab.appsflyer

import com.example.consentgatinglab.core.ConsentType

data class Effective(
    val allowAppsFlyer: Boolean
)

object AfConsentMapper {
    fun effective(
        granted: Set<ConsentType>,
        required: Set<ConsentType>,
        gdprSubject: Boolean,
        umpReady: Boolean,
        hasTcf: Boolean
    ): Effective {
        val underGdpr = gdprSubject
        val cmpAllows = !underGdpr || (umpReady && hasTcf)

        val allowsByPolicy = required.all { it in granted }
        val allow = allowsByPolicy && cmpAllows

        return Effective(allow)
    }
}
