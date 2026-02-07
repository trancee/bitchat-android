package com.permissionless.bitchat.mesh

import com.bitchat.android.model.BitchatMessage

interface MeshListener {
    fun onMessageReceived(message: BitchatMessage)
    fun onPeerListUpdated(peers: List<String>)
    fun onDeliveryAck(messageID: String, recipientPeerID: String)
    fun onReadReceipt(messageID: String, recipientPeerID: String)
    fun onVerifyChallenge(peerID: String, payload: ByteArray, timestampMs: Long)
    fun onVerifyResponse(peerID: String, payload: ByteArray, timestampMs: Long)
}
