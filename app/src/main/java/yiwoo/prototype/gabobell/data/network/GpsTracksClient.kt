package yiwoo.prototype.gabobell.data.network

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import yiwoo.prototype.gabobell.api.dto.request.GpsTracksRequest
import yiwoo.prototype.gabobell.helper.ApiProvider

class GpsTracksClient(private val context: Context) {
    private val gaboAPI = ApiProvider.provideGaboApi(context)

    suspend fun gasTracks(
        monitoringId: Long,
        latitude: Double,
        longitude: Double,
        trackTime: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        try {
            val response = withContext(Dispatchers.IO) {
                gaboAPI.gpsTracks(
                    gpsTracksRequest = GpsTracksRequest(
                        eventId = monitoringId,
                        latitude = latitude,
                        longitude = longitude,
                        trackTime = trackTime
                    )
                )
            }

            if (response.isSuccessful) {
                onSuccess()
            } else {
                onFailure("Error: ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            onFailure("Network Error: ${e.localizedMessage}")
        }
    }
}