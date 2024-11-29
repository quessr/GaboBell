package yiwoo.prototype.gabobell.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.label.Label
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelStyles
import com.kakao.vectormap.label.LabelTextBuilder
import com.kakao.vectormap.label.LabelTextStyle
import yiwoo.prototype.gabobell.R
import yiwoo.prototype.gabobell.databinding.ActivityMonitoringBinding
import yiwoo.prototype.gabobell.helper.ApiSender
import yiwoo.prototype.gabobell.helper.LocationHelper
import yiwoo.prototype.gabobell.helper.Logger
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

    // 출발지, 도착지 editText 선택 -> 주소 검색 화면 이동
    private fun setupAddressSelectionListeners() {
        listOf(
            binding.etDeparture to true,
            binding.etDestination to false
        ).forEach { (editText, isDeparture) ->
            editText.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    editText.clearFocus()

                    // Activity의 포커스를 Window에 넘김
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
            val cameraUpdate = CameraUpdateFactory.newCenterPosition(currentLatLng)
            kakaoMap.moveCamera(cameraUpdate)
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

        Log.d("MonitoringActivity@@", "Departure marker updated: $newLatLng")
    }

    private fun addLabelToMap(position: LatLng, drawableResId: Int, text: String): Label? {
        return map?.labelManager?.layer?.addLabel(
            LabelOptions.from(position)
                .setStyles(setPinStyle(this, drawableResId))
                .setTexts(LabelTextBuilder().setTexts(text))
        )
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
