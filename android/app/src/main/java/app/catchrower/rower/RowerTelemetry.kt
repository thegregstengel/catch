package app.catchrower.rower

/**
 * Live session metrics surfaced to the dashboard.
 */
enum class SessionPhase {
    Idle,
    Running,
    Paused,
}

data class RowerTelemetry(
    val elapsedSec: Int = 0,
    val distanceMeters: Float = 0f,
    val splitSecPer500: Float? = null,
    val spm: Float? = null,
    val watts: Float? = null,
    val avgWatts: Float? = null,
    val calories: Float? = null,
    val strokeCount: Int = 0,
    val resistance: Int = 5,
    val avgSplitSecPer500: Float? = null,
    val heartRateBpm: Int? = null,
    val source: TelemetrySource = TelemetrySource.Mock,
    val connected: Boolean = false,
    val portPath: String? = null,
    val sessionPhase: SessionPhase = SessionPhase.Idle,
) {
    val zone: Int?
        get() {
            val hr = heartRateBpm ?: return null
            return when {
                hr < 100 -> 1
                hr < 120 -> 2
                hr < 140 -> 3
                hr < 160 -> 4
                else -> 5
            }
        }
}

enum class TelemetrySource {
    Serial,
    Mock,
}

interface RowerHal {
    val telemetry: kotlinx.coroutines.flow.StateFlow<RowerTelemetry>
    suspend fun start()
    suspend fun stop()
    fun setResistance(level: Int)
    fun setHeartRate(bpm: Int?)
    /** Start or resume the workout clock and stroke accumulation. */
    fun startSession()
    /** Freeze the workout clock; keep live resistance / connection. */
    fun pauseSession()
    /** Clear metrics and return to idle (clock at 0). */
    fun resetSession()
}
