package app.catchrower.update

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object UpToDate : UpdateState
    data class Available(val release: Release) : UpdateState
    /** In-app install is withheld. The sideload instructions stay until the module and app are installed. */
    data object SideloadRequired : UpdateState
    data class Downloading(val release: Release, val progress: Float) : UpdateState
    data class Installing(val release: Release) : UpdateState
    data class Updated(val version: String) : UpdateState
    data class Failed(val message: String, val release: Release? = null) : UpdateState
}
