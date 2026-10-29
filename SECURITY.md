# Security policy

Catch runs with more access than a normal app. The Magisk module installs it as
a privileged system app, gives it root for the rower's serial port, and starts a
system process that hosts the streaming apps. Catch also installs its own
updates. A security problem in Catch can therefore affect the whole tablet, so
please report it privately.

## Report a problem

Use [GitHub private vulnerability reporting](https://github.com/thegregstengel/catch/security/advisories/new).
Don't open a public issue. Include:

- The Catch version, from **Catch Settings > Updates**.
- What an attacker could do, and what they would need, such as access to the
  same Wi-Fi network or an app installed on the tablet.
- The steps to reproduce the problem.

You'll get a reply within a week. Once a fix is released, the advisory is
published with credit to you, unless you'd rather stay anonymous.

## Supported versions

Only the latest release gets security fixes. Catch updates itself, so the fix
reaches tablets once their owners install the update from Catch Settings.

## What's in scope

- The Catch app, including the updater and how it checks downloads.
- The media pane host (`catch_display_host`) and its binder interface.
- The Magisk module: the permissions allowlist, SELinux rules, and `service.sh`.
- The release workflow and how releases are signed.

Rooting a Hydrow is itself a security tradeoff. Problems with Magisk, Android,
or Hydrow's own software are out of scope; please report those to their
projects.
