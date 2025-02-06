package yiwoo.prototype.gabobell.repository

import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import yiwoo.prototype.gabobell.GaboApplication
import yiwoo.prototype.gabobell.ble.BleManager
import yiwoo.prototype.gabobell.helper.ApiSender
import yiwoo.prototype.gabobell.helper.Logger

class BleServiceRepository(
    private val context: Context,
    private var application: Application
) {
    private var bluetoothAdapter: BluetoothAdapter? = null

    fun startMainService() {
        if (BleManager.instance == null) {
            val intent = Intent(context, BleManager::class.java)
            context.startService(intent)
            Logger.d("BleServiceRepository_startService")
        }
    }

    fun enableBleAdapter(): Boolean? {
        val bluetoothManager: BluetoothManager = context.getSystemService(BluetoothManager::class.java)
        bluetoothAdapter = bluetoothManager.adapter
        Logger.d("bluetoothAdapter_info : $bluetoothAdapter")
        Logger.d("bluetoothAdapter_boolean : ${bluetoothAdapter?.isEnabled}")

        return bluetoothAdapter?.isEnabled
    }


    //상태관리 repo 로 따로 관리 필요한지
    fun stateEmergency(): Boolean {
        return (application as GaboApplication).isEmergency
    }

    fun stateConnected(): Boolean {
        return (application as GaboApplication).isConnected
    }

    fun disConnectStateCancelEmergency(): Boolean {
        val eventId = (application as GaboApplication).eventId
        ApiSender.cancelEvent(context, eventId)

        return !(application as GaboApplication).isEmergency
    }
}