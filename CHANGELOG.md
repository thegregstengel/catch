# Changelog

Catch versions are dates: `2026.10.29` is a release, `2026.10.29.2` a second
release that day, and `2026.10.29-beta.1` a beta. The release workflow publishes
the section that matches the tag as the release notes.

## [Unreleased]

### Added

- The screen goes dark after 5 minutes when nobody is rowing or tapping.
  A show in the pane keeps it on. Tap to wake it. Catch stays where you
  left it, including a workout in progress. The switch is on the Rower
  card in Catch Settings, behind the settings PIN, and it applies when
  you tap Save. It is on by default.

## [2026.11.01.3]

### Changed

- The settings PIN opens Catch Settings only. Switching profiles, opening
  another rower's profile, and adding a profile do not ask for it. Household
  reset and the family goal stay in Catch Settings. Opening Android Settings
  from the home screen does not ask for the PIN.

## [2026.11.01.2]

### Added

- Catch Settings has one Save button. Save applies the PIN, the monthly family
  goal, the app grid, and the update channel, then returns to the home dashboard.
- A 4-digit PIN for Catch Settings. After it is saved, Catch Settings, other
  profiles, household reset, and the family goal ask for it. A rower can still
  open their own profile without the PIN. The PIN does not lock Android Settings.
- Each profile can reset that rower's stats. Catch Settings can reset the whole
  household's metrics. Both ask you to confirm, and name what will be erased,
  before anything is deleted.

### Changed

- Weekly goals and challenges run from Sunday to the next Sunday, in the tablet
  timezone (America/New_York), matching the rowing calendar. Monthly goals and
  challenges run from the first of the month to the next first.
- The family goal is monthly. Set it in meters in Catch Settings.
- Distance is shown in meters.

## [2026.11.01]

### Added

- Brave, Prime Video, and Apple TV are on the home grid, with Netflix, Plex,
  Paramount+, and Disney+.
- Each profile has a monthly calendar of days rowed, with that rower's stats
  and awards. Saved sessions stay on the tablet, so later months and years
  still fill in.
- During the first week of January, a short year in review opens when the
  previous year has saved sessions.

## [2026.10.29.3]

Everything in 2026.10.29.3-beta.1.

## [2026.10.29.3-beta.1]

### Removed

- Amazon Music is no longer on the home grid. Music is easier from a phone.

## [2026.10.29.2]

Everything in 2026.10.29.2-beta.1.

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
