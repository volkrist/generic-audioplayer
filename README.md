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

### Banner

<p align="center">
  <img src="screenshots/banner.png" alt="AudioPlayer banner" width="100%">
</p>

### Light theme

<p align="center">
  <img src="screenshots/Light/storage_access.jpg" width="32%" alt="Storage access">
  <img src="screenshots/Light/storage_scan.jpg" width="32%" alt="Library scan">
  <img src="screenshots/Light/all_songs_malibu.jpg" width="32%" alt="All songs">
</p>
<p align="center">
  <img src="screenshots/Light/albums.jpg" width="32%" alt="Albums">
  <img src="screenshots/Light/artists.jpg" width="32%" alt="Artists">
  <img src="screenshots/Light/playlists.jpg" width="32%" alt="Playlists">
</p>
<p align="center">
  <img src="screenshots/Light/genres.jpg" width="32%" alt="Genres">
  <img src="screenshots/Light/search.jpg" width="32%" alt="Search">
  <img src="screenshots/Light/album_collection.jpg" width="32%" alt="Album collection">
</p>
<p align="center">
  <img src="screenshots/Light/now_playing_malibu.jpg" width="32%" alt="Now playing">
  <img src="screenshots/Light/now_playing_magenta.jpg" width="32%" alt="Now playing magenta">
  <img src="screenshots/Light/now_playing_melrose.jpg" width="32%" alt="Now playing melrose">
</p>

### Dark theme

<p align="center">
  <img src="screenshots/Dark/storage_access.jpg" width="32%" alt="Storage access">
  <img src="screenshots/Dark/storage_scan.jpg" width="32%" alt="Library scan">
  <img src="screenshots/Dark/all_songs_malibu.jpg" width="32%" alt="All songs">
</p>
<p align="center">
  <img src="screenshots/Dark/albums.jpg" width="32%" alt="Albums">
  <img src="screenshots/Dark/artists.jpg" width="32%" alt="Artists">
  <img src="screenshots/Dark/playlists.jpg" width="32%" alt="Playlists">
</p>
<p align="center">
  <img src="screenshots/Dark/genres.jpg" width="32%" alt="Genres">
  <img src="screenshots/Dark/search.jpg" width="32%" alt="Search">
  <img src="screenshots/Dark/album_collection.jpg" width="32%" alt="Album collection">
</p>
<p align="center">
  <img src="screenshots/Dark/now_playing_malibu.jpg" width="32%" alt="Now playing">
  <img src="screenshots/Dark/now_playing_magenta.jpg" width="32%" alt="Now playing magenta">
  <img src="screenshots/Dark/now_playing_melrose.jpg" width="32%" alt="Now playing melrose">
</p>

### Accent colors (Material You)

Same screen with different dynamic accents — light and dark.

<p align="center">
  <img src="screenshots/Light/all_songs_default.jpg" width="16%" alt="Default">
  <img src="screenshots/Light/all_songs_elm.jpg" width="16%" alt="Elm">
  <img src="screenshots/Light/all_songs_jacksonspurple.jpg" width="16%" alt="Jacksons Purple">
  <img src="screenshots/Light/all_songs_magenta.jpg" width="16%" alt="Magenta">
  <img src="screenshots/Light/all_songs_malibu.jpg" width="16%" alt="Malibu">
  <img src="screenshots/Light/all_songs_melrose.jpg" width="16%" alt="Melrose">
</p>
<p align="center">
  <img src="screenshots/Dark/all_songs_default.jpg" width="16%" alt="Default">
  <img src="screenshots/Dark/all_songs_elm.jpg" width="16%" alt="Elm">
  <img src="screenshots/Dark/all_songs_jacksonspurple.jpg" width="16%" alt="Jacksons Purple">
  <img src="screenshots/Dark/all_songs_magenta.jpg" width="16%" alt="Magenta">
  <img src="screenshots/Dark/all_songs_malibu.jpg" width="16%" alt="Malibu">
  <img src="screenshots/Dark/all_songs_melrose.jpg" width="16%" alt="Melrose">
</p>

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
