package yiwoo.prototype.gabobell.ble

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
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

        /**
         * - 안드로이드 12 에서는 시스템 브로드캐스트에 반응하여 서비스를 시작하는 경우
         *   예외적으로 startService를 허용하여 오류가 발생하지 않음
         *   (ACTION_BOOT_COMPLETED은 허용된 브로드캐스트 중 하나)
         * - 안드로이드 8.0 (Oreo) 이상 startForegroundService 사용
         */
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            Logger.d("ACTION_BOOT_COMPLETED")
            val serviceIntent = Intent(context, BleManager::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context?.startForegroundService(serviceIntent)
            } else {
                context?.startService(serviceIntent)
            }
        }
    }
}
