package com.wli.bleconnection.ui

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.wli.bleconnection.R
import com.wli.bleconnection.data.model.ConnectedDeviceModel
import com.wli.bleconnection.utils.batteryCharUUID
import com.wli.bleconnection.utils.batteryServiceUUID
import com.wli.bleconnection.utils.descriptor_uuid
import com.wli.bleconnection.viewmodel.BaseActivityViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * This file contains:
 1) User permission related to BLE devices
 2) Searching near by available devices for connection
 3) Connection to disconnection process of device
 4) Get connected device battery and signal strength
 */
open class BaseActivity : AppCompatActivity() {

    open var rootContainerView: View? = null
    private val viewModel: BaseActivityViewModel by viewModels()
    private val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
    private var bluetoothManager: BluetoothManager? = null
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothLeScanner: BluetoothLeScanner? = null
    private var bleGatt: BluetoothGatt? = null

    //required for bluetooth permission below Android 12
    private val locationPermission = arrayOf(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    //required for bluetooth permission on and above Android 12
    @RequiresApi(Build.VERSION_CODES.S)
    private val bluetoothOnSPermission = arrayOf(
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT
    )
    private val bluetoothPermissionRequest = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val granted = permissions.entries.all {
                it.value
            }
            viewModel.onPermissionGrant(granted)
        }

