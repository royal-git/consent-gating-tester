package com.example.consentgatinglab.core

data class ConsentSnapshot(
    val userId: String?,
    val granted: Set<ConsentType>,
    val tsMillis: Long = System.currentTimeMillis()
)
