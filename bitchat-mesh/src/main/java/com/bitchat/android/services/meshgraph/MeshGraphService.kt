package com.bitchat.android.services.meshgraph

/**
 * No-op mesh graph service for library builds.
 */
class MeshGraphService private constructor() {
    companion object {
        private val INSTANCE = MeshGraphService()
        fun getInstance(): MeshGraphService = INSTANCE
    }

    fun updateFromAnnouncement(peerID: String, nickname: String?, neighbors: List<String>?, timestamp: ULong) {
        // Intentionally no-op in library builds.
    }

    fun removePeer(peerID: String) {
        // Intentionally no-op in library builds.
    }
}
