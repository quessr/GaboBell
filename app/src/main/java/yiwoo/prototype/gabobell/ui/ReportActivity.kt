package yiwoo.prototype.gabobell.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.CountDownTimer
import android.os.IBinder
import yiwoo.prototype.gabobell.ble.BleManager
import yiwoo.prototype.gabobell.databinding.ActivityReportBinding
import yiwoo.prototype.gabobell.helper.Logger

class ReportActivity : BaseActivity<ActivityReportBinding>(ActivityReportBinding::inflate) {

    private var bleManager: BleManager? = null
    private var countDownTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initUi()
        bindService()
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }

    private fun initUi() {

        // 타이머 동작
        countDownTimer = object: CountDownTimer(5_000, 1_000) {
            override fun onTick(millisUntilFinished: Long) {
                binding.reportCounter.text = (millisUntilFinished / 1_000).toString()
            }

            override fun onFinish() {
                reportEmergency()
            }
        }.start()

        binding.btnReport.setOnClickListener {
            reportEmergency()
        }

        binding.btnCancellations.setOnClickListener {
            bleManager?.cmdEmergency(false)
            finish()
        }
    }

    private fun reportEmergency() {
        // TODO: API 호출 시점은?
        //       B2 수신부에서 호출해야할까? (벨에서 바로 신고가 올라오는 경우가 있으니?)
        //       그렇다면 여기에서는 벨 연동 가능 상태에 따른 분기가 필요하겠다.

        bleManager?.cmdEmergency(true)
        countDownTimer?.cancel()
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
                    Logger.d("ReportActivity onServiceConnected")
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            bleManager = null
            Logger.d("onServiceDisconnected")
        }
    }
}