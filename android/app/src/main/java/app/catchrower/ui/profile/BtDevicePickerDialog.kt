package app.catchrower.ui.profile

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.catchrower.BuildConfig
import app.catchrower.data.db.ProfileBtDeviceEntity

data class BtChoice(
    val mac: String,
    val name: String,
    val kind: String,
)

@SuppressLint("MissingPermission")
@Composable
fun BtDevicePickerDialog(
    remembered: List<ProfileBtDeviceEntity>,
    onDismiss: () -> Unit,
    onPick: (mac: String, name: String, kind: String) -> Unit,
) {
    val context = LocalContext.current
    val bonded = remember {
        bondedDevices(context)
    }
    val choices = buildList {
        remembered.forEach {
            add(BtChoice(it.macAddress, it.displayName, it.kind))
        }
        bonded.forEach { b ->
            if (none { it.mac.equals(b.mac, ignoreCase = true) }) add(b)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Bluetooth for this profile") },
        text = {
            Column {
                Text("Pick headphones, a watch, or a heart rate strap. Catch remembers the MAC for next time.")
                LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                    items(choices, key = { it.mac }) { device ->
                        ListItem(
                            headlineContent = { Text(device.name) },
                            supportingContent = { Text("${device.mac} · ${device.kind}") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onPick(device.mac, device.name, device.kind)
                                },
                        )
                    }
                    if (choices.isEmpty()) {
                        item {
                            Text("No bonded devices found. Pair in Android Settings first, then return.")
                        }
                    }
                }
                if (BuildConfig.DEBUG) {
                    TextButton(
                        onClick = {
                            onPick("00:00:00:00:00:00", "Demo HR", "hr")
                        },
                    ) {
                        Text("Use demo HR device (dev)")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Skip") }
        },
    )
}

@SuppressLint("MissingPermission")
private fun bondedDevices(context: Context): List<BtChoice> {
    return try {
        val adapter =
            (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
                ?: return emptyList()
        adapter.bondedDevices.orEmpty().map { device ->
            val name = device.name ?: device.address
            val kind = when {
                name.contains("watch", ignoreCase = true) ||
                    name.contains("hr", ignoreCase = true) ||
                    name.contains("polar", ignoreCase = true) ||
                    name.contains("tickr", ignoreCase = true) -> "hr"
                name.contains("buds", ignoreCase = true) ||
                    name.contains("head", ignoreCase = true) ||
                    name.contains("airpod", ignoreCase = true) -> "audio"
                else -> "other"
            }
            BtChoice(device.address, name, kind)
        }
    } catch (_: SecurityException) {
        emptyList()
    }
}
