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
import com.kakao.vectormap.MapView
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

    private val searchAddressLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            Log.d("MonitoringActivity@@", "Result code: ${result.resultCode}")
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                val placeName = data?.getStringExtra("selected_place_name")
                val isDeparture = data?.getBooleanExtra("is_departure", true) ?: true

                if (isDeparture) {
                    binding.etDeparture.setText(placeName) // 출발지 업데이트
                } else {
                    binding.etDestination.setText(placeName) // 도착지 업데이트
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val mapView = MapView(this)
        binding.mapView.addView(mapView)

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
                // 마커가 표시될 위치
                val departureLatLng = LatLng.from(37.5848659, 126.88598)
                val destinationLatLng = LatLng.from(37.5810471, 126.8905417)
//                val currentLatLng = LatLng.from(37.58376, 126.8867)


                // 레이블을 지도에 추가 (출발지점)
                kakaoMap.labelManager?.layer?.addLabel(
                    LabelOptions.from(departureLatLng)
                        .setStyles(
                            setPinStyle(
                                this@MonitoringActivity,
                                R.drawable.marker_departure
                            )
                        )
                        .setTexts(
                            LabelTextBuilder().setTexts("출발")
                        )
                )

//                // 레이블을 지도에 추가 (현재지점)
//                kakaoMap.labelManager?.layer?.addLabel(
//                    LabelOptions.from(currentLatLng)
//                        .setStyles(setPinStyle(this@MonitoringActivity, R.drawable.marker_current))
//                        .setTexts(
//                            LabelTextBuilder().setTexts("현재")
//                        )
//                )

                // 레이블을 지도에 추가 (도착지점)
                kakaoMap.labelManager?.layer?.addLabel(
                    LabelOptions.from(destinationLatLng)
                        .setStyles(
                            setPinStyle(
                                this@MonitoringActivity,
                                R.drawable.marker_destination
                            )
                        )
                        .setTexts(
                            LabelTextBuilder().setTexts("도착") // 여기에 원하는 텍스트 추가
                        )
                )

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
