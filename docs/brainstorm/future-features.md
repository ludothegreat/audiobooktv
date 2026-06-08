# audiobooktv — Future features

Things explicitly deferred during v1 brainstorming. Not forgotten, not in scope yet.

## Shipped in v1.1 (2026-06-06)
- Real app icon (adaptive, foreground/background vectors) and refined banner
- Sleep timer with persistent preset + countdown that pauses with playback
- Theme picker (Gruvbox + NeonLightning) — supersedes the original "dark mode only, no toggle" decision

## Shipped in v1.2 (2026-06-08)
- Phone + tablet touch UI as a peer presentation surface (Material3). Same APK, same Audiobookshelf backend, same data layer — cross-device resume is automatic via ABS mediaProgress. See decisions.md #28.

## v1.x — polish (applies to both TV and touch unless noted)
- Library search (focusable field on TV; top-of-grid search field on touch)
- Jump-to-chapter menu on the player
- Bookmark rename / delete from the panel/sheet
- Global bookmarks view (across all books)
- "End of chapter" preset on the sleep timer (deferred from v1.1; needs chapter-boundary detection feeding into SleepCountdown)
- Forward30 icon polish on touch (currently using the stock Material `Forward30` glyph; a hand-tuned version would match the cover-art weight better)

## v2 — bigger features
- QR-code first-run pairing (touch device scans, TV pairs — cross-surface flow is the natural pitch now that both surfaces exist)
- Offline downloads / per-book caching (touch UI is streaming-only today; ABS official mobile app is the recommended offline option)
- Remote crash/error reporting (self-hosted GlitchTip)
- Multiple ABS servers / switch between
- Cloudflare Access integration for friend-facing public deployment
- User-created themes (community-supplied palettes loaded at runtime, so new themes don't require a maintainer to add them)

## Maybe never
- Skip-increment customization (locked to 30s)
- Detailed playback-engine tuning
- Telemetry of any kind
- Direct SMB / WebDAV / Google Drive / Dropbox backends (ABS is the backend)
