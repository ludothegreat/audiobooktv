# audiobooktv — Decisions

## Core Product

| # | Question | Answer |
|---|----------|--------|
| 0 | What is this? | **Dead-simple Android TV audiobook player.** Open app → pick book → hit play. Auto-saves position on stop, resumes on next open. Only extras: speed control + bookmarks. Backend: Audiobookshelf. Target user: self + technical friends. "Mom-grade" = low-friction for the *operator*, not literal accessibility — readable text UI is fine. |
| 1 | Why a separate app instead of extending the-source-desktop-player? | **Forced split — the-source-desktop-player is PySide6/python-mpv, neither runs on Android TV.** A TV app needs a TV-native codebase. Will be independent (reads its own remote source), not a the-source-desktop-player client. |
| 2 | Target hardware? | **Fire TV (Fire OS), built-in Google/Android TV, Onn boxes.** As many devices as possible. Implies: minSdk low enough for Fire OS 7 (API 28), must run on weak hardware (Onn = 1-2GB RAM, MediaTek). |
| 3 | Distribution? | **Sideload APK only.** No Play Store, no Amazon Appstore. Audience = self + friends (technical enough to sideload). No install-UX worry for now. |

## Architecture

| # | Question | Answer |
|---|----------|--------|
| 4 | UI framework? | **Native Kotlin + Jetpack Compose for TV.** Modern, lean APK, first-class D-pad/focus, runs on Fire OS 7+. Audio engine: **Media3 (ExoPlayer)** — handles M4B chapters, MP3, FLAC, gapless, speed adjustment natively. |
| 5 | Source of books? | **Audiobookshelf backend** via its REST API. We do not browse SMB/WebDAV directly. ABS handles library scan, metadata, cover art, chapters, resume position, bookmarks, multi-user. App = thin TV client over ABS API. Operator runs ABS as a Docker stack pointed at their audiobook library. |
| 6 | Connectivity model? | **App is connectivity-agnostic.** User enters any reachable ABS URL (`http://` or `https://`) at setup. App does not care if it's LAN, Tailscale, public+SSL, or Cloudflare Access — server admin is responsible for how the URL resolves and for transport security. App must accept both `http` and `https`, including self-signed certs (typical setup is internal-only). |

## User Experience

| # | Question | Answer |
|---|----------|--------|
| 7 | First-run setup UX? | **Single-screen form: URL + username + password + Connect.** Validate URL by hitting `GET /ping` before saving. Credentials saved to EncryptedSharedPreferences. Setup screen only shown again if auth fails or user invokes "log out." |
| 8 | Home screen / launch behavior? | **Conditional launch screen, three states:** (1) **Active book exists** → open directly to player screen for that book at saved position, paused. Cover + metadata visible. Press Play → resume. No "Continue" label, no auto-play, no popup, no decoration. (2) **No active book, library has items** → library grid. (3) **Empty library** → flat message: "Load books into your Audiobookshelf library." No instructions, no link, nothing else. "Active book" = ABS most-recently-updated in-progress item (progress > 0 and < 100%). Finished books drop out of active state. |
| 9 | Navigation from player to library? | **Persistent left-side nav rail.** D-pad Left from player focuses the rail. Standard Compose-for-TV `NavigationRail`. Visible on all screens for consistency. |
| 10 | Nav rail items? | **Three: Now Playing, Library, Settings.** "Now Playing" is the player screen. "Library" is the grid. "Settings" holds server URL, log out, version. App opens with focus on Now Playing if a book is active, else Library. |
| 11 | Player screen layout? | Cover (large) + title + author + chapter title (text only) + progress bar with elapsed/total. Five focusable controls in one row: **Skip-back-30s, Play/Pause, Skip-forward-30s, Speed, Bookmark.** Skip increments fixed at 30s each direction. Chapter shown as text, no jump-to-chapter menu in v1. No sleep timer in v1. |
| 12 | Bookmark UX? | **Bookmark button opens a panel.** Top entry pre-focused: "+ Add bookmark here at HH:MM:SS" — OK silently creates it with timestamp label, no prompt, no on-screen keyboard. Below: list of existing bookmarks for this book (timestamp + label). OK on an existing one jumps the player there. Back closes the panel. Bookmarks sync via ABS `POST /api/me/item/:id/bookmark`. Renaming, deleting, and global bookmark view = later. |
| 13 | Speed control UX? | **Panel listing presets** (0.75, 1.0, 1.25, 1.5, 1.75, 2.0). Current speed pre-focused, OK selects, Back cancels. **Per-book persistence** — speed remembered for each title and restored on resume. Synced server-side via ABS user-media-progress (or local fallback keyed by item id). |
| 14 | Library grid? | Grid, **5 covers across**, tile = cover art + title strip underneath (no author — too noisy). Sort: **most-recently-played first, then never-played alphabetical by author** below. No filters in v1. **Finished books** muted visually but still listed (re-listen is common). OK on a tile → Now Playing screen for that book, paused at saved position (or 00:00 if never played). |
| 15 | Settings screen? | Six items: **Server URL** (display + Change), **Username** (display), **Log out**, **Refresh library**, **Diagnostic log** (off by default, toggle), **App version** (bottom line). Dark mode only — no theme toggle. No skip-increment customization, no multi-server, no playback tuning. |
| 16 | Network drop mid-playback? | Pause silently and retry every 5s. After 30s of failed retries, show a small "Reconnecting…" indicator on the player. Resume automatically when the server is back. No popups. |
| 17 | App opens with no network? | Show the active book's player UI in disabled state with a low-key "Offline" line. Library tab shows the last-known cached list (read-only — can't start new books). Settings remains fully functional. |
| 18 | Offline downloads? | **No — streaming only.** Defer indefinitely. TVs don't travel; phones cover the offline case. ABS progress syncs across devices anyway. |

## Identity & Packaging

| # | Question | Answer |
|---|----------|--------|
| 19 | User-facing name? | **audiobooktv.** Keep boring. No branding work. |
| 20 | Android package name (app ID)? | **`xyz.ludothegreat.audiobooktv`** — uses an owned domain, globally unique, stable for life. |
| 21 | App icon? | **Placeholder for v1.** Simple mark later. Banner 320x180 + icon 512x512 required by Android TV launcher. |

## Legal & Operations

| # | Question | Answer |
|---|----------|--------|
| 22 | License? | **MIT.** LICENSE file in repo from day one. |
| 23 | Telemetry / analytics? | **None. Ever.** No network calls beyond ABS API + (optional) update check. |
| 24 | Crash reporting / logging? | **Local rolling log file, OFF by default**, with a Settings toggle to enable. When on, writes to app's external-files dir so it's grabbable via `adb pull`. No remote reporting. |
| 25 | ABS API version pinning? | **Target ABS v2.x stable.** Declare minimum supported version in README. On launch, hit `/ping` + `/api/status`, read server version, refuse to proceed (with clear message naming the supported range) if the server is older than minimum or much newer than tested. |
| 26 | Logout side-effects? | Log out clears credentials **and** the cached library list, bookmarks, and any other per-user state. Next user logging in starts clean. |
| 27 | Self-signed HTTPS certs? | App ships with strict cert validation by default. First-run form has a clearly labeled **"Trust this server's certificate (skip TLS verification)"** toggle, **off by default**. Toggling on attaches a per-host trust override saved with the URL. Prevents foot-guns; supports internal LANs. |
