package yiwoo.prototype.gabobell.repository

import android.content.Context
import com.google.android.gms.location.LocationSettingsResponse
import yiwoo.prototype.gabobell.helper.LocationHelper

class LocationRepository(private val context: Context) {

    fun startLocation(callback: (Double, Double) -> Unit) {
        LocationHelper.startLocation(context) { latitude, longitude ->
            callback(latitude, longitude)
        }
    }

    fun checkLocationSettings(callback: (Boolean, LocationSettingsResponse?, Exception?) -> Unit) {
        LocationHelper.checkLocationSettings(context) { isLocationAccuracy, response, exception ->
            callback(isLocationAccuracy, response, exception)
        }
    }

}