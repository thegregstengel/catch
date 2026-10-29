package app.catchrower.sysdisplay;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.os.Binder;
import android.os.IBinder;
import android.os.Looper;
import android.os.Process;
import android.os.SystemClock;
import android.util.Log;
import android.view.Display;
import android.view.InputDevice;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Surface;

import java.lang.reflect.Method;

/**
 * Catch system-display host.
 *
 * Runs via {@code app_process} as uid 1000 (started by the Magisk module). It is
 * the only component allowed to do the privileged work the media window needs on
 * this ROM:
 *
 * <ul>
 *   <li>Create a <b>secure, public, system-owned</b> virtual display. The
 *       device's own {@code ActivityStackSupervisor} skips the
 *       "not-embedded activity on virtual display" rejection when the display is
 *       owned by uid 1000, so ordinary streaming apps (which do not set
 *       {@code allowEmbedded}) stay in the pane instead of being kicked to the
 *       main display.</li>
 *   <li>Launch the guest onto that display.</li>
 *   <li>Inject touch and key events, since a virtual display has no input
 *       device of its own.</li>
 * </ul>
 *
 * Catch (an ordinary third-party-signed app) cannot do any of this: the relevant
 * permissions are platform-signature level. Catch only supplies a Surface and
 * forwards touches over {@link ICatchDisplayHost}.
 *
 * The binder is published by name in servicemanager ({@link #SERVICE_NAME});
 * Catch looks it up when it needs the host, so start order does not matter.
 */
public final class SystemDisplayHost extends ICatchDisplayHost.Stub {

    private static final String TAG = "CatchSysDisp";

    // Published under this name in servicemanager; Catch looks it up by name.
    // Discoverable by any process, but every call is uid-checked against Catch.
    static final String SERVICE_NAME = "catch_display_host";
    static final String CLIENT_PACKAGE = "app.catchrower.catch";

    // Virtual-display flags. SECURE is
    // gated to uid 1000 on this ROM and is required: protected (DRM) buffers on
    // a non-secure display wedge SurfaceFlinger. System decorations are left off
    // so no status or navigation bar insets eat into the pane.
    static final int DISPLAY_FLAGS = DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC
            | DisplayManager.VIRTUAL_DISPLAY_FLAG_SECURE
            | DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY;

    private static final int INVALID_DISPLAY = -1;
    private static final long UPDATE_POLL_MS = 5_000;

    private final Context context;
    private final DisplayManager displayManager;
    private final int clientUid;

    private volatile VirtualDisplay virtualDisplay;
    private String guestPackage;
    private IBinder clientToken;
    private IBinder.DeathRecipient clientDeath;
    private int densityDpi = 160;

