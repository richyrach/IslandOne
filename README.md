# IslandOne

**A Dynamic Island-style Android overlay. Open source, no ads.**

Built for centered punch-hole displays, including the Galaxy S24 FE. IslandOne is an early preview; the design and interaction details are still being refined on actual phones.

## Download

**[Download the latest preview APK](https://github.com/richyrach/IslandOne/releases/download/nightly/IslandOne-preview.apk)**

The Android APK is automatically compiled and published using [GitHub Actions](../../actions). Source code is in this repository.

### Current features

- Native Android settings screen, styled island preview and compact/rounded style selection
- Floating expandable music player with artwork, media controls, a progress bar and timestamps
- Notification previews and action buttons when supported by the originating app
- Charging/battery status and quick timers
- Vertical/horizontal positioning, adjustable width and height
- Foreground service so the island can keep running; reduced idle polling
- Optional GitHub update checks when opening the app, plus **Download** in an update dialog

No file tray, camera, ad SDK, or trackers.

### Enable on a Samsung

1. Install the APK, open IslandOne, and enable **Display over other apps**.
2. Enable **Notification access** for media sessions and preview notifications.
3. Go to **Settings → Apps → IslandOne → Battery** and select **Unrestricted**, if shown. Actual battery settings vary by One UI version.
4. Adjust the island position around your camera cutout. Turn off competing island apps while comparing them.

If Android blocks notification access for a sideloaded app, open its **App info**, tap the upper-right menu, and check whether **Allow restricted settings** is available. Only allow access if you trust the app.

### Updates and signing

The update checker fetches a JSON file from this project's public GitHub release. It performs no analytics and sends no identifiers. Downloading an update opens GitHub in your browser; Android controls installation.

**Important:** These are GitHub **debug APKs**. Their signing identity can change between builds, so Android may refuse to install the next APK on top of the previous one. In that case, uninstall the old preview first (this resets settings). Stable update-in-place behavior needs an owner-controlled signing key configured securely in GitHub Actions; never commit that private key.

### Limitations

Android does not give third-party overlays Apple's system-level Dynamic Island integration. Some Android or Samsung system UI takes priority over overlays. Notification data and actions vary by app. The overlay is not a replacement for Samsung's Now Bar, and cannot remove the physical camera cutout.

For now, this is **preview software**, not a production-signed release or something tested on every phone.

## Building locally

Use Android Studio with JDK 17 and Android SDK 35, or execute `gradle assembleDebug` with Gradle 8.9. The CI builds the Android app from scratch and attaches an APK to the nightly pre-release.

## Project license

MIT. See [LICENSE](LICENSE). Contributions, bug reports and device screenshots are welcome.
