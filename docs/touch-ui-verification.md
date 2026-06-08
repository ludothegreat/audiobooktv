# Touch UI cross-device verification

Manual on-device checks to confirm the touch UI is working AND that cross-device
resume between TV and phone/tablet round-trips through Audiobookshelf
correctly. You'll need:

- The current `audiobooktv` repo at `/hoard/lab/audiobooktv` (branch
  `feat/touch-ui` for this round).
- The TV install already on the Onn box at `192.168.1.143:5555`.
- A phone and a tablet running Android 9+ with USB debugging enabled.
- The ABS server at `http://192.168.1.101:13378` reachable from all three
  devices on the same LAN / VPN.

Credentials live in `pass`:

```
pass show audiobookshelf/ludo        # password for the ABS "ludo" user
```

## Build the touch APK

```
cd /hoard/lab/audiobooktv
JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew assembleRelease
ls -la app/build/outputs/apk/release/app-release.apk
```

Expect ~5.5 MB. Same APK runs on TV, phone, and tablet — the launcher picks
the right UI based on `FEATURE_LEANBACK`.

## Install on phone (USB)

Plug the phone in via USB. Accept the "Allow USB debugging" prompt.

```
adb devices                          # confirm the phone is listed
adb install -r /hoard/lab/audiobooktv/app/build/outputs/apk/release/app-release.apk
adb shell am start -n xyz.ludothegreat.audiobooktv/.MainActivity
```

## Install on tablet (USB or wifi)

Wifi (preferred for tablets, mirrors the TV flow):

```
adb connect <tablet-ip>:5555
adb -s <tablet-ip>:5555 install -r /hoard/lab/audiobooktv/app/build/outputs/apk/release/app-release.apk
adb -s <tablet-ip>:5555 shell am start -n xyz.ludothegreat.audiobooktv/.MainActivity
```

## First-run setup (each device)

1. App opens to the Material3 setup form (touch surface).
2. Server URL: `http://192.168.1.101:13378`
3. Username: `ludo`
4. Password: `pass show audiobookshelf/ludo`
5. Leave "Trust this server's certificate" OFF (the server speaks plain http).
6. Tap **Connect**. The form submits, the TLS-pin enrollment is a no-op for
   http URLs, and the app lands in the touch root scaffold.

If a previous build is already logged in, the setup screen is skipped and
the app goes straight to the active book (or the Library tab if none).

## Touch UI smoke checklist

Run this once on the phone and once on the tablet. The tablet differs only
in the nav layout: it must show a **left-side `NavigationRail`** instead of
the phone's bottom `NavigationBar`.

1. **Form-factor routing**: app does not show the TV nav rail. Phone shows a
   bottom bar with 3 tabs (Now Playing / Library / Settings). Tablet shows a
   left-side rail with the same 3 tabs.
2. **Library grid**: open the Library tab. Books load (cached or live), grid
   tiles are roughly 140dp wide and cleanly fill the screen. In-progress
   books show a thin progress bar across the cover. Finished books render
   at ~50% alpha.
3. **Pull-to-refresh**: drag down on the grid. The pull indicator appears,
   refresh runs, indicator disappears.
4. **Open a book**: tap any cover. Player opens at the saved position,
   paused, cover loads, title/author/chapter all populate.
5. **Touch scrubber**: drag the slider thumb left/right. Timestamp under the
   thumb updates while you drag. Release; the player seeks to that position.
   Verify with the elapsed counter and chapter label.
6. **Big play/pause**: tap the central round button. Audio comes out. Tap
   again to pause. The icon swaps Play ↔ Pause.
7. **Skip ±30**: tap Replay30 then Forward30. Position jumps backward then
   forward by 30 seconds each.
8. **Speed chip → bottom sheet**: tap the Speed chip. Bottom sheet slides
   up with 6 chips. Tap 1.5x. Sheet dismisses, playback rate audibly
   changes, chip label updates to "1.50x". Force-stop and relaunch; the
   speed is still 1.5x.
