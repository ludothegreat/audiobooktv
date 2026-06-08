# audiobooktv

> [!NOTE]
> **Dual-platform.** audiobooktv runs as a TV-remote-first app on Android TV / Fire TV / Onn boxes **and** as a touch-first app on Android phones and tablets. Same APK; the launcher picks the right UI for the device. Cross-device resume is automatic via your Audiobookshelf server - pause on the TV, pick up on the phone at the same second.

A dead-simple audiobook player. Backed by an [Audiobookshelf](https://www.audiobookshelf.org/) server. Open it, pick a book, hit play.

Built for sideload - no store listings, no telemetry, no accounts beyond your existing ABS user.

## What it does

- Auto-resumes whatever book you were last on, paused at the exact second you left off - on whichever device you open next.
- Position, bookmarks, and per-book playback speed all sync server-side, so the TV, phone, tablet, and ABS web stay in lock-step.
- **TV:** D-pad-driven UI with a six-button player row (skip-back-30, play/pause, skip-forward-30, speed, sleep, bookmark) and a left-side nav rail.
- **Touch:** full-width cover, draggable scrubber, large round play/pause, Speed / Sleep / Bookmark chips that open bottom sheets, bottom nav bar on phones and a nav rail on tablets.
- Six speed presets (0.75x to 2x).
- Bookmark add (silent, no on-screen keyboard) and jump-to, sorted by timestamp.
- Library grid: 5-across on TV, adaptive on touch (~140dp tiles). In-progress books first, then alphabetical by author.
- Sleep timer with a persistent preset and a countdown that pauses with playback.
- Silent retry on mid-playback network drops with a small "Reconnecting…" indicator after 30s.
- Cold-launch offline shows your cached library with an "Offline" badge instead of a blank screen.
- "Stop playback when app closes" toggle for people who don't want the background-audio behavior.
- Themes: Gruvbox (default) and NeonLightning.

## What it doesn't do (yet)

Jump-to-chapter menu, bookmark rename/delete, library search, QR-code pairing, offline downloads, remote crash reporting. See `docs/brainstorm/future-features.md` for the full deferred list and the reasoning.

## Requirements

- Android 9 (API 28) or newer. Fire OS 7+ on Fire TV, current Google/Android TV, and current Android phones/tablets all qualify.
- An Audiobookshelf v2.20.0 or newer server you can reach. http and https both work.
- For self-signed https, the app supports trust-on-first-use cert pinning - toggle it on at first-run setup.

## Sideload

Same APK installs on TV, phone, or tablet.

```
adb connect <device-ip>:5555           # TV (adb-over-wifi)
adb -s <device-ip>:5555 install -r app-release.apk
# or
adb install -r app-release.apk         # phone/tablet over USB
```

Or use any standard sideload tool (Downloader on Fire TV, File Manager on Google TV, a file manager on Android phones, etc).

First launch: enter the server URL (must start with `http://` or `https://`), your ABS username, and your password. Press Connect. After that the app never asks for credentials again unless you log out from Settings.

## Build from source

Requires JDK 21 and Android SDK 35.

```
git clone https://github.com/ludothegreat/audiobooktv.git
cd audiobooktv
JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew assembleRelease
```

The APK lands at `app/build/outputs/apk/release/app-release.apk` (signed with the debug key so it installs cleanly without secret management).

## Privacy and telemetry

None. The app talks to your Audiobookshelf server. It does not phone home, does not collect analytics, does not load remote resources. The only optional outbound traffic is an off-by-default rolling diagnostic log file written to the app's external-files directory for grabbing via `adb pull` when something goes wrong.

## License

MIT, see `LICENSE`.
