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
import com.kakao.vectormap.label.Label
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelStyles
import com.kakao.vectormap.label.LabelTextBuilder
import com.kakao.vectormap.label.LabelTextStyle
import yiwoo.prototype.gabobell.R
import yiwoo.prototype.gabobell.databinding.ActivityMonitoringBinding
import yiwoo.prototype.gabobell.helper.LocationHelper
import yiwoo.prototype.gabobell.ui.searchAddress.SearchAddressActivity

class MonitoringActivity :
    BaseActivity<ActivityMonitoringBinding>(ActivityMonitoringBinding::inflate) {
    //    var currentLatLng: LatLng? = null
    private var searchPlaceLongitude: Double = 0.0
    private var searchPlaceLatitude: Double = 0.0
    private var isDeparture: Boolean = true
    private var map: KakaoMap? = null
    private var departureLocationLabel: Label? = null
    private var destinationLocationLabel: Label? = null

    private val searchAddressLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            Log.d("MonitoringActivity@@", "Result code: ${result.resultCode}")
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                val placeName = data?.getStringExtra("selected_place_name")
                isDeparture = data?.getBooleanExtra("is_departure", true) ?: true
                searchPlaceLongitude = data?.getDoubleExtra("search_place_longitude", 0.0) ?: 0.0
                searchPlaceLatitude = data?.getDoubleExtra("search_place_latitude", 0.0) ?: 0.0

                Log.d(
                    "MonitoringActivity@@",
                    "searchPlaceLongitude: ${searchPlaceLongitude}, searchPlaceLatitude: $searchPlaceLatitude"
                )

                if (isDeparture) {
                    binding.etDeparture.setText(placeName) // 출발지 업데이트
                } else {
                    binding.etDestination.setText(placeName) // 도착지 업데이트
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val mapView = binding.mapView

        listOf(
            binding.etDeparture to true,
            binding.etDestination to false
        ).forEach { (editText, isDeparture) ->
            editText.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    val intent = Intent(this, SearchAddressActivity::class.java).apply {
                        putExtra("is_departure", isDeparture) // 출발지/도착지 구분값 전달

                        Log.d("MonitoringActivity@@", "isDeparture: $isDeparture")
                    }
                    searchAddressLauncher.launch(intent)
                }
            }
        }

        LocationHelper.locationInit(this)

        mapView.start(object : MapLifeCycleCallback() {
            override fun onMapResumed() {
                super.onMapResumed()

                if (isDeparture) updateDepartureMarker() else updateDestinationMarker()

                Log.d("MonitoringActivity@@", "onMapResumed")
            }

            override fun onMapDestroy() {
                // 지도 API가 정상적으로 종료될 때 호출됨
            }

            override fun onMapError(error: Exception) {
                // 인증 실패 및 지도 사용 중 에러가 발생할 때 호출됨
            }
        }, object : KakaoMapReadyCallback() {
            override fun getPosition(): LatLng {

                return LatLng.from(37.58376, 126.8867)
            }

            override fun onMapReady(kakaoMap: KakaoMap) {
                map = kakaoMap

                // 현재 위치 가져오기
                LocationHelper.getCurrentLocation(this@MonitoringActivity) { lat, lng ->
                    val latitude = lat ?: 37.559984
                    val longitude = lng ?: 126.9753071

                    val currentLatLng = LatLng.from(latitude, longitude)

                    // 레이블을 지도에 추가 (현재지점)
                    kakaoMap.labelManager?.layer?.addLabel(
                        LabelOptions.from(currentLatLng)
                            .setStyles(
                                setPinStyle(
                                    this@MonitoringActivity,
                                    R.drawable.marker_current
                                )
                            )
                            .setTexts(LabelTextBuilder().setTexts("현재"))
                    )
                }
            }
        })

    }

    override fun onResume() {
        super.onResume()
        binding.mapView.resume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.pause()
    }


    private fun updateDepartureMarker() {
        // 검색된 좌표로 마커를 업데이트

        if (searchPlaceLongitude == 0.0 && searchPlaceLatitude == 0.0) {
            Log.d("MonitoringActivity@@", "Invalid coordinates for departure marker")
            return
        }

        if (map == null) {
            Log.d("MonitoringActivity@@", "KakaoMap is not ready yet. Cannot update marker.")
            return
        }

        val newLatLng = LatLng.from(searchPlaceLatitude, searchPlaceLongitude)

        departureLocationLabel?.moveTo(newLatLng)
            ?: // 새 레이블 추가
            run {
                departureLocationLabel = map?.labelManager?.layer?.addLabel(
                    LabelOptions.from(newLatLng)
                        .setStyles(
                            setPinStyle(this, R.drawable.marker_departure)
                        )
                        .setTexts(LabelTextBuilder().setTexts("출발"))
                )
            }
    }

    private fun updateDestinationMarker() {
        // 검색된 좌표로 마커를 업데이트

        if (searchPlaceLongitude == 0.0 && searchPlaceLatitude == 0.0) {
            Log.d("MonitoringActivity@@", "Invalid coordinates for departure marker")
            return
        }

        if (map == null) {
            Log.d("MonitoringActivity@@", "KakaoMap is not ready yet. Cannot update marker.")
            return
        }

        val newLatLng = LatLng.from(searchPlaceLatitude, searchPlaceLongitude)

        destinationLocationLabel?.moveTo(newLatLng)
            ?: // 새 레이블 추가
            run {
                destinationLocationLabel = map?.labelManager?.layer?.addLabel(
                    LabelOptions.from(newLatLng)
                        .setStyles(
                            setPinStyle(this, R.drawable.marker_destination)
                        )
                        .setTexts(LabelTextBuilder().setTexts("도착"))
                )
            }

        Log.d("MonitoringActivity@@", "Departure marker updated: $newLatLng")
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
