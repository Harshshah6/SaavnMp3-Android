# 32-bit FFmpeg native libraries

These `armeabi-v7a` and `x86` libraries are required so the app can be installed on
32-bit Android devices (for example, many Android 10 car head units).

## Why they are here

The project uses `dev.ffmpegkit-maintained:ffmpeg-kit-audio:8.1.9` for MP3/OGG
transcoding. That artifact publishes native code for **64-bit ABIs only**
(`arm64-v8a` and `x86_64`). If an APK contains native libraries but none matching
the device CPU, Android refuses to install it with:

```
INSTALL_FAILED_NO_MATCHING_ABIS
```

which package installers commonly display as "Application is incompatible with
this device".

The 32-bit libraries in this directory come from the last official LTS build,
`com.arthenica:ffmpeg-kit-audio:6.0-2.LTS`, and are merged into the APK by Gradle.
The 64-bit libraries continue to come from the maintained 8.1.9 AAR, which is
16 KB page-size aligned for newer Android devices.

## Contents

Only the baseline (non-NEON) armv7 libraries are included. FFmpegKit automatically
falls back to them when the optional `*_neon` libraries are absent, which keeps the
APK smaller and avoids NEON crashes on older car head units.

Do not delete these files. Do not replace the 64-bit libraries in this directory
with the 6.0-2.LTS versions: the 8.1.9 64-bit libraries are 16 KB aligned and are
needed on newer Android releases.

## License

FFmpegKit and FFmpeg are licensed under the LGPL v3.0 (the non-GPL packages).
See the FFmpegKit project for the full license text and third-party notices.
