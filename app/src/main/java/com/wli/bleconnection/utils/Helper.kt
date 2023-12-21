package com.wli.bleconnection.utils

import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.viewbinding.ViewBinding
import com.wli.bleconnection.R

inline fun <T : ViewBinding> AppCompatActivity.viewBinding(
    crossinline bindingInflater: (LayoutInflater) -> T
) =
    lazy(LazyThreadSafetyMode.NONE) {
        bindingInflater.invoke(layoutInflater)
    }

fun AppCompatButton.setDoneIcon() {
    setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_done, 0, 0, 0)
}

fun AppCompatButton.setCancelIcon() {
    setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_cancel, 0, 0, 0)
}