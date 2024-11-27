package yiwoo.prototype.gabobell.ui

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.MapView
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelTextBuilder
import yiwoo.prototype.gabobell.R
import yiwoo.prototype.gabobell.databinding.ActivityMainBinding
import yiwoo.prototype.gabobell.helper.LocationHelper
import yiwoo.prototype.gabobell.helper.Logger
import yiwoo.prototype.gabobell.helper.UserDeviceManager

class MainActivity : BaseActivity<ActivityMainBinding>(ActivityMainBinding::inflate) {

    private var bluetoothAdapter: BluetoothAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initUi()
        initMap()
        checkPermissions()
    }

    private fun initMap() {
        val mapView = MapView(this)
        binding.mapView.addView(mapView)
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
                            MonitoringActivity.setPinStyle(
                                this@MainActivity,
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
                            MonitoringActivity.setPinStyle(
                                this@MainActivity,
                                R.drawable.marker_destination
                            )
                        )
                        .setTexts(
                            LabelTextBuilder().setTexts("도착") // 여기에 원하는 텍스트 추가
                        )
                )

                // 현재 위치 가져오기
                LocationHelper.getCurrentLocation(this@MainActivity) { lat, lng ->
                    val latitude = lat ?: 37.559984
                    val longitude = lng ?: 126.9753071

                    val currentLatLng = LatLng.from(latitude, longitude)

                    // 레이블을 지도에 추가 (현재지점)
                    kakaoMap.labelManager?.layer?.addLabel(
                        LabelOptions.from(currentLatLng)
                            .setStyles(
                                MonitoringActivity.setPinStyle(
                                    this@MainActivity,
                                    R.drawable.marker_current
                                )
                            )
                            .setTexts(LabelTextBuilder().setTexts("현재"))
                    )
                }
            }
        })
    }

    private fun initUi() {

        // 귀가 모니터링
        binding.btnMonitoring.setOnClickListener {
            val intent = Intent(this, MonitoringActivity::class.java)
            startActivity(intent)
        }

        // 신고하기
        binding.btnEmergencyReport.setOnClickListener {
            val intent = Intent(this, ReportActivity::class.java)
            startActivity(intent)
        }

        // 설정
        binding.btnSetting.setOnClickListener {
            if (UserDeviceManager.isRegister(this)) {
                val intent = Intent(this, DeviceSettingsActivity::class.java)
                startActivity(intent)
            } else {
                enableBleAdapter()
            }
        }
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            //android 12 이상
            requestMultiplePermissions.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            )
        } else {
            //android 11 이하
            requestMultiplePermissions.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            )
        }
    }

    private fun startLocation() {
        LocationHelper.locationInit(this)
        LocationHelper.startLocation(this) { lat, lng ->
            val locationLat: Double? = lat
            val locationLng: Double? = lng
            Logger.d("LatLng: $locationLat | $locationLng")
        }
    }

    private val requestMultiplePermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value } // 모든 권한이 승인되었는지 확인
        if (allGranted) {
            Logger.d("모든 권한이 허용됨")
            //모든 권한 허용 후 gps 추적 시작
            startLocation()
        } else {
            Logger.d("일부 권한이 거부됨")
            // TODO: 권한 거부에 대한 시나리오는 추후 반영
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

    private fun enableBleAdapter() {
        val bluetoothManager: BluetoothManager = getSystemService(BluetoothManager::class.java)
        bluetoothAdapter = bluetoothManager.adapter
        Logger.d("bluetoothAdapter_info : $bluetoothAdapter")
        Logger.d("bluetoothAdapter_boolean : ${bluetoothAdapter?.isEnabled}")

        // 블루투스 활성화 상태 체크
        if (bluetoothAdapter?.isEnabled == false) {
            // 활성화가 안 되어 있을 경우
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            requestBluetooth.launch(enableBtIntent)

        } else { // 활성화가 되어 있을 경우
            val intent = Intent(this, RegisterDeviceActivity::class.java)
            startActivity(intent)
        }
    }
}