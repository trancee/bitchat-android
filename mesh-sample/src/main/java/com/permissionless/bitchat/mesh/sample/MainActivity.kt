package com.permissionless.bitchat.mesh.sample

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.app.ActivityCompat
import com.permissionless.bitchat.mesh.MeshListener
import com.permissionless.bitchat.mesh.MeshManager
import com.bitchat.android.model.BitchatMessage
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.ArrayDeque

class MainActivity : AppCompatActivity() {
    private lateinit var meshManager: MeshManager
    private lateinit var logView: TextView
    private lateinit var logScroll: ScrollView
    private lateinit var statusText: TextView
    private lateinit var autoScrollSwitch: SwitchCompat
    private val logLines: ArrayDeque<String> = ArrayDeque()
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
    private var isAutoScrollEnabled = true
    private val maxLogLines = 250

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        meshManager = MeshManager(applicationContext)

        val startButton = findViewById<Button>(R.id.start_button)
        val stopButton = findViewById<Button>(R.id.stop_button)
        val sendButton = findViewById<Button>(R.id.send_button)
        val clearButton = findViewById<Button>(R.id.clear_button)
        val messageInput = findViewById<EditText>(R.id.message_input)
        logView = findViewById(R.id.log_view)
        logScroll = findViewById(R.id.log_scroll)
        statusText = findViewById(R.id.status_text)
        autoScrollSwitch = findViewById(R.id.autoscroll_switch)
        autoScrollSwitch.isChecked = true
        isAutoScrollEnabled = true

        meshManager.setListener(object : MeshListener {
            override fun onMessageReceived(message: BitchatMessage) {
                appendLog("message from ${message.sender}: ${message.content}")
            }

            override fun onPeerListUpdated(peers: List<String>) {
                appendLog("peers: ${peers.size}")
            }

            override fun onDeliveryAck(messageID: String, recipientPeerID: String) {
                appendLog("delivered to ${recipientPeerID} (${messageID})")
            }

            override fun onReadReceipt(messageID: String, recipientPeerID: String) {
                appendLog("read by ${recipientPeerID} (${messageID})")
            }

            override fun onVerifyChallenge(peerID: String, payload: ByteArray, timestampMs: Long) {
                appendLog("verify challenge from ${peerID}")
            }

            override fun onVerifyResponse(peerID: String, payload: ByteArray, timestampMs: Long) {
                appendLog("verify response from ${peerID}")
            }
        })

        startButton.setOnClickListener {
            ensurePermissions()
            meshManager.start(nickname = "sample")
            updateStatus(true)
            appendLog("mesh started")
        }

        stopButton.setOnClickListener {
            meshManager.stop()
            updateStatus(false)
            appendLog("mesh stopped")
        }

        sendButton.setOnClickListener {
            val text = messageInput.text?.toString()?.trim().orEmpty()
            if (text.isNotEmpty()) {
                meshManager.sendBroadcastMessage(text)
                appendLog("sent: ${text}")
                messageInput.setText("")
            }
        }

        clearButton.setOnClickListener {
            logLines.clear()
            logView.text = ""
            appendLog("log cleared")
        }

        autoScrollSwitch.setOnCheckedChangeListener { _, isChecked ->
            isAutoScrollEnabled = isChecked
        }

        updateStatus(false)
    }

    private fun ensurePermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            permissions.add(Manifest.permission.BLUETOOTH_ADVERTISE)
        }
        ActivityCompat.requestPermissions(this, permissions.toTypedArray(), 1001)
    }

    private fun appendLog(line: String) {
        runOnUiThread {
            val timestamp = LocalTime.now().format(timeFormatter)
            logLines.addLast("$timestamp  $line")
            while (logLines.size > maxLogLines) {
                logLines.removeFirst()
            }
            logView.text = logLines.joinToString("\n")
            if (isAutoScrollEnabled) {
                logScroll.post {
                    logScroll.fullScroll(ScrollView.FOCUS_DOWN)
                }
            }
        }
    }

    private fun updateStatus(isRunning: Boolean) {
        statusText.text = if (isRunning) {
            getString(R.string.status_running)
        } else {
            getString(R.string.status_stopped)
        }
        val backgroundRes = if (isRunning) {
            R.drawable.bg_status_chip_running
        } else {
            R.drawable.bg_status_chip
        }
        statusText.setBackgroundResource(backgroundRes)
    }
}