    private SystemDisplayHost(Context context) {
        this.context = context;
        this.displayManager =
                (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
        this.clientUid = resolveClientUid(context);
    }

    public static void main(String[] args) {
        try {
            if (Looper.getMainLooper() == null) {
                Looper.prepareMainLooper();
            }
            Context ctx = systemContext();
            log("uid=" + Process.myUid() + " pid=" + Process.myPid()
                    + " context=" + (ctx == null ? "null" : ctx.getPackageName()));
            if (ctx == null) {
                log("no system context; aborting");
                return;
            }

            SystemDisplayHost host = new SystemDisplayHost(ctx);
            publishService(host);
            host.exitWhenCatchUpdates();
            log("host published as '" + SERVICE_NAME + "'; entering loop");
            Looper.loop();
        } catch (Throwable t) {
            log("fatal in main: " + t);
            t.printStackTrace();
        }
    }

    // ---- ICatchDisplayHost ----

    @Override
    public synchronized int startGuest(Surface surface, String packageName, int width,
            int height, int dpi, IBinder clientToken) {
        enforceClient();
        try {
            releaseLocked();
            watchClient(clientToken);
            this.densityDpi = dpi;
            VirtualDisplay vd = displayManager.createVirtualDisplay(
                    "CatchMediaDisplay", width, height, dpi, surface, DISPLAY_FLAGS);
            if (vd == null) {
                log("createVirtualDisplay returned null");
                return INVALID_DISPLAY;
            }
            this.virtualDisplay = vd;
            this.guestPackage = packageName;
            int displayId = vd.getDisplay().getDisplayId();
            log("created displayId=" + displayId + " for " + packageName
                    + " (" + width + "x" + height + " @" + dpi + ")");
            boolean launched = launchOnDisplay(packageName, displayId);
            if (!launched) {
                log("launch failed for " + packageName);
            }
            return displayId;
        } catch (Throwable t) {
            log("startGuest failed: " + t);
            t.printStackTrace();
            releaseLocked();
            return INVALID_DISPLAY;
        }
    }

    @Override
    public synchronized void resize(int width, int height, int dpi) {
        enforceClient();
        VirtualDisplay vd = virtualDisplay;
        if (vd == null) return;
        try {
            this.densityDpi = dpi;
            vd.resize(width, height, dpi);
        } catch (Throwable t) {
            log("resize failed: " + t);
        }
    }

    @Override
    public boolean injectMotion(int action, float x, float y, long downTime, long eventTime) {
        enforceClient();
        int displayId = getDisplayId();
        if (displayId == INVALID_DISPLAY) return false;
        MotionEvent event = MotionEvent.obtain(downTime, eventTime, action, x, y, 0);
        event.setSource(InputDevice.SOURCE_TOUCHSCREEN);
        setDisplayId(event, displayId);
        try {
            return injectInputEvent(event);
        } finally {
            event.recycle();
        }
    }

    @Override
    public boolean injectKey(int keyCode) {
        enforceClient();
        int displayId = getDisplayId();
        if (displayId == INVALID_DISPLAY) return false;
        long now = SystemClock.uptimeMillis();
        boolean ok = injectOneKey(keyCode, KeyEvent.ACTION_DOWN, now, displayId);
        ok &= injectOneKey(keyCode, KeyEvent.ACTION_UP, now, displayId);
        return ok;
    }

    @Override
    public int getDisplayId() {
        VirtualDisplay vd = virtualDisplay;
        if (vd == null) return INVALID_DISPLAY;
        Display d = vd.getDisplay();
        return d == null ? INVALID_DISPLAY : d.getDisplayId();
    }

    @Override
    public synchronized void stopGuest() {
        enforceClient();
        releaseLocked();
    }

    @Override
    public boolean ping() {
        enforceClient();
        return true;
    }

    @Override
    public synchronized void setSurface(Surface surface) {
        enforceClient();
        VirtualDisplay vd = virtualDisplay;
        if (vd == null) return;
        try {
            vd.setSurface(surface);
        } catch (Throwable t) {
            log("setSurface failed: " + t);
        }
    }

    // ---- internals ----

    private boolean launchOnDisplay(String packageName, int displayId) {
        ComponentName component = resolveLauncher(context, packageName);
        if (component == null) {
            log("no launcher activity for " + packageName);
            return false;
        }
        return amStartOnDisplay(component, displayId);
    }

    private static ComponentName resolveLauncher(Context ctx, String packageName) {
        Intent intent = ctx.getPackageManager().getLaunchIntentForPackage(packageName);
        return intent == null ? null : intent.getComponent();
    }

    /**
     * Launch through the {@code am} command. A system-context startActivity is
     * refused with START_PERMISSION_DENIED on this ROM for a null-token
     * background start onto a secondary display; {@code am} (running as uid 1000,
     * which also holds INTERNAL_SYSTEM_WINDOW) takes the allowed path. Launch is
     * one-shot per guest, so the subprocess cost is immaterial.
     */
    private static boolean amStartOnDisplay(ComponentName component, int displayId) {
        String cmd = "am start --display " + displayId
                + " -a android.intent.action.MAIN -c android.intent.category.LAUNCHER"
                + " -f 0x10000000"
                + " -n " + component.getPackageName() + "/" + component.getClassName();
        try {
            java.lang.Process p = Runtime.getRuntime().exec(new String[] {"sh", "-c", cmd});
            int code = p.waitFor();
            String err = readStream(p.getErrorStream());
            log("am start --display " + displayId + " code=" + code
                    + (err.isEmpty() ? "" : " err=" + err));
            String lower = err.toLowerCase();
            return code == 0 && !lower.contains("error") && !lower.contains("exception");
        } catch (Throwable t) {
            log("am start failed: " + t);
            return false;
        }
    }

    private static String readStream(java.io.InputStream in) {
        try {
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[1024];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            return out.toString().trim();
        } catch (Throwable t) {
            return "";
        }
    }

    private boolean injectOneKey(int keyCode, int action, long when, int displayId) {
        KeyEvent event = new KeyEvent(when, when, action, keyCode, 0, 0,
                KeyCharacterMap.VIRTUAL_KEYBOARD, 0,
                KeyEvent.FLAG_FROM_SYSTEM, InputDevice.SOURCE_KEYBOARD);
        setDisplayId(event, displayId);
        return injectInputEvent(event);
    }

    private void watchClient(IBinder token) throws android.os.RemoteException {
        IBinder.DeathRecipient recipient = new IBinder.DeathRecipient() {
            @Override
            public void binderDied() {
                synchronized (SystemDisplayHost.this) {
                    if (clientDeath != this) return;
                    log("Catch died; stopping guest");
                    releaseLocked();
                }
            }
        };
        token.linkToDeath(recipient, 0);
        clientToken = token;
        clientDeath = recipient;
    }

    private void releaseLocked() {
        if (clientToken != null) {
            clientToken.unlinkToDeath(clientDeath, 0);
            clientToken = null;
            clientDeath = null;
        }
        String pkg = guestPackage;
        if (pkg != null) {
            forceStop(pkg);
            guestPackage = null;
        }
        VirtualDisplay vd = virtualDisplay;
        if (vd != null) {
            try {
                vd.release();
            } catch (Throwable t) {
                log("display release failed: " + t);
            }
            virtualDisplay = null;
            log("released display for " + pkg);
        }
    }

    private void forceStop(String packageName) {
        // ActivityManager.forceStopPackage is signature-gated and uid 1000 holds
        // it; fall back to the am shell if reflection ever fails.
        try {
            Object am = context.getSystemService(Context.ACTIVITY_SERVICE);
            Method m = am.getClass().getMethod("forceStopPackage", String.class);
            m.invoke(am, packageName);
            return;
        } catch (Throwable ignored) {
            // fall through
        }
        try {
            Runtime.getRuntime().exec(new String[] {"am", "force-stop", packageName}).waitFor();
        } catch (Throwable t) {
            log("force-stop failed for " + packageName + ": " + t);
        }
    }

    /**
     * A bare app_process cannot register broadcast receivers (the activity
     * manager has no process record for it), so the binder is published by name
     * in servicemanager instead. servicemanager drops the entry automatically
     * when this process dies, and the keepalive republishes it on restart.
     */
    private static void publishService(IBinder service) {
        try {
            Class<?> sm = Class.forName("android.os.ServiceManager");
            sm.getMethod("addService", String.class, IBinder.class)
                    .invoke(null, SERVICE_NAME, service);
        } catch (Throwable t) {
            throw new RuntimeException("addService(" + SERVICE_NAME + ") failed", t);
        }
    }

    private void enforceClient() {
        int caller = Binder.getCallingUid();
        // Allow the host's own thread and the Catch uid.
        if (caller == Process.myUid()) return;
        if (clientUid != -1 && caller == clientUid) return;
        throw new SecurityException("caller uid " + caller + " is not Catch (" + clientUid + ")");
    }

    /**
     * This process runs code from the Catch APK it was started with. After Catch
     * updates itself, exit so the Magisk keepalive restarts the host from the new
     * APK; otherwise an old host would serve a newer Catch.
     */
    private void exitWhenCatchUpdates() {
        final String startedFrom = System.getProperty("java.class.path", "");
        Thread watcher = new Thread(() -> {
            while (true) {
                SystemClock.sleep(UPDATE_POLL_MS);
                String current = catchApkPath();
                if (current != null && !current.equals(startedFrom)) {
                    log("Catch APK changed to " + current + "; exiting for restart");
                    stopGuest();
                    System.exit(0);
                }
            }
        }, "catch-update-watch");
        watcher.setDaemon(true);
        watcher.start();
    }

    private String catchApkPath() {
        try {
            return context.getPackageManager().getApplicationInfo(CLIENT_PACKAGE, 0).sourceDir;
        } catch (Throwable t) {
            return null;
        }
    }

    private static int resolveClientUid(Context ctx) {
        try {
            return ctx.getPackageManager().getPackageUid(CLIENT_PACKAGE, 0);
        } catch (Throwable t) {
            log("could not resolve Catch uid: " + t);
            return -1;
        }
    }

    // ---- hidden-API bridges (this process is app_process, so exempt) ----

    private boolean injectInputEvent(android.view.InputEvent event) {
        try {
            Object im = context.getSystemService(Context.INPUT_SERVICE);
            Method inject = im.getClass().getMethod(
                    "injectInputEvent", android.view.InputEvent.class, int.class);
            // INJECT_INPUT_EVENT_MODE_ASYNC = 0
            Object result = inject.invoke(im, event, 0);
            return Boolean.TRUE.equals(result);
        } catch (Throwable t) {
            log("injectInputEvent failed: " + t);
            return false;
        }
    }

    private static void setDisplayId(Object event, int displayId) {
        try {
            Method m = event.getClass().getMethod("setDisplayId", int.class);
            m.invoke(event, displayId);
        } catch (Throwable t) {
            log("setDisplayId failed: " + t);
        }
    }

    private static Context systemContext() {
        try {
            Class<?> at = Class.forName("android.app.ActivityThread");
            Object thread = at.getMethod("systemMain").invoke(null);
            return (Context) at.getMethod("getSystemContext").invoke(thread);
        } catch (Throwable t) {
            log("systemContext failed: " + t);
            return null;
        }
    }

    private static void log(String message) {
        System.out.println("[CatchSysDisp] " + message);
        System.out.flush();
        Log.i(TAG, message);
    }
}
