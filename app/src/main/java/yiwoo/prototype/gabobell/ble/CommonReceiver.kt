package yiwoo.prototype.gabobell.ble

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import yiwoo.prototype.gabobell.GaboApplication
import yiwoo.prototype.gabobell.helper.Logger

open class CommonReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {

        if (intent?.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
            val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
            when (state) {
                BluetoothAdapter.STATE_ON -> {
                    BleManager.instance?.reconnect()
                    Logger.d("블루투스_활성화_상태: ON")
                }
                BluetoothAdapter.STATE_OFF -> {
                    (context?.applicationContext as GaboApplication).isConnected = false
                    Logger.d("블루투스_활성화_상태: OFF")
                }
            }
        }
    }
}
