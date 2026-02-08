package com.permissionless.bitchat.mesh

import com.bitchat.android.model.BitchatMessage

interface MeshListener {
    fun onMessageReceived(message: BitchatMessage)
    fun onReceived(message: BitchatMessage) {}
    fun onSent(messageID: String?, recipientPeerID: String?) {}
    fun onPeerListUpdated(peers: List<String>)
    fun onFound(peerID: String) {}
    fun onLost(peerID: String) {}
    fun onConnected(peerID: String) {}
    fun onDisconnected(peerID: String) {}
    fun onEstablished(peerID: String) {}
    fun onRSSIUpdated(peerID: String, rssi: Int) {}
    fun onStarted() {}
    fun onStopped() {}
    fun onDeliveryAck(messageID: String, recipientPeerID: String)
    fun onReadReceipt(messageID: String, recipientPeerID: String)
    fun onVerifyChallenge(peerID: String, payload: ByteArray, timestampMs: Long)
    fun onVerifyResponse(peerID: String, payload: ByteArray, timestampMs: Long)
}
