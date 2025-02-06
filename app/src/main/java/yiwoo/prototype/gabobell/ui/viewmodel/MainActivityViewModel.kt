package yiwoo.prototype.gabobell.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.location.LocationSettingsResponse
import com.kakao.vectormap.LatLng
import yiwoo.prototype.gabobell.BuildConfig
import yiwoo.prototype.gabobell.ble.BleManager
import yiwoo.prototype.gabobell.helper.Logger
import yiwoo.prototype.gabobell.repository.BleServiceRepository
import yiwoo.prototype.gabobell.repository.LocationRepository
import yiwoo.prototype.gabobell.repository.PermissionRepository
import yiwoo.prototype.gabobell.repository.UserDeviceRepository

class MainActivityViewModel
    (
    private val repository: PermissionRepository,
    private val userDeviceRepository: UserDeviceRepository,
    private val locationRepository: LocationRepository,
    private val bleServiceRepository: BleServiceRepository
) : ViewModel() {

    private val _permissionState = MutableLiveData<Boolean>()
    val permissionState: LiveData<Boolean> = _permissionState

    private val _resultPermission = MutableLiveData<Boolean>()
    val resultPermission: LiveData<Boolean> = _resultPermission

    private val _isDebugMode = MutableLiveData<Boolean>()
    val isDebugMode: LiveData<Boolean> = _isDebugMode

    private val _currentLocation = MutableLiveData<LatLng>()
    val currentLocation: LiveData<LatLng> = _currentLocation

    private val _isLocationAccuracy = MutableLiveData<Pair<LocationSettingsResponse?, Exception?>>()
    val isLocationAccuracy: LiveData<Pair<LocationSettingsResponse?, Exception?>> =
        _isLocationAccuracy

    private val _isRegister = MutableLiveData<Boolean>()
    val isRegister: LiveData<Boolean> = _isRegister

    private val _isEnableBleAdapter = MutableLiveData<Boolean>()
    val isEnableBleAdapter: LiveData<Boolean> = _isEnableBleAdapter

    private val _isEmergency = MutableLiveData<Boolean>()
    val isEmergency: LiveData<Boolean> = _isEmergency
    private val _isUpdateUi = MutableLiveData<Boolean>()
    val isUpdateUi: LiveData<Boolean> = _isUpdateUi

    private val _isConnected = MutableLiveData<Boolean>()
    val isConnected: LiveData<Boolean> = _isConnected


    /**
     * 기기연결상태 신고취소
     */
    fun connectStateCancelEmergency() {
        BleManager.instance?.cmdEmergency(false)
    }

    /**
     * 기기미연결상태 신고취소
     */
    fun disConnectStateCancelEmergency() {
        bleServiceRepository.disConnectStateCancelEmergency()
    }

    /**
     * 기기연결상태
     */
    fun stateConnected() {
        _isConnected.value = bleServiceRepository.stateConnected()
    }

    /**
     * 신고중인상태 여부
     */
    fun stateEmergency() {
        _isEmergency.value = bleServiceRepository.stateEmergency()
    }

    /**
     * 신고중인상태 UI
     */
    fun stateEmergencyUi() {
        _isUpdateUi.value = bleServiceRepository.stateEmergency()
    }

    /**
     * 블루투스 활성화 여부
     */
    fun enableBleAdapter() {
        _isEnableBleAdapter.value = bleServiceRepository.enableBleAdapter()
    }

    /**
     * ble 등록 여부
     */
    fun bleRegister() {
        _isRegister.value = userDeviceRepository.bleRegister()
    }

    /**
     * 권한 상태 체크 후 LiveData 업데이트
     */
    fun checkPermissions() {
        _permissionState.value = repository.checkPermissions()
        Logger.d("mainActivityViewModel_permission : ${_permissionState.value}")
    }

    /**
     * 요청해야 할 권한
     */
    fun requestCheckPermissions(): Array<String> {
        return repository.requestCheckPermissions()
    }


    /**
     * 권한 결과 처리
     */
    fun handlePermissionResult(result: Map<String, Boolean>) {
        val allGranted = result.all { it.value }
        if (allGranted) {
            Logger.d("모든 권한이 허용됨")
            _resultPermission.value = true

        } else {
            Logger.d("일부 권한이 거부됨")
            _resultPermission.value = false
        }
    }

    /**
     * 현재위치정보
     */
    fun startLocation() {
        locationRepository.startLocation { latitude, longitude ->
            Logger.d("mainActivityViewModel_startLocation : $latitude | $longitude")
            _currentLocation.postValue(LatLng.from(latitude, longitude))
        }
    }

    /**
     * 위치 정보(정확도) 설정
     */
    fun checkLocationSettings() {
        locationRepository.checkLocationSettings { isLocationAccuracy, response, exception ->
            if (!isLocationAccuracy) {
                Logger.d("mainActivityViewModel_위치 정보(정확도) 비활성화되어 있습니다.")
                _isLocationAccuracy.postValue(Pair(response, exception))
            }
        }
    }

    /**
     * Service 활성화
     */
    fun startMainService() {
        bleServiceRepository.startMainService()
    }

    /**
     * deBugMode 상태
     */
    fun debugMode() {
        _isDebugMode.value = BuildConfig.DEBUG_MODE
    }
}

//MainActivityViewModel 클래스가 Repository 클래스에 의존하기 때문에 factory 필요
class MainActivityViewModelFactory
    (
    private val permissionRepository: PermissionRepository,
    private val userDeviceRepository: UserDeviceRepository,
    private val locationRepository: LocationRepository,
    private val bleServiceRepository: BleServiceRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainActivityViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainActivityViewModel(
                permissionRepository,
                userDeviceRepository,
                locationRepository,
                bleServiceRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}