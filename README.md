# AudioPlayer

<p align="center">
  <img src="app/src/main/res/ic_launcher-playstore.png" alt="AudioPlayer icon" width="128" height="128">
</p>

<p align="center">
  <strong>Material You music player for Android</strong><br>
  Library, playlists, folders, equalizer, widgets, and more.
</p>

<p align="center">
  <a href="https://github.com/volkrist/zen-music-android">Source code (GitHub)</a>
  &nbsp;·&nbsp;
  <a href="https://play.google.com/store/apps/details?id=com.generic.audioplayes">Google Play</a>
</p>

<p align="center">
  <a href="https://play.google.com/store/apps/details?id=com.generic.audioplayes">
    <img alt="Get it on Google Play" src="https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png" height="64">
  </a>
</p>

---

## Highlights

- **Material You** — dynamic color, light/dark themes, multiple accent palettes  
- **Library** — tracks, albums, artists, genres, playlists, folder browser with natural path sorting  
- **Playback** — queue, shuffle, sleep timer, now playing with rich controls  
- **Audio** — built-in equalizer  
- **Tags** — view and edit embedded metadata in the app  
- **Widgets** — home screen player widgets  
- **Extras** — backup & restore, dictaphone, search, blacklist, Crashlytics-ready build  

---

## Screenshots

The old screenshot set (legacy branding) was removed from the repository. Add new images under `screenshots/` and embed them here when you have up-to-date captures.

---

## Tech stack

| Area | Libraries |
|------|-----------|
| Language | Kotlin |
| UI | Jetpack Compose, Material 3 |
| Async | Coroutines, Flow |
| DI | Hilt |
| Player | ExoPlayer / Media3 |
| Database | Room |
| Preferences | DataStore, Protocol Buffers |
| Images | Coil |
| Other | Navigation, WorkManager, Glance widgets, Firebase Crashlytics (optional), Lottie, LeakCanary (debug), Timber |

**Requirements:** Android **6.0+** (API **23**), target/compile SDK **34**. Current app version **1.2.5** (see `buildSrc`).

---

## Build from source

```bash
git clone https://github.com/volkrist/zen-music-android.git
cd zen-music-android
```

1. Set `sdk.dir` in **`local.properties`** (Android SDK path).  
2. Add **`app/google-services.json`** from [Firebase Console](https://console.firebase.google.com) if you use Google Services / Crashlytics (CI or local placeholder).  
3. Build debug APK:

```bash
# Windows
.\gradlew.bat assembleDebug

# macOS / Linux
./gradlew assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk` (debug package id ends with `.debug`).

---

## License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file.
