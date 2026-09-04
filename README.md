# Youseif Player Pro

**Slogan:** Youseif plarer pro

Professional Android **Web Video Player** — Kotlin + Jetpack Compose + WebView (not ExoPlayer).

## What was fixed / completed in this revision

- **WebView ↔ controls binding**: `onWebViewReady` exposes live WebView; Play/Pause/Seek/Mute/Speed/AudioOnly call real JS bridge commands
- **Seek bar**: interactive slider when duration is known from `<video>`
- **M3U Import**: from **URL** + Text + File
- **M3U Export**: save `.m3u` via system Create Document picker
- Stronger video detection script + re-inject after page load
- Gestures, Fullscreen, PiP, Audio Only, Data Saver, UA/Referer/Headers
- Library starts **empty** (no demo data)
- Autoplay **OFF** by default
- Language EN/AR changes **text only**
- **Player controls + bottom nav + gestures are forced LTR** (`CompositionLocalProvider` + `LayoutDirection.Ltr`) — Arabic never reverses button order
- Direct media URLs (mp4/m3u8/…) load an internal HTML5 `<video>` page so the bridge always works
- Hint when no `<video>` is detected on a web page
- Quick Play remembers last URL
- M3U import shows valid/error counts

## Build

1. Open `YouseifPlayerPro` in Android Studio (JDK 17)
2. Sync Gradle → Run (API 26+)

```bash
./gradlew :app:assembleDebug
```

## Architecture

```
ui/          screens + components (WebPlayerView, PlayerControls)
viewmodel/   Library, Player, Settings
data/        Room, DataStore, M3U parser, repositories
webview/     WebPlayerClient, ChromeClient, JsBridge
utils/       UrlAnalyzer, NetworkFetcher
```

## Notes

- Primary engine is **WebView + HTML5**, not Media3/ExoPlayer
- No sample channels or fake playback stats
- Unsupported features are hidden/disabled, not fake buttons
