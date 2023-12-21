package com.wli.bleconnection.data.model

data class ConnectedDeviceModel(
    val deviceName: String,
    val deviceMac: String,
    var isConnected: Boolean,
    var batteryLevel: String,
    var rssi: String
)