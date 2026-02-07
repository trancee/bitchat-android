package com.bitchat.android.services.meshgraph

/**
 * Stub gossip TLV encoder for library builds (mesh graph disabled).
 */
object GossipTLV {
    fun encodeNeighbors(peers: List<String>): ByteArray = ByteArray(0)

    fun decodeNeighborsFromAnnouncementPayload(payload: ByteArray): List<String>? = null
}
