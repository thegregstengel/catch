package app.catchrower.rower

import android.util.Log
import kotlinx.coroutines.CoroutineScope

object RowerHalFactory {
    private const val TAG = "RowerHalFactory"

    /**
     * Prefer live UART; fall back to [MockRowerHal] when serial open fails.
     */
    fun create(scope: CoroutineScope, forceMock: Boolean = false): RowerHal {
        if (!forceMock) {
            val serial = SerialRowerHal.tryOpen(scope)
            if (serial != null) {
                Log.i(TAG, "Using serial rower HAL")
                return serial
            }
            Log.i(TAG, "UART unavailable; using MockRowerHal")
        }
        return MockRowerHal(scope)
    }
}
