# YT Grab

An Android app (Kotlin + Jetpack Compose) that wraps [yt-dlp](https://github.com/yt-dlp/yt-dlp) to download YouTube videos as MP4 or extract audio as MP3, with quality selection and full-playlist downloading.

Built for personal sideloading — **not distributed on the Play Store**, since Google's policy bans YouTube-downloader apps.

## Requirements

- JDK 17
- Android SDK (`compileSdk`/`targetSdk` 35, `minSdk` 29), with `sdk.dir` set in `local.properties` (or the `ANDROID_HOME` env var)

## Build

```bash
./gradlew :app:assembleDebug     # -> app/build/outputs/apk/debug/app-debug.apk
```

Or use the helper script, which checks your environment (gradlew permissions, a working Java runtime) first:

```bash
./build-apk.sh debug     # or: ./build-apk.sh release
```

A release build only produces a *signed* APK if `local.properties` has `RELEASE_STORE_FILE` / `RELEASE_STORE_PASSWORD` / `RELEASE_KEY_ALIAS` / `RELEASE_KEY_PASSWORD` pointing at a real keystore; otherwise it falls back to an unsigned release build. `local.properties` is gitignored — create your own locally.

To build for every ABI instead of just `arm64-v8a` (the default, to keep APK size down), remove the `ndk { abiFilters += "arm64-v8a" }` block in `app/build.gradle.kts`.

## Install

```bash
adb install -r -t app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n dev.quenguyen.ytgrab/.MainActivity
```

## Test

```bash
./gradlew :app:testDebugUnitTest                                                    # all JVM unit tests
./gradlew :app:testDebugUnitTest --tests "*.FormatArgsTest"                         # a single test class
./gradlew :app:testDebugUnitTest --tests "*.MetadataParserTest.playlist url exposes entries*"  # a single test method
```

There's no lint/format command configured. No instrumented (`androidTest`) tests exist — everything under `app/src/test/` is a plain JVM unit test. The yt-dlp/ffmpeg native integration, Compose UI, MediaStore writes, and the foreground download service aren't covered by unit tests and need manual verification on a device/emulator (see `CLAUDE.md`).

## Architecture

See [`CLAUDE.md`](CLAUDE.md) for a detailed breakdown of the app's architecture, the yt-dlp integration, and known sharp edges.
