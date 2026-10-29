<p align="center">
  <img src="docs/images/catch-logo.png" alt="Catch" width="240">
</p>

<h3 align="center">Your Hydrow, your shows, your family.</h3>

<p align="center">
  An open-source home screen for the Hydrow rower. Row with live metrics while
  Netflix, Plex, or Disney+ plays right beside them, and turn every session into
  goals, challenges, and badges the whole household chases together.
</p>

<p align="center">
  <a href="LICENSE"><img alt="MIT license" src="https://img.shields.io/badge/license-MIT-1479FD"></a>
  <a href="https://github.com/thegregstengel/catch/releases/latest"><img alt="Latest release" src="https://img.shields.io/github/v/release/thegregstengel/catch?color=7E4DFD&label=release"></a>
  <a href="https://github.com/thegregstengel/catch/actions/workflows/ci.yml"><img alt="CI" src="https://github.com/thegregstengel/catch/actions/workflows/ci.yml/badge.svg"></a>
  <img alt="Device" src="https://img.shields.io/badge/device-Hydrow%20rower%20(rooted)-0CE568">
</p>

---

## The idea

A Hydrow is a great rower with a big screen attached, and that screen only
does one thing. When the membership lapses, or you would rather row through an
episode than a class, the screen has nothing for you.

Catch gives the screen back:

**Pick who is rowing. Tap Go. Watch whatever you like. Your meters count.**

Catch replaces the stock launcher on a rooted Hydrow tablet. It reads the
rower's brake directly, so power, split, stroke rate, and distance are live from
the first stroke, and it plays streaming apps inside its own frame, so the
numbers never leave the screen. Every meter lands on a family board, where
weekly goals, challenges, streaks, and badges keep everyone coming back.

<p align="center">
  <img src="docs/images/dashboard.png" alt="The Catch dashboard during a row" width="820">
</p>

## What it looks like

Screens from the current build, captured on a Hydrow tablet with a demo family.

<table>
  <tr>
    <td width="50%"><img src="docs/images/summary.png" alt="Session summary"><br><sub><b>Session summary.</b> Time, distance, average split, stroke rate, strokes, power, and calories, plus every goal, challenge tier, and badge the row just earned.</sub></td>
    <td width="50%"><img src="docs/images/challenges.png" alt="Goals and awards"><br><sub><b>Goals and awards.</b> Family weekly meters, monthly bronze, silver, and gold tiers, show-up weeks, and scenic routes. Always on: row and they fill in.</sub></td>
  </tr>
  <tr>
    <td width="50%"><img src="docs/images/profiles.png" alt="Who is rowing?"><br><sub><b>Who is rowing?</b> A profile for everyone in the house, with a photo or an illustrated avatar. Catch remembers each person's headphones and heart rate strap.</sub></td>
    <td width="50%"><img src="docs/images/updates.png" alt="Updates in Catch Settings"><br><sub><b>Updates.</b> Catch checks for new releases once a day and installs them from Settings in a few seconds, on a stable or beta channel.</sub></td>
  </tr>
</table>

## What it does today

- **Live rower metrics.** Power, calories, split, average split, stroke rate,
  strokes, elapsed time, and distance against your session goal, read straight
  from the Hydrow brake about 100 times a second. Set the resistance from Catch.
- **Watch while you row.** Netflix, Plex, Paramount+, Disney+, and Amazon Music
  open inside the Catch frame with touch and Back working, so the metrics and
  the family board stay on screen. Aurora Store installs the apps without
  Google Play.
- **Family profiles.** Everyone picks themselves on the "Who is rowing?" screen.
  Each profile keeps its own history, badges, and Bluetooth devices.
- **Heart rate.** Pair a Bluetooth heart rate strap once and Catch connects it
  whenever that person rows, with live heart rate and zone.
- **Goals, challenges, and badges.** A weekly family leaderboard with last
  week's winner, family weekly and monthly goals, tiered challenges, and
  distance and streak badges.
- **Session summary.** Finish a row and see the whole session, plus everything
  it earned, at a glance.
- **Your data stays home.** Everything lives on the tablet. Export workouts as
  JSON or CSV, or wipe personal data, from Catch Settings.
- **Updates in place.** Catch updates itself from GitHub Releases, checks every
  download's SHA-256, and keeps all of your history.

## Get it

Catch runs on a **rooted** Hydrow rower tablet (Android 10). Rooting needs a
computer, about an hour, and some comfort with the Android command line.

1. Follow [Install Catch](docs/install.md) to root the tablet with Magisk.
1. Download `catch-magisk-<version>.zip` from the
   [latest release](https://github.com/thegregstengel/catch/releases/latest)
   and install it with Magisk.
1. Reboot. Catch opens as the Home screen and walks you through setup.

After that, Catch updates itself. See [Updates and releases](docs/updates.md).

## How it works

```text
            Hydrow brake ──serial──▶ ┌──────────────────────┐ ◀── Bluetooth heart rate
                                     │        Catch         │
                                     │  metrics, profiles,  │
     Streaming app ◀─secure display─ │  goals, badges       │ ◀── GitHub Releases
     (Netflix, Plex, ...)            └──────────────────────┘
                                       privileged Home app
                                       (Magisk module)
```

- **Catch is the Home app.** A Magisk module installs it as a privileged system
  app and turns off the stock Hydrow launcher, including its remote commands,
  updater, and logging.
- **The rower is read directly.** Catch speaks the brake controller's serial
  protocol itself. No cloud and no membership are involved.
- **Streaming apps play in a secure virtual display** owned by a small system
  process that ships inside the Catch APK. That is what lets copy-protected
  video play inside Catch's frame.
- **Updates are signed and verified.** Releases are built and signed by GitHub
  Actions, and tablets only accept updates signed with the same key.

The details are in [How Catch works](docs/how-it-works.md).

## Documentation

- [Install Catch](docs/install.md)
- [How Catch works](docs/how-it-works.md)
- [Updates and releases](docs/updates.md)
- [Develop Catch](docs/development.md)
- [Changelog](CHANGELOG.md)

## Help build Catch

Issues and pull requests are welcome. The most useful things right now:

1. **Run Catch on your Hydrow and report what breaks.** Open an issue with what
   you did, what happened, and `adb logcat` output if you can get it.
1. **Try other streaming apps in the pane** and report which ones work.
1. **Send a pull request** for a bug you can reproduce or a small, well-scoped
   feature. For anything larger, open an issue first so we can agree on the
   shape.

[CONTRIBUTING.md](CONTRIBUTING.md) has the ground rules, and
[Develop Catch](docs/development.md) has the toolchain.

## License

MIT. See [LICENSE](LICENSE).

Hydrow is a trademark of Hydrow, Inc. Catch is an independent project and is
not affiliated with, endorsed by, or supported by Hydrow. Rooting your rower is
at your own risk. Netflix, Plex, Paramount+, Disney+, and Amazon Music are
trademarks of their respective owners.
