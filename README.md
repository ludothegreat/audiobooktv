# audiobooktv

Dead-simple Android TV audiobook player backed by [Audiobookshelf](https://www.audiobookshelf.org/).

Open app, pick a book, press Play. Position, bookmarks, and per-book speed sync to the server.

## Status

Phase 1 - skeleton boots. See `docs/brainstorm/decisions.md` for the locked feature set and `docs/brainstorm/plan.md` for build phases.

## Targets

- Fire TV (Fire OS 7+, Android 9+)
- Google TV / Android TV
- Onn streaming boxes
- Sideload only - no Play Store / Amazon Appstore

## Server requirements

Audiobookshelf v2.x reachable from the device. App is connectivity-agnostic - any `http://` or `https://` URL works (including LAN, Tailnet, Cloudflare Access). Self-signed certs supported via opt-in toggle on first run.

## Build

```
JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew assembleDebug
```

APK lands at `app/build/outputs/apk/debug/app-debug.apk`. Sideload via `adb install` or any Fire TV sideload helper.

## License

MIT, see `LICENSE`.
