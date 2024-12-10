package yiwoo.prototype.gabobell.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.widget.Toast
import yiwoo.prototype.gabobell.GaboApplication
import yiwoo.prototype.gabobell.R
import yiwoo.prototype.gabobell.ble.BleManager
import yiwoo.prototype.gabobell.databinding.ActivityDeviceSettingsBinding
import yiwoo.prototype.gabobell.helper.UserDeviceManager
import yiwoo.prototype.gabobell.helper.UserSettingsManager

class DeviceSettingsActivity :
    BaseActivity<ActivityDeviceSettingsBinding>(ActivityDeviceSettingsBinding::inflate) {

    private var bleManager: BleManager? = null

    private val statusUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BleManager.BLE_STATUS_UPDATE -> handleStatusUpdate(intent)

                // 안심벨 기기 설정은
                // 안심벨을 통해 신고를 했을 때의 동작을 설정하는 부분으로 판단된다.
                // 1. 무음 모드 : 벨에 직접 설정한다.
                // 2. 메시지 : (미구현)
                // 3. 5초 지연 : Pref 에 설정 (벨로 신고시 5초 뒤에 API 를 호출한다.)
                // 4. 손전등 : Pref 에 설정 (신고시 플래시 동작)

                // LED 상태가 변하는 것은 큰 의미가 없다. (설정값에 영향을 미치지 않는다.)
                // BleManager.BLE_LED_SETTING_CHANGED -> handleLedSettingChanged(intent)
                // 벨 사운드 상태가 변하는 것은 큰 의미가 없다. (설정값에 영향을 미치지 않는다.)
                // BleManager.BLE_BELL_SETTING_CHANGED -> handleBellSettingChanged(intent)
            }
        }
    }

    private fun handleStatusUpdate(intent: Intent) {
        val chargingStatus = intent.getStringExtra("status_charging") ?: "알 수 없는 상태"
        val bellStatus = intent.getStringExtra("status_bell") ?: "벨 상태 알 수 없음"
        val versionStatus = intent.getStringExtra("status_version") ?: "버전 알 수 없음"
        val ledStatus = intent.getStringExtra("status_led") ?: "LED 상태 알 수 없음"

        // 베터리
        val resource = when (chargingStatus) {
            "충전중" -> R.drawable.batterry_status_charging
            "완충" -> R.drawable.batterry_status_maximum
            "충전필요" -> R.drawable.batterry_status_minimum
            "기타" -> R.drawable.batterry_status_normal
            else -> R.drawable.batterry_status_normal
        }
        binding.ivBatteryStatus.setBackgroundResource(resource)

        // 펌웨어 버전
        binding.tvVersion.text = "버전 $versionStatus"

        // 벨 (무음모드)
        binding.toggleSound.isSelected = bellStatus == "벨 On"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bleManager = BleManager.instance
        initUi()
        updateUi()
        // 화면 진입 시, 안심벨 상태 정보 요청
        bleManager?.cmdGetStatus()

        val filter = IntentFilter().apply {
            addAction(BleManager.BLE_STATUS_UPDATE)
            addAction(BleManager.BLE_LED_SETTING_CHANGED)
            addAction(BleManager.BLE_BELL_SETTING_CHANGED)
        }
        registerReceiver(statusUpdateReceiver, filter)
    }

    private fun initUi() {

        binding.toggleSound.isSelected = false
        binding.toggleMessage.isSelected = UserSettingsManager.getEmergencyMessage(this@DeviceSettingsActivity)
        binding.toggleDelay.isSelected = UserSettingsManager.getEmergencyDelay(this@DeviceSettingsActivity)
        binding.toggleLed.isSelected = UserSettingsManager.getEmergencyFlash(this@DeviceSettingsActivity)

        binding.ivBatteryStatus.setBackgroundResource(R.drawable.batterry_status_disconnect)

        binding.toggleSound.setOnClickListener {
            it.isSelected = !it.isSelected
        }

        binding.toggleMessage.setOnClickListener {
            it.isSelected = !it.isSelected
        }

        binding.toggleDelay.setOnClickListener {
            it.isSelected = !it.isSelected
        }

        binding.toggleLed.setOnClickListener {
            it.isSelected = !it.isSelected
        }

        binding.btnSave.setOnClickListener {
            // 최종 설정 정보 저장
            // 무음모드
            if (binding.toggleSound.isSelected) {
                bleManager?.cmdBellSetting(BleManager.BellCommand.ON)
            } else {
                bleManager?.cmdBellSetting(BleManager.BellCommand.OFF)
            }
            // 메시지
            UserSettingsManager.setEmergencyMessage(
                this@DeviceSettingsActivity,
                binding.toggleMessage.isSelected
            )
            // 5초 지연
            UserSettingsManager.setEmergencyDelay(
                this@DeviceSettingsActivity,
                binding.toggleDelay.isSelected
            )
            // 손전등
            UserSettingsManager.setEmergencyFlash(
                this@DeviceSettingsActivity,
                binding.toggleLed.isSelected
            )
            finish()
        }

        binding.btnClose.setOnClickListener {
            finish()
        }

        binding.btnDisconnect.setOnClickListener {
            bleManager?.disconnect()
            UserDeviceManager.deleteDevice(this)
            Toast.makeText(
                this@DeviceSettingsActivity,
                "기기 연결이 해제되었습니다.",
                Toast.LENGTH_SHORT
            ).show()
            finish()
        }
    }

    private fun updateUi() {
        val isConnected = (application as GaboApplication).isConnected
        if (isConnected) {
            binding.ivBellConnection.setBackgroundResource(R.drawable.bell_connected)
        } else {
            binding.ivBellConnection.setBackgroundResource(R.drawable.bell_disconnected)
            binding.ivBatteryStatus.setBackgroundResource(R.drawable.batterry_status_disconnect)

        }
        binding.toggleSound.isEnabled = isConnected
        binding.toggleMessage.isEnabled = isConnected
        binding.toggleDelay.isEnabled = isConnected
        binding.toggleLed.isEnabled = isConnected
        binding.btnSave.isEnabled = isConnected
    }
}