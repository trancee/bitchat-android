package com.permissionless.bitchat.mesh

import android.content.Context
import com.bitchat.android.mesh.BluetoothMeshDelegate
import com.bitchat.android.mesh.BluetoothMeshService
import com.bitchat.android.model.BitchatFilePacket
import com.bitchat.android.model.BitchatMessage
import com.bitchat.android.services.NicknameProvider

class MeshManager(private val context: Context) {
    private var service: BluetoothMeshService? = null
    private var listener: MeshListener? = null
    private var nickname: String? = null

    fun setListener(listener: MeshListener?) {
        this.listener = listener
        service?.delegate = buildDelegate()
    }

    fun start(nickname: String? = null) {
        this.nickname = nickname?.trim()?.ifEmpty { null }
        NicknameProvider.setNickname(this.nickname)
        val mesh = BluetoothMeshService(context.applicationContext)
        mesh.delegate = buildDelegate()
        mesh.startServices()
        service = mesh
    }

    fun stop() {
        service?.stopServices()
        service = null
    }

    fun myPeerId(): String? = service?.myPeerID

    fun isRunning(): Boolean = service?.isReusable() == true

    fun sendBroadcastMessage(content: String, mentions: List<String> = emptyList(), channel: String? = null) {
        service?.sendMessage(content, mentions, channel)
    }

    fun sendPrivateMessage(
        content: String,
        recipientPeerID: String,
        recipientNickname: String,
        messageID: String? = null
    ) {
        service?.sendPrivateMessage(content, recipientPeerID, recipientNickname, messageID)
    }

    fun initiateNoiseHandshake(peerID: String) {
        service?.initiateNoiseHandshake(peerID)
    }

    fun sendFileBroadcast(file: BitchatFilePacket) {
        service?.sendFileBroadcast(file)
    }

    fun sendFilePrivate(recipientPeerID: String, file: BitchatFilePacket) {
        service?.sendFilePrivate(recipientPeerID, file)
    }

    fun peerNicknames(): Map<String, String> = service?.getPeerNicknames().orEmpty()

    fun peerRssi(): Map<String, Int> = service?.getPeerRSSI().orEmpty()

    private fun buildDelegate(): BluetoothMeshDelegate {
        return object : BluetoothMeshDelegate {
            override fun didReceiveMessage(message: BitchatMessage) {
                listener?.onMessageReceived(message)
                listener?.onReceived(message)
            }

            override fun didSendMessage(messageID: String?, recipientPeerID: String?) {
                listener?.onSent(messageID, recipientPeerID)
            }

            override fun didUpdatePeerList(peers: List<String>) {
                listener?.onPeerListUpdated(peers)
            }

            override fun didFindPeer(peerID: String) {
                listener?.onFound(peerID)
            }

            override fun didLosePeer(peerID: String) {
                listener?.onLost(peerID)
            }

            override fun didConnectPeer(peerID: String) {
                listener?.onConnected(peerID)
            }

            override fun didDisconnectPeer(peerID: String) {
                listener?.onDisconnected(peerID)
            }

            override fun didEstablishSession(peerID: String) {
                listener?.onEstablished(peerID)
            }

            override fun didUpdatePeerRSSI(peerID: String, rssi: Int) {
                listener?.onRSSIUpdated(peerID, rssi)
            }

            override fun didStart() {
                listener?.onStarted()
            }

            override fun didStop() {
                listener?.onStopped()
            }

            override fun didReceiveChannelLeave(channel: String, fromPeer: String) {
                // Not exposed in the minimal listener API.
            }

            override fun didReceiveDeliveryAck(messageID: String, recipientPeerID: String) {
                listener?.onDeliveryAck(messageID, recipientPeerID)
            }

            override fun didReceiveReadReceipt(messageID: String, recipientPeerID: String) {
                listener?.onReadReceipt(messageID, recipientPeerID)
            }

            override fun didReceiveVerifyChallenge(peerID: String, payload: ByteArray, timestampMs: Long) {
                listener?.onVerifyChallenge(peerID, payload, timestampMs)
            }

            override fun didReceiveVerifyResponse(peerID: String, payload: ByteArray, timestampMs: Long) {
                listener?.onVerifyResponse(peerID, payload, timestampMs)
            }

            override fun decryptChannelMessage(encryptedContent: ByteArray, channel: String): String? {
                return null
            }

            override fun getNickname(): String? {
                return nickname
            }

            override fun isFavorite(peerID: String): Boolean {
                return false
            }
        }
    }
}
