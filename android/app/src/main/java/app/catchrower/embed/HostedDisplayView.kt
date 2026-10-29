package app.catchrower.embed

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

private const val TAG = "CatchSysDisp"

/**
 * Shows [packageName] inside this composable's bounds.
 *
 * The app runs on a system-owned virtual display created by the system-display
 * host; this view only supplies the surface that display renders into and
 * forwards touches and Back to it. [onFailed] is called on the main thread when
 * the host is not running or the launch fails.
 */
@Composable
fun HostedDisplayView(
    packageName: String,
    modifier: Modifier = Modifier,
    onFailed: (String) -> Unit,
) {
    val context = LocalContext.current
    val currentOnFailed = rememberUpdatedState(onFailed)
    key(packageName) {
        val session = remember {
            GuestSession(
                packageName = packageName,
                densityDpi = context.resources.displayMetrics.densityDpi,
                onFailed = { currentOnFailed.value(it) },
            )
        }
        DisposableEffect(session) {
            onDispose { session.stop() }
        }
        BackHandler { session.back() }
        AndroidView(
            modifier = modifier,
            factory = { ctx -> session.createView(ctx) },
        )
    }
}

/** One hosted guest. All host calls run in order on a single worker thread. */
private class GuestSession(
    private val packageName: String,
    private val densityDpi: Int,
    private val onFailed: (String) -> Unit,
) : SurfaceHolder.Callback {

    private val worker: ExecutorService = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())

    // Touched only on the worker thread.
    private var started = false
    private var stopped = false

    // SurfaceView clears its parent canvas to punch a hole for the guest. Compose
    // does not clip an AndroidView's drawing to its bounds, so the FrameLayout
    // (which clips its children) keeps the hole from erasing the rest of Catch.
    @SuppressLint("ClickableViewAccessibility")
    fun createView(context: Context): View {
        val surfaceView = SurfaceView(context).apply {
            holder.addCallback(this@GuestSession)
            setOnTouchListener { _, event -> forward(event); true }
        }
        return FrameLayout(context).apply {
            addView(
                surfaceView,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                ),
            )
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder) = Unit

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        val surface = holder.surface
        submit {
            if (!started) {
                if (!SystemDisplayHostClient.isAvailable()) {
                    fail("The media host is not running. Restart the rower and try again.")
                    return@submit
                }
                val displayId = SystemDisplayHostClient.startGuest(
                    surface, packageName, width, height, densityDpi,
                )
                if (displayId < 0) {
                    fail("Could not open this app in the window.")
                    return@submit
                }
                started = true
                Log.i(TAG, "$packageName hosted on display $displayId (${width}x$height)")
            } else {
                SystemDisplayHostClient.setSurface(surface)
                SystemDisplayHostClient.resize(width, height, densityDpi)
            }
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        submit { if (started) SystemDisplayHostClient.setSurface(null) }
    }

    fun back() {
        submit { if (started) SystemDisplayHostClient.injectKey(KeyEvent.KEYCODE_BACK) }
    }

    fun stop() {
        submit {
            stopped = true
            if (started) SystemDisplayHostClient.stopGuest()
            started = false
        }
        worker.shutdown()
    }

    private fun forward(event: MotionEvent) {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_MOVE,
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL,
            -> {
                val copy = MotionEvent.obtain(event)
                submit {
                    if (started) SystemDisplayHostClient.forwardTouch(copy)
                    copy.recycle()
                }
            }
        }
    }

    private fun fail(message: String) {
        Log.w(TAG, "$packageName: $message")
        main.post { onFailed(message) }
    }

    private fun submit(task: () -> Unit) {
        if (worker.isShutdown) return
        worker.execute {
            if (stopped) return@execute
            runCatching(task).onFailure { Log.w(TAG, "host call failed", it) }
        }
    }
}
