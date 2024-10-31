package yiwoo.prototype.gabobell.ui

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.os.Bundle
import android.os.CountDownTimer
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts

import retrofit2.Retrofit
import yiwoo.prototype.gabobell.GaboApplication
import yiwoo.prototype.gabobell.api.GaboAPI
import yiwoo.prototype.gabobell.ble.BleManager
import yiwoo.prototype.gabobell.constants.MediaFormatConstants
import yiwoo.prototype.gabobell.databinding.ActivityReportBinding
import yiwoo.prototype.gabobell.helper.ApiSender
import yiwoo.prototype.gabobell.helper.Logger
import yiwoo.prototype.gabobell.helper.UserSettingsManager
import yiwoo.prototype.gabobell.`interface`.EventIdCallback

import yiwoo.prototype.gabobell.module.RetrofitModule

class ReportActivity : BaseActivity<ActivityReportBinding>(ActivityReportBinding::inflate),
    EventIdCallback {

    private var bleManager: BleManager? = null
    private var gaboApi: GaboAPI? = null
    private var countDownTimer: CountDownTimer? = null
    private val timeLimit: Long = 5_000
    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val retrofit: Retrofit = RetrofitModule.provideRetrofit(this)
        gaboApi = retrofit.create(GaboAPI::class.java)
        initUi()
        initReceiver()
        bindService()

        activityResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                if (it.resultCode == RESULT_OK) {
                    Toast.makeText(this.applicationContext, "사진이 등록되었습니다.", Toast.LENGTH_SHORT)
                        .show()
                }
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
        unregisterReceiver(emergencyReceiver)
    }

    private fun initUi() {
        // 타이머 동작

        countDownTimer =
            object : CountDownTimer(timeLimit, 1_000) {
                override fun onTick(millisUntilFinished: Long) {
                    binding.reportCounter.text = (millisUntilFinished / 1_000).toString()
                }

                override fun onFinish() {
                    reportEmergency()
                }
            }

        if (!isEmergency()) {
            countDownTimer?.start()
        }

        binding.btnReport.setOnClickListener {
            //응답에대한 결과처리 해줘야함 onReceive
            reportEmergency()
        }

        binding.btnCancellations.setOnClickListener {
            cancelEmergency()
        }

        uiEmergency()
    }

    private fun uiEmergency() {
        if (isEmergency()) {
            binding.btnReport.isEnabled = false
            binding.btnCancellations.isEnabled = true
            binding.reportCounter.text = "신고중"
        } else {
            binding.btnReport.isEnabled = true
            binding.btnCancellations.isEnabled = false
            binding.reportCounter.text = (timeLimit / 1_000).toString()
        }
    }

    private fun initReceiver() {
        val filter = IntentFilter().apply {
            addAction(BleManager.BLE_REPORTE_EMERGENCY)
            addAction(BleManager.BLE_CANCEL_REPORTE_EMERGENCY)
        }
        registerReceiver(emergencyReceiver, filter)
    }


    private val emergencyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BleManager.BLE_REPORTE_EMERGENCY, BleManager.BLE_CANCEL_REPORTE_EMERGENCY -> {
                    uiEmergency()
                }
            }
        }
    }

    private fun isConnected(): Boolean {
        return (application as GaboApplication).isConnected
    }

    private fun isEmergency(): Boolean {
        return (application as GaboApplication).isEmergency
    }

    private fun reportEmergency() {
        countDownTimer?.cancel()
        if (isConnected()) {
            bleManager?.cmdEmergency(true)
        } else {
            // TODO: 해당 메소드를 서비스에서도 호출해야하므로 따로 뺄것.
            // 벨 연동 없이 직접 API 호출을 수행함.
            sendEmergencyCreate()
        }
    }

    private fun cancelEmergency() {
        if (isConnected()) {
            bleManager?.cmdEmergency(false)
        } else {
            sendEmergencyCancel()
        }
    }


    // region * API (단말 미연결시 호출)
    private fun sendEmergencyCancel() {
//        ApiSender.reportEmergency(this@ReportActivity)
        val eventId = (application as GaboApplication).eventId
        ApiSender.cancelEmergency(this@ReportActivity, eventId)
        (application as GaboApplication).isEmergency = false
        uiEmergency()
    }

    private fun sendEmergencyCreate() {

        ApiSender.reportEmergency(this) { eventId ->
            Logger.d("Received event ID in SomeActivity: $eventId")

            tryMediaCapture(eventId)
        }
    }

    private fun tryMediaCapture(eventId: Long) {
        val captureFormat = UserSettingsManager.getEmergencyFormat(this)
        if (captureFormat != UserSettingsManager.EmergencyFormatType.NONE) {
            val intent = Intent(this, MediaCaptureActivity::class.java)
            intent.putExtra("eventId", eventId)
            intent.putExtra("mediaFormat", captureFormat.value)
            activityResultLauncher.launch(intent)
        }

        (application as GaboApplication).isEmergency = true
        uiEmergency()
    }

    // endregion

    // region * Bind
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
                    service.setEventIdCallback(this@ReportActivity)
                    Logger.d("ReportActivity onServiceConnected")
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Logger.d("onServiceDisconnected")
            bleManager = null
        }
    }

    override fun onEventId(eventId: Long) {
        tryMediaCapture(eventId)
    }
    // endregion
}