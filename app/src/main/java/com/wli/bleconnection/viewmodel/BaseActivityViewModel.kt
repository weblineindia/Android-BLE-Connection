package com.wli.bleconnection.viewmodel

import android.bluetooth.BluetoothDevice
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.wli.bleconnection.data.model.ConnectedDeviceModel

class BaseActivityViewModel : ViewModel() {

    private val _uiEvent = MutableLiveData<UiEvent>()
    val uiEvent: LiveData<UiEvent>
        get() = _uiEvent

    fun onPermissionGrant(isGranted: Boolean) {
        _uiEvent.value = UiEvent.OnPermissionGrant(isGranted)
    }

    fun onBluetoothEnable(isEnable: Boolean) {
        _uiEvent.value = UiEvent.OnBluetoothEnable(isEnable)
    }

    fun onDeviceFound(foundDevice: BluetoothDevice) {
        _uiEvent.value = UiEvent.OnDeviceFound(foundDevice)
    }

    fun onSearchFailed() {
        _uiEvent.value = UiEvent.OnSearchFailed
    }

    sealed class UiEvent {
        data class OnPermissionGrant(val isGranted: Boolean) : UiEvent()
        data class OnBluetoothEnable(val isEnable: Boolean) : UiEvent()
        data class OnDeviceFound(val foundDevice: BluetoothDevice) : UiEvent()
        data object OnSearchFailed : UiEvent()
    }
}