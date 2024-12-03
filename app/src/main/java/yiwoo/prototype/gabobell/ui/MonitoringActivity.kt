package yiwoo.prototype.gabobell.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.LatLngBounds
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.label.Label
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelStyles
import com.kakao.vectormap.label.LabelTextBuilder
import com.kakao.vectormap.label.LabelTextStyle
import yiwoo.prototype.gabobell.GaboApplication
import yiwoo.prototype.gabobell.R
import yiwoo.prototype.gabobell.databinding.ActivityMonitoringBinding
import yiwoo.prototype.gabobell.helper.ApiSender
import yiwoo.prototype.gabobell.helper.LocationHelper
import yiwoo.prototype.gabobell.helper.Logger
import yiwoo.prototype.gabobell.helper.UserDataStore
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
                return LatLng.from(37.58376, 126.8867)
            }

            override fun onMapReady(kakaoMap: KakaoMap) {
                setupKakaoMap(kakaoMap)
            }
        })
    }

    private fun setupKakaoMap(kakaoMap: KakaoMap) {
        map = kakaoMap

        // 현재 위치 가져오기
        LocationHelper.startLocation(this@MonitoringActivity) { latitude, longitude ->
            val currentLatLng = LatLng.from(latitude, longitude)
            currentLocationLabel?.moveTo(currentLatLng) ?: run {
                currentLocationLabel =
                    addLabelToMap(currentLatLng, R.drawable.marker_current, "현재")
            }

            // 현재위치 좌표에 따라 지도 카메라 업데이트
//            val cameraUpdate = CameraUpdateFactory.newCenterPosition(currentLatLng)
//            kakaoMap.moveCamera(cameraUpdate)
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
                departureLocationLabel = addLabelToMap(newLatLng, R.drawable.marker_departure, "출발")
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
                    addLabelToMap(newLatLng, R.drawable.marker_destination, "도착")
            }

        isDestinationMarkerUpdated = true
        checkIfMarkersUpdated()

        Log.d("MonitoringActivity@@", "Departure marker updated: $newLatLng")
    }

    private fun addLabelToMap(position: LatLng, drawableResId: Int, text: String): Label? {
        return map?.labelManager?.layer?.addLabel(
            LabelOptions.from(position)
                .setStyles(setPinStyle(this, drawableResId))
                .setTexts(LabelTextBuilder().setTexts(text))
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

    private fun startMonitoringCreate() {
        ApiSender.createEvent(
            uuid = UserDataStore.getUUID(this),
            context = this@MonitoringActivity,
            serviceType = ApiSender.Event.MONITORING.serviceType,
            latitude = departureLatitude,
            longitude = destinationLongitude,
            dstLatitude = destinationLatitude,
            dstLongitude = destinationLongitude
        ) { monitoringId ->
            Logger.d("Received monitoring ID in MonitoringActivity: $monitoringId")

            isMonitoring = true
            binding.btnStart.isVisible = false
            binding.btnFinish.isVisible = true
        }
    }

    // backkey를 눌렀을때도 적용
    private fun finishMonitoringEvent() {
        AlertDialog.Builder(this)
            .setTitle(R.string.pop_emergency_cancel_title)
            .setMessage(R.string.pop_monitoring_finish_description)
            .setCancelable(false)
            .setPositiveButton(R.string.pop_btn_yes) { _, _ ->
                ApiSender.cancelEvent(this, (application as GaboApplication).monitorId)
                isMonitoring = false
                finish()
            }
            .setNegativeButton(R.string.pop_btn_no) { _, _ ->
                // no code
            }
            .show()
    }

    private fun showErrorDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.pop_monitoring_setting_error_title)
            .setPositiveButton(R.string.pop_btn_confirm) { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun showMonitoringActiveDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.pop_monitoring_running_title)
            .setMessage(R.string.pop_monitoring_running_description)
            .setPositiveButton(R.string.pop_btn_confirm) { dialog, _ -> dialog.dismiss() }
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
