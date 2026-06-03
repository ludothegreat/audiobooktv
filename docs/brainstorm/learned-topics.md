# Learned Topics

Topics that emerged during this brainstorm that aren't in the default template — fold into future brainstorms when conditions match.

## Architecture
- **Existing-tool-versus-new-app check**: Before designing a new client, ask whether an existing project in the user's workspace already solves the same problem and can be extended instead. Relevant when: the brainstorm topic overlaps an in-progress project in MEMORY.md.
  - Discovered: 2026-06-03, audiobooktv (the-source-desktop-player overlap).
- **Backend leverage**: For media/library apps, ask whether a self-hostable server (Audiobookshelf, Jellyfin, Navidrome, Calibre-web) can be the backend so the client becomes a thin UI. Relevant when: project involves library, metadata, multi-device progress, or multi-user.
  - Discovered: 2026-06-03, audiobooktv (ABS chosen over raw file backends).

## User Experience
- **Conditional launch screen**: Apps that have an "active item" can skip menus entirely and open directly to that item. Worth asking when: the app has an obvious "primary in-progress thing" (an unfinished book, the last note edited, the active workout).
  - Discovered: 2026-06-03, audiobooktv.
- **TV-remote input cost**: Every focusable element is a D-pad stop. Ask for each proposed UI control whether it earns its focus stop. Relevant when: project is TV, set-top, or any 10-foot UI.
  - Discovered: 2026-06-03, audiobooktv.
- **No on-screen-keyboard moments**: Identify and minimize any flow that forces text entry on a TV remote. Default to silent-add + edit-later rather than prompt-on-add. Relevant when: TV or limited-input device.
  - Discovered: 2026-06-03, audiobooktv (bookmark labels).

## Architecture (Android-specific)
- **Android cleartext-HTTP / self-signed-TLS gotchas**: API 28+ blocks `http://` and self-signed by default. Ask early whether the app must accept LAN-only servers and design the trust toggle into first-run UX rather than as an afterthought. Relevant when: Android client app pointing at user-supplied URLs.
  - Discovered: 2026-06-03, audiobooktv.
- **Package-name immutability**: The Android package/app-ID is a forever decision (changing it forces reinstall and loses local state). Lock it in the brainstorm before any code. Relevant when: project targets Android.
  - Discovered: 2026-06-03, audiobooktv.

## Legal & Operations
- **Server-version compatibility refusal**: For clients of self-hosted servers with rolling APIs (ABS, Jellyfin, Sonarr, etc.), ask whether the client should detect server version on launch and refuse with a clear message when outside tested range. Relevant when: client depends on a single self-hosted backend with active development.
  - Discovered: 2026-06-03, audiobooktv.
