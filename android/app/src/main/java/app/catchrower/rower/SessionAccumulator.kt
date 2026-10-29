package app.catchrower.rower

/**
 * Turns stroke / instantaneous packets into rolling [RowerTelemetry].
 * Elapsed time only advances while the session is [SessionPhase.Running].
 */
class SessionAccumulator(
    private val source: TelemetrySource,
    private val portPath: String? = null,
) {
    private var phase: SessionPhase = SessionPhase.Idle
    private var accumulatedMs: Long = 0L
    private var runningSinceMs: Long = 0L
    private var distanceMeters: Float = 0f
    private var strokeCount: Int = 0
    private var lastStrokeAtMs: Long = 0L
    private var wattSum: Double = 0.0
    private var wattSamples: Int = 0
    private var lastWatts: Float? = null
    private var lastSplit: Float? = null
    private var lastSpm: Float? = null
    private var splitSum: Double = 0.0
    private var splitSamples: Int = 0
    private var resistance: Int = 5
    private var heartRateBpm: Int? = null
    private var weightKg: Float = 75f

    fun reset(weightKg: Float? = null) {
        phase = SessionPhase.Idle
        accumulatedMs = 0L
        runningSinceMs = 0L
        distanceMeters = 0f
        strokeCount = 0
        lastStrokeAtMs = 0L
        wattSum = 0.0
        wattSamples = 0
        lastWatts = null
        lastSplit = null
        lastSpm = null
        splitSum = 0.0
        splitSamples = 0
        if (weightKg != null) this.weightKg = weightKg
    }

    fun startOrResume() {
        if (phase == SessionPhase.Running) return
        runningSinceMs = System.currentTimeMillis()
        phase = SessionPhase.Running
    }

    fun pause() {
        if (phase != SessionPhase.Running) return
        accumulatedMs += System.currentTimeMillis() - runningSinceMs
        runningSinceMs = 0L
        phase = SessionPhase.Paused
    }

    fun setResistance(level: Int) {
        resistance = level.coerceIn(1, 20)
    }

    fun setHeartRate(bpm: Int?) {
        heartRateBpm = bpm
    }

    fun onStroke(
        distance: Float,
        powerWatts: Float,
        distanceIsAbsolute: Boolean = false,
    ) {
        if (phase != SessionPhase.Running) return
        val now = System.currentTimeMillis()
        val priorDistance = distanceMeters
        if (distanceIsAbsolute) {
            distanceMeters = distance.coerceAtLeast(distanceMeters)
        } else {
            distanceMeters += distance.coerceAtLeast(0f)
        }
        val strokeDistance = (distanceMeters - priorDistance).coerceAtLeast(0f)
        strokeCount += 1
        lastWatts = powerWatts
        wattSum += powerWatts
        wattSamples += 1

        if (lastStrokeAtMs > 0L) {
            val dtSec = ((now - lastStrokeAtMs) / 1000.0).coerceAtLeast(0.4)
            lastSpm = (60.0 / dtSec).toFloat().coerceIn(10f, 50f)
            val distForSplit = if (strokeDistance > 0.1f) strokeDistance else distance
            if (distForSplit > 0.1f) {
                val speed = distForSplit / dtSec.toFloat()
                if (speed > 0.1f) {
                    lastSplit = 500f / speed
                    splitSum += lastSplit!!
                    splitSamples += 1
                }
            }
        }
        lastStrokeAtMs = now
    }

    fun onInstantWatts(watts: Float) {
        if (phase != SessionPhase.Running) return
        lastWatts = watts
        wattSum += watts
        wattSamples += 1
    }

    fun snapshot(connected: Boolean = true): RowerTelemetry {
        val elapsedSec = (elapsedMs() / 1000L).toInt().coerceAtLeast(0)
        val avgWatts = if (wattSamples > 0) (wattSum / wattSamples).toFloat() else lastWatts
        val avgSplit = if (splitSamples > 0) (splitSum / splitSamples).toFloat() else lastSplit
        val calories = estimateCalories(elapsedSec, avgWatts)
        return RowerTelemetry(
            elapsedSec = elapsedSec,
            distanceMeters = distanceMeters,
            splitSecPer500 = lastSplit,
            spm = lastSpm,
            watts = lastWatts,
            avgWatts = avgWatts,
            calories = calories,
            strokeCount = strokeCount,
            resistance = resistance,
            avgSplitSecPer500 = avgSplit,
            heartRateBpm = heartRateBpm,
            source = source,
            connected = connected,
            portPath = portPath,
            sessionPhase = phase,
        )
    }

    private fun elapsedMs(): Long {
        val running = if (phase == SessionPhase.Running && runningSinceMs > 0L) {
            System.currentTimeMillis() - runningSinceMs
        } else {
            0L
        }
        return accumulatedMs + running
    }

    private fun estimateCalories(elapsedSec: Int, avgWatts: Float?): Float {
        val w = avgWatts ?: return 0f
        // Rough metabolic estimate: watts * seconds / 4184 * ~4 (mechanical to kcal factor)
        val kg = weightKg.coerceIn(40f, 160f)
        val hours = elapsedSec / 3600f
        val metish = (w / kg) * 0.12f
        return (metish * hours * kg).coerceAtLeast(0f) + (w * elapsedSec / 4184f)
    }
}
