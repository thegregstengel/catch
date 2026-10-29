package app.catchrower.media

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat

/**
 * Owns a MediaSession + AudioFocus so hardware volume keys route to media stream
 * while Catch hosts streaming apps. Guest apps may also hold focus; we request
 * transient ducking when Catch itself needs attention.
 */
class CatchMediaSession(context: Context) {
    private val appContext = context.applicationContext
    private val audioManager =
        appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val session = MediaSessionCompat(appContext, "CatchMedia").apply {
        setFlags(
            MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS,
        )
        setPlaybackState(
            PlaybackStateCompat.Builder()
                .setActions(
                    PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_PLAY_PAUSE,
                )
                .setState(PlaybackStateCompat.STATE_PLAYING, 0L, 1f)
                .build(),
        )
        isActive = true
    }

    private var focusRequest: AudioFocusRequest? = null
    private val handler = Handler(Looper.getMainLooper())

    fun activate() {
        session.isActive = true
        requestFocusWhenReady(attemptsLeft = FOCUS_RETRIES)
    }

    // Catch starts as Home before the audio service is up on a cold boot, and
    // AudioManager throws instead of failing gracefully until it is.
    private fun requestFocusWhenReady(attemptsLeft: Int) {
        if (runCatching { requestFocus() }.isSuccess) return
        if (attemptsLeft <= 0) {
            Log.w(TAG, "Audio service never became ready; skipping audio focus")
            return
        }
        handler.postDelayed({ requestFocusWhenReady(attemptsLeft - 1) }, FOCUS_RETRY_MS)
    }

    fun deactivate() {
        handler.removeCallbacksAndMessages(null)
        runCatching { abandonFocus() }
        session.isActive = false
    }

    fun release() {
        deactivate()
        session.release()
    }

    fun adjustVolume(direction: Int) {
        runCatching {
            audioManager.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                direction,
                AudioManager.FLAG_SHOW_UI,
            )
        }
    }

    private companion object {
        const val TAG = "CatchMedia"
        const val FOCUS_RETRIES = 30
        const val FOCUS_RETRY_MS = 1_000L
    }

    private fun requestFocus() {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
            .build()
        val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(attrs)
            .setOnAudioFocusChangeListener { }
            .build()
        focusRequest = req
        audioManager.requestAudioFocus(req)
    }

    private fun abandonFocus() {
        focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        focusRequest = null
    }
}