9. **Sleep chip → bottom sheet**: tap the Sleep chip. Bottom sheet slides
   up with 7 chips (Off + 5/10/15/30/45/60 min). Tap 5 min. Sheet dismisses,
   chip label changes to "5m" then immediately starts ticking "4:59",
   "4:58", … Pause playback; the countdown pauses. Resume; it resumes.
10. **Bookmark chip → bottom sheet**: tap the Bookmark chip. Bottom sheet
    opens with a sticky "Add bookmark at H:MM:SS" pill at top and any
    existing bookmarks listed below. Tap the pill; a bookmark is added at
    the current position (no on-screen keyboard, silent). Tap an existing
    bookmark; player jumps there. Pull the sheet down to dismiss.
11. **Settings tab**: open Settings. Server URL and username display. Theme
    chip row shows Gruvbox + NeonLightning; tap NeonLightning; the whole UI
    re-themes to the magenta/black palette immediately. Toggle Stop-on-app-
    close on then off. Refresh-library button shows a transient "Library
    refresh requested." line. Log out works (returns to setup form) — then
    log back in to continue.

## Cross-device resume (the headline test)

### TV → phone

1. On the **TV**, open the player and let a book play for ~60 seconds, then
   press Pause. Note the timestamp shown (e.g. `0:01:23`).
2. On the **phone**, open the same book from Library (or it should be the
   pre-loaded active book on app start).
3. The position bar must show the same `0:01:23` (±3 seconds — that's the
   `POSITION_DRIFT_TOLERANCE_SEC` in PlayerViewModel). Press Play. Audio
   resumes from that point.

### Phone → TV

1. On the **phone**, drag the scrubber to roughly mid-book. The release
   pushes the new position to ABS immediately (`PlayerViewModel.seekToAbsoluteSec`
   calls `pushPositionToServer`).
2. Add a bookmark via the Bookmark sheet with an obvious label timestamp.
3. On the **TV**, navigate to the same book. If the TV player was already
   open paused on that book, the paused-poll picks the new position up
   within 15 seconds (`PAUSED_POLL_INTERVAL_MS`). Pressing Play forces an
   immediate refresh from ABS regardless.
4. Open the Bookmark panel on the TV. The bookmark added on the phone is
   in the list at the right timestamp.

### Tablet → phone (or any pair)

The flow is identical because all three surfaces share the same
`PlaybackRepository.syncProgress` / `fetchSavedPositionSec` code paths. The
authoritative source of truth is ABS `mediaProgress`; the app is the same
single source on every device.

## How to test offline (touch surface)

```
# block phone from reaching ABS
sudo iptables -I DOCKER-USER -s <phone-ip> -p tcp -m conntrack --ctorigdstport 13378 -j DROP
sudo conntrack -D -s <phone-ip> -p tcp --orig-port-dst 13378

# undo
sudo iptables -D DOCKER-USER -s <phone-ip> -p tcp -m conntrack --ctorigdstport 13378 -j DROP
```

Re-open the Library tab on the phone — the cached grid renders with an
"Offline -- showing cached library" badge at the top.

## What this verifies

- `FormFactorRouter` correctly mounts the touch surface on phones/tablets.
- Material3 `Slider` + `seekToAbsoluteSec` honors the server-truth invariant
  (the seek is pushed before Play's pre-play refresh can yank it back).
- `ModalBottomSheet` for Speed / Sleep / Bookmark works with the existing
  `PlayerViewModel` panel visibility flags.
- `LibrarySorter` ordering is identical to the TV grid (visual sanity
  check).
- Cross-device resume between TV and touch surfaces uses the same ABS
  `mediaProgress` round-trip the TV-only build was already using.

If any step fails, the most likely culprit is a Compose state ordering
issue in the scrubber (drag state should reset when `positionSec` advances
*and* `dragging == false`). Capture an `adb logcat` and grep for
`PlayerViewModel` / `PlaybackRepository`.
