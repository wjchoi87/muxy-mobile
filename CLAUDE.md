# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Repo layout

Monorepo with two **completely separate** native mobile clients for the Muxy macOS server (which lives in `~/Projects/muxy`). There is **no shared code** between `ios/` and `android/` — the wire protocol is implemented twice (Swift in `ios/MuxyShared/`, Kotlin under `android/app/src/main/java/com/muxy/app/net/` and `model/`). When changing the protocol, both implementations must be updated in lockstep.

Both apps connect to the server over WebSocket (default port `4865`) using a `{type, payload}` envelope. Auth flow: try `authenticateDevice` with a stored UUID+token; on `401` fall back to `pairDevice` and wait for the user to approve the device on the Mac.

## iOS (`ios/`)

SwiftUI app, iOS 17+, Xcode project at `ios/MuxyMobile.xcodeproj`. `MuxyShared/` is consumed as a local Swift package via `XCLocalSwiftPackageReference` pointing at `ios/Package.swift`.

Key files in `ios/MuxyMobile/`:
- `ConnectionManager.swift` (~36k) — central state holder, owns the WebSocket and decodes protocol messages into observable state. `ConnectionManager+VCS.swift` extends it for git operations.
- `DemoBackend.swift` — in-process fake server used when running without a real Muxy mac app.
- `TerminalView.swift` (~47k) — terminal renderer/input.
- `MuxyShared/MuxyProtocol.swift` + `ProtocolParams.swift` — canonical protocol definitions.

Common commands (run from `ios/`):
```sh
scripts/run-mobile.sh              # build + boot sim + install + launch (default: iPhone 16e)
scripts/run-mobile.sh "iPhone 15"  # pick a sim by name
scripts/run-mobile.sh restart      # terminate then relaunch
scripts/run-mobile.sh stop
swiftformat --lint .               # CI runs --lint (no auto-fix); use `swiftformat .` to fix
swiftlint lint --strict --quiet    # CI uses --strict
xcodebuild -project MuxyMobile.xcodeproj -scheme MuxyMobile \
  -destination "generic/platform=iOS Simulator" build
```

Tool versions are pinned in `ios/.tool-versions` (read by CI).

## Android (`android/`)

Kotlin + Jetpack Compose, `minSdk 29`, `compileSdk 35`, JVM target 17. Package layout under `com.muxy.app`: `ui/{connect,projects,workspace,terminal,theme}`, `net/`, `model/`, `data/`. Vendored terminal emulator code lives under `com.termux.terminal`.

Common commands (run from `android/`):
```sh
./gradlew assembleDebug
./gradlew test                 # JUnit; includes JSON envelope round-trip tests
./gradlew test --tests "com.muxy.app.net.SomeTest"   # single test
./gradlew installDebug         # to a connected device/emulator
./gradlew lint
scripts/run-mobile.sh          # convenience wrapper
```

Phase-1 manual verification path is documented in `android/README.md` (pair against a running mac server, approve, reconnect).

## CI (`.github/workflows/`)

PRs are path-filtered — only the relevant platform's checks run.
- `ios-checks.yml` — SwiftFormat lint, SwiftLint `--strict`, simulator build.
- `android-checks.yml` — Gradle lint, debug assemble, unit tests.
- `ios-release.yml` — manual `workflow_dispatch`; archives, signs, uploads to App Store Connect. Requires the secrets listed in the root `README.md`.
- `android-release.yml` — scaffold only; signing + Play upload are TODO.

## License

Source-available under FSL-1.1-ALv2 (see `LICENSE`, `LICENSE-NOTES.md`). Don't relicense or strip headers.
