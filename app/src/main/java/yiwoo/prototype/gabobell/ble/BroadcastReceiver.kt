package yiwoo.prototype.gabobell.ble

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

open class BroadcastReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {

        when (intent?.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                val bootIntent = Intent(context, BleManager::class.java)
                context?.startService(bootIntent)
            }
        }
    }
}