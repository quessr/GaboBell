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
import androidx.activity.OnBackPressedCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import yiwoo.prototype.gabobell.api.GaboAPI
import yiwoo.prototype.gabobell.module.RetrofitModule
import yiwoo.prototype.gabobell.api.dto.CreateEvent
import yiwoo.prototype.gabobell.api.dto.CreateEventRequest
import yiwoo.prototype.gabobell.api.dto.EventStatus
import yiwoo.prototype.gabobell.api.dto.UpdateEventRequest
import yiwoo.prototype.gabobell.ble.BleManager
import yiwoo.prototype.gabobell.databinding.ActivityReportBinding
import yiwoo.prototype.gabobell.helper.Logger
import yiwoo.prototype.gabobell.helper.UserDeviceManager
import yiwoo.prototype.gabobell.helper.UserSettingsManager
import kotlin.concurrent.timer

class ReportActivity : BaseActivity<ActivityReportBinding>(ActivityReportBinding::inflate) {

    private lateinit var gaboApi: GaboAPI

    private var bleManager: BleManager? = null
    private var countDownTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Logger.d("onCreate===================================>")
        val retrofit: Retrofit = RetrofitModule.provideRetrofit(this)
        gaboApi = retrofit.create(GaboAPI::class.java)

        initUi()
        bindService()

        val filter = IntentFilter().apply {
            addAction(BleManager.BLE_REPORTE_EMERGENCY)
            addAction(BleManager.BLE_CANCEL_REPORTE_EMERGENCY)
        }
        registerReceiver(reportEmergencyReceive, filter)


        //신고화면에서 취소 버튼 이전에 뒤로가기시 리시버 스택 쌓임 방지
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Logger.d("handleOnBackPressed===================================>")
//                unregisterReceiver(reportEmergencyReceive)
//                finish()
            }

        })
    }

    private val reportEmergencyReceive = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BleManager.BLE_REPORTE_EMERGENCY -> {
                    val result = intent.getStringExtra("cmd")
                    if (result != null) {
                        Logger.d("result: $result")
                        Logger.d("신고요청=======================>")
//                        sendEventCreate()

                        // Todo : 신고중인 상태를 전역으로 관리 필요
                    }
                }
                BleManager.BLE_CANCEL_REPORTE_EMERGENCY -> {
                    val result = intent.getStringExtra("cmd")
                    if (result != null){
                        Logger.d("result: $result")
                        Logger.d("신고취소=======================>")
//                        sendEventUpdate()
                        //취소 요청후 리시버 해제
//                        context?.unregisterReceiver(this)
                        finish()
                    }
                }
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        Logger.d("onDestroy===================================>")
        countDownTimer?.cancel()
        unregisterReceiver(reportEmergencyReceive)
    }

    private fun initUi() {
//        // 타이머 동작
//        countDownTimer = object : CountDownTimer(5_000, 1_000) {
//            override fun onTick(millisUntilFinished: Long) {
//                binding.reportCounter.text = (millisUntilFinished / 1_000).toString()
//            }
//
//            override fun onFinish() {
//                if (bleManager?.isConnected() == false) {
//                    Logger.d("sendEventCreate")
//                    sendEventCreate()
//                }
//                reportEmergency()
//            }
//        }.start()

        binding.btnReport.setOnClickListener {
            reportEmergency()
            bleManager?.cmdEmergency(true)
        }
        binding.btnCancellations.setOnClickListener {
            bleManager?.cmdEmergency(false)
//            finish()
        }
    }

    private fun reportEmergency() {
        // TODO: API 호출 시점은?
        //       B2 수신부에서 호출해야할까? (벨에서 바로 신고가 올라오는 경우가 있으니?)
        //       그렇다면 여기에서는 벨 연동 가능 상태에 따른 분기가 필요하겠다.
        bleManager?.cmdEmergency(true)
        countDownTimer?.cancel()
    }

    private fun sendEventUpdate() {
        val requestBody = UpdateEventRequest(
            EventStatus(
                serviceState = "종료",
                closureType = "자동 종료"
            )
        )
        Logger.d("Request Body: $requestBody") // 요청 데이터 로그 출력
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = gaboApi.updateEvent(10, requestBody)
                Logger.d("Response Body: ${response.body()}")
                if (response.isSuccessful) {
                    response.body()?.let {
                        val eventStatus = it.result.status
                        val eventMessage = it.result.message
                        val eventId = 10
                        val serviceState = it.data?.eventStatus?.serviceState
                        val closureType = it.data?.eventStatus?.closureType
                        val monitorUuid = it.data?.eventStatus?.monitorUuid
                        Logger.d(
                            "eventStatus: $eventStatus \n eventMessage: $eventMessage \n " +
                                    "id: $eventId \n serviceState: $serviceState \n closureType: $closureType \n " +
                                    "monitorUuid: $monitorUuid"
                        )
                    }
                } else {
                    Logger.e(
                        "Request failed with code: ${response.code()} \n " +
                                "errorBody: ${
                                    response.errorBody()?.string()
                                }"
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun sendEventCreate() {
        val requestBody = CreateEventRequest(
            CreateEvent(
                userUuid = UserSettingsManager.getUuid(this),
                latitude = 37.585057,
                longitude = 126.885347
            )
        )
        Logger.d("Request Body: $requestBody") // 요청 데이터 로그 출력
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = gaboApi.createEvent(requestBody)
                Logger.d("responseBody: ${response.body()}")
                if (response.isSuccessful) {
                    response.body()?.let {
                        val eventId = it.data.createEvent.id
                        val eventAddress = it.data.createEvent.eventAddress
                        val eventStatus = it.result.status
                        val eventMessage = it.result.message
                        val latitude = it.data.createEvent.latitude
                        val longitude = it.data.createEvent.longitude

                        Logger.d(
                            "eventStatus: $eventStatus \n eventMessage: $eventMessage \n " +
                                    "eventId: $eventId \n eventAddress: $eventAddress \n " +
                                    "latitude: $latitude \n evelongitudentAddress: $longitude"
                        )
                    }
                } else {
                    Logger.e(
                        "Request failed with code: ${response.code()} \n " +
                                "errorBody: ${
                                    response.errorBody()?.string()
                                }"
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
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
                    Logger.d("ReportActivity onServiceConnected")

                    //서비스가 바인딩 되어도 ble 연결 안되어있을 경우 처리
                    Logger.d("Ble_disconnect: ${bleManager?.isConnected()}")
                    if (bleManager?.isConnected() == false) {

                        binding.btnReport.setOnClickListener {
                            sendEventCreate()
//                            countDownTimer?.cancel()
                        }
                        binding.btnCancellations.setOnClickListener {
                            sendEventUpdate()
                            finish()
                        }
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