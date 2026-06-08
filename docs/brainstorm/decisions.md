# audiobooktv — Decisions

## Core Product

| # | Question | Answer |
|---|----------|--------|
| 0 | What is this? | **Dead-simple audiobook player for TV and touch.** Open app → pick book → hit play. Auto-saves position on stop, resumes on next open (cross-device via Audiobookshelf's server-truth mediaProgress). Only extras: speed control + bookmarks + sleep timer. Backend: Audiobookshelf. Target user: self + technical friends. "Mom-grade" = low-friction for the *operator*, not literal accessibility — readable text UI is fine. |
| 1 | Why not extend an existing desktop audiobook player? | **Forced split — typical desktop players (PySide6/python-mpv-based, etc.) don't run on Android TV.** A TV app needs a TV-native codebase. This is a standalone Android client. |
| 2 | Target hardware? | **Fire TV, built-in Google/Android TV, Onn boxes (TV-remote UI) plus Android phones and tablets (touch UI).** As many devices as possible. minSdk 28 (Fire OS 7) holds for both surfaces. `[CHANGED 2026-06-08 v1.2: added phone/tablet touch UI as a peer presentation. Same APK, same data layer, same Audiobookshelf backend — see #28.]` |
| 3 | Distribution? | **Sideload APK only.** No Play Store, no Amazon Appstore. Audience = self + friends (technical enough to sideload). The single APK installs on TV, phone, or tablet — launcher picks the right UI. |

## Architecture

| # | Question | Answer |
|---|----------|--------|
| 4 | UI framework? | **Native Kotlin + Jetpack Compose.** TV surface uses `androidx.tv.material3` (D-pad/focus-first). Touch surface uses `androidx.compose.material3` (touch-target / gesture-first). Both surfaces consume the same ViewModels, repositories, MediaSessionService, DataStore, and EncryptedSharedPreferences — there is exactly one core. Audio engine: **Media3 (ExoPlayer)** — handles M4B chapters, MP3, FLAC, gapless, speed adjustment natively. `[AMENDED 2026-06-08 v1.2: added Material3 touch surface; TV surface unchanged.]` |
| 5 | Source of books? | **Audiobookshelf backend** via its REST API. We do not browse SMB/WebDAV directly. ABS handles library scan, metadata, cover art, chapters, resume position, bookmarks, multi-user. App = thin client over ABS API. Operator runs ABS as a Docker stack pointed at their audiobook library. |
| 6 | Connectivity model? | **App is connectivity-agnostic.** User enters any reachable ABS URL (`http://` or `https://`) at setup. App does not care if it's LAN, Tailscale, public+SSL, or Cloudflare Access — server admin is responsible for how the URL resolves and for transport security. App must accept both `http` and `https`, including self-signed certs (typical setup is internal-only). |

## User Experience

| # | Question | Answer |
|---|----------|--------|
| 7 | First-run setup UX? | **Single-screen form: URL + username + password + Connect.** Validate URL by hitting `GET /ping` before saving. Credentials saved to EncryptedSharedPreferences. Setup screen only shown again if auth fails or user invokes "log out." Touch surface renders the same form with Material3 inputs and an on-screen keyboard. |
| 8 | Home screen / launch behavior? | **Conditional launch screen, three states:** (1) **Active book exists** → open directly to player screen for that book at saved position, paused. Cover + metadata visible. Press Play → resume. No "Continue" label, no auto-play, no popup, no decoration. (2) **No active book, library has items** → library grid. (3) **Empty library** → flat message: "Load books into your Audiobookshelf library." No instructions, no link, nothing else. "Active book" = ABS most-recently-updated in-progress item (progress > 0 and < 100%). Finished books drop out of active state. Behavior identical on TV and touch. |
| 9 | Navigation? | **TV: persistent left-side `NavigationRail` (Compose-for-TV), D-pad-driven.** Touch: bottom `NavigationBar` on phones (`sw < 600dp`), Material3 `NavigationRail` on tablets (`sw >= 600dp`). Same three destinations on both surfaces. `[AMENDED 2026-06-08 v1.2: added touch nav variants. TV nav unchanged.]` |
| 10 | Nav rail items? | **Three: Now Playing, Library, Settings.** "Now Playing" is the player screen. "Library" is the grid. "Settings" holds server URL, log out, theme, version. App opens with focus on Now Playing if a book is active, else Library. |
| 11 | Player screen layout? | **TV:** cover (large) + title + author + chapter title + progress bar with elapsed/total. Six focusable controls in one row: Skip-back-30s, Play/Pause, Skip-forward-30s, Speed, Sleep, Bookmark. Skip increments fixed at 30s each direction. Chapter shown as text, no jump-to-chapter menu. **Touch:** full-width cover (max 360dp), metadata block, draggable Material3 `Slider` scrubber (drag-local state, server push on release via the new `PlayerViewModel.seekToAbsoluteSec`), 56–72dp Replay30 / Play-Pause / Forward30 circular controls, then a Speed / Sleep / Bookmark `AssistChip` row. Bottom-sheet panels for each. `[AMENDED 2026-06-08 v1.2: added touch player + scrubber. TV layout unchanged. The new VM method is shared but TV does not call it.]` |
| 12 | Bookmark UX? | **TV:** Bookmark button opens a panel. Top entry pre-focused: "+ Add bookmark here at HH:MM:SS" — OK silently creates it, no on-screen keyboard. List below: existing bookmarks (timestamp + label). OK on a row jumps the player there. Back closes. **Touch:** same semantics in a `ModalBottomSheet`. Sticky "+ Add here" pill at top; tap any list row to jump. Renaming, deleting, and a global bookmark view across all books remain deferred. `[AMENDED 2026-06-08 v1.2: added touch presentation; ABS API calls unchanged.]` |
| 13 | Speed control UX? | **TV:** Panel listing the six presets (0.75, 1.0, 1.25, 1.5, 1.75, 2.0). Current pre-focused, OK selects, Back cancels. **Touch:** `FilterChip` list in a `ModalBottomSheet`. Per-book persistence is shared — speed remembered for each title and restored on resume across either surface. `[AMENDED 2026-06-08 v1.2: touch presentation; persistence unchanged.]` |
| 14 | Library grid? | **TV:** 5 covers across, tile = cover + title strip; sort: most-recently-played first, then never-played alphabetical by author. Finished books muted (kept in list, re-listen is common). **Touch:** adaptive grid (`GridCells.Adaptive(minSize = 140.dp)`), same `LibrarySorter` so the order is identical, finished books rendered at 50% alpha, in-progress books show a thin progress bar overlay on the cover. Pull-to-refresh replaces the D-pad refresh menu item. Tap → Now Playing. No search field on either surface yet — backlog. `[AMENDED 2026-06-08 v1.2: touch grid; sort and behavior unchanged.]` |
| 15 | Settings screen? | **Items unchanged across surfaces:** Server URL (display), Username (display), Theme picker (Gruvbox / NeonLightning), Stop-on-app-close toggle, Diagnostic log toggle, Refresh library button, Log out button, App version footer. **TV:** rendered with `androidx.tv.material3` Surfaces + Buttons. **Touch:** rendered as a Material3 list with `Switch`, `FilterChip` (theme), and `Button` (refresh/logout, error color for logout). `[SUPERSEDED 2026-06-06 v1.1: original wording said "Dark mode only — no theme toggle." A Gruvbox / NeonLightning picker shipped in v1.1; the dark-only constraint no longer holds. Both themes are dark, but a picker exists.]` `[AMENDED 2026-06-08 v1.2: touch presentation; setting set unchanged.]` |
| 16 | Network drop mid-playback? | Pause silently and retry every 5s. After 30s of failed retries, show a small "Reconnecting…" indicator on the player. Resume automatically when the server is back. No popups. Identical on both surfaces. |
| 17 | App opens with no network? | Show the active book's player UI in disabled state with a low-key "Offline" line. Library tab shows the last-known cached list (read-only — can't start new books). Settings remains fully functional. Identical on both surfaces. |
| 18 | Offline downloads? | **No — streaming only on both TV and touch.** Defer indefinitely. `[CONTEXT AMENDED 2026-06-08 v1.2: the original reasoning was "TVs don't travel; phones cover the offline case via the official Audiobookshelf phone app." That still applies — for genuine offline playback (commute, plane), users should keep using the official ABS mobile app; audiobooktv's touch UI is streaming-only and is intended for the same-LAN-or-VPN case (resume on the couch, in the kitchen, in bed). If offline becomes a real ask we revisit, otherwise out of scope on this surface too.]` |

