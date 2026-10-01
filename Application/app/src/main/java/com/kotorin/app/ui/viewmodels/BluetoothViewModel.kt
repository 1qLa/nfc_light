package com.kotorin.app.ui.viewmodels

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

data class BleDevice(
    val id: String,
    val name: String?,
    val rssi: Int,
    val device: BluetoothDevice? = null,  // デモデータは null
    val isDemo: Boolean = false
)

enum class ConnectionState {
    NONE, CONNECTING, CONNECTED, DISCONNECTED, ERROR
}

class BluetoothViewModel : ViewModel() {
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _foundDevices = MutableStateFlow<List<BleDevice>>(emptyList())
    val foundDevices: StateFlow<List<BleDevice>> = _foundDevices.asStateFlow()

    private val _connectionState = MutableStateFlow(ConnectionState.NONE)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _connectedDevice = MutableStateFlow<BleDevice?>(null)
    val connectedDevice: StateFlow<BleDevice?> = _connectedDevice.asStateFlow()

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothGatt: BluetoothGatt? = null
    private val handler = Handler(Looper.getMainLooper())
    private var appContext: Context? = null

    companion object {
        val NUS_SERVICE_UUID: UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E")
        val NUS_RX_CHAR_UUID: UUID = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E")
        val NUS_TX_CHAR_UUID: UUID = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E")
        val CCC_DESCRIPTOR_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

        val DEMO_DEVICES = listOf(
            BleDevice("AA:BB:CC:DD:EE:01", "XIAO_ESP32C3",      rssi = -52, isDemo = true),
            BleDevice("AA:BB:CC:DD:EE:02", "XIAO_ESP32C3_02",   rssi = -67, isDemo = true),
            BleDevice("AA:BB:CC:DD:EE:03", "IoT-Sensor-Node",   rssi = -74, isDemo = true),
            BleDevice("AA:BB:CC:DD:EE:04", "SmartLock-A1B2",    rssi = -81, isDemo = true),
        )
    }

    fun initBluetooth(context: Context) {
        appContext = context.applicationContext
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
    }

    fun loadDemoDevices() {
        _foundDevices.value = DEMO_DEVICES
    }

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            if (device.name != null) {
                val bleDevice = BleDevice(device.address, device.name, result.rssi, device)
                if (!_foundDevices.value.any { it.id == bleDevice.id }) {
                    _foundDevices.value = _foundDevices.value + bleDevice
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startScan() {
        if (_isScanning.value) return
        val scanner = bluetoothAdapter?.bluetoothLeScanner ?: return

        _foundDevices.value = emptyList()
        _isScanning.value = true

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        scanner.startScan(null, settings, scanCallback)
        handler.postDelayed({ stopScan() }, 10000)
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        if (!_isScanning.value) return
        bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
        _isScanning.value = false
    }

    @SuppressLint("MissingPermission")
    fun connect(device: BleDevice) {
        stopScan()
        _connectedDevice.value = device
        _connectionState.value = ConnectionState.CONNECTING

        if (device.isDemo) {
            // デモデバイスは2秒後に接続完了をシミュレート
            handler.postDelayed({
                _connectionState.value = ConnectionState.CONNECTED
            }, 2000)
            return
        }

        val context = appContext ?: return
        bluetoothGatt = device.device?.connectGatt(
            context,
            false,
            gattCallback,
            BluetoothDevice.TRANSPORT_LE
        )
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        bluetoothGatt?.disconnect()
        if (_connectedDevice.value?.isDemo == true) {
            _connectionState.value = ConnectionState.DISCONNECTED
        }
    }

    @SuppressLint("MissingPermission")
    fun sendData(data: ByteArray) {
        if (_connectedDevice.value?.isDemo == true) return  // デモは送信スキップ
        val gatt = bluetoothGatt ?: return
        val rxChar = gatt.getService(NUS_SERVICE_UUID)
            ?.getCharacteristic(NUS_RX_CHAR_UUID) ?: return
        @Suppress("DEPRECATION")
        rxChar.value = data
        rxChar.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
        @Suppress("DEPRECATION")
        gatt.writeCharacteristic(rxChar)
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                _connectionState.value = ConnectionState.ERROR
                gatt.close()
                bluetoothGatt = null
                return
            }
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    _connectionState.value = ConnectionState.CONNECTED
                    gatt.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    _connectionState.value = ConnectionState.DISCONNECTED
                    gatt.close()
                    bluetoothGatt = null
                }
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) return
            val txChar = gatt.getService(NUS_SERVICE_UUID)
                ?.getCharacteristic(NUS_TX_CHAR_UUID) ?: return
            gatt.setCharacteristicNotification(txChar, true)
            val descriptor = txChar.getDescriptor(CCC_DESCRIPTOR_UUID) ?: return
            @Suppress("DEPRECATION")
            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            @Suppress("DEPRECATION")
            gatt.writeDescriptor(descriptor)
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            if (characteristic.uuid == NUS_TX_CHAR_UUID) onDataReceived(value)
        }

        @Suppress("DEPRECATION")
        @Deprecated("Deprecated in API 33")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            if (characteristic.uuid == NUS_TX_CHAR_UUID) {
                @Suppress("DEPRECATION")
                onDataReceived(characteristic.value)
            }
        }
    }

    private fun onDataReceived(data: ByteArray) {
        // XIAO ESP32C3から受信したデータの処理をここに追加
    }

    override fun onCleared() {
        super.onCleared()
        disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
    }
}
