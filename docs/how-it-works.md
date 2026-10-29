# How Catch works

Catch is one Android app plus a Magisk module. The app is the Home screen, the
dashboard, and the family's data. The module gives the app the system access a
normal app can't have on the Hydrow tablet.

```text
 ┌──────────────────────── Hydrow tablet (Android 10, rooted) ────────────────────────┐
 │                                                                                    │
 │   Catch (privileged Home app)                     Media pane host (uid 1000)       │
 │   ┌──────────────────────────────┐   binder       ┌─────────────────────────────┐  │
 │   │ Dashboard, profiles, goals   │ ─────────────▶ │ Secure virtual display      │  │
 │   │ Room database                │   surface,     │ Launches the streaming app  │  │
 │   │ Rower HAL ◀── /dev/ttyS1     │   touches      │ Injects touch and Back      │  │
 │   │ Heart rate (Bluetooth LE)    │                └─────────────────────────────┘  │
 │   │ Updater ◀── GitHub Releases  │                                                 │
 │   └──────────────────────────────┘                                                 │
 │                                                                                    │
 │   Magisk module: priv-app install, permissions allowlist, SELinux rules,          │
 │   host keepalive, boot screens, stock launcher disabled                           │
 └────────────────────────────────────────────────────────────────────────────────────┘
```

## The rower

The tablet talks to the rower's brake controller over a serial port,
`/dev/ttyS1` (also linked as `/dev/ttyMT1`), at 921,600 baud. The protocol is
plain text: space-separated fields, with commands ending in `\n\r`.

| Direction | Example | Meaning |
| --- | --- | --- |
| Command | `Cm 1` | Turn on continuous reporting. |
| Command | `Cl 8` | Set the resistance level. |
| Report | `ds2 ...` | A completed stroke: distance and average power. |
| Report | `di2 rpm rev watts handleMeters sequence` | Instantaneous samples, about 100 per second. |
| Report | `rl 8` | The current resistance level. |

`rower/BrakeProtocol.kt` parses the reports, and `rower/SessionAccumulator.kt`
turns them into session metrics. Catch publishes instantaneous power about ten
times a second, and strokes and resistance changes immediately.

SELinux on this build denies privileged apps a direct open of the serial port,
even with the module's live policy rules. Catch therefore reads the port through
a long-lived `su` process, which the module grants without a prompt. If no port
works at all, Catch runs a simulated rower so the dashboard still behaves.

## Watching while you row

Android 10 normally refuses to keep an app on another app's virtual display
unless the app opts in with `allowEmbedded`, and streaming apps don't. Android
skips that check when the system user (uid 1000) owns the display.

So the module starts a small host process as uid 1000, using `app_process` and a
class that ships inside the Catch APK (`sysdisplay/SystemDisplayHost.java`):

1. The host publishes a binder service named `catch_display_host`. Every call
   checks that the caller is Catch.
1. When you open an app, Catch hands the host a surface from the media pane.
1. The host creates a **secure**, public virtual display that renders into that
   surface, and launches the app onto it.
1. Catch forwards touches and Back to the host, which injects them into the
   display, since a virtual display has no input device of its own.
1. When you close the pane, or if Catch dies, the host stops the app and releases
   the display.

The display must be secure: copy-protected video on a non-secure display stalls
the graphics compositor until the tablet reboots, and only uid 1000 can create a
secure virtual display on this tablet.

The host runs code from the Catch APK it started with. After Catch updates, the
host notices the new APK within five seconds and exits, and the module's
keepalive loop starts it again from the new APK.

The interface between Catch and the host is
[`ICatchDisplayHost.aidl`](../android/app/src/main/aidl/app/catchrower/sysdisplay/ICatchDisplayHost.aidl).

## Data

Everything stays on the tablet in a Room database: profiles, workouts, badges,
challenge progress, and remembered Bluetooth devices. Nothing is sent anywhere.
Catch Settings can export workouts as JSON or CSV, and can wipe all personal
data.

Every schema change ships with a migration, and the exported schemas live in
`android/app/schemas`. An update never wipes the family's history.

## The Magisk module

The module source is in [`magisk/`](../magisk), and
[`scripts/build-module.sh`](../scripts/build-module.sh) assembles the zip.

| Piece | What it does |
| --- | --- |
| `system/priv-app/Catch` | Installs Catch as a privileged system app. |
| `system/etc/permissions` | Allowlists Catch's privileged permission (`INSTALL_PACKAGES`). |
| `service.sh` | Sets Catch as Home, disables the stock Hydrow launcher, applies the SELinux rules live, grants the serial port, and keeps the media pane host running. |
| `sepolicy.rule` | The same SELinux rules, for devices where Magisk can load them at boot. |
| `system/media/bootanimation.zip` | The Catch boot animation. |
| `overlays/fallback-home` | Paints the screen at the end of boot black instead of the stock loading screen. |
| `overlays/systemui-nav` | Catch's Back and Home navigation icons. |

This tablet enforces the privileged-permission allowlist: a privileged
permission that Catch requests but the module does not allowlist stops the
tablet from booting. A release that adds one must ship the module change first.
