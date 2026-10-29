package app.catchrower.hr

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * Opportunistic BLE Heart Rate Profile listener.
 * Connects to a preferred MAC when provided; otherwise stays idle.
 */
class HeartRateMonitor(private val context: Context) {
    private val _bpm = MutableStateFlow<Int?>(null)
    val bpm: StateFlow<Int?> = _bpm.asStateFlow()

    private val _connectedMac = MutableStateFlow<String?>(null)
    val connectedMac: StateFlow<String?> = _connectedMac.asStateFlow()

    private var gatt: BluetoothGatt? = null
    private var preferredMac: String? = null

    private val adapter: BluetoothAdapter?
        get() = (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter

    fun setPreferredMac(mac: String?) {
        preferredMac = mac?.uppercase()
        if (mac == null) {
            disconnect()
            return
        }
        if (_connectedMac.value?.equals(mac, ignoreCase = true) != true) {
            connectPreferred()
        }
    }

    @SuppressLint("MissingPermission")
    fun connectPreferred() {
        val mac = preferredMac ?: return
        val adapter = adapter ?: return
        if (!adapter.isEnabled) return
        disconnect()
        try {
            val device = adapter.getRemoteDevice(mac)
            gatt = device.connectGatt(context, false, callback, BluetoothDevice.TRANSPORT_LE)
        } catch (e: Exception) {
            Log.w(TAG, "HR connect failed", e)
        }
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        runCatching { gatt?.close() }
        gatt = null
        _connectedMac.value = null
        _bpm.value = null
    }

    @SuppressLint("MissingPermission")
    private val callback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                _connectedMac.value = g.device.address
                g.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                _connectedMac.value = null
                _bpm.value = null
            }
        }

        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) return
            val service = g.getService(HR_SERVICE) ?: return
            val characteristic = service.getCharacteristic(HR_MEASUREMENT) ?: return
            g.setCharacteristicNotification(characteristic, true)
            val descriptor = characteristic.getDescriptor(CCCD) ?: return
            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            g.writeDescriptor(descriptor)
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(
            g: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
        ) {
            if (characteristic.uuid != HR_MEASUREMENT) return
            _bpm.value = parseHeartRate(characteristic.value)
        }

        override fun onCharacteristicChanged(
            g: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
        ) {
            if (characteristic.uuid != HR_MEASUREMENT) return
            _bpm.value = parseHeartRate(value)
        }
    }

    companion object {
        private const val TAG = "HeartRateMonitor"
        private val HR_SERVICE: UUID = UUID.fromString("0000180D-0000-1000-8000-00805f9b34fb")
        private val HR_MEASUREMENT: UUID = UUID.fromString("00002A37-0000-1000-8000-00805f9b34fb")
        private val CCCD: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

        fun parseHeartRate(data: ByteArray?): Int? {
            if (data == null || data.isEmpty()) return null
            val flags = data[0].toInt() and 0xFF
            return if (flags and 0x01 == 0) {
                if (data.size < 2) null else data[1].toInt() and 0xFF
            } else {
                if (data.size < 3) null else ((data[1].toInt() and 0xFF) or ((data[2].toInt() and 0xFF) shl 8))
            }
        }
    }
}
