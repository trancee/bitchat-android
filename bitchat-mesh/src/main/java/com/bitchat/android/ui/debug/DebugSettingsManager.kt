package com.bitchat.android.ui.debug

import com.bitchat.android.protocol.BitchatPacket
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Lightweight debug settings manager for library builds.
 */
class DebugSettingsManager private constructor() {
    val gattServerEnabled = MutableStateFlow(true)
    val gattClientEnabled = MutableStateFlow(true)
    val packetRelayEnabled = MutableStateFlow(true)
    val maxConnectionsOverall = MutableStateFlow(8)
    val maxServerConnections = MutableStateFlow(8)
    val maxClientConnections = MutableStateFlow(8)

    companion object {
        @Volatile
        private var INSTANCE: DebugSettingsManager? = null

        fun getInstance(): DebugSettingsManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DebugSettingsManager().also { INSTANCE = it }
            }
        }
    }

    fun setNicknameResolver(resolver: (String) -> String?) {
        // Intentionally no-op in library builds.
    }

    fun logOutgoing(
        packetType: String,
        toPeerID: String?,
        toNickname: String?,
        toDeviceAddress: String,
        previousHopPeerID: String?,
        packetVersion: UByte,
        routeInfo: String?
    ) {
        // Intentionally no-op in library builds.
    }

    fun logPacketRelayDetailed(
        packetType: String,
        senderPeerID: String,
        senderNickname: String?,
        fromPeerID: String?,
        fromNickname: String?,
        fromDeviceAddress: String?,
        toPeerID: String?,
        toNickname: String?,
        toDeviceAddress: String,
        ttl: UByte,
        isRelay: Boolean,
        packetVersion: UByte,
        routeInfo: String?
    ) {
        // Intentionally no-op in library builds.
    }

    fun logIncoming(
        packet: BitchatPacket,
        fromPeerID: String,
        fromNickname: String?,
        fromDeviceAddress: String?,
        myPeerID: String
    ) {
        // Intentionally no-op in library builds.
    }

    fun logIncomingPacket(
        peerID: String,
        nickname: String?,
        messageType: String,
        relayAddress: String?
    ) {
        // Intentionally no-op in library builds.
    }

    fun logPeerConnection(peerID: String, nickname: String, deviceAddress: String, isInbound: Boolean) {
        // Intentionally no-op in library builds.
    }

    fun logPeerDisconnection(peerID: String, nickname: String, deviceAddress: String) {
        // Intentionally no-op in library builds.
    }

    fun addScanResult(result: DebugScanResult) {
        // Intentionally no-op in library builds.
    }
}
