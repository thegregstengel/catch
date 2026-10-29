package app.catchrower.rower

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * Default HAL when UART open fails. Simulates realistic stroke cadence and power.
 */
class MockRowerHal(
    private val scope: CoroutineScope,
) : RowerHal {
    private val acc = SessionAccumulator(TelemetrySource.Mock)
    private val _telemetry = MutableStateFlow(acc.snapshot(connected = true))
    override val telemetry: StateFlow<RowerTelemetry> = _telemetry.asStateFlow()

    private var job: Job? = null
    private var resistance = 5

    override suspend fun start() {
        stop()
        acc.reset()
        acc.setResistance(resistance)
        _telemetry.value = acc.snapshot(connected = true)
        job = scope.launch {
            var tick = 0
            while (isActive) {
                if (_telemetry.value.sessionPhase == SessionPhase.Running) {
                    tick++
                    // Instantaneous watts between strokes
                    val phase = (tick % 50) / 50.0
                    val driveBoost = if (phase < 0.35) sin(phase / 0.35 * PI) else 0.15
                    val base = 90.0 + resistance * 8.0
                    val watts = (base + driveBoost * 120.0 + Random.nextDouble(-8.0, 8.0)).toFloat()
                    acc.onInstantWatts(watts)

                    // Stroke roughly every 2.4–2.8s (~22–25 spm)
                    if (tick % 25 == 0) {
                        val strokeDist = 8.5f + Random.nextFloat() * 1.5f
                        val strokePower = watts * (0.9f + Random.nextFloat() * 0.2f)
                        acc.onStroke(strokeDist, strokePower)
                    }
                }
                _telemetry.value = acc.snapshot(connected = true)
                delay(100)
            }
        }
    }

    override suspend fun stop() {
        job?.cancel()
        job = null
    }

    override fun setResistance(level: Int) {
        resistance = level.coerceIn(1, 20)
        acc.setResistance(resistance)
        _telemetry.value = acc.snapshot(connected = true)
    }

    override fun setHeartRate(bpm: Int?) {
        acc.setHeartRate(bpm)
        _telemetry.value = acc.snapshot(connected = true)
    }

    override fun startSession() {
        acc.startOrResume()
        _telemetry.value = acc.snapshot(connected = true)
    }

    override fun pauseSession() {
        acc.pause()
        _telemetry.value = acc.snapshot(connected = true)
    }

    override fun resetSession() {
        acc.reset()
        acc.setResistance(resistance)
        _telemetry.value = acc.snapshot(connected = true)
    }
}
