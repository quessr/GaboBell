package yiwoo.prototype.gabobell.api.dto.request

data class GpsTracksRequest(
    val eventId: Long,
    val latitude: Double,
    val longitude: Double,
    val trackTime: String
)
