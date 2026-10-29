package app.catchrower.rower

import android.util.Base64
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter

/**
 * Opens the Hydrow brake UART. Prefers a direct open; when SELinux blocks
 * `priv_app` from `ttyS_device` (this Hydrow), falls back to a long-lived
 * Magisk `su` reader/writer so metrics still flow.
 */
class SerialRowerHal private constructor(
    private val scope: CoroutineScope,
    private val path: String,
    private val transport: Transport,
) : RowerHal {
    private val acc = SessionAccumulator(TelemetrySource.Serial, path)
    private val _telemetry = MutableStateFlow(acc.snapshot(connected = false))
    override val telemetry: StateFlow<RowerTelemetry> = _telemetry.asStateFlow()

    private var readJob: Job? = null
    private var clockJob: Job? = null
    private var output: OutputStreamWriter? = null
    private var input: BufferedReader? = null
    private var directIn: FileInputStream? = null
    private var directOut: FileOutputStream? = null
    private var rootReadProc: Process? = null

    private enum class Transport { Direct, Root }

    override suspend fun start() {
        stop()
        withContext(Dispatchers.IO) {
            when (transport) {
                Transport.Direct -> openDirect()
                Transport.Root -> openRoot()
            }
            writeRaw(BrakeProtocol.continuousModeOn())
            writeRaw(BrakeProtocol.queryResistance())
        }
        acc.reset()
        _telemetry.value = acc.snapshot(connected = true)
        readJob = scope.launch(Dispatchers.IO) {
            val reader = input ?: return@launch
            var lastEmitMs = 0L
            var pendingInstant = false
            while (isActive) {
                val line = try {
                    reader.readLine()
                } catch (e: Exception) {
                    Log.w(TAG, "UART read failed", e)
                    break
                } ?: break
                var emitNow = false
                when (val parsed = BrakeProtocol.parseLine(line)) {
                    is BrakeProtocol.ParsedPacket.Stroke -> {
                        acc.onStroke(
                            parsed.data.distanceMeters,
                            parsed.data.averagePowerWatts,
                            parsed.data.distanceIsAbsolute,
                        )
                        emitNow = true
                    }
                    is BrakeProtocol.ParsedPacket.Instant -> {
                        acc.onInstantWatts(parsed.data.watts)
                        pendingInstant = true
                    }
                    is BrakeProtocol.ParsedPacket.Resistance -> {
                        acc.setResistance(parsed.level)
                        emitNow = true
                    }
                    null -> Unit
                }
                val now = System.currentTimeMillis()
                // di2 arrives ~100Hz; publish at most ~10Hz unless stroke/resistance.
                if (emitNow || (pendingInstant && now - lastEmitMs >= EMIT_MIN_MS)) {
                    _telemetry.value = acc.snapshot(connected = true)
                    lastEmitMs = now
                    pendingInstant = false
                }
            }
            _telemetry.value = acc.snapshot(connected = false)
        }
    }

    private fun openDirect() {
        val file = File(path)
        if (!file.exists()) error("UART path missing: $path")
        if (!file.canRead() || !file.canWrite()) {
            error("UART not readable/writable: $path")
        }
        val configured = runCatching {
            Runtime.getRuntime().exec(arrayOf("sh", "-c", stty(path))).waitFor() == 0
        }.getOrDefault(false)
        directIn = FileInputStream(file)
        directOut = FileOutputStream(file)
        input = BufferedReader(InputStreamReader(directIn, Charsets.UTF_8))
        output = OutputStreamWriter(directOut, Charsets.UTF_8)
        if (configured) {
            Log.i(TAG, "Opened $path at ${BrakeProtocol.BAUD}")
        } else {
            Log.w(TAG, "Opened $path, but stty failed (baud may be wrong)")
        }
    }

    private fun openRoot() {
        // service.sh grants Catch a silent Magisk policy, so this never prompts.
        val cmd = "${stty(path)} 2>/dev/null; exec cat $path"
        val proc = Runtime.getRuntime().exec(arrayOf("su", "-c", cmd))
        rootReadProc = proc
        input = BufferedReader(InputStreamReader(proc.inputStream, Charsets.UTF_8))
        Log.i(TAG, "Opened $path via Magisk root cat @ ${BrakeProtocol.BAUD}")
    }

    override fun startSession() {
        acc.startOrResume()
        _telemetry.value = acc.snapshot(connected = _telemetry.value.connected)
        startClockJob()
    }

    override fun pauseSession() {
        acc.pause()
        clockJob?.cancel()
        clockJob = null
        _telemetry.value = acc.snapshot(connected = _telemetry.value.connected)
    }

    override fun resetSession() {
        acc.reset()
        clockJob?.cancel()
        clockJob = null
        _telemetry.value = acc.snapshot(connected = _telemetry.value.connected)
    }

    private fun startClockJob() {
        clockJob?.cancel()
        clockJob = scope.launch {
            while (isActive) {
                kotlinx.coroutines.delay(250)
                val snap = acc.snapshot(connected = _telemetry.value.connected)
                if (snap.sessionPhase != SessionPhase.Running) break
                _telemetry.value = snap
            }
        }
    }

    override suspend fun stop() {
        readJob?.cancel()
        readJob = null
        clockJob?.cancel()
        clockJob = null
        withContext(Dispatchers.IO) {
            runCatching { output?.close() }
            runCatching { input?.close() }
            runCatching { directIn?.close() }
            runCatching { directOut?.close() }
            runCatching { rootReadProc?.destroy() }
            output = null
            input = null
            directIn = null
            directOut = null
            rootReadProc = null
        }
    }

    override fun setResistance(level: Int) {
        acc.setResistance(level)
        scope.launch(Dispatchers.IO) {
            writeRaw(BrakeProtocol.setResistance(level))
        }
        _telemetry.value = acc.snapshot(connected = true)
    }

    override fun setHeartRate(bpm: Int?) {
        acc.setHeartRate(bpm)
        _telemetry.value = acc.snapshot(connected = _telemetry.value.connected)
    }

    private fun writeRaw(payload: String) {
        when (transport) {
            Transport.Direct -> {
                val out = output ?: return
                try {
                    out.write(payload)
                    out.flush()
                } catch (e: Exception) {
                    Log.w(TAG, "UART write failed", e)
                }
            }
            Transport.Root -> {
                runCatching {
                    val b64 = Base64.encodeToString(payload.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
                    val cmd = "echo $b64 | base64 -d > $path"
                    Runtime.getRuntime().exec(arrayOf("su", "-c", cmd)).waitFor()
                }.onFailure {
                    Log.w(TAG, "root UART write failed", it)
                }
            }
        }
    }

    companion object {
        private const val TAG = "SerialRowerHal"
        private const val EMIT_MIN_MS = 100L

        private fun stty(path: String) = "stty -F $path ${BrakeProtocol.BAUD} raw -echo cs8 -parenb -cstopb"

        fun tryOpen(scope: CoroutineScope): SerialRowerHal? {
            for (path in BrakeProtocol.UART_CANDIDATES) {
                val file = File(path)
                if (!file.exists()) continue
                if (file.canRead() && file.canWrite()) {
                    Log.i(TAG, "Direct UART candidate $path")
                    return SerialRowerHal(scope, path, Transport.Direct)
                }
                Log.i(TAG, "Direct open blocked for $path — will try Magisk root")
            }
            for (path in BrakeProtocol.UART_CANDIDATES) {
                if (!File(path).exists()) continue
                if (rootCanOpen(path)) {
                    Log.i(TAG, "Root UART candidate $path")
                    return SerialRowerHal(scope, path, Transport.Root)
                }
            }
            return null
        }

        private fun rootCanOpen(path: String): Boolean {
            return runCatching {
                val proc = Runtime.getRuntime().exec(
                    arrayOf("su", "-c", "test -e $path && test -r $path && echo OK"),
                )
                val out = proc.inputStream.bufferedReader().readText()
                proc.waitFor()
                out.contains("OK")
            }.getOrDefault(false)
        }
    }
}
