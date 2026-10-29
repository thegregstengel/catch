package app.catchrower.ui.profile

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.catchrower.ui.theme.CatchAccent

@Composable
fun ProfileAvatar(
    name: String,
    photoUri: String?,
    size: Dp,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
) {
    val borderMod = if (selected) {
        Modifier.border(3.dp, CatchAccent, CircleShape)
    } else {
        Modifier
    }
    Box(
        modifier = modifier
            .size(size)
            .then(borderMod)
            .clip(CircleShape)
            .background(CatchAccent.copy(alpha = 0.35f)),
        contentAlignment = Alignment.Center,
    ) {
        val defaultRes = DefaultAvatars.drawableRes(photoUri)
        when {
            defaultRes != null -> {
                Image(
                    painter = painterResource(defaultRes),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(size),
                )
            }
            !photoUri.isNullOrBlank() -> {
                ContentUriAvatar(uriString = photoUri, size = size)
            }
            else -> {
                Text(
                    text = name.take(1).uppercase().ifEmpty { "?" },
                    style = when {
                        size >= 80.dp -> MaterialTheme.typography.headlineLarge
                        size >= 40.dp -> MaterialTheme.typography.titleMedium
                        else -> MaterialTheme.typography.labelLarge
                    },
                )
            }
        }
    }
}

@Composable
private fun ContentUriAvatar(uriString: String, size: Dp) {
    val context = LocalContext.current
    val bitmap = remember(uriString) {
        runCatching {
            val uri = Uri.parse(uriString)
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
        }.getOrNull()
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(size),
        )
    } else {
        Text("?", style = MaterialTheme.typography.titleMedium)
    }
}
