package app.catchrower.ui.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.catchrower.R
import app.catchrower.ui.theme.CatchTextSecondary

@Composable
fun AvatarPickerGrid(
    selectedUri: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(R.string.setup_profile_avatar),
            style = MaterialTheme.typography.titleSmall,
            color = CatchTextSecondary,
        )
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 56.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 220.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 4.dp),
        ) {
            items(DefaultAvatars.all, key = { it.id }) { avatar ->
                val uri = DefaultAvatars.uriFor(avatar)
                ProfileAvatar(
                    name = avatar.id,
                    photoUri = uri,
                    size = 56.dp,
                    selected = selectedUri == uri,
                    modifier = Modifier.clickable { onSelect(uri) },
                )
            }
        }
    }
}
