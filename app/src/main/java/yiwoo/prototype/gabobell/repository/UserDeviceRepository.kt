package yiwoo.prototype.gabobell.repository

import android.content.Context
import yiwoo.prototype.gabobell.helper.UserDeviceManager

class UserDeviceRepository(private val context: Context) {
    fun bleRegister(): Boolean {
        return UserDeviceManager.isRegister(context)
    }
}