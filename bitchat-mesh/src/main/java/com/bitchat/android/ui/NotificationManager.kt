package com.bitchat.android.ui

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import com.bitchat.android.util.NotificationIntervalManager

/**
 * No-op notification manager for library builds.
 */
class NotificationManager(
    private val context: Context,
    private val manager: NotificationManagerCompat,
    private val intervalManager: NotificationIntervalManager
) {
    fun setAppBackgroundState(isBackground: Boolean) {
        // Intentionally no-op in library builds.
    }

    fun showPrivateMessageNotification(peerID: String, nickname: String?, preview: String?) {
        // Intentionally no-op in library builds.
    }
}
