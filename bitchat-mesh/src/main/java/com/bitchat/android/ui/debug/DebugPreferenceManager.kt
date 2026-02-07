package com.bitchat.android.ui.debug

/**
 * No-op debug preference helpers for library builds.
 */
object DebugPreferenceManager {
    fun getSeenPacketCapacity(defaultValue: Int): Int = defaultValue
    fun getGcsMaxFilterBytes(defaultValue: Int): Int = defaultValue
    fun getGcsFprPercent(defaultValue: Double): Double = defaultValue
}
