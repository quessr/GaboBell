package yiwoo.prototype.gabobell.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.opengl.Visibility
import android.os.Bundle
import android.os.IBinder
import android.view.View
import yiwoo.prototype.gabobell.ble.BleManager
import yiwoo.prototype.gabobell.databinding.ActivityDeviceSettingsBinding
import yiwoo.prototype.gabobell.helper.Logger
import yiwoo.prototype.gabobell.ui.BaseActivity

class DeviceSettingsActivity :
    BaseActivity<ActivityDeviceSettingsBinding>(ActivityDeviceSettingsBinding::inflate) {

    private var bleManager: BleManager? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initUi()
        bindService()
    }

    private fun initUi() {
        binding.btnStatusCheck.setOnClickListener {
            bleManager?.cmdGetStatus()
        }
        binding.btnBellOn.setOnClickListener {
            bleManager?.cmdBellSetting(BleManager.BellCommand.ON)
        }
        binding.btnBellOff.setOnClickListener {
            bleManager?.cmdBellSetting(BleManager.BellCommand.OFF)
        }
        binding.btnLedOn.setOnClickListener {
            bleManager?.cmdLedSetting(true)
        }
        binding.btnLedOff.setOnClickListener {
            bleManager?.cmdLedSetting(false)
        }
    }

    private fun bindService() {
        val intent = Intent(this, BleManager::class.java)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }
    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            bleManager = (service as BleManager.LocalBinder).getService()
            bleManager?.let { service ->
                if (!service.initialize()) {
                    Logger.e("Unable to initialize Bluetooth")
                    finish()
                } else {
                    Logger.d("DeviceSettingsActivity onServiceConnected")
                    if (bleManager?.isConnected() == true) {
                        binding.btnDisconnectStatus.visibility = View.GONE
                    } else {
                        binding.btnConnectStatus.visibility = View.GONE
                    }
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            bleManager = null
            Logger.d("onServiceDisconnected")
        }
    }
}