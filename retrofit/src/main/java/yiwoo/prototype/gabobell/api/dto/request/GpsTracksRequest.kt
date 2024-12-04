package yiwoo.prototype.gabobell.api.dto.request

data class GpsTracksRequest(
    val eventId: Long,
    val latitude: Long,
    val longitude: Long,
    val trackTime: String
)
