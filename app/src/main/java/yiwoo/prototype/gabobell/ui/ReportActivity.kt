package yiwoo.prototype.gabobell.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.CountDownTimer
import android.os.IBinder
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import retrofit2.Retrofit
import yiwoo.prototype.gabobell.GaboApplication
import yiwoo.prototype.gabobell.api.GaboAPI
import yiwoo.prototype.gabobell.ble.BleManager
import yiwoo.prototype.gabobell.databinding.ActivityReportBinding
import yiwoo.prototype.gabobell.helper.ApiSender
import yiwoo.prototype.gabobell.helper.LocationHelper
import yiwoo.prototype.gabobell.helper.Logger
import yiwoo.prototype.gabobell.helper.UserSettingsManager
import yiwoo.prototype.gabobell.`interface`.EventIdCallback
import yiwoo.prototype.gabobell.module.RetrofitModule

class ReportActivity : BaseActivity<ActivityReportBinding>(ActivityReportBinding::inflate),
    EventIdCallback {

    private var bleManager: BleManager? = null
    private var gaboApi: GaboAPI? = null
    private var countDownTimer: CountDownTimer? = null
    private val timeLimit: Long = 6_000
    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val retrofit: Retrofit = RetrofitModule.provideRetrofit(this)
        gaboApi = retrofit.create(GaboAPI::class.java)
        LocationHelper.locationInit(this)

        initUi()
        initLauncher()
        bindService()
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
        BleManager.instance?.setEventIdCallback(null)
    }

    private fun initUi() {
        // 타이머 동작
        countDownTimer = object : CountDownTimer(timeLimit, 1_000) {
            override fun onTick(millisUntilFinished: Long) {
                // 카운터 표기
                binding.reportCounter.text = (millisUntilFinished / 1_000).toString()
            }

            override fun onFinish() {
                // 타이머 종료 (= 신고하기)
                reportEmergency()
            }
        }
        countDownTimer?.start()

        // '즉시신고' 버튼
        binding.btnReport.setOnClickListener {
            //응답에대한 결과처리 해줘야함 onReceive
            reportEmergency()
        }

        // 취소 버튼 (화면 종료)
        binding.btnCancellations.setOnClickListener {
            // cancelEmergency()
            finish()
        }
    }

    private fun initLauncher() {
        activityResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                // 미디어 등록 확인용 (임시주석)
                /*
                if (it.resultCode == RESULT_OK) {
                    // Toast.makeText(this, "미디어 파일이 등록되었습니다.", Toast.LENGTH_SHORT).show()
                } else if (it.resultCode == RESULT_CANCELED) {
                    val errorMessage = it.data?.getStringExtra("onFailure")
                    Toast.makeText(this, errorMessage ?: "미디어 파일 등록 실패", Toast.LENGTH_SHORT).show()
                }
                */

                // MainActivity 측으로 신고 완료 알림
                setResult(RESULT_OK)
                finish()
            }
    }

    private fun isConnected(): Boolean {
        return (application as GaboApplication).isConnected
    }

    // 신고 처리 (API 호출)
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

    // 신고 API 호출 (직접 호출)
    private fun sendEmergencyCreate() {
        LocationHelper.getCurrentLocation(this) { lat, lng ->
            val locationLat: Double = lat
            val locationLng: Double = lng
            Logger.d("Emergency_currentLocation: $locationLat | $locationLng")
            /**
             * getCurrentLocation 함수는 비동기로 처리되기 때문에,
             * 위치 값을 가져오기전 reportEmergency함수가 호출 될 경우 NullPointerException 발생
             * 바동기 콜백 내부에서 reportEmergency 함수 호출
             */
            ApiSender.createEvent(
                context = this@ReportActivity,
                serviceType = ApiSender.Event.EMERGENCY.serviceType,
                latitude = lat,
                longitude = lng
            ) { eventId ->
                Logger.d("Received event ID in SomeActivity: $eventId")
                (application as GaboApplication).isEmergency = true
                sendEmergencyVideo(eventId)
            }
        }
    }

    // 긴급 상황 동영상 전달
    private fun sendEmergencyVideo(eventId: Long) {
        // 원래는 설정 값에 의해서 '사진/동영상/미전송' 으로 구분되나,
        // 영업용은 동영상으로 fixed 한다.
        val intent = Intent(this, MediaCaptureActivity::class.java)
        intent.putExtra("eventId", eventId)
        intent.putExtra("mediaFormat",  UserSettingsManager.EmergencyFormatType.VIDEO.value)
        activityResultLauncher.launch(intent)

        /*
        val captureFormat = UserSettingsManager.getEmergencyFormat(this)
        if (captureFormat != UserSettingsManager.EmergencyFormatType.NONE) {
            val intent = Intent(this, MediaCaptureActivity::class.java)
            intent.putExtra("eventId", eventId)
            intent.putExtra("mediaFormat", captureFormat.value)
            activityResultLauncher.launch(intent)
        }
        */
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
                    // 기기와 연결되어 있는 경우
                    // 신고 시 API 호출을 BleManager가 담당하고 호출 결과(eventId)를 수신하기 위해서
                    // 아래와 같이 콜백은 등록한다.
                    service.setEventIdCallback(this@ReportActivity)
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Logger.d("onServiceDisconnected")
            bleManager = null
        }
    }

    // 신고 API 전송 완료 (via BleManager)
    override fun onEventId(eventId: Long) {
        // 긴급상황(동영상) 촬영
        sendEmergencyVideo(eventId)
    }
}