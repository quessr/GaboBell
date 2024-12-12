package yiwoo.prototype.gabobell.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.kakao.vectormap.GestureType
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.LatLngBounds
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.MapView
import com.kakao.vectormap.camera.CameraPosition
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.label.Label
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelStyles
import com.kakao.vectormap.label.LabelTextStyle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import yiwoo.prototype.gabobell.GaboApplication
import yiwoo.prototype.gabobell.R
import yiwoo.prototype.gabobell.api.dto.response.PoliceResultItem
import yiwoo.prototype.gabobell.constants.MapConstants
import yiwoo.prototype.gabobell.data.network.GpsTracksClient
import yiwoo.prototype.gabobell.data.network.PoliceClient
import yiwoo.prototype.gabobell.databinding.ActivityMonitoringBinding
import yiwoo.prototype.gabobell.helper.ApiSender
import yiwoo.prototype.gabobell.helper.LocationHelper
import yiwoo.prototype.gabobell.helper.Logger
import yiwoo.prototype.gabobell.helper.UserDataStore
import yiwoo.prototype.gabobell.ui.popup.CustomPopup
import yiwoo.prototype.gabobell.ui.searchAddress.SearchAddressActivity


class MonitoringActivity :
    BaseActivity<ActivityMonitoringBinding>(ActivityMonitoringBinding::inflate) {

    // 출발점 경위도
    private var departureLatitude: Double = 0.0
    private var departureLongitude: Double = 0.0

    // 도착점 경위도
    private var destinationLatitude: Double = 0.0
    private var destinationLongitude: Double = 0.0

    private var isDeparture: Boolean = true
    private var map: KakaoMap? = null
    private var departureLocationLabel: Label? = null
    private var destinationLocationLabel: Label? = null
    private var currentLocationLabel: Label? = null

    private var isDepartureMarkerUpdated = false
    private var isDestinationMarkerUpdated = false
    private var isMonitoring: Boolean = false
    private var monitoringId: Long = -1

    private var bounds: LatLngBounds? = null
    private val policeMarkers = mutableListOf<Label>()
    private var policeLabel: Label? = null

    private val gpsTracksClient = GpsTracksClient(this)

    private val searchAddressLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            Log.d("MonitoringActivity@@", "Result code: ${result.resultCode}")
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                val placeName = data?.getStringExtra("selected_place_name")
                isDeparture = data?.getBooleanExtra("is_departure", true) ?: true
                val searchPlaceLongitude =
                    data?.getDoubleExtra("search_place_longitude", 0.0) ?: 0.0
                val searchPlaceLatitude = data?.getDoubleExtra("search_place_latitude", 0.0) ?: 0.0

                Log.d(
                    "MonitoringActivity@@",
                    "searchPlaceLongitude: ${searchPlaceLongitude}, searchPlaceLatitude: $searchPlaceLatitude"
                )

                if (isDeparture) {
                    binding.etDeparture.setText(placeName) // 출발지 텍스트 업데이트
                    departureLatitude = searchPlaceLatitude
                    departureLongitude = searchPlaceLongitude
                } else {
                    binding.etDestination.setText(placeName) // 도착지 텍스트 업데이트
                    destinationLatitude = searchPlaceLatitude
                    destinationLongitude = searchPlaceLongitude
                }

                Log.d(
                    "MonitoringActivity@@",
                    "출발 departureLatitude: ${departureLatitude}, 출발 departureLongitude: $departureLongitude"
                )

                Log.d(
                    "MonitoringActivity@@",
                    "도착 destinationLatitude: ${destinationLatitude}, 도착 destinationLongitude: $destinationLongitude"
                )
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupAddressSelectionListeners()
        initMapView()
        initUi()
        LocationHelper.locationInit(this)
        handleBackPressed()
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.resume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.pause()
    }

    private fun initUi() {
        binding.btnStart.setOnClickListener {
            if (departureLatitude == 0.0 || departureLongitude == 0.0 ||
                destinationLatitude == 0.0 || destinationLongitude == 0.0
            ) {
                showErrorDialog()
            } else startMonitoringCreate()
        }
        binding.btnFinish.setOnClickListener {
            finishMonitoringEvent()
        }
        binding.btnClose.setOnClickListener {
            if (isMonitoring) {
                finishMonitoringEvent()
            } else {
                finish()
            }
        }
    }

    // 출발지, 도착지 editText 선택 -> 주소 검색 화면 이동
    private fun setupAddressSelectionListeners() {
        listOf(
            binding.etDeparture to true,
            binding.etDestination to false
        ).forEach { (editText, isDeparture) ->
            editText.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    if (isMonitoring) {
                        // 귀가 모니터링 실행 중일 때 알림 표시
                        showMonitoringActiveDialog()
                        editText.clearFocus() // 포커스 제거
                    } else {
                        editText.clearFocus()
                        currentFocus?.clearFocus()

                        val intent = Intent(this, SearchAddressActivity::class.java).apply {
                            putExtra("is_departure", isDeparture) // 출발지/도착지 구분값 전달
                            Log.d("MonitoringActivity@@", "isDeparture: $isDeparture")
                        }
                        searchAddressLauncher.launch(intent)
                    }
                }
            }
        }
    }

    private fun initMapView() {
        val mapView = binding.mapView

        mapView.start(object : MapLifeCycleCallback() {
            override fun onMapResumed() {
                super.onMapResumed()
                if (isDeparture) updateDepartureMarker() else updateDestinationMarker()
            }

            override fun onMapDestroy() {}

            override fun onMapError(error: Exception) {}

        }, object : KakaoMapReadyCallback() {
            override fun getPosition(): LatLng {
                return LatLng.from(37.566535, 126.9779692)
            }

            override fun onMapReady(kakaoMap: KakaoMap) {
                setupKakaoMap(kakaoMap)

                kakaoMap.setOnCameraMoveEndListener { map, cameraPosition, _ ->
                    updateBounds(map, mapView, cameraPosition)

                    val zoomLevel = map.zoomLevel
                    if (zoomLevel >= MapConstants.ZOOMLEVEL) {
                        callPoliceApi(map)
                    } else {
                        //zoomLevel 이 13 미만(12 이하)일 경우 마커 clear
                        policeMarkers.forEach { it.remove() }
                        policeMarkers.clear()
                    }
                }
            }
        })
    }

    // 실시간 위치 정보를 수신하여 처리하는 메소드
    private fun locationCallback(latitude: Double, longitude: Double) {
        val currentLatLng = LatLng.from(latitude, longitude)
        currentLocationLabel?.moveTo(currentLatLng) ?: run {
            currentLocationLabel =
                addLabelToMap(currentLatLng, R.drawable.current_marker)
        }

        //회전 동작 고정(제스쳐)
        map?.setGestureEnable(GestureType.Rotate, false)

        // 현재위치 좌표에 따라 지도 카메라 업데이트
//            val cameraUpdate = CameraUpdateFactory.newCenterPosition(currentLatLng)
//            kakaoMap.moveCamera(cameraUpdate)

        // 귀가 모니터링 중...
        if (isMonitoring && monitoringId > 0) {
            val currentTime = System.currentTimeMillis().toString()
            CoroutineScope(Dispatchers.IO).launch {
                gpsTracksClient.gasTracks(
                    monitoringId = monitoringId,
                    latitude = latitude,
                    longitude = longitude,
                    trackTime = currentTime,
                    onSuccess = { Logger.d("Location successfully sent: Lat=$latitude, Lng=$longitude") },
                    onFailure = { errorMessage ->
                        Logger.e("Failed to send location: $errorMessage")
                    }
                )
            }
        }
    }

    private fun setupKakaoMap(kakaoMap: KakaoMap) {
        map = kakaoMap

        // 현재 위치 가져오기
        LocationHelper.startLocation(this@MonitoringActivity) { latitude, longitude ->
            locationCallback(latitude, longitude)
        }
    }

    // 출발지 마커 업데이트
    private fun updateDepartureMarker() {
        if (departureLatitude == 0.0 && departureLongitude == 0.0) {
            Log.d("MonitoringActivity@@", "Invalid coordinates for departure marker")
            return
        }

        if (map == null) {
            Log.d("MonitoringActivity@@", "KakaoMap is not ready yet. Cannot update marker.")
            return
        }

        val newLatLng = LatLng.from(departureLatitude, departureLongitude)

        departureLocationLabel?.moveTo(newLatLng)
            ?: // 새 레이블 추가
            run {
                departureLocationLabel = addLabelToMap(newLatLng, R.drawable.departure_marker)
            }

        isDepartureMarkerUpdated = true
        checkIfMarkersUpdated()
    }

    // 도착지 마커 업데이트
    private fun updateDestinationMarker() {
        if (destinationLatitude == 0.0 && destinationLongitude == 0.0) {
            Log.d("MonitoringActivity@@", "Invalid coordinates for departure marker")
            return
        }

        if (map == null) {
            Log.d("MonitoringActivity@@", "KakaoMap is not ready yet. Cannot update marker.")
            return
        }

        val newLatLng = LatLng.from(destinationLatitude, destinationLongitude)

        destinationLocationLabel?.moveTo(newLatLng)
            ?: // 새 레이블 추가
            run {
                destinationLocationLabel =
                    addLabelToMap(newLatLng, R.drawable.destination_marker)
            }

        isDestinationMarkerUpdated = true
        checkIfMarkersUpdated()

        Log.d("MonitoringActivity@@", "Departure marker updated: $newLatLng")
    }

    private fun addLabelToMap(position: LatLng, drawableResId: Int): Label? {
        return map?.labelManager?.layer?.addLabel(
            LabelOptions.from(position)
                .setStyles(setPinStyle(this, drawableResId))
        )
    }

    private fun checkIfMarkersUpdated() {
        if (isDepartureMarkerUpdated && isDestinationMarkerUpdated) {
            Log.d("MonitoringActivity", "Both markers have been updated.")
            fitBoundsToShowMarkers() // 지도 카메라 업데이트
        }
    }

    private fun fitBoundsToShowMarkers() {
        if (map == null) {
            Log.d("MonitoringActivity", "KakaoMap is not initialized.")
            return
        }

        if (departureLatitude == 0.0 || departureLongitude == 0.0 ||
            destinationLatitude == 0.0 || destinationLongitude == 0.0
        ) {
            Log.d("MonitoringActivity", "Invalid coordinates for fitting bounds.")
            return
        }

        val departureLatLng = LatLng.from(departureLatitude, departureLongitude)
        val destinationLatLng = LatLng.from(destinationLatitude, destinationLongitude)

        val builder = LatLngBounds.Builder()
        map?.moveCamera(
            CameraUpdateFactory.fitMapPoints(
                builder.include(departureLatLng)
                    .include(destinationLatLng)
                    .build(),
                100
            )
        )
    }

    // 지구대
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

        //라벨 추가
        val labelLayer = map.labelManager?.layer
        for (police in policeDataList) {
            val position = LatLng.from(police.latitude, police.longitude)
            policeLabel = labelLayer?.addLabel(
                LabelOptions.from(position).setStyles(
                    setPinStyle(
                        this,
                        R.drawable.police_marker
                    )
                )
            )
            policeLabel?.let { policeMarkers.add(it) }
        }
    }

    private fun updateBounds(kakaoMap: KakaoMap, mapView: MapView, cameraPosition: CameraPosition) {
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


    private fun startMonitoringCreate() {
        ApiSender.createEvent(
            uuid = UserDataStore.getUUID(this),
            context = this@MonitoringActivity,
            serviceType = ApiSender.Event.MONITORING.serviceType,
            latitude = departureLatitude,
            longitude = departureLongitude,
            dstLatitude = destinationLatitude,
            dstLongitude = destinationLongitude
        ) { eventId ->
            Logger.d("Received monitoring ID in MonitoringActivity: $eventId")

            isMonitoring = true
            monitoringId = eventId
            binding.btnStart.isVisible = false
            binding.btnFinish.isVisible = true

//            CoroutineScope(Dispatchers.IO).launch {
//                trackUserLocation(monitoringId)
//            }
        }
    }

    /*
    private suspend fun trackUserLocation(monitoringId: Long) {
        LocationHelper.startLocation(this@MonitoringActivity) { latitude, longitude ->

            Log.d("@!@", "startLocation for tracking")

            val currentTime = System.currentTimeMillis().toString()
            CoroutineScope(Dispatchers.IO).launch {
                gpsTracksClient.gasTracks(
                    monitoringId = monitoringId,
                    latitude = latitude,
                    longitude = longitude,
                    trackTime = currentTime,
                    onSuccess = { Logger.d("Location successfully sent: Lat=$latitude, Lng=$longitude") },
                    onFailure = { errorMessage ->
                        Logger.e("Failed to send location: $errorMessage")
                    }
                )
            }
        }
    }
    */

    // backkey를 눌렀을때도 적용
    private fun finishMonitoringEvent() {
        CustomPopup.Builder(this)
            .setTitle(getString(R.string.pop_emergency_cancel_title))
            .setMessage(getString(R.string.pop_monitoring_finish_description))
            .setOnOkClickListener(getString(R.string.pop_btn_yes)) {
                ApiSender.cancelEvent(this, (application as GaboApplication).monitorId)
                isMonitoring = false
                monitoringId = -1
                LocationHelper.stopLocation()
                finish()
            }
            .setOnCancelClickListener(getString(R.string.pop_btn_no)) {
                // no code
            }
            .build()
            .show()
    }

    private fun handleBackPressed() {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isMonitoring) {
                    finishMonitoringEvent()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }

        }
        this.onBackPressedDispatcher.addCallback(
            this,
            callback
        )
    }

    private fun showErrorDialog() {
        CustomPopup.Builder(this)
            .setTitle(getString(R.string.pop_emergency_completed_title))
            .setMessage(getString(R.string.pop_monitoring_setting_error_title))
            .setOnOkClickListener(getString(R.string.pop_btn_confirm)) {
                //no code
            }
            .build()
            .show()
    }

    private fun showMonitoringActiveDialog() {
        CustomPopup.Builder(this)
            .setTitle(getString(R.string.pop_monitoring_running_title))
            .setMessage(getString(R.string.pop_monitoring_running_description))
            .setOnOkClickListener(getString(R.string.pop_btn_confirm)) {
                //no code
            }
            .build()
            .show()
    }

    companion object {
        fun setPinStyle(context: Context, drawableResId: Int): LabelStyles {
            return LabelStyles.from(
                LabelStyle.from(drawableResId)
                    .setTextStyles(
                        LabelTextStyle.from(35, ContextCompat.getColor(context, R.color.black))
                    )
            )
        }
    }
}
