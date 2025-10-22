package com.example.consentgatinglab.core

sealed class InitResult {
    data object Success : InitResult()
    data class Failure(val error: Throwable) : InitResult()
}
