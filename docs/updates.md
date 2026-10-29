# Updates and releases

## How Catch updates

Catch updates itself from [GitHub Releases](https://github.com/thegregstengel/catch/releases).

1. Once a day, when Catch comes to the front, it asks GitHub for the newest
   release on its channel. **Stable** follows releases; **Beta** also follows
   pre-releases. Pick the channel in **Catch Settings > Updates**.
1. When there is a newer version, the "Who is rowing?" screen says so.
   **Catch Settings > Updates** shows the release notes and an **Install update**
   button. Catch never installs on its own, so an update can't interrupt a row.
1. Catch downloads `catch-<version>.apk` and checks it against the SHA-256 that
   GitHub publishes for the file (or the release's `SHA256SUMS`). A mismatch stops
   the install.
1. Catch installs the APK silently with `PackageInstaller`, using the
   `INSTALL_PACKAGES` permission that the Magisk module grants. Android stops
   Catch, puts the new version in place, and starts Catch again as Home. Catch
   says "Updated to <version>" in Settings.
1. The media pane host notices the new APK and restarts from it.

An update keeps everything: profiles, workouts, badges, settings, and the
privileged status that the module gave Catch. Android only accepts the update if
it is signed with the same key as the installed Catch.

Updates replace the app, not the module. A release whose notes say "Module
update required" changes something only the module can, such as the boot screens
or a new privileged permission; install that release's module zip as in
[Install Catch](install.md#step-5-install-the-catch-module).

## Versions

Versions are dates.

| Version | Meaning |
| --- | --- |
| `2026.10.29` | A release. |
| `2026.10.29.2` | A second release on the same day. |
| `2026.10.29-beta.1` | A beta, published as a GitHub pre-release. |
| `2026.10.29.2-dev` | A local build made after `2026.10.29`. |

Nothing in the source names a version. The release workflow takes it from the
tag, and `android/app/build.gradle.kts` derives the Android version code from it
so every build installs over the one before: `2026.10.29-beta.1` is 261029101,
`2026.10.29` is 261029199, and `2026.10.29.2-dev` is 261029200.

## Cut a release

1. Move the notes under `## [Unreleased]` in [`CHANGELOG.md`](../CHANGELOG.md) to a
   new `## [2026.10.29]` heading, and commit. If the release needs a new module,
   start its notes with **Module update required**.
1. Tag and push:

   ```bash
   git tag v2026.10.29
   git push origin v2026.10.29
   ```

The [Release workflow](../.github/workflows/release.yml) then does the following:

1. Runs the unit tests and lint, and builds the signed release APK with the
   version from the tag.
1. Refuses to publish if the APK isn't signed with the Catch key, or if its
   version doesn't match the tag.
1. Builds the Magisk module zip around the same APK.
1. Publishes `catch-<version>.apk`, `catch-magisk-<version>.zip`, and `SHA256SUMS`
   as a GitHub release, with the matching changelog section as the notes. A tag
   with a suffix such as `-beta.1` becomes a pre-release, which only tablets on the
   Beta channel see.

Try a change on the Beta channel first: tag `v2026.10.29-beta.1`, update a tablet
set to Beta, then tag `v2026.10.29` from the same commit.

## The signing key

Every Catch release must be signed with the same key, or tablets refuse the
update. The release workflow reads it from four repository secrets:

| Secret | Value |
| --- | --- |
| `CATCH_KEYSTORE_BASE64` | `base64 -w0 catch-upload.jks` |
| `CATCH_KEYSTORE_PASSWORD` | The keystore password. |
| `CATCH_KEY_ALIAS` | `catch` |
| `CATCH_KEY_PASSWORD` | The key password. |

Keep an offline backup of the keystore and its passwords. If the key is lost,
every tablet has to remove Catch and install the module again.

To sign local release builds, copy
`android/keystore/keystore.properties.example` to `keystore.properties` next to
it and fill it in. That file and the keystore are gitignored.
