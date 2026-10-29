// Binder interface between Catch (client, ordinary app uid) and the Catch
// system-display host (server, run via app_process as uid 1000).
//
// All privileged work lives on the host side: this ROM gates the display and
// input APIs behind platform-signature permissions that Catch cannot hold, and
// only a display owned by the system user escapes the secondary-display launch
// rejection that breaks embedding. Catch therefore does no privileged work; it
// hands over a Surface and forwards touches through this interface.
package app.catchrower.sysdisplay;

import android.view.Surface;

interface ICatchDisplayHost {
    // Create a secure, system-owned, public virtual display that renders into
    // `surface`, launch `packageName`'s launcher activity onto it, and return
    // the new display id. Returns -1 on failure. The guest is stopped if the
    // process owning `clientToken` dies.
    int startGuest(in Surface surface, String packageName, int width, int height, int densityDpi,
            IBinder clientToken);

    // Resize the hosted display to match the on-screen surface.
    void resize(int width, int height, int densityDpi);

    // Forward one touch point. Coordinates are already in hosted-display space.
    boolean injectMotion(int action, float x, float y, long downTime, long eventTime);

    // Inject a key (for example KEYCODE_BACK) into the hosted display.
    boolean injectKey(int keyCode);

    // The current hosted display id, or -1 when nothing is hosted.
    int getDisplayId();

    // Force-stop the current guest and release the display.
    void stopGuest();

    // Liveness probe.
    boolean ping();

    // Point the hosted display at a new surface, or detach it with null. The
    // guest keeps running while detached, so it survives the pane's surface
    // being destroyed and recreated.
    void setSurface(in Surface surface);
}
