package yiwoo.prototype.gabobell.helper

import android.util.Log

object Logger {

    private const val TAG = "BLE!@!@"

    fun d(msg: String) {
        Log.d(TAG, msg)
    }

    fun e(msg: String) {
        Log.e(TAG, msg)
    }
}