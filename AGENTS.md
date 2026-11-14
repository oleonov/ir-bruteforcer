# IR Brute — agent guide

Android app (Kotlin, Jetpack Compose) that finds a working IR code for a device by sending
codes from the Flipper/Momentum universal libraries through the phone's IR emitter.
Package `com.pandorika.irbruteforcer` (keep it: changing applicationId breaks updates).

## Layout

- `app/src/main/java/com/pandorika/irbruteforcer/`
  - `ir/` — `.ir` parser, asset loader, Android `ConsumerIrManager` transmitter, pattern chunker.
  - `ir/protocol/` — Kotlin port of the Flipper Zero infrared encoders (GPL-3.0 headers, keep them).
  - `remote/` — `RemoteViewModel` (scan, shared cursor per category, ownership of transmission),
    `CursorStore` (SharedPreferences), `RemoteCategory`/`RemoteButton` catalog.
  - `ui/pixel/` — pixel-style components; `ui/remote/` — the screen.
- `app/src/main/assets/infrared/*.ir` — Momentum universal libraries, verbatim (provenance in `SOURCES.txt`).
- `app/src/main/res/values` (English, default) and `values-ru` (Russian) must keep identical key sets.
- Launcher icon: `res/drawable/ic_launcher_{background,foreground,monochrome}.xml`, generated from the
  12x12 pixel map that `LauncherIconTest` asserts — change the map and the test together.

## Build and test

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug --console=plain
```

This is the gate for every change. Unit tests are plain JVM tests (`app/src/test`), coroutine code uses
`kotlinx-coroutines-test` virtual time. CI runs the same command on every push to `main` and every PR
(`.github/workflows/ci.yml`).

## Device safety (mandatory)

Never run `connectedAndroidTest` / `connectedDebugAndroidTest`, `adb uninstall`, `pm clear`, or install a
build whose signature differs from the installed one on a physical phone: Gradle's instrumented tests
uninstall the app afterwards, and the owner's phones hold data that must not be lost. Instrumented tests
run only on an emulator or a spare device. Ask the owner before installing anything on a phone.

## Versioning and releases

- Version comes from the git tag. `vMAJOR.MINOR.PATCH` → `versionName MAJOR.MINOR.PATCH`,
  `versionCode MAJOR*10000 + MINOR*100 + PATCH` (minor and patch < 100). Gradle reads it from
  `-PappVersion=MAJOR.MINOR.PATCH`; local builds without it are `1.0.0-dev` / versionCode 1.
- Release: commit to `main`, then
  ```bash
  git tag v1.0.1 && git push origin v1.0.1
  ```
  `.github/workflows/release.yml` runs the unit tests, builds `assembleRelease` signed with the release key,
  verifies the signature and publishes a GitHub Release with `ir-brute-<version>.apk` and generated notes.
- Never reuse or move a published tag; bump the patch number instead.

## Signing

- Release key: PKCS12 keystore, alias `irbrute`, certificate `CN=IR Brute`,
  SHA-256 `81:E8:CA:63:7E:10:57:D1:5C:E1:06:FA:67:92:12:8D:BB:73:14:AE:F0:9D:F9:E2:04:DC:7C:4F:36:2E:E0:37`.
  The keystore and its password live with the owner (outside the repo); losing it means users must
  uninstall to update.
- GitHub Actions secrets: `IRBRUTE_KEYSTORE_BASE64` (base64 of the .jks), `IRBRUTE_KEYSTORE_PASSWORD`,
  `IRBRUTE_KEY_ALIAS`, `IRBRUTE_KEY_PASSWORD`. `app/build.gradle.kts` reads the same names from the
  environment (`IRBRUTE_KEYSTORE_PATH` points to the decoded file); without them `assembleRelease`
  produces an unsigned APK.
- Local signed build:
  ```bash
  IRBRUTE_KEYSTORE_PATH=/path/to/ir-brute-release.jks IRBRUTE_KEYSTORE_PASSWORD=... \
  IRBRUTE_KEY_ALIAS=irbrute IRBRUTE_KEY_PASSWORD=... \
  ./gradlew :app:assembleRelease -PappVersion=1.0.0
  ```
- Never commit keystores or passwords (`*.jks`, `*.keystore` are git-ignored).
- Debug builds use the machine's default debug key, so a release build cannot be installed over a debug
  build (different signature) — that needs an uninstall, see Device safety.

## License

GPL-3.0 (`LICENSE`); third-party attributions in `NOTICE`. New code ported from Flipper/Momentum must keep
an SPDX header and a NOTICE entry; new `.ir` assets must be recorded in `assets/infrared/SOURCES.txt`.

## Commits

Commits are authored by the owner only. No `Co-Authored-By`, no AI attribution lines in commits, tags,
PR descriptions or release notes.
