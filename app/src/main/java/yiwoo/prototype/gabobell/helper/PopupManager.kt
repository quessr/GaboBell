package yiwoo.prototype.gabobell.helper

import android.content.Context
import android.view.View
import yiwoo.prototype.gabobell.ui.popup.CustomPopup

object PopupManager {

    fun registorPopup(
        context: Context,
        title: String,
//        message: String,
        message: String,
        btnText: String,
        onOkClick: (() -> Unit)? = null,
    ) {
        CustomPopup.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setOnCancelClickListener(btnText, View.OnClickListener { onOkClick?.invoke() })
            .build()
            .show()
    }
}