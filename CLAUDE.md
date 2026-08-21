# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

YT Grab — an Android app (Kotlin + Jetpack Compose) that wraps yt-dlp to download YouTube videos as MP4 or extract audio as MP3, with quality selection and full-playlist downloading. It's built for personal sideloading (not Play Store — Google's policy bans YouTube-downloader apps).

## Commands

```bash
./gradlew :app:assembleDebug                                    # build debug APK -> app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:testDebugUnitTest                                 # run all JVM unit tests
./gradlew :app:testDebugUnitTest --tests "*.FormatArgsTest"       # run a single test class
./gradlew :app:testDebugUnitTest --tests "*.MetadataParserTest.playlist url exposes entries*"  # single test method
```

There is no lint/format command configured. No instrumented (androidTest) tests exist — all tests are plain JVM unit tests under `app/src/test/`.

### Manual device verification

Unit tests do not cover yt-dlp/ffmpeg native integration, Compose UI, MediaStore writes, or the foreground service — those were verified manually against a running emulator (`adb install -r -t app-debug.apk`, `adb shell am start -n dev.quenguyen.ytgrab/.MainActivity`, then exercising the flow and checking `adb logcat` for `FATAL EXCEPTION`). Do the same after touching `ytdlp/`, `download/`, or the Selection/Downloads screens — these are exactly the areas where past changes silently broke at runtime despite compiling and passing unit tests (see below).

## Architecture

Single-module MVVM app, no DI framework — `ViewModel`s are wired up manually in `MainActivity.kt` via `SimpleViewModelFactory` (a tiny `ViewModelProvider.Factory` that wraps a no-arg lambda), pulling shared singletons off the `YtGrabApp` Application subclass (`ytDlpRepository`, `settingsRepository`, `database`).

### The yt-dlp integration is the load-bearing part

- `io.github.junkfood02.youtubedl-android` (a maintained fork of `yausername/youtubedl-android`) embeds a Python interpreter + yt-dlp + ffmpeg as Android native libraries. `YtGrabApp.onCreate` calls `YoutubeDL.getInstance().init()`, `FFmpeg.init()`, and `updateYoutubeDL()` on a background coroutine at every app start (yt-dlp needs frequent updates or YouTube extraction breaks).
- **`app/build.gradle.kts` sets `packaging { jniLibs { useLegacyPackaging = true } }`.** This is required, not optional: the library's "native libraries" are actually zip blobs (Python stdlib, yt-dlp binary) read via plain file I/O, but AGP's default packaging maps `.so` files straight out of the APK without extracting them to disk — without this flag, yt-dlp/ffmpeg init throws `FileNotFoundException` on `libpython.zip.so` at runtime (it compiles and installs fine; it just crashes on first launch).
- `ytdlp/YtDlpRepository.fetchMetadata()` does **not** use the library's built-in `YoutubeDL.getInstance().getInfo()`/`VideoInfo` model — that model has no `entries` field and silently can't represent playlists. Instead it runs `yt-dlp --flat-playlist --dump-single-json --no-playlist` itself and `ytdlp/MetadataParser` hand-parses the raw JSON (`org.json`) to detect playlists via the `entries` array and pull real per-video format heights.
- `--no-playlist` on the metadata fetch is load-bearing, not cosmetic: without it, YouTube sometimes attaches an autoplay "Mix"/recommended queue to a plain watch URL, and yt-dlp reports that as an empty playlist, breaking single-video downloads for popular videos. `MetadataParser` also treats a non-null-but-empty `entries` array as "not a playlist" as a second line of defense.
- Progress callback shape from the library is `(progress: Float, etaSeconds: Long, line: String) -> Unit` — easy to get wrong since some upstream docs/examples show only 2 params.

### Download queue and service

- `download/DownloadQueueRepository` is a process-wide singleton (`MutableStateFlow<List<DownloadTask>>` + a `Channel<String>` of pending task IDs). Both the UI and `DownloadService` read/write this same object instead of passing task data through `Intent` extras.
- `DownloadService` (a `LifecycleService`) fans out N worker coroutines (N = `settings.concurrency`) that all pull from the same pending-ID channel, run `YoutubeDL.execute()`, save the finished file into the public `MediaStore` collection (`Movies/YtGrab` or `Music/YtGrab`) via `MediaStoreSaver`, and delete the app-private temp copy.
- **Cancel handling is subtle**: `destroyProcessById()` kills the yt-dlp process, which makes `execute()` throw inside `runDownload`'s try block — the generic catch must check `if (status == CANCELLED) then don't overwrite it as FAILED`, otherwise a user-cancelled download shows as "Failed" and a bogus history-looking row leaks into the UI.
- yt-dlp names its in-progress temp file deterministically from the video title+id, so a cancelled download's `.part` file is already present on the *next* attempt at the same video — a before/after directory diff can't detect it as "new" and won't clean it up. `DownloadService.deletePartialFiles()` deletes by `.part`/`.ytdl` suffix instead, not by diffing.
- `DownloadsScreen`'s "Active" list and "History" list share the same `DownloadTask.id` for a given item (the completed task's id is reused as the history row id) — the Active-section filter must exclude `COMPLETED`/`CANCELLED` or a `LazyColumn` duplicate-key crash follows the moment a download finishes while the screen is open.

### Settings

`settings/SettingsRepository` wraps a single DataStore Preferences store (`AppSettings`: default format, default video height cap, default audio bitrate arg, concurrency, Wi-Fi-only). `SettingsScreen` conditionally shows the video-height chips or the audio-bitrate radio buttons based on the currently-selected default format — both `SettingsScreen` and `SelectionScreen` need this same MP4/MP3 branch for their quality picker, and both need `Modifier.horizontalScroll()` on the quality chip `Row` (it overflows the screen width otherwise; a plain `Row` won't wrap or scroll on its own).

### Navigation / screen flow

`MainActivity` hosts a `NavHost` with four routes (`home`, `selection`, `downloads`, `settings`); there's no shared nav-scoped ViewModel. Instead, `ui/common/PendingSessionRepository` is a small singleton `StateFlow<YtMetadata?>` that `HomeViewModel` populates after a successful `fetchMetadata()` call and `SelectionScreen`/`SelectionViewModel` read from — the same pattern as `DownloadQueueRepository`, used to avoid passing complex objects through `NavHost` arguments.

`MainActivity` also handles `ACTION_SEND` (share-from-YouTube-app / share-from-browser): it extracts `EXTRA_TEXT`, and `HomeScreen` auto-fills the URL field and auto-submits via `viewModel.submit()` as soon as it sees a non-null shared URL.

### Format-selector building

`ytdlp/FormatArgs.videoFormatSelector(heightCap: Int?)` is the one piece of yt-dlp-arg-building logic kept free of Android/library types on purpose, specifically so it's unit-testable on the plain JVM (see `FormatArgsTest`). MP3 quality doesn't need an equivalent builder — `OutputQuality.Audio` already stores the literal `--audio-quality` arg value ("0", "192K", "128K") picked from the fixed `MP3_QUALITY_PRESETS` list in `model/YtMetadata.kt`.
