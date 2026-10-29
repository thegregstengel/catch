#!/system/bin/sh
until [ "$(getprop sys.boot_completed)" = "1" ]; do
  sleep 1
done

# Catch is Home. FallbackHome must stay enabled: Catch is not direct-boot aware,
# and boot stalls without a direct-boot Home.
pm enable com.android.settings/.FallbackHome >/dev/null 2>&1
cmd package set-home-activity app.catchrower.catch/app.catchrower.ui.home.HomeActivity >/dev/null 2>&1

# The stock Hydrow launcher runs as the system user and carries Hydrow's remote
# commands, app updater, and logging. Catch replaces it entirely.
pm disable com.truerowing.hydrowlauncher >/dev/null 2>&1
pm disable com.mediatek.batterywarning/.BatteryWarningActivity >/dev/null 2>&1

# Let non-resizable guests (Netflix and others) fill the media pane's display.
settings put global force_resizable_activities 1 >/dev/null 2>&1
settings put global enable_freeform_support 1 >/dev/null 2>&1

# SELinux. Magisk finds no preinit partition on this device, so it cannot load
# sepolicy.rule at boot; apply the same rules live.
magiskpolicy --live "allow priv_app ttyS_device chr_file *" >/dev/null 2>&1
magiskpolicy --live "allow priv_app ttySDIO_device chr_file *" >/dev/null 2>&1
magiskpolicy --live "allow priv_app default_android_service service_manager find" >/dev/null 2>&1

# Rower UART. SELinux still denies Catch a direct open on this build, so Catch
# reads it through su; grant that silently (no prompt, no toast).
chmod 666 /dev/ttyS1 /dev/ttyMT1 2>/dev/null
CATCH_UID=$(dumpsys package app.catchrower.catch 2>/dev/null | sed -n 's/.*userId=\([0-9][0-9]*\).*/\1/p' | head -1)
if [ -n "$CATCH_UID" ]; then
  magisk --sqlite "REPLACE INTO policies (uid,policy,until,logging,notification) VALUES($CATCH_UID,2,0,0,0)" >/dev/null 2>&1
fi

# Media pane host: runs as the system user (uid 1000) so the display it creates
# is system-owned, which lets ordinary streaming apps stay on it. It loads from
# the currently installed Catch APK, exits when Catch updates, and is restarted
# here within a few seconds.
(
  while true; do
    if ! service check catch_display_host 2>/dev/null | grep -q ': found'; then
      APK=$(pm path app.catchrower.catch 2>/dev/null | sed -n 's/^package://p' | head -1)
      if [ -n "$APK" ]; then
        su 1000 -c "CLASSPATH=$APK exec app_process /system/bin --nice-name=catch_display_host app.catchrower.sysdisplay.SystemDisplayHost" >/dev/null 2>&1
      fi
    fi
    sleep 5
  done
) &
