# audiobooktv — Future features

Things explicitly deferred during v1 brainstorming. Not forgotten, not in scope yet.

## Shipped in v1.1 (2026-06-06)
- Real app icon (adaptive, foreground/background vectors) and refined banner
- Sleep timer with persistent preset + countdown that pauses with playback

## v1.x — polish
- Library search (focusable field at the top of the Library screen)
- Jump-to-chapter menu on the player
- Bookmark rename / delete from the panel
- Global bookmarks view (across all books)
- "End of chapter" preset on the sleep timer (deferred from v1.1; needs chapter-boundary detection feeding into SleepCountdown)

## v2 — bigger features
- QR-code first-run pairing (phone supplies URL/user/pass)
- Offline downloads / per-book caching
- Remote crash/error reporting (self-hosted GlitchTip)
- Multiple ABS servers / switch between
- Cloudflare Access integration for friend-facing public deployment
- User-created themes (community-supplied palettes loaded at runtime, so new themes don't require a maintainer to add them)

## Maybe never
- Theme picker (locked to dark)
- Skip-increment customization (locked to 30s)
- Detailed playback-engine tuning
- Telemetry of any kind
- Direct SMB / WebDAV / Google Drive / Dropbox backends (ABS is the backend)
