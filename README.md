# IslandOne

An open-source Dynamic Island-style overlay for Android.

**Early preview.** Designed around the centered punch-hole camera on the Galaxy S24 FE; also intended to support other Android devices.

### Goals
- Compact, animated floating pill
- Music playback and album artwork
- Notification previews
- Battery and charging indicators
- On-device timers
- Adjustable size and position

No file tray or camera tools. No trackers, ads, or internet permission.

### Get the APK
When the Android build workflow succeeds, download the test APK from [Actions](../../actions). A publicly downloadable preview release will be published by the workflow as **nightly**.

> Pre-release software: not yet tested on physical S24 FE hardware. Android overlays cannot truly replace the system status bar, the cutout, the lock screen or all system UI. Some integrations require notification access and may vary by app.

### Building
Android Studio + JDK 17, Android SDK 35 and Gradle 8.9. Run `gradle assembleDebug` or use the GitHub Actions workflow.

### Privacy
Only overlay and notification access are requested for the relevant features. There is no network permission. Notification previews may display private text over other apps; grant access only if comfortable.

### License
MIT. See [LICENSE](LICENSE).
