package com.example.farmforward.utils

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import com.example.farmforward.R

class LoadingDialog(context: Context) {

    private val dialog: Dialog = Dialog(context)

    init {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_loading, null)
        dialog.setContentView(view)

        dialog.setCancelable(false)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }

    fun show() {
        if (!dialog.isShowing) {
            dialog.show()
        }
    }

    fun dismiss() {
        if (dialog.isShowing) {
            dialog.dismiss()
        }
    }
}