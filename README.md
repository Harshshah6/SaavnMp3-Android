# Melotune

Melotune (formerly SaavnMp3) is a free, ad-free Android music streaming and
offline-listening app. Browse and play music, manage your queue and playlists,
view synchronized lyrics, and download tracks in multiple formats. No account
is required.

[![Latest GitHub release](https://img.shields.io/github/v/release/Harshshah6/SaavnMp3-Android)](https://github.com/Harshshah6/SaavnMp3-Android/releases)
[![Get it at IzzyOnDroid](https://img.shields.io/endpoint?url=https://apt.izzysoft.de/fdroid/api/v1/shield/com.harsh.shah.saavnmp3)](https://apt.izzysoft.de/packages/com.harsh.shah.saavnmp3)
[![GitHub downloads](https://img.shields.io/github/downloads/Harshshah6/SaavnMp3-Android/total?label=GitHub%20downloads)](https://github.com/Harshshah6/SaavnMp3-Android/releases)
[![IzzyOnDroid Downloads](https://img.shields.io/badge/dynamic/json?url=https://dlstats.izzyondroid.org/iod-stats-collector/stats/basic/yearly/rolling.json&query=$.['com.harsh.shah.saavnmp3']&label=IzzyOnDroid%20Downloads&style=)](https://apt.izzysoft.de/packages/com.harsh.shah.saavnmp3)

**Website:** [Melotune](https://harshshah6.github.io/melotune-web/)
· **Downloads:** [GitHub Releases](https://github.com/Harshshah6/SaavnMp3-Android/releases)


## Features

- **Ad-Free Listening**: Stream music without in-app advertisements.
- **Queue Management**: View upcoming tracks, remove songs, and **drag-and-drop to reorder** the playback queue on the fly.
- **Smart Mini-Player**: Persistent mini-player across all screens featuring a live, colour-adaptive progress bar.
- **Player Gestures**: Swipe left or right on album art to smoothly skip to next or previous tracks.
- **Synchronized Lyrics**: Built-in dynamic LRC lyrics display with real-time tracking and tap-to-toggle view.
- **Search with History**: Instant global search with recent query chips, category filters (Songs, Albums, Playlists, Artists), and auto-suggestions.
- **Background Playback Settings**: Configurable toggle to keep playing or stop playback when the app is removed from recents.
- **Library & Playlists**: Create personalized playlists with full JSON **Export & Import** backup support.
- **Multi-Format Downloads**: Download tracks in **MP3** (recommended for car stereos and universal compatibility), **M4A (AAC)** (native source stream), or **OGG (Vorbis)** with embedded synchronized LRC lyrics, full metadata, and high-res artwork.
- **Offline Download Manager**: Browse downloaded tracks, inspect detailed audio specs, play offline, or delete songs directly from device storage.
- **Modern Dark/Light UI**: Sleek, Spotify-inspired interface with dynamic color extraction, shimmer loaders, and smooth animations.
- **No Account Required**: The app does not require an account or include in-app analytics. Outbound links use UTM campaign parameters; third-party sites may apply their own privacy policies.

## Screenshots

<div align="center">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/home.png" alt="Home screen" width="220"/>
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/settings.png" alt="Settings screen" width="220"/>
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/about.png" alt="About screen" width="220"/>
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/player.png" alt="Player screen" width="220"/>
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/widgets.png" alt="Widgets screen" width="220"/>
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/music_queue.png" alt="Music queue screen" width="220"/>
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/artist.png" alt="Artist screen" width="220"/>
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/search.png" alt="Search screen" width="220"/>
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/album.png" alt="Album screen" width="220"/>
</div>


## Getting Started

### Requirements

- Android Studio with the Android SDK for API 37
- JDK 17 (the Gradle wrapper downloads the configured Gradle version)
- Android 7.0 (API 24) or later device/emulator

### Build and test

Clone the repository and open it in Android Studio, or build from a terminal:

```bash
git clone https://github.com/Harshshah6/SaavnMp3-Android.git
cd SaavnMp3-Android
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

On Windows, run `gradlew.bat` instead of `./gradlew`. The debug APK is written
to `app/build/outputs/apk/debug/`.

To install and run, open the project in Android Studio and select a connected
device or emulator. Release builds are signed with the debug key unless release
signing credentials are configured; do not distribute such a build.

## Built With

*   [Java 17](https://openjdk.org/projects/jdk/17/) & [Kotlin](https://kotlinlang.org/) - Programming languages.
*   [saavn.sumit.co](https://github.com/sumitkolhe/jiosaavn-api) - The unofficial JioSaavn API for music search, metadata, and streams.
*   [Media3 ExoPlayer](https://developer.android.com/media/media3/exoplayer) - Audio playback engine for streaming and caching.
*   [FFmpegKit (Maintained)](https://github.com/arthenica/ffmpeg-kit) - Audio transcoding engine for converting downloads into MP3 (LAME) and OGG (Vorbis).
*   [JAudioTagger](https://github.com/RouHim/jaudiotagger) - Audio metadata tagging library for ID3v2, MP4, and Vorbis tags, lyrics, and album art.
*   [LrcLib](https://lrclib.net/) - Client for fetching synchronized LRC and plain lyrics.
*   [Room Database](https://developer.android.com/training/data-storage/room) - Local SQLite persistence for offline caching.
*   [Glide](https://github.com/bumptech/glide) - Image loading, memory/disk caching, and bitmap transformations.
*   [Gson](https://github.com/google/gson) - Java serialization and deserialization library for JSON.
*   [OkHttp](https://github.com/square/okhttp) & [Logging Interceptor](https://github.com/square/okhttp/tree/master/okhttp-logging-interceptor) - HTTP client for network operations.
*   [SlidingRootNav](https://github.com/yarolegovich/SlidingRootNav) - Drawer layout with smooth slide animations.
*   [Shimmer](https://github.com/facebookarchive/shimmer-android) - Shimmer placeholder animations for loading views.
*   [Overscroll-decor](https://github.com/EverythingMe/overscroll-decor) - iOS-like overscroll bounce physics.
*   [SSP](https://github.com/intuit/ssp) & [SDP](https://github.com/intuit/sdp) - Responsive size units for scalable screen densities.
*   [Palette](https://developer.android.com/develop/ui/views/graphics/palette-colors) - Dynamic color extraction from artwork.
*   [SwipeRefreshLayout](https://developer.android.com/jetpack/androidx/releases/swiperefreshlayout) - Pull-to-refresh swipe gesture.
*   [Paginate](https://github.com/MarkoMilos/Paginate) - Pagination and infinite scroll for RecyclerViews.

<!-- ## Star History
<a href="https://www.star-history.com/?repos=harshshah6%2FSaavnMp3-Android&type=date&legend=top-left">
 <picture>
   <source media="(prefers-color-scheme: dark)" srcset="https://api.star-history.com/chart?repos=harshshah6/SaavnMp3-Android&type=date&theme=dark&legend=top-left&sealed_token=TnLJLQULG0pZ0ArZCpyK9vygQTzTn5T5MVEYjIA8G3a5ZWmhLqfAUT-QhCWo5MpS2eFbYABtgTcM4Tlmw_aN-fOif42bR9HEn4E9tNORkcuFoy4HRPJDMQ" />
   <source media="(prefers-color-scheme: light)" srcset="https://api.star-history.com/chart?repos=harshshah6/SaavnMp3-Android&type=date&legend=top-left&sealed_token=TnLJLQULG0pZ0ArZCpyK9vygQTzTn5T5MVEYjIA8G3a5ZWmhLqfAUT-QhCWo5MpS2eFbYABtgTcM4Tlmw_aN-fOif42bR9HEn4E9tNORkcuFoy4HRPJDMQ" />
   <img alt="Star History Chart" src="https://api.star-history.com/chart?repos=harshshah6/SaavnMp3-Android&type=date&legend=top-left&sealed_token=TnLJLQULG0pZ0ArZCpyK9vygQTzTn5T5MVEYjIA8G3a5ZWmhLqfAUT-QhCWo5MpS2eFbYABtgTcM4Tlmw_aN-fOif42bR9HEn4E9tNORkcuFoy4HRPJDMQ" />
 </picture>
</a> -->

## Contributing

Contributions are welcome. See the [contribution guide](CONTRIBUTING.md) for
setup, coding, and pull request guidance.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgements

- Inspired by the clean and minimal UI of Spotify.
- A big thanks to [saavn.sumit.co](https://github.com/sumitkolhe/jiosaavn-api) for the unofficial JioSaavn API.
- A big thanks to [@Samyak2403](https://github.com/samyak2403) for implementing the LrcLib for lyrics.

## Contributors ✨

Thanks to these wonderful people for their contributions:

<a href="https://github.com/Harshshah6/SaavnMp3-Android/graphs/contributors">
  <img src="https://contrib.rocks/image?repo=Harshshah6/SaavnMp3-Android" />
</a>

Contributions of any kind are welcome!
