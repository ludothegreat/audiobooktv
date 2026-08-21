### Installing over an older version

Releases before v1.3.0 were each signed with a different throwaway key, so
Android refused to install one on top of another. Starting with v1.3.0 every
build is signed with a fixed key checked into this repo, and updates install
normally from here on.

If you already have an older build, this one upgrade needs an uninstall
first, which clears the saved server URL and login:

1. Uninstall audiobooktv.
2. Install this APK.
3. Enter your Audiobookshelf server and credentials again.

Your listening positions and bookmarks live on the Audiobookshelf server, not
in the app, so nothing you have listened to is lost.

---
