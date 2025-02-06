package yiwoo.prototype.gabobell.ui

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.IntentSender
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.common.api.ResolvableApiException
import com.kakao.vectormap.GestureType
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.label.Label
import com.kakao.vectormap.label.LabelOptions
import yiwoo.prototype.gabobell.R
import yiwoo.prototype.gabobell.ble.BleManager
import yiwoo.prototype.gabobell.databinding.ActivityMainBinding
import yiwoo.prototype.gabobell.helper.FlashUtil
import yiwoo.prototype.gabobell.helper.LocationHelper
import yiwoo.prototype.gabobell.helper.Logger
import yiwoo.prototype.gabobell.repository.BleServiceRepository
import yiwoo.prototype.gabobell.repository.LocationRepository
import yiwoo.prototype.gabobell.repository.PermissionRepository
import yiwoo.prototype.gabobell.repository.UserDeviceRepository
import yiwoo.prototype.gabobell.ui.popup.CustomPopup
import yiwoo.prototype.gabobell.ui.viewmodel.MainActivityViewModel
import yiwoo.prototype.gabobell.ui.viewmodel.MainActivityViewModelFactory

class MainActivity : BaseActivity<ActivityMainBinding>(ActivityMainBinding::inflate) {
    // SensorEventListener {

    //by ViewModels()를 사용하면 ViewModelProvider를 사용하지 않고 ViewModel을 지연 생성(viewModel 라이브러리 추가)
    //but : viewModel 클래스가 매개변수를 필요로 하는 경우 Factory 명시적으로 제공 필요
//    private val mainActivityViewModel: MainActivityViewModel by viewModels()
    private val permissionRepository: PermissionRepository by lazy { PermissionRepository(this) }
    private val userDeviceRepository: UserDeviceRepository by lazy { UserDeviceRepository(applicationContext) }
    private val locationRepository: LocationRepository by lazy { LocationRepository(applicationContext) }
    private val bleServiceRepository: BleServiceRepository by lazy { BleServiceRepository(applicationContext, application) }
    private val mainActivityViewModel: MainActivityViewModel by viewModels {
        MainActivityViewModelFactory(
            permissionRepository,
            userDeviceRepository,
            locationRepository,
            bleServiceRepository
        )
    }

    private var currentLocationLabel: Label? = null
    // private var isVisibleFab: Boolean = false
    // private var isActivePolice: Boolean = false
    private lateinit var emergencyLauncher: ActivityResultLauncher<Intent>

    // private var bounds: LatLngBounds? = null
    // private var isPoliceActive: Boolean = false // 토글 상태를 저장
    // private val policeMarkers = mutableListOf<Label>() // 기존 라벨 관리 리스트
    // private var policeLabel: Label? = null
    private var map: KakaoMap? = null
    private var currentPosition: LatLng? = null
    private var isFirstLocationUpdate = true    // 카메라 처음 위치 업데이트 여부를 확인

    private var mediaPlayer: MediaPlayer? = null
    private lateinit var audioManager: AudioManager
    private lateinit var flashUtil: FlashUtil

    /*
    private lateinit var sensorManager: SensorManager
    private var acceleration = 0f
    private var currentAcceleration = 0f
    private var lastAcceleration = 0f
    private var shakeCount = 0
    private var lastShakeTime: Long = 0
    private val shakeThreshold = 30.0f  // 흔들기 감지 임계값
    */


    // 권한 요청 필요한지?
    private var isNecessaryToRequestPermission = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        flashUtil = FlashUtil.getInstance(this@MainActivity)

        initObserve()
        initUi()
        initLauncher()
        initMap()

