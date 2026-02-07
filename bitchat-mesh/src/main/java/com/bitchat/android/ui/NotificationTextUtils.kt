package com.bitchat.android.ui

import com.bitchat.android.model.BitchatMessage

/**
 * Minimal notification text helper for library builds.
 */
object NotificationTextUtils {
    fun buildPrivateMessagePreview(message: BitchatMessage): String {
        return message.content
    }
}
