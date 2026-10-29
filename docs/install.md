# Install Catch on a Hydrow

This guide takes a Hydrow rower tablet from stock software to Catch. It covers
rooting the tablet with Magisk, installing the Catch Magisk module, and setting
up the streaming apps.

> **Warning:** Rooting replaces software on a device you paid for. Unlocking the
> bootloader erases the tablet, and a mistake while flashing can leave it unable
> to boot. Hydrow does not support any of this, and it may affect your warranty
> or your Hydrow membership. Read the whole guide before you start, and only go
> ahead if you are comfortable recovering an Android device from a computer.

## What you need

- A Hydrow rower whose tablet matches the [tested hardware](#tested-hardware).
- A Linux computer with [Android platform tools](https://developer.android.com/tools/releases/platform-tools)
  (`adb` and `fastboot`) and Python 3.
- A USB cable that reaches from the computer to the tablet.
- [mtkclient](https://github.com/bkerler/mtkclient) to read and write the boot
  partition.
- The [Magisk](https://github.com/topjohnwu/Magisk/releases) APK. Catch is tested
  with Magisk 30.7.
- The latest `catch-magisk-<version>.zip` from
  [Catch releases](https://github.com/thegregstengel/catch/releases).

## Tested hardware

Catch is built and tested on one Hydrow Origin, the original rower with the
22-inch pivoting screen. It was bought used, so its original purchase date is
unknown, and it has been in use for a few years. Before you start, check that your tablet matches. In the Android
**Settings** app (see Step 1 for how to open it), tap **About tablet**:

| Field | Tested value |
| --- | --- |
| Rower | Hydrow Origin |
| Model | `foenix_h` |
| Android version | 10 |
| Build number | `QP1A.190711.020.user.255` |

The tablet uses a MediaTek MT8167 chip with a 1920 x 1080 screen. A different
model or Android version may still work, but this guide hasn't been tried on it.
If you try Catch on a Hydrow Wave, a Hydrow Arc, or an Origin with
different details, please file a
[hardware report](https://github.com/thegregstengel/catch/issues/new?template=hardware_report.yml).

## Step 1: Turn on USB debugging

These steps start from a stock Hydrow, with no changes made yet.

1. In the Hydrow app, open the Wi-Fi settings and tap **Advanced**. The tablet
   opens Android's own **Settings** app.
1. Tap **About tablet**, then tap **Build number** repeatedly until a message
   says that you are now a developer.
1. Go back and tap **System > Advanced > Developer options**.
1. Turn on **USB debugging**.
1. Connect the tablet to your computer and accept the debugging prompt.
1. Confirm the connection:

   ```bash
   adb devices
   ```

## Step 2: Unlock the bootloader

Unlocking erases all data on the tablet.

```bash
adb reboot bootloader
fastboot flashing unlock
fastboot reboot
```

Confirm the unlock on the tablet if it asks. The unlock resets the tablet, so
after the reboot, repeat [Step 1](#step-1-turn-on-usb-debugging) to turn on USB
debugging again.

The tablet now shows an "Orange State" warning for a few seconds at every boot.
That is expected for an unlocked bootloader.

## Step 3: Back up the boot partition

Magisk patches the tablet's own boot image, so you need a copy of it. `fastboot`
can't read partitions on this tablet, so read it with mtkclient in the MediaTek
preloader mode.

On the MT8167, mtkclient stalls after the first 64 KiB of a read. To fix it, in
`mtkclient/Library/DA/xflash/xflash_lib.py`, add `0x8167` to the chip list in
`ack()` that sends the acknowledgement as a single 16-byte write (the branch that
already handles `0x6781`).

1. Power the rower fully off, and confirm the computer sees no MediaTek device:

   ```bash
   lsusb -d 0e8d:
   ```

1. Start the read **before** you power on, so mtkclient catches the short
   preloader window:

   ```bash
   python mtk.py r boot_a boot_a.img
   ```

1. Power the rower on without pressing any buttons. The read finishes in under a
   minute.
1. Check that the image is 16 MiB and starts with `ANDROID!`, then keep a copy
   somewhere safe. It's your way back to stock.

If mtkclient hangs on a device that shows up as `0e8d:2000` before you power on,
the preloader is stuck from an earlier session. Stop mtkclient, cut power to the
rower, and start again from the first step.

## Step 4: Root with Magisk

1. Install Magisk and copy the boot image to the tablet:

   ```bash
   adb install Magisk-v30.7.apk
   adb push boot_a.img /sdcard/Download/
   ```

1. In the Magisk app, tap **Install**, choose **Select and Patch a File**, and pick
   `boot_a.img`. Magisk writes `magisk_patched-<id>.img` to the Download folder.
1. Copy the patched image back and flash it with mtkclient, catching the
   preloader the same way as before:

   ```bash
   adb pull /sdcard/Download/magisk_patched-<id>.img magisk_patched.img
   python mtk.py w boot_a magisk_patched.img
   ```

1. Boot the tablet, open Magisk, and confirm it reports as installed. Check root
   from your computer:

   ```bash
   adb shell su -c id
   ```

## Step 5: Install the Catch module

```bash
adb push catch-magisk-<version>.zip /data/local/tmp/
adb shell su -c 'magisk --install-module /data/local/tmp/catch-magisk-<version>.zip'
adb reboot
```

The first boot after installing takes a minute longer than usual while Android
prepares Catch. Catch then opens as the Home app.

The module does the following:

- Installs Catch as a privileged system app and makes it the Home app.
- Starts the media pane host, which lets streaming apps play inside Catch.
- Replaces the boot animation and the dark screen at the end of boot with Catch's.
- Turns off the stock Hydrow launcher, including its remote commands, app
  updater, and logging.
- Grants Catch access to the rower's serial port and lets Catch install its own
  updates.

## Step 6: Set up Catch

The first-run wizard walks through Wi-Fi, confirming Catch as Home, Aurora Store,
choosing which streaming apps to show, and creating the first profile.

### Streaming apps

The tablet has no Google Play Store and no Google Play services, and nothing
in this guide adds them. Catch and the streaming apps run without them.

[Aurora Store](https://auroraoss.com/) installs and updates the apps instead.
It downloads them from the Google Play catalog using an anonymous account, so
you don't sign in to Google on the rower.

1. Open **Aurora Store** from the setup wizard or from
   **Catch Settings > Home and apps**. If Aurora Store isn't on the tablet yet,
   download it from [auroraoss.com](https://auroraoss.com/).
1. Choose **Anonymous** when Aurora Store asks how to sign in.
1. Install Netflix, Plex, Paramount+, and Disney+, then sign in to each app
   with your own streaming account.
1. To keep the apps current, open Aurora Store's **Updates** tab now and then,
   or turn on automatic updates in its settings.

## Updates

Catch updates itself. It checks for a new version once a day; when there is one,
the "Who is rowing?" screen says so, and **Catch Settings > Updates** installs it.
See [Updates and releases](updates.md) for details.

A new module zip is only needed when a release says so, for example when a
release changes the boot screens or the permissions Catch needs.

## Go back to stock

1. Remove the Catch module in the Magisk app (**Modules**) and reboot. The stock
   Hydrow launcher comes back.
1. To remove root as well, flash your original `boot_a.img` with
   `python mtk.py w boot_a boot_a.img`.

## Troubleshooting

| Symptom | What to try |
| --- | --- |
| The media pane says the media host is not running. | `adb shell service check catch_display_host` should report `found`. Host logs: `adb logcat -s CatchSysDisp`. |
| Metrics stay at zero while you row. | Check the rower log: `adb logcat -s SerialRowerHal RowerHalFactory`. Catch falls back to a simulated rower when it can't open the serial port. |
| The screen freezes and the tablet restarts on its own. | Reboot the tablet. If it keeps happening while a streaming app is open, open an issue with `adb logcat -d` output. |
| You need Android's own settings. | Open the profile menu in the top-left corner and tap **System Settings**. |