        // initShakeDetection()
        initEmergencyStateReceiver()
        handleBackPressed()
        debugMode()
    }

    override fun onResume() {
        super.onResume()
        initEmergencyStateReceiver()
        binding.mapView.resume()
        /*
        shakeCount = 0
        sensorManager.registerListener(
            this@MainActivity,
            sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
            SensorManager.SENSOR_DELAY_NORMAL
        )
        */

        updateUi()
        mainActivityViewModel.checkLocationSettings()
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(emergencyStateReceiver)
        binding.mapView.pause()
        // sensorManager.unregisterListener(this@MainActivity)
    }

    override fun onDestroy() {
        super.onDestroy()
        LocationHelper.stopLocation()
    }

    private fun debugMode() {
        mainActivityViewModel.debugMode()
    }

    private fun initObserve() {

        /**
         * 퍼미션_observe
         */
        mainActivityViewModel.permissionState.observe(this) { isGranted ->
            if (isGranted) {
                isNecessaryToRequestPermission = false
                startMainService()
            } else {
                Handler(Looper.getMainLooper()).postDelayed({
                    // 맵이 보여지기 전에 mapView.pause() 가 호출되면 맵이 출력되지 않아서
                    // 권한 요청을 늦췄다.
                    requestMultiplePermissions.launch(mainActivityViewModel.requestCheckPermissions())
                }, 1_000)
            }
        }
        mainActivityViewModel.checkPermissions()

        /**
         * 퍼미션 결과_observe
         */
        mainActivityViewModel.resultPermission.observe(this) { allGranted ->
            if (allGranted) {
                startMainService()
                if (map != null) {
                    mainActivityViewModel.startLocation()
                }
            } else {
                Logger.d("일부 권한이 거부됨======")
                CustomPopup.Builder(this)
                    .setTitle(getString(R.string.pop_emergency_completed_title))
                    .setMessage(getString(R.string.pop_failed_permission_message))
                    .setOnOkClickListener(getString(R.string.pop_btn_confirm)) {
                        finish()
                    }
                    .build()
                    .show()
            }
        }

        /**
         * debugMode_observe
         */
        mainActivityViewModel.isDebugMode.observe(this) { isDebugMode ->
            if (isDebugMode) {
                Toast.makeText(this@MainActivity, "개발자 모드", Toast.LENGTH_LONG).show()
            }
        }

        /**
         * 현재위치_observe
         */
        mainActivityViewModel.currentLocation.observe(this) { location ->
            updateCurrentLocationMarker(map!!, location.latitude, location.longitude)
            if (isFirstLocationUpdate) {
                map?.moveCamera(CameraUpdateFactory.newCenterPosition(LatLng.from(location.latitude, location.longitude)))
                isFirstLocationUpdate = false // 첫 위치 업데이트 이후로는 카메라 이동하지 않음
            }
        }

        /**
         * 위치정보(정확도)설정_observe
         */
        mainActivityViewModel.isLocationAccuracy.observe(this) { (_, exception) ->
            if (exception is ResolvableApiException) {
                try {
                    // ResolvableApiException에서 제공되는 IntentSender를 올바르게 처리하기 위해사용
                    val intentSenderRequest =
                        IntentSenderRequest.Builder(exception.resolution).build()
                    locationSettingsLauncher.launch(intentSenderRequest)
                } catch (sendEx: IntentSender.SendIntentException) {
                    sendEx.printStackTrace()
                }
            }
        }

        /**
         * ble 등록 여부_observe
         */
        mainActivityViewModel.isRegister.observe(this) { isRegister ->
            if (isRegister) {
                val intent = Intent(this, DeviceSettingsActivity::class.java)
                startActivity(intent)
            } else {
                mainActivityViewModel.enableBleAdapter()
            }
        }

        /**
         * Bluetooth 활성화 여부_observe
         */
        mainActivityViewModel.isEnableBleAdapter.observe(this) { isEnable ->
            if (isEnable == false) {
                // 활성화가 안 되어 있을 경우
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                requestBluetooth.launch(enableBtIntent)
            } else {
                val intent = Intent(this, RegisterDeviceActivity::class.java)
                startActivity(intent)
            }
        }

        /**
         * 위급 상황 신고 여부_observe
         */
        mainActivityViewModel.isEmergency.observe(this) { isEmergency ->
            if (isEmergency) {  //신고중인 상태(=true)
                CustomPopup.Builder(this)
                    .setTitle(getString(R.string.pop_emergency_completed_title))
                    .setMessage(getString(R.string.pop_emergency_cancel_description))
//                    .setConfirmButtonText(getString(R.string.pop_btn_yes))
                    .setOnOkClickListener(getString(R.string.pop_btn_yes)) {
                        mainActivityViewModel.stateConnected()
                    }
                    .setOnCancelClickListener(getString(R.string.pop_btn_no)) {
                        // no code
                    }
                    .build()
                    .show()
            } else {
                // 신고화면 이동
                val intent = Intent(this, ReportActivity::class.java)
                emergencyLauncher.launch(intent)
                emergencyEffect(true)
            }
        }

        /**
         * 기기 연결 상태_observe
         */
        mainActivityViewModel.isConnected.observe(this) { isConnected ->
            if (isConnected) {
                // 기기 연결 상태에서 신고 취소
                mainActivityViewModel.connectStateCancelEmergency()
            } else {
                // 기기 미연결 상태에서 신고 취소
                mainActivityViewModel.disConnectStateCancelEmergency()
                updateUi()
                emergencyEffect(false)
            }
        }

        /**
         * 신고중 UI 상태_observe
         */
        mainActivityViewModel.isUpdateUi.observe(this) { isEmergencyUi ->
            if (isEmergencyUi) {
                // binding.btnEmergencyReport.text = "신고취소"
                binding.btnEmergencyReport.setBackgroundResource(R.drawable.btn_emergency_cancel_selector)
            } else {
                binding.btnEmergencyReport.setBackgroundResource(R.drawable.btn_emergency_selector)
            }
        }
    }

    private fun initUi() {

        // 귀가 모니터링
        /*
        binding.btnMonitoring.setOnClickListener {
            val intent = Intent(this, MonitoringActivity::class.java)
            startActivity(intent)
        }
        */

        // 신고/신고취소하기
        binding.btnEmergencyReport.setOnClickListener {
            mainActivityViewModel.stateEmergency()
        }

        // 설정
        binding.btnSetting.setOnClickListener {
            mainActivityViewModel.bleRegister()
        }

        /*
        // 안심시설 (지구대)
        visibleFab(isVisibleFab)
        togglePolice(isActivePolice)

        binding.btnFacilities.setOnClickListener {
            isVisibleFab = !isVisibleFab
            visibleFab(isVisibleFab)
        }

        binding.fabPolice.setOnClickListener {
            isActivePolice = !isActivePolice
            togglePolice(isActivePolice)
        }
        */

        //GPS, ZoomIn, ZoomOut
        binding.zoomIn.setOnClickListener {
            map?.moveCamera(CameraUpdateFactory.zoomIn())
        }
        binding.zoomOut.setOnClickListener {
            map?.moveCamera(CameraUpdateFactory.zoomOut())
        }
        binding.moveCurrentLocation.setOnClickListener {
            Logger.d("moveCurrentLocation : $currentPosition")
            map?.moveCamera(CameraUpdateFactory.newCenterPosition(currentPosition))
        }
    }

    /*
    private fun initShakeDetection() {
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        acceleration = 10f
        currentAcceleration = SensorManager.GRAVITY_EARTH
        lastAcceleration = SensorManager.GRAVITY_EARTH
    }
    */

    private fun initEmergencyStateReceiver() {
        val filter = IntentFilter().apply {
            addAction(BleManager.BLE_REPORTE_EMERGENCY)
            addAction(BleManager.BLE_CANCEL_REPORTE_EMERGENCY)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(emergencyStateReceiver, filter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(emergencyStateReceiver, filter)
        }
    }

    // 긴급 상태 수신 (신고, 신고취소)
    private val emergencyStateReceiver = object : BroadcastReceiver() {
        // 화면이 foreground 상태에서 신고/신고 취소 이벤트를 수신한다.
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BleManager.BLE_REPORTE_EMERGENCY -> {
                    updateUi()
                }
                BleManager.BLE_CANCEL_REPORTE_EMERGENCY -> {
                    updateUi()
                    emergencyEffect(false)
                }
            }
        }
    }

    private fun handleBackPressed() {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                CustomPopup.Builder(this@MainActivity)
                    .setTitle(getString(R.string.pop_emergency_completed_title))
                    .setMessage("앱을 종료하시겠습니까?")
                    .setOnOkClickListener(getString(R.string.pop_btn_yes)) {
                        finish()
                    }
                    .setOnCancelClickListener(getString(R.string.pop_btn_no)) {
                        // no code
                    }
                    .build()
                    .show()
            }
        }
        this.onBackPressedDispatcher.addCallback(
            this,
            callback
        )
    }

    private fun initMap() {
        LocationHelper.locationInit(this)

        val mapView = binding.mapView
        mapView.start(object : MapLifeCycleCallback() {
            override fun onMapDestroy() {
                // 지도 API가 정상적으로 종료될 때 호출됨
            }

            override fun onMapError(error: Exception) {
                // 인증 실패 및 지도 사용 중 에러가 발생할 때 호출됨
            }
        }, object : KakaoMapReadyCallback() {
            override fun getPosition(): LatLng {
                Logger.d("===> getPosition")
                // TODO: 초기 맵 위치
                return LatLng.from(37.566535, 126.9779692)
            }

            override fun onMapReady(kakaoMap: KakaoMap) {
                Logger.d("===> onMapReady")

                if (!isNecessaryToRequestPermission) {
                    mainActivityViewModel.startLocation()
                }

                /**
                 * getPosition 함수에 의해서 초기 좌표로 카메라가 설정되는 과정에서 카메라가 이동 되면서,
                 *  setOnCameraMoveEndListener 가 호출이 됨
                 * 카메라 이동시 센터 포지션 값에 대한 ne, sw 좌표 변경
                 */
                map = kakaoMap
                /*
                map!!.setOnCameraMoveEndListener { map, cameraPosition, _ ->
                    updateBounds(map, mapView, cameraPosition)

                    //zoomLevel 값에 따른 마커 처리
                    val zoomLevel = map.zoomLevel
                    Logger.d("getZoomLevel : $zoomLevel")
                    if (isPoliceActive) {
                        if (zoomLevel >= MapConstants.ZOOMLEVEL) {
                            callPoliceApi(map)
                        } else {
                            //zoomLevel 이 13 미만(12 이하)일 경우 마커 clear
                            policeMarkers.forEach {
                                it.remove()
                            }
                            policeMarkers.clear()
                        }
                    }
                }
                */

                //회전 동작 고정(제스쳐)
                map?.setGestureEnable(GestureType.Rotate, false)
            }
        })
    }

    /*
    private fun callPoliceApi(map: KakaoMap) {
        bounds?.let { currentBounds ->
            Logger.d("Police API 호출: ${currentBounds.southwest} ~ ${currentBounds.northeast}")
            PoliceClient.getBoundsPolice(
                this,
                currentBounds.southwest.latitude, currentBounds.southwest.longitude,
                currentBounds.northeast.latitude, currentBounds.northeast.longitude
            ) { policeDataList ->
                runOnUiThread {
                    if (policeDataList != null) {
                        addPoliceMaker(map, policeDataList)
                    } else {
                        Logger.e("지구대 리스트 조회 실패")
                    }
                }
            }
        }
    }

    private fun addPoliceMaker(map: KakaoMap, policeDataList: List<PoliceResultItem>) {
        Logger.d("policeMarkers : ${policeMarkers.size}")
        //기존 마커 제거
        policeMarkers.forEach { it.remove() }
        policeMarkers.clear()
        Logger.d("policeMarkers_clear : ${policeMarkers.size}")

        if (!isActivePolice) {
            return
        }

        //라벨 추가
        val labelLayer = map.labelManager?.layer
        for (police in policeDataList) {
            val position = LatLng.from(police.latitude, police.longitude)
            policeLabel = labelLayer?.addLabel(
                LabelOptions.from(position).setStyles(
                    MonitoringActivity.setPinStyle(
                        this,
                        R.drawable.police_marker
                    )
                )
            )
            policeLabel?.let { policeMarkers.add(it) }
        }
    }



    private fun updateBounds(kakaoMap: KakaoMap ,mapView: MapView, cameraPosition: CameraPosition) {
        //카메라의 현재 위치 정보
        val centerLat = cameraPosition.position.latitude
        val centerLng = cameraPosition.position.longitude
        Logger.d("centerPosition: $centerLat | $centerLng")

        //카카오맵 화면 크기
        val viewWidth = mapView.width
        val viewHeight = mapView.height

        val ne = screenToLatLng(kakaoMap, viewWidth, 0) // 우측 상단 (북동)
        val sw = screenToLatLng(kakaoMap, 0, viewHeight) // 좌측 하단 (남서)

        // 북동쪽과 남서쪽 좌표 출력
        Logger.d("북동쪽 위도: ${ne.latitude} | 북동쪽 경도: ${ne.longitude}")
        Logger.d("남서쪽 위도: ${sw.latitude} | 남서쪽 경도: ${sw.longitude}")

        //LatLngBounds 객체 생성
        val northeast = LatLng.from(ne.latitude, ne.longitude)
        val southwest = LatLng.from(sw.latitude, sw.longitude)
        bounds = LatLngBounds(northeast, southwest)
    }
    private fun screenToLatLng(map: KakaoMap, x: Int, y: Int): LatLng {
        return map.fromScreenPoint(x.toDouble().toInt(), y.toDouble().toInt())!!
    }
    */

    private fun initLauncher() {
        emergencyLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                // 신고 화면 -> 신고
                updateUi()
                // 신고 완료 팝업
                CustomPopup.Builder(this)
                    .setTitle(getString(R.string.pop_emergency_completed_title))
                    .setMessage(getString(R.string.pop_emergency_completed_description))
                    .build()
                    .show()

            } else if (it.resultCode == RESULT_CANCELED) {
                // 신고 화면 -> 취소
                emergencyEffect(false)
            }
        }
    }

    private fun updateUi() {
        mainActivityViewModel.stateEmergencyUi()
    }

    /*
    private fun visibleFab(visible: Boolean) {
         binding.fabPolice.visibility = if (visible) View.VISIBLE else View.GONE
    }
    */

    /*
    private fun togglePolice(isActive: Boolean) {
        isPoliceActive = isActive
        if (isActive) {
            // binding.fabPolice.setBackgroundResource(R.color.pink)
            binding.fabPolice.isSelected = true
            // 지구대 마커 표기
            Logger.d("toggleOnOffLabel_level : ${map?.zoomLevel}")
            if (map?.zoomLevel!! >= MapConstants.ZOOMLEVEL) {
                callPoliceApi(map!!)
            } else {
                //zoomLevel 이 13 미만(12 이하)일 경우 마커 clear
                policeMarkers.forEach {
                    it.remove()
                }
                policeMarkers.clear()
            }
        } else {
//            binding.fabPolice.setBackgroundResource(R.color.black_op_66)
            binding.fabPolice.isSelected = false
            // 지구대 마커 해제
            policeMarkers.forEach { it.remove() }
            policeMarkers.clear()
        }
    }
    */

    private fun updateCurrentLocationMarker(
        map: KakaoMap, latitude: Double, longitude: Double) {
//        val position = LatLng.from(latitude, longitude)
        currentPosition = LatLng.from(latitude, longitude)
        val labelLayer = map.labelManager?.layer
        if (currentLocationLabel != null) {
            // 현재 위치 마커 이동
            currentLocationLabel?.moveTo(currentPosition)
        } else {
            // 현재 위치 마커 생성
            currentLocationLabel = labelLayer?.addLabel(
                LabelOptions.from(currentPosition)
                    .setStyles(
                        // TODO: 추후 공통 처리
                        MonitoringActivity.setPinStyle(
                            this@MainActivity,
                            R.drawable.current_marker
                        )
                    )
            )
        }
    }

    // 신고 사운드/플래시
    private fun emergencyEffect(isPlay: Boolean) {
        if (isPlay) {
            flashUtil.startEmergencySignal(lifecycleScope)
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, AudioManager.FLAG_PLAY_SOUND)
            mediaPlayer = MediaPlayer.create(this, R.raw.siren).apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        // 갤럭시 저사양 (버전 7, 8)에서 사이렌 발생하지 않음.
                        // USAGE_MEDIA 으로 속성 변경하고 추이 확인 필요
                        // .setUsage(AudioAttributes.USAGE_ALARM)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setVolume(1.0f, 1.0f)
                isLooping = true
                start()
            }
        } else {
            flashUtil.stopEmergencySignal()
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                release()
            }
            mediaPlayer = null
        }
    }


    private val requestMultiplePermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        mainActivityViewModel.handlePermissionResult(permissions)
    }

    private val locationSettingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            Logger.d("위치 정보(정확도) 설정이 완료되었습니다.")
            isNecessaryToRequestPermission = false
        } else {
            Logger.d("위치 정보(정확도) 설정이 취소되었습니다.")
            finish()
        }
    }


    //블루투스 활성화 요청 콜백
    private var requestBluetooth = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            //granted
            Logger.d("Bluetooth 활성화 완료")
            val intent = Intent(this, RegisterDeviceActivity::class.java)
            startActivity(intent)
        } else {
            //deny
            Logger.d("Bluetooth 활성화 거부")
            finish()
        }
    }


    private fun startMainService() {
        mainActivityViewModel.startMainService()
    }

    // region * 흔들기 감지 처리
    /*
    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val currentTime = System.currentTimeMillis()
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            lastAcceleration = currentAcceleration
            currentAcceleration = kotlin.math.sqrt((x * x + y * y + z * z).toDouble()).toFloat()
            val delta: Float = currentAcceleration - lastAcceleration
            acceleration = acceleration * 0.9f + delta

            if (abs(acceleration) > shakeThreshold) {
                if (shakeCount == 0 || (currentTime - lastShakeTime) > 2_000) {
                    shakeCount = 1
                } else {
                    shakeCount++
                }

                Log.d("@!@", "shakeCount : $shakeCount")

                lastShakeTime = currentTime
                if (shakeCount >= 5) {
                    handleEmergencyShake()
                    shakeCount = 0
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // no code (정확도 변경 시 필요한 처리)
    }

    // 흔들기 감지 완료
    private fun handleEmergencyShake() {
        Log.d("@!@", "handleEmergencyShake : ${isEmergency()}")
        if (isEmergency()) {
            shakeCount = 0
            return
        }
        val intent = Intent(this, ReportActivity::class.java)
        emergencyLauncher.launch(intent)
    }
    */
    // endregion
}