## Identity & Packaging

| # | Question | Answer |
|---|----------|--------|
| 19 | User-facing name? | **audiobooktv.** Keep boring. No branding work. |
| 20 | Android package name (app ID)? | **`xyz.ludothegreat.audiobooktv`** — uses an owned domain, globally unique, stable for life. |
| 21 | App icon? | **Adaptive launcher icon** (`mipmap-anydpi-v26/ic_launcher{,_round}.xml` + foreground/background vector layers) + TV banner (`drawable/banner.xml`). Same assets used on TV and touch; the launcher selects the appropriate one. `[AMENDED 2026-06-06 v1.1: adaptive icon shipped (originally "placeholder for v1").]` |

## Legal & Operations

| # | Question | Answer |
|---|----------|--------|
| 22 | License? | **MIT.** LICENSE file in repo from day one. |
| 23 | Telemetry / analytics? | **None. Ever.** No network calls beyond ABS API + (optional) update check. |
| 24 | Crash reporting / logging? | **Local rolling log file, OFF by default**, with a Settings toggle to enable. When on, writes to app's external-files dir so it's grabbable via `adb pull`. No remote reporting. |
| 25 | ABS API version pinning? | **Target ABS v2.x stable.** Declare minimum supported version in README. On launch, hit `/ping` + `/api/status`, read server version, refuse to proceed (with clear message naming the supported range) if the server is older than minimum or much newer than tested. |
| 26 | Logout side-effects? | Log out clears credentials **and** the cached library list, bookmarks, and any other per-user state. Next user logging in starts clean. |
| 27 | Self-signed HTTPS certs? | App ships with strict cert validation by default. First-run form has a clearly labeled **"Trust this server's certificate (skip TLS verification)"** toggle, **off by default**. Toggling on attaches a per-host trust override saved with the URL. Prevents foot-guns; supports internal LANs. Identical default and toggle on both surfaces. |

