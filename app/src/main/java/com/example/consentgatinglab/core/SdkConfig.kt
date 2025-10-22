package com.example.consentgatinglab.core

data class SdkConfig(
    val id: SdkId,
    val requiredConsent: Set<ConsentType>,
    val initOrder: Int,
    val thread: ThreadKind
)
