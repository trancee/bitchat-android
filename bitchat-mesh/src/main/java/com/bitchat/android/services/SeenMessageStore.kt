package com.bitchat.android.services

import android.content.Context
import java.util.Collections

/**
 * In-memory seen-message store for library builds.
 */
class SeenMessageStore private constructor() {
    private val delivered = Collections.synchronizedSet(mutableSetOf<String>())
    private val read = Collections.synchronizedSet(mutableSetOf<String>())

    companion object {
        @Volatile
        private var INSTANCE: SeenMessageStore? = null

        fun getInstance(appContext: Context): SeenMessageStore {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SeenMessageStore().also { INSTANCE = it }
            }
        }
    }

    fun hasDelivered(messageID: String): Boolean = delivered.contains(messageID)
    fun markDelivered(messageID: String) { delivered.add(messageID) }

    fun hasRead(messageID: String): Boolean = read.contains(messageID)
    fun markRead(messageID: String) { read.add(messageID) }

    fun clear() {
        delivered.clear()
        read.clear()
    }
}
