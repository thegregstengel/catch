## What changed

<!-- What this pull request does, and why. Link the issue it fixes, if any. -->

## How you tested it

<!-- For example: unit tests, or rowed on a Hydrow with build 2026.10.29.3. -->

## Checklist

- [ ] `./gradlew testReleaseUnitTest lintRelease` passes.
- [ ] `CHANGELOG.md` has a line under `## [Unreleased]`, if rowers would notice the change.
- [ ] Database changes include a migration and an updated schema in `app/schemas`.
- [ ] New privileged permissions are in the module's allowlist first.
- [ ] No secrets, device serial numbers, or personal data are included.
