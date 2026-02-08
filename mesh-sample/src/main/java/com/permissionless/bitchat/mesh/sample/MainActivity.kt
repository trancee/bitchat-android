package com.permissionless.bitchat.mesh.sample

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatSpinner
import androidx.appcompat.widget.SwitchCompat
import androidx.core.app.ActivityCompat
import com.permissionless.bitchat.mesh.MeshListener
import com.permissionless.bitchat.mesh.MeshManager
import com.bitchat.android.model.BitchatMessage
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.ArrayDeque
import java.io.BufferedReader
import java.io.InputStreamReader

class MainActivity : AppCompatActivity() {
    private lateinit var meshManager: MeshManager
    private lateinit var logView: TextView
    private lateinit var logScroll: ScrollView
    private lateinit var statusText: TextView
    private lateinit var autoScrollSwitch: SwitchCompat
    private lateinit var myPeerIdValue: TextView
    private lateinit var pendingDirectIndicator: TextView
    private lateinit var handshakePendingBadge: TextView
    private lateinit var peerIdSpinner: AppCompatSpinner
    private lateinit var peerAdapter: ArrayAdapter<String>
    private val peerIds: MutableList<String> = mutableListOf()
    private val peerLabels: MutableList<String> = mutableListOf()
    private var peerNicknames: Map<String, String> = emptyMap()
    private var peerRssi: Map<String, Int> = emptyMap()
    private var lastPeers: List<String> = emptyList()
    private var pendingPeerUpdate: List<String>? = null
    private val establishedPeers: MutableSet<String> = mutableSetOf()
    private val pendingDirectMessages: MutableMap<String, ArrayDeque<PendingDirectMessage>> = mutableMapOf()
    private val pendingRetryRunnables: MutableMap<String, Runnable> = mutableMapOf()
    private val pendingRetryHandler = Handler(Looper.getMainLooper())
    private val pendingRetryDelayMs = 3000L
    private val pendingTimeoutMs = 15000L
    private val pendingMaxAttempts = 3
    private val logLines: ArrayDeque<String> = ArrayDeque()
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
    private var isAutoScrollEnabled = true
    private val maxLogLines = 1000
    private var logcatProcess: Process? = null
    private var logcatThread: Thread? = null
    private val logcatTagRegex = Regex("\\s[VDIWEF]\\s+([^:]+):")
    private val logcatBinary = "/system/bin/logcat"
    private val meshLogTags = setOf(
        "AndroidHandshake",
        "AndroidSymmetric",
        "BinaryProtocol",
        "BitchatFilePacket",
        "BluetoothConnectionManager",
        "BluetoothConnectionTracker",
        "BluetoothGattClientManager",
        "BluetoothGattServerManager",
        "BluetoothMeshService",
        "BluetoothPacketBroadcaster",
        "CompressionUtil",
        "EncryptionService",
        "FileSharingManager",
        "FileUtils",
        "FragmentManager",
        "GossipSyncManager",
        "MessageHandler",
        "NoiseChannelEncryption",
        "NoiseEncryptionService",
        "NoiseSession",
        "NoiseSessionManager",
        "PacketProcessor",
        "PacketRelayManager",
        "PeerFingerprintManager",
        "PeerManager",
        "PowerManager",
        "SecureIdentityStateManager",
        "SecurityManager",
        "StoreForwardManager"
    )
    private val meshTagMarkers = meshLogTags.map { " $it:" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        meshManager = MeshManager(applicationContext)

        val startButton = findViewById<Button>(R.id.start_button)
        val stopButton = findViewById<Button>(R.id.stop_button)
        val sendButton = findViewById<Button>(R.id.send_button)
        val sendDirectButton = findViewById<Button>(R.id.send_direct_button)
        val clearButton = findViewById<Button>(R.id.clear_button)
        val messageInput = findViewById<EditText>(R.id.message_input)
        peerIdSpinner = findViewById(R.id.peer_id_spinner)
        logView = findViewById(R.id.log_view)
        logScroll = findViewById(R.id.log_scroll)
        statusText = findViewById(R.id.status_text)
        autoScrollSwitch = findViewById(R.id.autoscroll_switch)
        myPeerIdValue = findViewById(R.id.my_peer_id_value)
        pendingDirectIndicator = findViewById(R.id.pending_direct_indicator)
        handshakePendingBadge = findViewById(R.id.handshake_pending_badge)
        peerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, mutableListOf<String>()).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        peerIdSpinner.adapter = peerAdapter
        autoScrollSwitch.isChecked = true
        isAutoScrollEnabled = true

