# Changelog

Catch versions are dates: `2026.10.29` is a release, `2026.10.29.2` a second
release that day, and `2026.10.29-beta.1` a beta. The release workflow publishes
the section that matches the tag as the release notes.

## [Unreleased]

## [2026.10.29.2-beta.1]

### Fixed

- Catch Settings keeps saying "Updated to" the new version after an update,
  even if Catch was reopened, until you check for updates again.
- Release notes in Catch Settings read as paragraphs instead of short lines.

## [2026.10.29]

The first stable release of Catch. It contains everything in 2026.10.29-beta.1,
and was tested by updating a tablet from that beta inside Catch.

## [2026.10.29-beta.1]

The first public build of Catch.

### Rowing

- Live metrics from the Hydrow brake: elapsed time, distance against a session
  goal, power, calories, split, average split, stroke rate, strokes, and heart
  rate and zone from a Bluetooth strap.
- Go, Pause, and Finish in the top bar, with resistance in Catch Settings.
- A session summary when you finish: every stat, plus the goals, challenge tiers,
  and badges the session earned.

### Family

- Profiles with photos or illustrated avatars, picked on a "Who is rowing?" screen.
- A weekly family meters board, family weekly and monthly goals, challenges with
  bronze, silver, and gold tiers, and distance and streak badges.

### Watch while you row

- Netflix, Plex, Paramount+, Disney+, and Amazon Music play inside the Catch
  frame, with touch and Back working in the pane.
- Aurora Store installs the streaming apps without Google Play.

### Updates

- Catch checks GitHub Releases once a day and installs updates from Catch
  Settings on a stable or beta channel. Downloads are checked against their
  SHA-256 before they install.

### Device

- A Magisk module makes Catch the privileged Home app, runs the media pane host,
  adds Catch boot screens, and turns off the stock Hydrow launcher, including its
  remote commands, updater, and logging.
