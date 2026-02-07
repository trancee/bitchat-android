package com.bitchat.android.services

/**
 * No-op app state store for the library module.
 */
object AppStateStore {
    fun setPeers(peers: List<String>) {
        // Intentionally no-op in library builds.
    }

    fun addPrivateMessage(peerID: String, message: Any) {
        // Intentionally no-op in library builds.
    }

    fun addChannelMessage(channel: String, message: Any) {
        // Intentionally no-op in library builds.
    }

    fun addPublicMessage(message: Any) {
        // Intentionally no-op in library builds.
    }

    fun clear() {
        // Intentionally no-op in library builds.
    }
}
