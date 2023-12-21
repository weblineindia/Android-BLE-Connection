package com.wli.bleconnection.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DividerItemDecoration
import com.wli.bleconnection.R
import com.wli.bleconnection.adapter.ConnectedListAdapter
import com.wli.bleconnection.data.model.ConnectedDeviceModel
import com.wli.bleconnection.databinding.ActivityMainBinding
import com.wli.bleconnection.utils.setCancelIcon
import com.wli.bleconnection.utils.setDoneIcon
import com.wli.bleconnection.utils.viewBinding
import com.wli.bleconnection.viewmodel.BaseActivityViewModel

class MainActivity : BaseActivity(), View.OnClickListener {

    private val binding by viewBinding(ActivityMainBinding::inflate)
    private val viewModel: BaseActivityViewModel by viewModels()
    private val foundDeviceList = ArrayList<ConnectedDeviceModel>()
    private var selectedDeviceModel: ConnectedDeviceModel? = null
    private val mAdapter by lazy {
        ConnectedListAdapter(listener = { selectedDevice ->
            if (!selectedDevice.isConnected) {
                selectedDeviceModel = selectedDevice
                connectToGatt(selectedDeviceModel)
            } else {
                disconnectGatt()
            }
        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        rootContainerView = binding.root
        super.onCreate(savedInstanceState)

        initClickListener()
        registerBroadcastReceiver()
        initObserver()
    }

    private fun initClickListener() {
        with(binding) {
            btnGrantPermission.setOnClickListener(this@MainActivity)
            btnStartScanning.setOnClickListener(this@MainActivity)

            rvDeviceList.apply {
                adapter = mAdapter
                addItemDecoration(
                    DividerItemDecoration(
                        this.context,
                        DividerItemDecoration.VERTICAL
                    )
                )
            }

            //If permissions are already granted then start next process on launch of application
            if (checkPermission()) {
                btnGrantPermission.performClick()
            }
        }
    }

    private fun initObserver() {
        viewModel.uiEvent.observe(this) {
            when (it) {
                is BaseActivityViewModel.UiEvent.OnPermissionGrant -> {
                    with(binding) {
                        if (it.isGranted) {
                            btnGrantPermission.apply {
                                text = getString(R.string.granted_permission)
                                setDoneIcon()
                            }
                            btnBluetooth.isVisible = true
                            //Ask user to enable bluetooth for scanning
                            onPermissionGrant()
                        } else {
                            btnGrantPermission.apply {
                                text = getString(R.string.denied_permission)
                                setCancelIcon()
                            }
                            btnBluetooth.isVisible = false
                            askPermissions()
                        }
                    }
                }

                is BaseActivityViewModel.UiEvent.OnBluetoothEnable -> {
                    with(binding) {
                        if (it.isEnable) {
                            btnBluetooth.apply {
                                text = getString(R.string.enabled_bluetooth)
                                setDoneIcon()
                            }
                            btnStartScanning.isVisible = true
                            //Start scanning near by devices
                            startScan()
                        } else {
                            btnBluetooth.apply {
                                text = getString(R.string.off_bluetooth)
                                setCancelIcon()
                            }
                            btnStartScanning.isVisible = false
                        }
                    }
                }

                is BaseActivityViewModel.UiEvent.OnDeviceFound -> {
                    if (checkPermission()) {
                        if (!it.foundDevice.name.isNullOrEmpty()) {
                            foundDeviceList.add(
                                ConnectedDeviceModel(
                                    deviceName = it.foundDevice.name ?: "",
                                    deviceMac = it.foundDevice.address,
                                    isConnected = false,
                                    batteryLevel = "",
                                    rssi = ""
                                )
                            )
                        }
                        mAdapter.addAll(foundDeviceList)
                    }
                }

                BaseActivityViewModel.UiEvent.OnSearchFailed -> {
                    with(binding) {
                        btnStartScanning.apply {
                            text = getString(R.string.re_start_scan)
                        }
                    }
                }
            }
        }
    }

    private fun getFoundDeviceFromList(): ConnectedDeviceModel? {
        return foundDeviceList.find { it == selectedDeviceModel }
    }

    private fun registerBroadcastReceiver() {
        val intentFilter = IntentFilter()
        val actionsList = arrayListOf(
            ACTION_GATT_CONNECTED,
            ACTION_CHARACTERISTICS_READ,
            ACTION_RSSI_VALUE,
            ACTION_BATTERY_UPDATE
        )
        actionsList.onEach {
            intentFilter.addAction(it)
        }
        apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                /**
                 * RECEIVER_NOT_EXPORTED -> To register a broadcast receiver that does not receive broadcasts from other apps,
                 * including system apps, register the receiver using the following code:
                 */
                registerReceiver(mGattUpdateReceiver, intentFilter, RECEIVER_EXPORTED)
            } else {
                registerReceiver(mGattUpdateReceiver, intentFilter)
            }
        }
    }

    private val mGattUpdateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                ACTION_GATT_CONNECTED -> {
                    val connectionStatus = intent.getStringExtra(EXTRA_DATA)
                    val isConnected = connectionStatus == "1"
                    getFoundDeviceFromList()?.let { foundDevice ->
                        foundDevice.isConnected = isConnected
                    }
                    updateList()
                    if (isConnected) {
                        discoverServices()
                    } else {
                        disconnectGatt()
                    }
                }

                ACTION_CHARACTERISTICS_READ -> {
                    val batteryLevel = intent.getStringExtra(EXTRA_DATA)
                    getFoundDeviceFromList()?.let { foundDevice ->
                        foundDevice.batteryLevel = batteryLevel.toString()
                    }
                    updateList()
                    getRemoteRssi()
                }

                ACTION_RSSI_VALUE -> {
                    val rssi = intent.getStringExtra(EXTRA_DATA)
                    getFoundDeviceFromList()?.let { foundDevice ->
                        foundDevice.rssi = rssi.toString()
                    }
                    updateList()
                    enabledNotification()
                }

                ACTION_BATTERY_UPDATE -> {
                    val batteryLevel = intent.getStringExtra(EXTRA_DATA)
                    getFoundDeviceFromList()?.let { foundDevice ->
                        foundDevice.batteryLevel = batteryLevel.toString()
                    }
                    updateList()
                }
            }
        }
    }

    private fun updateList() {
        mAdapter.notifyItemChanged(foundDeviceList.indexOf(getFoundDeviceFromList()))
    }

    override fun onDestroy() {
        unregisterReceiver(mGattUpdateReceiver)
        super.onDestroy()
    }

    override fun onClick(v: View?) {
        with(binding) {
            when (v) {
                btnGrantPermission -> {
                    askPermissions()
                }

                btnBluetooth -> {
                    onPermissionGrant()
                }

                btnStartScanning -> {
                    startScan()
                }
            }
        }
    }
}