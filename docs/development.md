# Develop Catch

## Build

You need JDK 17 (newer JDKs break this Gradle and Kotlin version) and the
Android SDK with platform 35 and build-tools 35.

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
export ANDROID_HOME=~/Android/Sdk
cd android
./gradlew testReleaseUnitTest lintRelease assembleRelease
```

CI runs the same tests and lint on every push and pull request.

## Run on a tablet

A Catch build that requests a privileged permission must reach the tablet inside
the module the first time, together with its allowlist. After that, while the
permissions are unchanged, `adb install -r` of a release build signed with the
Catch key is the quick way to iterate:

```bash
adb install -r app/build/outputs/apk/release/app-release.apk
```

To install the full module from a local build:

```bash
scripts/build-module.sh android/app/build/outputs/apk/release/app-release.apk \
  2026.10.29.2-dev 261029200 dist/catch-magisk-dev.zip
adb push dist/catch-magisk-dev.zip /data/local/tmp/
adb shell su -c 'magisk --install-module /data/local/tmp/catch-magisk-dev.zip'
adb reboot
```

The debug build (`assembleDebug`) installs as a separate app,
`app.catchrower.catch.debug`. It isn't privileged, so it can't use the media pane
host or the serial port, and it runs the simulated rower. That makes it handy
for UI work on an emulator.

## Code map

| Path | What's there |
| --- | --- |
| `ui/home` | `HomeActivity` and screen routing. |
| `ui/dashboard` | The dashboard: top bar, media pane, family board, metrics strip, session dialogs. |
| `ui/profile`, `ui/setup`, `ui/settings`, `ui/challenges` | The other screens. |
| `ui/CatchViewModel.kt` | App state and actions. |
| `rower` | The serial rower, its protocol, and the simulated rower. |
| `embed`, `sysdisplay` | The media pane client and the uid-1000 host. |
| `data` | Room entities, DAOs, the repository, badges, and challenges. |
| `update` | Release checks, download, verification, and install. |
| `hr`, `media` | Bluetooth heart rate, and audio focus and the streaming app list. |

## Conventions

- **Database changes** need a migration and a new exported schema in
  `app/schemas`. CI fails if the committed schemas are out of date.
- **Privileged permissions** go in the module allowlist first. See
  [How Catch works](how-it-works.md#the-magisk-module).
- **Screenshots** from `adb exec-out screencap -p` keep transparency. Flatten them
  onto black before judging them, for example
  `magick shot.png -background black -flatten shot.png`.
