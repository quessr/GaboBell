package yiwoo.prototype.gabobell.helper

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class SettingsReceiver: BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {

        /*
        val action = intent?.action ?: return
        if(!TextUtils.isEmpty(action)) {
            val value = intent.getIntExtra("value", 0)

            Log.d("@!@", "User config changed : $action, $value")

            when (action) {
                "set_user" -> {
                    UserSettingsManager.setUser(context!!, value)
                }
                "set_emergency_file_format" -> {
                    UserSettingsManager.setEmergencyFormat(context!!, value)
                }
                else -> {}
            }
        }
        */
    }
}