    private var bluetoothLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                viewModel.onBluetoothEnable(true)
            } else {
                viewModel.onBluetoothEnable(false)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        rootContainerView?.let { container ->
            setContentView(container)
        }
        init()
    }

    private fun init() {
        bluetoothManager = getSystemService(BluetoothManager::class.java)
        bluetoothAdapter = bluetoothManager?.adapter
        bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner
    }

    fun onPermissionGrant() {
        if (bluetoothAdapter?.isEnabled == true) {
            viewModel.onBluetoothEnable(true)
        } else {
            //Enable bluetooth after granting permission for it
            bluetoothLauncher.launch(enableBtIntent)
        }
    }

    fun askPermissions() {
        if (checkPermission()) {
            viewModel.onPermissionGrant(true)
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                bluetoothPermissionRequest.launch(bluetoothOnSPermission)
            } else {
                bluetoothPermissionRequest.launch(locationPermission)
            }
        }
    }

    fun checkPermission(): Boolean {
        //Android 12
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        } else {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

    fun startScan() {
        if (checkPermission()) {
            if (bluetoothAdapter?.isEnabled == true) {
                if (bluetoothLeScanner == null) {
                    bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner
                }
                val scanSettings = ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                    .build()
                stopScan()
                bluetoothLeScanner?.startScan(arrayListOf(), scanSettings, scanCallback)

                // Stops scanning after 10 seconds.
                CoroutineScope(Dispatchers.Default).launch {
                    withContext(Dispatchers.Main) {
                        delay(STOP_SEARCH_TIMER)
                        stopScan()
                        viewModel.onSearchFailed()
                    }
                }
            } else {
                bluetoothLauncher.launch(enableBtIntent)
            }
        } else {
            askPermissions()
        }
    }

    private fun stopScan() {
        if (checkPermission()) {
            bluetoothLeScanner?.stopScan(scanCallback)
        }
    }

    private val scanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device: BluetoothDevice = result.device
            Log.e("onScanResult", "$device")
            viewModel.onDeviceFound(device)
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e("onScanFailed", "$errorCode")
            viewModel.onSearchFailed()
        }
    }

    fun connectToGatt(selectedDevice: ConnectedDeviceModel?) {
        if (checkPermission()) {
            val device: BluetoothDevice? = bluetoothAdapter?.getRemoteDevice(selectedDevice?.deviceMac)
            bleGatt = device?.connectGatt(this, false, bluetoothGattCallback)
        }
    }

    private val bluetoothGattCallback = object : BluetoothGattCallback() {
        /**
         * Called when status is connected / disconnected
         */
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                broadcastUpdate(ACTION_GATT_CONNECTED, "1")
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                broadcastUpdate(ACTION_GATT_CONNECTED, "0")
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (!getSupportedGattServices().isNullOrEmpty() && checkPermission()) {
                /**
                 * get battery level of connected device
                 */
                getSupportedGattServices()?.find {
                    it?.uuid.toString().uppercase().split("-")[0].contains(batteryServiceUUID)
                }?.let { service ->
                    service.characteristics.find { char ->
                        char.uuid.toString().uppercase().split("-")[0].contains(batteryCharUUID)
                    }?.let {
                        gatt?.readCharacteristic(it)
                    }
                }
            }
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val uuid = characteristic.uuid.toString().uppercase().split("-")[0]
                if (uuid.contains(batteryCharUUID)) {
                    val batteryLevel = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0)
                    broadcastUpdate(ACTION_CHARACTERISTICS_READ, batteryLevel.toString())
                }
            }
        }

        @Deprecated("Used natively in Android 12 and lower")
        override fun onCharacteristicRead(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val uuid = characteristic?.uuid.toString().uppercase().split("-")[0]
                if (uuid.contains(batteryCharUUID)) {
                    val batteryLevel = characteristic?.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0)
                    broadcastUpdate(ACTION_CHARACTERISTICS_READ, batteryLevel.toString())
                }
            }
        }

        override fun onReadRemoteRssi(gatt: BluetoothGatt?, rssi: Int, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_RSSI_VALUE, rssi.toString())
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
            val char = characteristic.uuid.toString().uppercase()
            if (char.split("-")[0].contains(batteryCharUUID)) {
                val batteryLevel = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0)
                broadcastUpdate(ACTION_BATTERY_UPDATE, batteryLevel.toString())
            }
        }

        @Deprecated("Used natively in Android 12 and lower")
        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
            val char = characteristic?.uuid.toString().uppercase()
            if (char.split("-")[0].contains(batteryCharUUID)) {
                val batteryLevel = characteristic?.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0)
                broadcastUpdate(ACTION_BATTERY_UPDATE, batteryLevel.toString())
            }
        }
    }

    fun discoverServices() {
        if (checkPermission()) {
            bleGatt?.discoverServices()
        }
    }

    fun disconnectGatt() {
        if (checkPermission()) {
            bleGatt?.disconnect()
            Toast.makeText(this, getString(R.string.device_disconnect), Toast.LENGTH_SHORT).show()
            bleGatt?.close()
            bleGatt = null
        }
    }

    fun getRemoteRssi() {
        if (checkPermission()) {
            bleGatt?.readRemoteRssi()
        }
    }

    private fun getSupportedGattServices(): List<BluetoothGattService?>? {
        return try {
            if (bleGatt == null) null else bleGatt?.services
        } catch (e: Exception) {
            return emptyList()
        }
    }

    private fun broadcastUpdate(action: String, value: String) {
        val intent = Intent(action)
        intent.putExtra(EXTRA_DATA, value)
        sendBroadcast(intent)
    }

    private fun BluetoothGattCharacteristic.setCharacteristicNotification() {
        if (checkPermission()) {
            bleGatt?.setCharacteristicNotification(this, true)
            val descriptor = getDescriptor(UUID.fromString(descriptor_uuid))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                bleGatt?.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
            } else {
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                bleGatt?.writeDescriptor(descriptor)
            }
        }
    }

    fun enabledNotification() {
        getSupportedGattServices()?.find {
            it?.uuid.toString().uppercase().split("-")[0].contains(batteryServiceUUID)
        }?.let { service ->
            service.characteristics.find { char ->
                char.uuid.toString().uppercase().split("-")[0].contains(batteryCharUUID)
            }?.setCharacteristicNotification()
        }
    }

    companion object {
        const val EXTRA_DATA = "EXTRA_DATA"
        const val ACTION_GATT_CONNECTED = "ACTION_GATT_CONNECTED"
        const val ACTION_CHARACTERISTICS_READ = "ACTION_CHARACTERISTICS_READ"
        const val ACTION_RSSI_VALUE = "ACTION_RSSI_VALUE"
        const val ACTION_BATTERY_UPDATE = "ACTION_BATTERY_UPDATE"
        const val STOP_SEARCH_TIMER = 10000L
    }
}