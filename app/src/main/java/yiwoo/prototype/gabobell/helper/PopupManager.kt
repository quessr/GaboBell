package yiwoo.prototype.gabobell.helper

import android.content.Context
import android.view.View
import yiwoo.prototype.gabobell.ui.popup.CustomPopup

object PopupManager {

    fun registorPopup(
        context: Context,
        title: String,
//        message: String,
        message: CharSequence,
        btnText: String,
        onOkClick: (() -> Unit)? = null,
        onCloseClick: (() -> Unit)? = null
    ) {
        CustomPopup(context)
            .setTitle(title)
            .setMessage(message)
            .setConfirmButtonText(btnText)
            .setOnOkClickListener(View.OnClickListener { onOkClick?.invoke() })
            .setOnCloseClickListener(View.OnClickListener { onCloseClick?.invoke() })
            .show()
    }
}