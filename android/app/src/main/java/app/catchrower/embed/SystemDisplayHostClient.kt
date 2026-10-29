package app.catchrower.embed

import android.os.Binder
import android.os.IBinder
import android.util.Log
import android.view.MotionEvent
import android.view.Surface
import app.catchrower.sysdisplay.ICatchDisplayHost

/**
 * Catch-side client for the system-display host (see
 * [app.catchrower.sysdisplay.SystemDisplayHost]).
 *
 * The host runs as uid 1000 via `app_process` and does all the privileged work;
 * Catch only hands it a [Surface] and forwards touches. The host binder is
 * published by name in servicemanager, which Catch can read because
 * `ServiceManager.getService` is greylisted (allowed regardless of target SDK).
 */
object SystemDisplayHostClient {

    private const val TAG = "CatchSysDisp"
    private const val SERVICE_NAME = "catch_display_host"

    @Volatile
    private var host: ICatchDisplayHost? = null

    // Lets the host stop the guest if Catch's process dies.
    private val token = Binder()

    /** Whether the host is published and responding. */
    fun isAvailable(): Boolean = connect()?.let {
        runCatching { it.ping() }.getOrDefault(false)
    } ?: false

    /**
     * Create the hosted display backed by [surface] and launch [packageName]
     * onto it. Returns the display id, or -1 on failure.
     */
    fun startGuest(surface: Surface, packageName: String, width: Int, height: Int, dpi: Int): Int {
        val h = connect() ?: return -1
        return runCatching { h.startGuest(surface, packageName, width, height, dpi, token) }
            .onFailure { Log.w(TAG, "startGuest failed", it); invalidate() }
            .getOrDefault(-1)
    }

    fun resize(width: Int, height: Int, dpi: Int) {
        val h = host ?: return
        runCatching { h.resize(width, height, dpi) }
            .onFailure { Log.w(TAG, "resize failed", it) }
    }

    /** Forward one touch point (coordinates already in hosted-display space). */
    fun forwardTouch(event: MotionEvent): Boolean {
        val h = host ?: return false
        return runCatching {
            h.injectMotion(
                event.actionMasked,
                event.x,
                event.y,
                event.downTime,
                event.eventTime,
            )
        }.getOrDefault(false)
    }

    /** Reattach the hosted display to [surface], or detach it with null. */
    fun setSurface(surface: Surface?) {
        val h = host ?: return
        runCatching { h.setSurface(surface) }
            .onFailure { Log.w(TAG, "setSurface failed", it) }
    }

    fun injectKey(keyCode: Int): Boolean {
        val h = host ?: return false
        return runCatching { h.injectKey(keyCode) }.getOrDefault(false)
    }

    fun displayId(): Int = host?.let {
        runCatching { it.getDisplayId() }.getOrDefault(-1)
    } ?: -1

    fun stopGuest() {
        val h = host ?: return
        runCatching { h.stopGuest() }.onFailure { Log.w(TAG, "stopGuest failed", it) }
    }

    private fun connect(): ICatchDisplayHost? {
        host?.let { current ->
            if (current.asBinder().isBinderAlive) return current
            host = null
        }
        val binder = fetchHostBinder() ?: return null
        return ICatchDisplayHost.Stub.asInterface(binder).also { host = it }
    }

    private fun invalidate() {
        host = null
    }

    private fun fetchHostBinder(): IBinder? = runCatching {
        val serviceManager = Class.forName("android.os.ServiceManager")
        val getService = serviceManager.getMethod("getService", String::class.java)
        getService.invoke(null, SERVICE_NAME) as? IBinder
    }.onFailure { Log.w(TAG, "getService($SERVICE_NAME) failed", it) }.getOrNull()
}
