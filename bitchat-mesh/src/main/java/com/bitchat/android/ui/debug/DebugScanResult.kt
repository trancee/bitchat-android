package com.bitchat.android.ui.debug

/**
 * Minimal scan result payload for debug hooks.
 */
data class DebugScanResult(
    val deviceName: String?,
    val deviceAddress: String,
    val rssi: Int,
    val peerID: String?
)
