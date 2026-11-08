package com.jcotters.contract.time

fun interface AppClock {
    fun millis(): Long

    companion object {
        val System = AppClock { kotlin.time.Clock.System.now().toEpochMilliseconds() }
    }
}
