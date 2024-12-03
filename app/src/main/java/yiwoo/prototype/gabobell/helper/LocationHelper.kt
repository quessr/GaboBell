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
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.Task

object LocationHelper {
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null

    //BleManager 에서 onCreate() 함수에서 초기화
    fun locationInit(context: Context) {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
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
    }
}