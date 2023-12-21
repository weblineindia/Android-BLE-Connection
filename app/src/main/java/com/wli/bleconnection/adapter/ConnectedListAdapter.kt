package com.wli.bleconnection.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.wli.bleconnection.R
import com.wli.bleconnection.data.model.ConnectedDeviceModel
import com.wli.bleconnection.databinding.ItemDeviceListBinding

class ConnectedListAdapter(private val listener: (ConnectedDeviceModel) -> Unit) : RecyclerView.Adapter<ConnectedListAdapter.ViewHolder>() {

    private val mList = ArrayList<ConnectedDeviceModel>()

    inner class ViewHolder(private val binding: ItemDeviceListBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(data: ConnectedDeviceModel) {
            binding.apply {
                val context = txtDeviceName.context
                txtDeviceName.text = data.deviceName
                txtDeviceMac.text = data.deviceMac

                if (data.isConnected) {
                    btnConnect.text = context.getString(R.string.device_disconnect)
                    if (data.rssi.isNotBlank()) {
                        txtRssi.apply {
                            isVisible = true
                            text = context.getString(R.string.device_network, data.rssi)
                        }
                    }
                    if (data.batteryLevel.isNotBlank()) {
                        txtDeviceBattery.apply {
                            isVisible = true
                            text = context.getString(R.string.battery_level, data.batteryLevel, "%")
                        }
                    }
                } else {
                    btnConnect.text = context.getString(R.string.device_connect)
                    txtDeviceBattery.apply {
                        isVisible = false
                        text = ""
                    }
                    txtRssi.apply {
                        isVisible = false
                        text = ""
                    }
                }

                btnConnect.setOnClickListener {
                    listener.invoke(data)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val obj = ItemDeviceListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(obj)
    }

    override fun getItemCount(): Int {
        return mList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(mList[position])
    }

    fun addAll(list: ArrayList<ConnectedDeviceModel>) {
        mList.apply {
            clear()
            addAll(list.distinct())
            notifyItemChanged(mList.size - 1)
        }
    }
}