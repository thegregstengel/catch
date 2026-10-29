package app.catchrower.ui.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.catchrower.R
import app.catchrower.data.db.ProfileEntity

private const val LB_PER_KG = 2.2046226218f

@Composable
fun AddProfileDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, photoUri: String?, weightKg: Float?, ageYears: Int?, sex: String?) -> Unit,
) {
    ProfileFormDialog(
        title = stringResource(R.string.add_profile),
        confirmLabel = stringResource(R.string.profile_create),
        initial = null,
        onDismiss = onDismiss,
        onSave = onCreate,
    )
}

@Composable
fun EditProfileDialog(
    profile: ProfileEntity,
    onDismiss: () -> Unit,
    onSave: (ProfileEntity) -> Unit,
) {
    ProfileFormDialog(
        title = stringResource(R.string.edit_profile),
        confirmLabel = stringResource(R.string.profile_save),
        initial = profile,
        onDismiss = onDismiss,
        onSave = { name, photo, weightKg, age, sex ->
            onSave(
                profile.copy(
                    displayName = name,
                    photoUri = photo,
                    weightKg = weightKg,
                    ageYears = age,
                    sex = sex,
                ),
            )
        },
    )
}

@Composable
private fun ProfileFormDialog(
    title: String,
    confirmLabel: String,
    initial: ProfileEntity?,
    onDismiss: () -> Unit,
    onSave: (name: String, photoUri: String?, weightKg: Float?, ageYears: Int?, sex: String?) -> Unit,
) {
    var name by remember { mutableStateOf(initial?.displayName.orEmpty()) }
    var weightLb by remember {
        mutableStateOf(
            initial?.weightKg?.times(LB_PER_KG)?.let { "%.1f".format(it) }.orEmpty(),
        )
    }
    var age by remember { mutableStateOf(initial?.ageYears?.toString().orEmpty()) }
    var sex by remember { mutableStateOf(initial?.sex) }
    var photoUri by remember { mutableStateOf(initial?.photoUri) }

    val pickPhoto = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        if (uri != null) photoUri = uri.toString()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.setup_profile_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ProfileAvatar(
                        name = name.ifBlank { "?" },
                        photoUri = photoUri,
                        size = 64.dp,
                    )
                    OutlinedButton(
                        onClick = { pickPhoto.launch("image/*") },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            if (photoUri != null && !DefaultAvatars.isDefault(photoUri)) {
                                stringResource(R.string.setup_profile_photo_selected)
                            } else {
                                stringResource(R.string.setup_profile_photo)
                            },
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                AvatarPickerGrid(
                    selectedUri = photoUri,
                    onSelect = { photoUri = it },
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = weightLb,
                    onValueChange = { weightLb = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = { Text(stringResource(R.string.setup_profile_weight)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = age,
                    onValueChange = { age = it.filter { ch -> ch.isDigit() } },
                    label = { Text(stringResource(R.string.setup_profile_age)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.setup_profile_sex))
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = sex == "M",
                        onClick = { sex = if (sex == "M") null else "M" },
                        label = { Text("M") },
                    )
                    FilterChip(
                        selected = sex == "F",
                        onClick = { sex = if (sex == "F") null else "F" },
                        label = { Text("F") },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        name.trim(),
                        photoUri ?: initial?.photoUri,
                        weightLb.toFloatOrNull()?.div(LB_PER_KG),
                        age.toIntOrNull(),
                        sex,
                    )
                },
                enabled = name.isNotBlank(),
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}
