package com.example.consentgatinglab.diag

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class Event(
    val ts: Long = System.currentTimeMillis(),
    val tag: String,
    val msg: String
)

class EventBus(private val capacity: Int = 200) {
    private val buf = ArrayDeque<Event>()
    private val _flow = MutableStateFlow<List<Event>>(emptyList())
    val flow: StateFlow<List<Event>> = _flow

    fun post(tag: String, msg: String) {
        synchronized(buf) {
            if (buf.size >= capacity) {
                buf.removeFirst()
            }
            buf.addLast(Event(tag = tag, msg = msg))
            _flow.value = buf.toList().asReversed()
        }
    }
}
