package yiwoo.prototype.gabobell.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.CountDownTimer
import android.os.IBinder
import android.provider.MediaStore
import androidx.activity.result.ActivityResultLauncher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import yiwoo.prototype.gabobell.api.GaboAPI
import yiwoo.prototype.gabobell.api.RetrofitModule
import yiwoo.prototype.gabobell.api.dto.CreateEvent
import yiwoo.prototype.gabobell.api.dto.CreateEventRequest
import yiwoo.prototype.gabobell.ble.BleManager
import yiwoo.prototype.gabobell.databinding.ActivityReportBinding
import yiwoo.prototype.gabobell.helper.Logger
import java.io.File

class ReportActivity : BaseActivity<ActivityReportBinding>(ActivityReportBinding::inflate) {

    private lateinit var gaboApi: GaboAPI

    private var bleManager: BleManager? = null
    private var countDownTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val retrofit: Retrofit = RetrofitModule.provideRetrofit()
        gaboApi = retrofit.create(GaboAPI::class.java)

        initUi()
        bindService()
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }

    private fun initUi() {

        // 타이머 동작
        countDownTimer = object : CountDownTimer(5_000, 1_000) {
            override fun onTick(millisUntilFinished: Long) {
                binding.reportCounter.text = (millisUntilFinished / 1_000).toString()
            }

            override fun onFinish() {
                reportEmergency()
            }
        }.start()

        binding.btnReport.setOnClickListener {
            // TODO startActivity 대신 startForResultActivity로 수정
            // TODO eventId와 mediaFormat정보를 넘겨야한다. -> intent로 eventId와 format 정보 넘긴다.
            // TODO 파일 전송 api 처리
            val intent = Intent(this, MediaCaptureActivity::class.java)
            startActivity(intent)

//            reportEmergency()
//            bleManager?.cmdEmergency(true)

            //응답에대한 결과처리 해줘야함 onReceive
            sendPostRequest()
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

    private fun sendPostRequest() {
        val requestBody = CreateEventRequest(
            CreateEvent(
                userUuid = "283697cc-8e00-40da-947f-058ece8af8aa",
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
                    val responseBody = response.body()
                    responseBody?.let {
                        val eventId = it.data.createEvent.id
                        val eventAddress = it.data.createEvent.eventAddress
                        val eventStatus = it.result.status
                        val eventMessage = it.result.message

                        Logger.d(
                            "eventStatus: $eventStatus \n eventMessage: $eventMessage \n " +
                                    "eventId: $eventId \n eventAddress: $eventAddress"
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
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            bleManager = null
            Logger.d("onServiceDisconnected")
        }
    }
}