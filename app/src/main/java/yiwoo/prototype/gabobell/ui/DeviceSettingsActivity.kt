package yiwoo.prototype.gabobell.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.View
import android.widget.Toast
import yiwoo.prototype.gabobell.GaboApplication
import yiwoo.prototype.gabobell.ble.BleManager
import yiwoo.prototype.gabobell.databinding.ActivityDeviceSettingsBinding
import yiwoo.prototype.gabobell.helper.Logger
import yiwoo.prototype.gabobell.helper.UserDeviceManager

class DeviceSettingsActivity :
    BaseActivity<ActivityDeviceSettingsBinding>(ActivityDeviceSettingsBinding::inflate) {

    private var bleManager: BleManager? = null

    private val statusUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BleManager.BLE_STATUS_UPDATE -> handleStatusUpdate(intent)
                BleManager.BLE_LED_SETTING_CHANGED -> handleLedSettingChanged(intent)
                BleManager.BLE_BELL_SETTING_CHANGED -> handleBellSettingChanged(intent)
            }
        }
    }

    private fun handleStatusUpdate(intent: Intent) {
        val chargingStatus = intent.getStringExtra("status_charging") ?: "알 수 없는 상태"
        val bellStatus = intent.getStringExtra("status_bell") ?: "벨 상태 알 수 없음"
        val versionStatus = intent.getStringExtra("status_version") ?: "버전 알 수 없음"
        val ledStatus = intent.getStringExtra("status_led") ?: "LED 상태 알 수 없음"

        binding.tvBatteryStatusData.text = chargingStatus
        binding.tvBellData.text = bellStatus.substringAfter(" ")
        binding.tvFwVersionData.text = versionStatus
        binding.tvLedData.text = ledStatus.substringAfter(" ")
    }

    private fun handleLedSettingChanged(intent: Intent) {
        val isLedOn = intent.getStringExtra("status_led") ?: "LED 상태 알 수 없음"
        binding.tvLedData.text = isLedOn
    }

    private fun handleBellSettingChanged(intent: Intent) {
        val isBellOn = intent.getStringExtra("status_bell") ?: "BELL 상태 알 수 없음"
        binding.tvBellData.text = isBellOn
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bleManager = BleManager.instance
        initUi()
        updateUi()

        val filter = IntentFilter().apply {
            addAction(BleManager.BLE_STATUS_UPDATE)
            addAction(BleManager.BLE_LED_SETTING_CHANGED)
            addAction(BleManager.BLE_BELL_SETTING_CHANGED)
        }
        registerReceiver(statusUpdateReceiver, filter)
    }

    private fun initUi() {
        binding.tvDeviceName.text = UserDeviceManager.getDeviceName(this)

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
        binding.btnSettingsDisconnect.setOnClickListener {
            bleManager?.disconnect()
            UserDeviceManager.deleteDevice(this)
            Logger.d("연결 해제")
            Toast.makeText(
                this@DeviceSettingsActivity,
                "기기 연결이 해제되었습니다.",
                Toast.LENGTH_SHORT
            ).show()
            finish()
        }
    }

    private fun updateUi() {
        if ((application as GaboApplication).isConnected) {
            binding.btnDisconnectStatus.visibility = View.GONE
        } else {
            binding.btnConnectStatus.visibility = View.GONE
        }

        bleManager?.cmdGetStatus()
    }
}