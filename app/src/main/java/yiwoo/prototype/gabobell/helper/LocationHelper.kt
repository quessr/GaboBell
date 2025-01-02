package yiwoo.prototype.gabobell.helper

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsResponse
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.Task

object LocationHelper {
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null

    private var latestLocation: Location? = null
    private var latestCurrentTime: Long = 0

    fun latestValidLocation(): Location? {
        val diffTime = System.currentTimeMillis() - latestCurrentTime
        Logger.d("latestDiffTime : $diffTime")
        if (diffTime > 60_000) {
            Logger.d("diffTime : $diffTime")
            return null
        }
        return latestLocation
    }

    //BleManager 에서 onCreate() 함수에서 초기화
    fun locationInit(context: Context) {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
    }

    /**
     * Google play 서비스(위치 서비스) -> 위치 정보 on/off and 위치 정확도 개선 토글 버튼 on/off 상태 관리
     * checkLocationSettings를 사용하는 경우에는 별도로 isLocationEnabled 메서드를 구현하지 않아도 위치 서비스 활성화 상태 확인과 설정 화면 유도가 가능
     * checkLocationSettings -> FusedLocationProviderClient 사용할 경우(Google Play Services에 의존)
     *      , 위치 정확도 확인 가능
     * isLocationEnabled -> LocationManager를 사용해 GPS(GPS_PROVIDER)와 네트워크(NETWORK_PROVIDER) 위치 서비스가 각각 활성화되어 있는지 확인
     *      , 위치 정확도 상태는 확인 불가능
     *
     * ===> 참고용 앱은 LocationManager 를 이용하여 위치정보를 얻는것으로 보임
     */
    fun checkLocationSettings(context: Context, taskCallback: (Boolean, LocationSettingsResponse?, Exception?) -> Unit) {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            1000)
            .build()

        val locationSettingsRequest = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
            .setAlwaysShow(true) // 필요 시 사용자에게 설정 화면을 표시
            .build()

        val settingsClient = LocationServices.getSettingsClient(context)
        val task: Task<LocationSettingsResponse> = settingsClient.checkLocationSettings(locationSettingsRequest)

        task.addOnSuccessListener { response ->
            Logger.d("위치 정보(정확도) addOnSuccessListener=====")
            taskCallback(true, response, null)
        }

        task.addOnFailureListener { exception ->
            Logger.d("위치 정보(정확도) addOnFailureListener=====")
            taskCallback(false, null, exception)
        }
    }

    //최신 상태의 정확한 위치를 더 일관되게 가져온다.(현재 위치를 한 번 요청)
    fun getCurrentLocation(context: Context, locationResult: (Double, Double) -> Unit) {
        /**
         * Task<Location> (addOnSuccessListener) -> 비동기 작업
         * 비동기로 동작하는 Task<Location> 객체를 반환
         */
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val locationRequest: Task<Location> =
                fusedLocationProviderClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    null
                )
            locationRequest.addOnSuccessListener { location ->
                location?.let {
                    latestLocation = location
                    latestCurrentTime = System.currentTimeMillis()

                    val lat = location.latitude
                    val long = location.longitude
                    Logger.d("currentLocation: $lat | $long")
                    locationResult(lat, long)
                }
            }
        } else {
            Logger.e("Location_checkSelfPermission: DENIED")
        }
    }

    //현재 위치를 지속적으로 수신
    fun startLocation(context: Context, locationResult: (Double, Double) -> Unit) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val locationRequest = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                5000
            ).build()

            locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    super.onLocationResult(locationResult)
                    val location = locationResult.lastLocation
                    location?.let {
                        latestLocation = location
                        latestCurrentTime = System.currentTimeMillis()

                        val lat = it.latitude
                        val long = it.longitude
                        Log.d("KakaoMap", "LocationHelper_Updated Location: $lat | $long")
                        locationResult(lat, long)
                    }
                }
            }
            fusedLocationProviderClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            )
        } else {
            Log.e("KakaoMap", "Location_checkSelfPermission: DENIED")
        }
    }

    //위치 추적 중단
    fun stopLocation() {
        locationCallback?.let {
            fusedLocationProviderClient.removeLocationUpdates(it)
        }
        locationCallback = null
    }
}