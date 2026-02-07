# Implementation Plan

## Goal
Ship a standalone Android library module that provides the Bluetooth mesh stack (discovery, connect, relay, messaging) without Tor or Nostr, while keeping upstream sync easy.

## Scope
- New module: `:bitchat-mesh`
- Public API: manager-style wrapper (`MeshManager` + `MeshListener`)
- Excluded: Tor, Nostr, UI, app-only features

## Strategy
- Copy mesh, crypto, noise, protocol, model, sync, util packages into the library module.
- Replace app-only dependencies with minimal stubs (debug, notifications, state store, meshgraph).
- Keep the app module intact to allow clean upstream merges.

## Steps
1. Add `:bitchat-mesh` to Gradle settings and create module build file.
2. Copy core source packages into the module.
3. Remove Tor/Nostr references and network relay hooks.
4. Provide lightweight stubs for app-only services.
5. Add a minimal public API wrapper in `com.permissionless.bitchat.mesh`.
6. Document usage and permissions.

## Upstream Sync
- Library lives in its own module; upstream pulls should not require rebasing app files.
- Mesh changes from upstream can be copied into `:bitchat-mesh` with minimal diff.
- No changes required in upstream `:app` unless you want to migrate the app to use the library.

## Validation
- `./gradlew :bitchat-mesh:assembleDebug`
- Consumer app smoke test for BLE discovery and message relay