        updatePeerSpinner(emptyList())

        peerIdSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                val peerId = peerIds.getOrNull(position).orEmpty()
                if (peerId.isEmpty()) return
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }

        meshManager.setListener(object : MeshListener {
            override fun onMessageReceived(message: BitchatMessage) {
                val prefix = if (message.isPrivate) "direct" else "broadcast"
                appendLog("$prefix message from ${message.sender}: ${message.content}")
            }

            override fun onReceived(message: BitchatMessage) {
                appendLog("received ${message.id} from ${message.sender}")
            }

            override fun onSent(messageID: String?, recipientPeerID: String?) {
                val id = messageID ?: "unknown"
                val recipient = recipientPeerID ?: "broadcast"
                appendLog("sent ${id} to ${recipient}")
            }

            override fun onPeerListUpdated(peers: List<String>) {
                updatePeerSpinner(peers)
            }

            override fun onFound(peerID: String) {
                appendLog("found ${peerID}")
            }

            override fun onLost(peerID: String) {
                appendLog("lost ${peerID}")
                establishedPeers.remove(peerID)
                clearPendingDirectMessages(peerID, reason = "lost")
            }

            override fun onConnected(peerID: String) {
                appendLog("connected ${peerID}")
            }

            override fun onDisconnected(peerID: String) {
                appendLog("disconnected ${peerID}")
                establishedPeers.remove(peerID)
                clearPendingDirectMessages(peerID, reason = "disconnected")
            }

            override fun onEstablished(peerID: String) {
                appendLog("session established ${peerID}")
                establishedPeers.add(peerID)
                flushPendingDirectMessages(peerID)
            }

            override fun onRSSIUpdated(peerID: String, rssi: Int) {
                peerRssi = peerRssi.toMutableMap().apply { put(peerID, rssi) }
                updatePeerSpinner(lastPeers)
            }

            override fun onStarted() {
                updateMyPeerId()
                appendLog("mesh started")
            }

            override fun onStopped() {
                appendLog("mesh stopped")
            }

            override fun onDeliveryAck(messageID: String, recipientPeerID: String) {
                appendLog("delivered to ${recipientPeerID} (${messageID})")
            }

            override fun onReadReceipt(messageID: String, recipientPeerID: String) {
                appendLog("read by ${recipientPeerID} (${messageID})")
            }

            override fun onVerifyChallenge(peerID: String, payload: ByteArray, timestampMs: Long) {
                appendLog("handshake initiated by ${peerID}")
            }

            override fun onVerifyResponse(peerID: String, payload: ByteArray, timestampMs: Long) {
                appendLog("verify response from ${peerID}")
            }
        })

        startButton.setOnClickListener {
            ensurePermissions()
            meshManager.start(nickname = getDefaultNickname())
            updateStatus(true)
            appendLog("mesh started")
            updateMyPeerId()
        }

        stopButton.setOnClickListener {
            meshManager.stop()
            updateStatus(false)
            appendLog("mesh stopped")
            myPeerIdValue.text = getString(R.string.label_peer_id_unknown)
            updatePeerSpinner(emptyList())
        }

        sendButton.setOnClickListener {
            val text = messageInput.text?.toString()?.trim().orEmpty()
            if (text.isNotEmpty()) {
                meshManager.sendBroadcastMessage(text)
                appendLog("sent: ${text}")
                messageInput.setText("")
            }
        }

        sendDirectButton.setOnClickListener {
            val text = messageInput.text?.toString()?.trim().orEmpty()
            val peerId = peerIds.getOrNull(peerIdSpinner.selectedItemPosition).orEmpty()
            if (text.isEmpty() || peerId.isEmpty()) {
                appendLog("direct message needs a selected peer and message")
                return@setOnClickListener
            }
            val nickname = meshManager.peerNicknames()[peerId].orEmpty().ifEmpty { peerId }
            if (establishedPeers.contains(peerId)) {
                meshManager.sendPrivateMessage(text, peerId, nickname)
                appendLog("sent direct to $peerId ($nickname): $text")
            } else {
                queuePendingDirectMessage(peerId, nickname, text)
                meshManager.sendPrivateMessage(text, peerId, nickname)
                appendLog("queued direct to $peerId ($nickname): $text")
            }
            messageInput.setText("")
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
        startLogcatStreaming()
    }

    override fun onDestroy() {
        stopLogcatStreaming()
        super.onDestroy()
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
        appendLogLine(line, includeTimestamp = true)
    }

    private fun appendRawLog(line: String) {
        appendLogLine(line, includeTimestamp = false)
    }

    private fun appendLogLine(line: String, includeTimestamp: Boolean) {
        runOnUiThread {
            val formattedLine = if (includeTimestamp) {
                val timestamp = LocalTime.now().format(timeFormatter)
                "$timestamp  $line"
            } else {
                line
            }
            logLines.addLast(formattedLine)
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

    private fun startLogcatStreaming() {
        if (logcatThread != null) return
        try {
            val pid = android.os.Process.myPid().toString()
            val process = ProcessBuilder(logcatBinary, "-v", "time", "--pid", pid, "*:V")
                .redirectErrorStream(true)
                .start()
            logcatProcess = process
            appendLog("logcat streaming active for pid $pid")
            logcatThread = Thread {
                try {
                    BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                        var line: String?
                        while (!Thread.currentThread().isInterrupted) {
                            line = reader.readLine() ?: break
                            if (!line.isNullOrBlank() && shouldIncludeLogcatLine(line!!)) {
                                appendRawLog(line!!)
                            }
                        }
                    }
                } catch (_: java.io.InterruptedIOException) {
                    // Expected when the stream is closed during rotation or teardown.
                } catch (e: Exception) {
                    appendLog("logcat reader stopped: ${e.message}")
                }
            }.apply {
                name = "logcat-reader"
                start()
            }
        } catch (e: Exception) {
            appendLog("logcat stream failed: ${e.message}")
        }
    }

    private fun stopLogcatStreaming() {
        logcatThread?.interrupt()
        logcatThread = null
        logcatProcess?.destroy()
        logcatProcess = null
    }

    private fun shouldIncludeLogcatLine(line: String): Boolean {
        val match = logcatTagRegex.find(line)
        val tag = match?.groupValues?.getOrNull(1)
        if (tag != null) {
            return tag in meshLogTags || tag.startsWith("Bitchat") || tag.startsWith("Bluetooth")
        }
        if (meshTagMarkers.any { line.contains(it) }) {
            return true
        }
        return line.contains(" Bitchat") || line.contains(" Bluetooth")
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

    private fun updateMyPeerId() {
        val peerId = meshManager.myPeerId()?.trim().orEmpty()
        runOnUiThread {
            myPeerIdValue.text = if (peerId.isNotEmpty()) {
                peerId
            } else {
                getString(R.string.label_peer_id_unknown)
            }
        }
    }

    private fun formatPeerList(peers: List<String>): String {
        if (peers.isEmpty()) return "0"
        val nicknames = meshManager.peerNicknames()
        return peers.joinToString { peerId ->
            val nickname = nicknames[peerId]
            if (!nickname.isNullOrBlank()) {
                "$peerId ($nickname)"
            } else {
                peerId
            }
        }
    }

    private fun updatePeerSpinner(peers: List<String>) {
        peerNicknames = meshManager.peerNicknames()
        peerRssi = meshManager.peerRssi()

        runOnUiThread {
            val isPopupShowing = isSpinnerPopupShowing()
            val samePeers = peers == lastPeers
            if (isPopupShowing && samePeers) {
                return@runOnUiThread
            }

            if (isPopupShowing) {
                dismissSpinnerPopup()
            }
            applyPeerSpinnerUpdate(peers)
            pendingPeerUpdate = null
            if (isPopupShowing) {
                showSpinnerPopup()
            }
        }
    }

    private fun applyPeerSpinnerUpdate(peers: List<String>) {
        val updatedPeerIds = mutableListOf<String>()
        val updatedLabels = mutableListOf<String>()

        if (peers.isEmpty()) {
            lastPeers = emptyList()
            updatedPeerIds.add("")
            updatedLabels.add(getString(R.string.label_no_peers))
        } else {
            lastPeers = peers.toList()
            peers.forEach { peerId ->
                val nickname = peerNicknames[peerId]
                updatedPeerIds.add(peerId)
                updatedLabels.add(buildPeerLabel(peerId, nickname))
            }
        }

        val selectedPeerId = peerIds.getOrNull(peerIdSpinner.selectedItemPosition)
        peerIds.clear()
        peerIds.addAll(updatedPeerIds)

        peerLabels.clear()
        peerLabels.addAll(updatedLabels)

        peerAdapter.clear()
        peerAdapter.addAll(peerLabels)
        peerAdapter.notifyDataSetChanged()

        val newIndex = if (selectedPeerId != null) peerIds.indexOf(selectedPeerId) else -1
        if (newIndex >= 0) {
            peerIdSpinner.setSelection(newIndex, false)
        } else if (peerIds.isNotEmpty()) {
            peerIdSpinner.setSelection(0, false)
        }
    }

    private fun queuePendingDirectMessage(peerId: String, nickname: String, content: String) {
        val queue = pendingDirectMessages.getOrPut(peerId) { ArrayDeque() }
        queue.addLast(PendingDirectMessage(peerId, nickname, content, createdAtMs = System.currentTimeMillis()))
        updatePendingIndicator()
        schedulePendingRetry(peerId)
    }

    private fun flushPendingDirectMessages(peerId: String) {
        cancelPendingRetry(peerId)
        val queue = pendingDirectMessages[peerId] ?: return
        while (queue.isNotEmpty()) {
            val message = queue.removeFirst()
            meshManager.sendPrivateMessage(message.content, message.peerId, message.nickname)
            appendLog("sent queued direct to ${message.peerId} (${message.nickname}): ${message.content}")
        }
        if (queue.isEmpty()) {
            pendingDirectMessages.remove(peerId)
        }
        updatePendingIndicator()
    }

    private fun clearPendingDirectMessages(peerId: String, reason: String) {
        cancelPendingRetry(peerId)
        val queue = pendingDirectMessages.remove(peerId) ?: return
        if (queue.isNotEmpty()) {
            appendLog("pending direct messages cleared for $peerId ($reason)")
        }
        updatePendingIndicator()
    }

    private fun schedulePendingRetry(peerId: String) {
        if (pendingRetryRunnables.containsKey(peerId)) return
        val runnable = object : Runnable {
            override fun run() {
                if (establishedPeers.contains(peerId)) {
                    cancelPendingRetry(peerId)
                    return
                }
                val queue = pendingDirectMessages[peerId]
                if (queue.isNullOrEmpty()) {
                    cancelPendingRetry(peerId)
                    return
                }
                val now = System.currentTimeMillis()
                val iterator = queue.iterator()
                while (iterator.hasNext()) {
                    val message = iterator.next()
                    val expired = now - message.createdAtMs > pendingTimeoutMs
                    if (expired || message.attempts >= pendingMaxAttempts) {
                        appendLog("direct to ${message.peerId} (${message.nickname}) failed: session not established")
                        iterator.remove()
                        continue
                    }
                    meshManager.initiateNoiseHandshake(message.peerId)
                    message.attempts += 1
                }
                if (queue.isEmpty()) {
                    pendingDirectMessages.remove(peerId)
                    cancelPendingRetry(peerId)
                } else {
                    pendingRetryHandler.postDelayed(this, pendingRetryDelayMs)
                }
                updatePendingIndicator()
            }
        }
        pendingRetryRunnables[peerId] = runnable
        pendingRetryHandler.postDelayed(runnable, pendingRetryDelayMs)
    }

    private fun cancelPendingRetry(peerId: String) {
        val runnable = pendingRetryRunnables.remove(peerId) ?: return
        pendingRetryHandler.removeCallbacks(runnable)
    }

    private fun updatePendingIndicator() {
        runOnUiThread {
            val selectedPeerId = peerIds.getOrNull(peerIdSpinner.selectedItemPosition)
            val pendingCount = if (selectedPeerId.isNullOrBlank()) {
                0
            } else {
                pendingDirectMessages[selectedPeerId]?.size ?: 0
            }
            if (pendingCount <= 0) {
                pendingDirectIndicator.visibility = android.view.View.GONE
                updateHandshakeBadge(false)
                return@runOnUiThread
            }
            pendingDirectIndicator.text = getString(R.string.label_pending_direct, pendingCount)
            pendingDirectIndicator.visibility = android.view.View.VISIBLE
            updateHandshakeBadge(true)
        }
    }

    private fun updateHandshakeBadge(isPending: Boolean) {
        handshakePendingBadge.visibility = if (isPending) {
            android.view.View.VISIBLE
        } else {
            android.view.View.GONE
        }
    }

    private data class PendingDirectMessage(
        val peerId: String,
        val nickname: String,
        val content: String,
        val createdAtMs: Long,
        var attempts: Int = 0
    )

    private fun isSpinnerPopupShowing(): Boolean {
        return runCatching {
            val field = AppCompatSpinner::class.java.getDeclaredField("mPopup")
            field.isAccessible = true
            val popup = field.get(peerIdSpinner) ?: return false
            val method = popup.javaClass.getMethod("isShowing")
            (method.invoke(popup) as? Boolean) ?: false
        }.getOrDefault(false)
    }

    private fun dismissSpinnerPopup() {
        runCatching {
            val field = AppCompatSpinner::class.java.getDeclaredField("mPopup")
            field.isAccessible = true
            val popup = field.get(peerIdSpinner) ?: return
            val method = popup.javaClass.getMethod("dismiss")
            method.invoke(popup)
        }
    }

    private fun showSpinnerPopup() {
        runCatching {
            val field = AppCompatSpinner::class.java.getDeclaredField("mPopup")
            field.isAccessible = true
            val popup = field.get(peerIdSpinner) ?: return
            val method = popup.javaClass.getMethod("show")
            method.invoke(popup)
        }
    }

    private fun buildPeerLabel(peerId: String, nickname: String?): String {
        val name = if (!nickname.isNullOrBlank()) {
            "$peerId ($nickname)"
        } else {
            peerId
        }
        val rssi = peerRssi[peerId]
        return if (rssi != null) {
            val distanceMeters = estimateDistanceMeters(rssi)
            val distanceLabel = formatDistance(distanceMeters)
            "$name  [$distanceLabel]"
        } else {
            name
        }
    }

    private fun estimateDistanceMeters(rssi: Int): Double {
        // Simple path-loss estimate; good for a rough UI hint only.
        val txPower = -59
        val pathLossExponent = 2.0
        val ratio = (txPower - rssi) / (10.0 * pathLossExponent)
        return Math.pow(10.0, ratio)
    }

    private fun formatDistance(distanceMeters: Double): String {
        if (distanceMeters.isNaN() || distanceMeters.isInfinite()) return "distance n/a"
        return if (distanceMeters < 1.0) {
            "~${(distanceMeters * 100).toInt()} cm"
        } else if (distanceMeters < 10.0) {
            "~${"%.1f".format(distanceMeters)} m"
        } else {
            "~${distanceMeters.toInt()} m"
        }
    }

    private fun getDefaultNickname(): String {
        val deviceName = runCatching {
            Settings.Global.getString(contentResolver, Settings.Global.DEVICE_NAME)
        }.getOrNull()?.trim().orEmpty()
        if (deviceName.isNotEmpty()) return deviceName

        val modelName = Build.MODEL?.trim().orEmpty()
        return if (modelName.isNotEmpty()) modelName else "sample"
    }
}
