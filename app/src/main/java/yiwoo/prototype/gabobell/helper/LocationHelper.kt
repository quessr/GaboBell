package yiwoo.prototype.gabobell.helper

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.Task

object LocationHelper {
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    //BleManager 에서 onCreate() 함수에서 초기화
    fun locationInit(context: Context) {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
    }
    fun getCurrentLocation(context: Context, locationResult: (Double?, Double?) -> Unit) {
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
}