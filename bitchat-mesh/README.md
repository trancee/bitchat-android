# bitchat-mesh

Standalone Android library that provides Bluetooth mesh discovery, connections, relay, and messaging. Tor and Nostr are not included.

## Requirements
- Android minSdk 26
- Bluetooth LE capable device

## Add To Your App
1. Include the module in `settings.gradle.kts`:
   ```kotlin
   include(":bitchat-mesh")
   ```
2. Add the dependency:
   ```kotlin
   dependencies {
       implementation(project(":bitchat-mesh"))
   }
   ```

## Permissions
You must request BLE permissions in your app:
- `BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT`, `BLUETOOTH_ADVERTISE` (Android 12+)
- Location permission may be required on older Android versions for BLE scanning

## Basic Usage
```kotlin
val mesh = MeshManager(applicationContext)
mesh.setListener(object : MeshListener {
    override fun onMessageReceived(message: BitchatMessage) { }
    override fun onReceived(message: BitchatMessage) { }
    override fun onSent(messageID: String?, recipientPeerID: String?) { }
    override fun onPeerListUpdated(peers: List<String>) { }
    override fun onFound(peerID: String) { }
    override fun onLost(peerID: String) { }
    override fun onConnected(peerID: String) { }
    override fun onDisconnected(peerID: String) { }
    override fun onEstablished(peerID: String) { }
    override fun onRSSIUpdated(peerID: String, rssi: Int) { }
    override fun onStarted() { }
    override fun onStopped() { }
    override fun onDeliveryAck(messageID: String, recipientPeerID: String) { }
    override fun onReadReceipt(messageID: String, recipientPeerID: String) { }
    override fun onVerifyChallenge(peerID: String, payload: ByteArray, timestampMs: Long) { }
    override fun onVerifyResponse(peerID: String, payload: ByteArray, timestampMs: Long) { }
})

mesh.start(nickname = "alice")
mesh.sendBroadcastMessage("hello nearby")
mesh.sendPrivateMessage("hi", recipientPeerID, recipientNickname)
```

## Listener Hook + Send Example
```kotlin
val mesh = MeshManager(applicationContext)
mesh.setListener(object : MeshListener {
    override fun onMessageReceived(message: BitchatMessage) {
        Log.d("mesh", "${message.sender}: ${message.content}")
    }

    override fun onReceived(message: BitchatMessage) {
        Log.d("mesh", "received: ${message.id}")
    }

    override fun onSent(messageID: String?, recipientPeerID: String?) {
        Log.d("mesh", "sent: ${messageID ?: "(broadcast)"}")
    }

    override fun onPeerListUpdated(peers: List<String>) {
        Log.d("mesh", "peers: ${peers.size}")
    }

    override fun onFound(peerID: String) {
        Log.d("mesh", "found: $peerID")
    }

    override fun onLost(peerID: String) {
        Log.d("mesh", "lost: $peerID")
    }

    override fun onConnected(peerID: String) {
        Log.d("mesh", "connected: $peerID")
    }

    override fun onDisconnected(peerID: String) {
        Log.d("mesh", "disconnected: $peerID")
    }

    override fun onEstablished(peerID: String) {
        Log.d("mesh", "established: $peerID")
    }

    override fun onRSSIUpdated(peerID: String, rssi: Int) {
        Log.d("mesh", "rssi: $peerID -> $rssi")
    }

    override fun onStarted() {
        Log.d("mesh", "started")
    }

    override fun onStopped() {
        Log.d("mesh", "stopped")
    }

    override fun onDeliveryAck(messageID: String, recipientPeerID: String) { }
    override fun onReadReceipt(messageID: String, recipientPeerID: String) { }
    override fun onVerifyChallenge(peerID: String, payload: ByteArray, timestampMs: Long) { }
    override fun onVerifyResponse(peerID: String, payload: ByteArray, timestampMs: Long) { }
})

mesh.start(nickname = "alice")
mesh.sendBroadcastMessage("hello nearby")
```

## API Overview
- `MeshManager.start(nickname)` / `stop()`
- `MeshManager.sendBroadcastMessage(content, mentions, channel)`
- `MeshManager.sendPrivateMessage(content, recipientPeerID, recipientNickname)`
- `MeshManager.sendFileBroadcast(file)` / `sendFilePrivate(peerID, file)`
- `MeshManager.peerNicknames()` / `peerRssi()`
- `MeshListener` callbacks: `onStarted`, `onStopped`, `onFound`, `onLost`, `onConnected`, `onDisconnected`, `onEstablished`, `onSent`, `onReceived`, `onRSSIUpdated`

## Notes
- The library does not ship UI, notification handling, Tor, or Nostr.
- The host app should manage permissions and background execution policy.

## Sample App
A minimal sample app is provided in the `:mesh-sample` module.

Build/install:
```bash
./gradlew :mesh-sample:installDebug
```
