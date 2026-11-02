package app.catchrower.ui.idle

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import app.catchrower.R

/**
 * Black cover drawn above Catch, including dialogs. The first finger-down wakes.
 * Nothing is painted except black, so the panel reads as off.
 */
@Composable
fun IdleCover(onWake: () -> Unit) {
    val description = stringResource(R.string.screen_off_wake)
    Dialog(
        onDismissRequest = onWake,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        val view = LocalView.current
        val configuration = LocalConfiguration.current
        DisposableEffect(view) {
            (view.parent as? DialogWindowProvider)?.window?.let(IdleWindow::applyCover)
            onDispose { }
        }
        Box(
            modifier = Modifier
                .requiredSize(
                    width = configuration.screenWidthDp.dp,
                    height = configuration.screenHeightDp.dp,
                )
                .background(Color.Black)
                .semantics { contentDescription = description }
                .pointerInput(onWake) {
                    detectTapGestures(onPress = { onWake() })
                },
        )
    }
}
