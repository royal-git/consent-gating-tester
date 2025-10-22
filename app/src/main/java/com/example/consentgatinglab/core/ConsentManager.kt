package com.example.consentgatinglab.core

import kotlinx.coroutines.flow.StateFlow

interface ConsentManager {
    val flow: StateFlow<ConsentSnapshot>
    suspend fun current(): ConsentSnapshot
    suspend fun update(granted: Set<ConsentType>, userId: String?)
}
