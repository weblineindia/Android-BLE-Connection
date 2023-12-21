# BLE device connection

Welcome to BLE device connection sample app!

Generic Attribute Profile (GATT) is a specification in the Bluetooth Low Energy (BLE) protocol stack. It is built on top of the Attribute Protocol. 
It defines the structure in which data is exchanged between two devices.

## Table of contents
- [Android Support](#android-support)
- [Sample](#sample)
- [Features](#features)
- [Getting started](#getting-started)
- [Implementation](#implementation)
- [Want to Contribute?](#want-to-contribute)
- [Need Help / Support?](#need-help)
- [Collection of Components](#collection-of-Components)
- [Changelog](#changelog)
- [License](#license)
- [Keywords](#Keywords)

## Android Support

Version - Android 14

We have tested our program in above version, however you can use it in other versions as well.

## Sample
![](wli_connection.gif)

## Features

* Required user permission for connection process
* Searching near by available device and show in list
* Connection between application and BLE device
* Discover available characteristics of connected device
* Get and update of battery level of device
* Get signal strength of device

## Getting Started

To build and run the project, follow these steps:

1. Clone the repository
2. Open the project in Android Studio
3. Build and run the app on your device


### Implementation:

* **Description of options given in sample application**

    1) Start scanning process for near by devices
        ```
         bluetoothLeScanner?.startScan(arrayListOf(), scanSettings, scanCallback)
        ```
    2) Stop scanning after 10second of time interval
        ```
        bluetoothLeScanner?.stopScan(scanCallback)
        ```
    3) Connect to device with bluetooth gatt
       ```
       private var bleGatt: BluetoothGatt? = null
       .
       .
       bleGatt = device?.connectGatt(this, false, bluetoothGattCallback)
       ```
    4) Declare GATT callback in BaseActivity.kt
       ```
       private val bluetoothGattCallback = object : BluetoothGattCallback() {
           override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
               if (newState == BluetoothProfile.STATE_CONNECTED) {
                   // successfully connected to the GATT Server
               } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                   // disconnected from the GATT Server
               }
           }
       }
       ```
    5) Broadcast updates
        ```
       private val mGattUpdateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
           override fun onReceive(context: Context, intent: Intent) {
               when (intent.action) {
                   .
                   .
                   .
               }
           }
       }
       ```
    6) Register broadcast receiver
        ```
       if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          registerReceiver(mGattUpdateReceiver, intentFilter, RECEIVER_EXPORTED)
        } else {
          registerReceiver(mGattUpdateReceiver, intentFilter)
        }
       ```
    7) Unregister broadcast receiver
       ````
          override fun onDestroy() {
              unregisterReceiver(mGattUpdateReceiver)
              super.onDestroy()
           }
        ````
    8) Discovered BLE characteristics
        ````
          private fun getSupportedGattServices(): List<BluetoothGattService?>? {
            return try {
               if (bleGatt == null) null else bleGatt?.services
            } catch (e: Exception) {
               return emptyList()
            }
          }
       ````
    9) Read BLE characteristics
        ````
       bluetoothGatt?.let { gatt ->
            gatt.readCharacteristic(characteristic)
        } ?: run {
            Log.w(TAG, "BluetoothGatt not initialized")
            Return
        }
       ````
    10) Enable notification via descriptor
        ````
         bleGatt?.setCharacteristicNotification(this, true)
         bleGatt?.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
        ````
    11) Callback for listening notification
        ````
        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
          .
          .
        }
        ````


## Want to Contribute?

- Created something awesome, made this code better, added some functionality, or whatever (this is the hardest part).
- [Fork it](http://help.github.com/forking/).
- Create new branch to contribute your changes.
- Commit all your changes to your branch.
- Submit a [pull request](http://help.github.com/pull-requests/).


## Collection of Components
We have built many other components and free resources for software development in various programming languages. Kindly click here to view our [Free Resources for Software Development.](https://www.weblineindia.com/software-development-resources.html)


## Changelog
Detailed changes for each release are documented in [CHANGELOG](./CHANGELOG).

## License
[MIT](LICENSE)

[mit]: ./LICENSE

## Keywords
BluetoothGatt, Bluetooth Low Energy (BLE) device, Generic Attribute Profile (GATT)