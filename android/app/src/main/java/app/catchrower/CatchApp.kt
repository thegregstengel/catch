package app.catchrower

import android.app.Application
import app.catchrower.data.CatchRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class CatchApp : Application() {
    val repository by lazy { CatchRepository(this) }
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            repository.ensureSettings()
            repository.archiveLastWeekWinnerIfNeeded()
        }
    }
}
