package yiwoo.prototype.gabobell.ui

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.label.Label
import com.kakao.vectormap.label.LabelOptions
import yiwoo.prototype.gabobell.GaboApplication
import yiwoo.prototype.gabobell.R
import yiwoo.prototype.gabobell.databinding.ActivityMainBinding
import yiwoo.prototype.gabobell.helper.ApiSender
import yiwoo.prototype.gabobell.helper.LocationHelper
import yiwoo.prototype.gabobell.helper.Logger
import yiwoo.prototype.gabobell.helper.UserDeviceManager

class MainActivity : BaseActivity<ActivityMainBinding>(ActivityMainBinding::inflate) {

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var currentLocationLabel: Label? = null
    private var isVisibleFab: Boolean = false
    private var isActivePolice: Boolean = false
    private lateinit var emergencyLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initUi()
        initLauncher()
        initMap()
        checkPermissions()
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.resume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.pause()
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
                // TODO: 초기 맵 위치
                return LatLng.from(37.58376, 126.8867)
            }

            override fun onMapReady(kakaoMap: KakaoMap) {
                LocationHelper.startLocation(this@MainActivity) { latitude, longitude ->
                    updateCurrentLocationMarker(kakaoMap, latitude, longitude)
                }
            }
        })
    }

    private fun initLauncher() {
        emergencyLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                // 신고 화면 -> 신고
                updateUi()
                // 신고 완료 팝업
                AlertDialog.Builder(this)
                    .setTitle(R.string.pop_emergency_completed_title)
                    .setMessage(R.string.pop_emergency_completed_description)
                    .setCancelable(false)
                    .setPositiveButton(R.string.pop_btn_confirm) { _, _ ->
                    }
                    .show()

            } else if (it.resultCode == RESULT_CANCELED) {
                // 신고 화면 -> 취소
            }
        }
    }

    private fun updateUi() {
        if (isEmergency()) {
            binding.btnEmergencyReport.text = "신고취소"
        } else {
            binding.btnEmergencyReport.text = "긴급신고"
        }
    }



    private fun initUi() {

        // 귀가 모니터링
        binding.btnMonitoring.setOnClickListener {
            val intent = Intent(this, MonitoringActivity::class.java)
            startActivity(intent)
        }

        // 신고/신고취소하기
        binding.btnEmergencyReport.setOnClickListener {
            if (isEmergency()) {
                val eventId = (application as GaboApplication).eventId
                // 신고 취소 (자동종료)
                ApiSender.cancelEmergency(this@MainActivity, eventId)
                (application as GaboApplication).isEmergency = false
                updateUi()
            } else {
                // 신고화면 이동
                val intent = Intent(this, ReportActivity::class.java)
                emergencyLauncher.launch(intent)
            }
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
    }

    // 신고 여부 반환
    private fun isEmergency(): Boolean {
        return (application as GaboApplication).isEmergency
    }

    private fun visibleFab(visible: Boolean) {
        binding.fabPolice.visibility = if (visible) View.VISIBLE else View.GONE
    }

    private fun togglePolice(isActive: Boolean) {
        Log.d("@!@", ">>>togglePolice : $isActive")
        if (isActive) {
            binding.fabPolice.setBackgroundResource(R.color.pink)
            // TODO: 지구대 마커 표기
        } else {
            binding.fabPolice.setBackgroundResource(R.color.black_op_66)
            // TODO: 지구대 마커 해제
        }
    }

    private fun updateCurrentLocationMarker(
        map: KakaoMap, latitude: Double, longitude: Double) {
        val position = LatLng.from(latitude, longitude)
        val labelLayer = map.labelManager?.layer
        if (currentLocationLabel != null) {
            // 현재 위치 마커 이동
            currentLocationLabel?.moveTo(position)
        } else {
            // 현재 위치 마커 생성
            currentLocationLabel = labelLayer?.addLabel(
                LabelOptions.from(position)
                    .setStyles(
                        // TODO: 추후 공통 처리
                        MonitoringActivity.setPinStyle(
                            this@MainActivity,
                            R.drawable.marker_current
                        )
                    )
            )
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

    /*
    private fun startLocation() {
        LocationHelper.locationInit(this)
        LocationHelper.startLocation(this) { lat, lng ->
            val locationLat: Double? = lat
            val locationLng: Double? = lng
            Logger.d("LatLng: $locationLat | $locationLng")
        }
    }
    */

    private val requestMultiplePermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value } // 모든 권한이 승인되었는지 확인
        if (allGranted) {
            Logger.d("모든 권한이 허용됨")
            //모든 권한 허용 후 gps 추적 시작 (확인용)
            // startLocation()
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