## Touch surface

| # | Question | Answer |
|---|----------|--------|
| 28 | How is the touch UI packaged? | **Single APK, single `MainActivity`, form-factor-routed at startup. `[ADDED 2026-06-08 v1.2.]`** `MainActivity.onCreate` calls `FormFactorRouter.choose(packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK))` and mounts either `RootScaffold` (TV, wrapped in `AudiobooktvTheme` from `androidx.tv.material3`) or `TouchRootScaffold` (touch, wrapped in `AudiobooktvMaterialTheme` from `androidx.compose.material3`). Both presentation trees share **all** ViewModels (`SetupViewModel`, `LibraryViewModel`, `PlayerViewModel`, `SettingsViewModel`, `RootViewModel`), the `MediaSessionService`, the Audiobookshelf REST + cache + auth layer, DataStore (`AppSettings`, `SpeedStore`, library cache), `EncryptedSharedPreferences` (credentials + TLS pin), and the Hilt graph. Rejected alternatives: separate `:phone` module sharing a `:core` library (cost: Hilt-graph extraction, doubled CI, no real benefit at 5 MB APK), and product flavors (cost: another build dimension for no install-time benefit). Color tokens live in `ui/theme/Palette.kt` so both `AudiobooktvTheme` and `AudiobooktvMaterialTheme` consume identical Gruvbox / NeonLightning palettes. |
