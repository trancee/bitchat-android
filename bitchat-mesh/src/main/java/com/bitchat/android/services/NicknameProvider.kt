package com.bitchat.android.services

import android.content.Context

/**
 * Simple nickname provider for library usage.
 * Consumers can set an override nickname via setNickname().
 */
object NicknameProvider {
    @Volatile
    private var overrideNickname: String? = null

    fun setNickname(nickname: String?) {
        overrideNickname = nickname?.trim()?.ifEmpty { null }
    }

    fun getNickname(context: Context, myPeerID: String): String {
        return overrideNickname ?: myPeerID
    }
}
