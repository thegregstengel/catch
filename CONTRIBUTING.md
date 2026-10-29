# Contributing to Catch

Thanks for helping. Catch is a small project, so a little coordination goes a
long way.

## Before you start

- For a bug, open an issue with the steps to reproduce it, what you expected,
  and what happened. `adb logcat` output helps a lot.
- For a feature or anything larger than an afternoon, open an issue first so we
  can agree on the shape before you write it.

## Pull requests

1. Fork the repository and work on a branch.
1. Follow [Develop Catch](docs/development.md) to build, and run the checks CI
   runs:

   ```bash
   cd android
   ./gradlew testReleaseUnitTest lintRelease
   ```

1. Add a line under `## [Unreleased]` in [CHANGELOG.md](CHANGELOG.md) for any
   change a rower would notice.
1. Open the pull request against `main` and describe what changed and how you
   tested it, ideally on a real Hydrow.

## Ground rules

- **Never commit secrets or device data.** No keystores, passwords, serial
  numbers, partition dumps, or captured traffic.
- **Don't copy Hydrow's code.** Catch talks to the rower's hardware through its
  own implementation, and stays MIT licensed.
- **Database changes need a migration.** Updates must never lose a family's
  history. CI checks that the exported schemas are current.
- **Privileged permissions need the module first.** A privileged permission
  without a matching allowlist entry stops the tablet from booting. See
  [How Catch works](docs/how-it-works.md#the-magisk-module).

By contributing, you agree that your contributions are licensed under the
[MIT License](LICENSE).
