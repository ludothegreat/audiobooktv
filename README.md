# audiobooktv

A dead-simple Android TV audiobook player. Backed by an [Audiobookshelf](https://www.audiobookshelf.org/) server. Open it, pick a book, hit play.

Built for sideload on Fire TV, Google/Android TV, and Onn streaming boxes. No store listings, no telemetry, no accounts beyond your existing ABS user.

## What it does

- Auto-resumes whatever book you were last on, paused at the exact second you left off.
- Position, bookmarks, and per-book playback speed all sync server-side, so the TV stays in lock-step with ABS web and the official phone apps.
- Five-button player UI tuned for a D-pad remote: skip back 30, play/pause, skip forward 30, speed, bookmark.
- Six speed presets (0.75x to 2x) with a panel that pre-focuses your current speed.
- Bookmark add (silent, no on-screen keyboard) and jump-to, sorted by timestamp.
- Library grid of all your books, 5 wide, in-progress sorted first.
- Silent retry on mid-playback network drops with a small "Reconnecting…" indicator after 30s.
- Cold-launch offline shows your cached library with an "Offline" badge instead of a blank screen.
- "Stop playback when app closes" toggle for people who don't want the background-audio behavior.

## What it doesn't do (yet)

Sleep timer, jump-to-chapter menu, bookmark rename/delete, search, QR-code pairing, offline downloads, remote crash reporting. See `docs/brainstorm/future-features.md` for the full deferred list and the reasoning.

## Requirements

- Android TV / Fire TV / Onn device running Android 9 (Fire OS 7) or newer.
- An Audiobookshelf v2.20.0 or newer server you can reach. http and https both work.
- For self-signed https, the app supports trust-on-first-use cert pinning - toggle it on at first-run setup.

## Sideload

Grab the APK from the release tag, then on the device:

```
adb connect <tv-ip>:5555
adb -s <tv-ip>:5555 install -r app-release.apk
```

Or use any standard sideload tool (Downloader on Fire TV, File Manager on Google TV, etc).

First launch: enter the server URL (must start with `http://` or `https://`), your ABS username, and your password. Press Connect. After that the app never asks for credentials again unless you log out from Settings.

## Build from source

Requires JDK 21 and Android SDK 35.

```
git clone https://github.com/ludothegreat/audiobooktv.git
cd audiobooktv
JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew assembleRelease
```

The APK lands at `app/build/outputs/apk/release/app-release.apk` (~5 MB, signed with the debug key so it installs cleanly without secret management).

## Privacy and telemetry

None. The app talks to your Audiobookshelf server. It does not phone home, does not collect analytics, does not load remote resources. The only optional outbound traffic is an off-by-default rolling diagnostic log file written to the app's external-files directory for grabbing via `adb pull` when something goes wrong.

## License

MIT, see `LICENSE`.
