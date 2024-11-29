package yiwoo.prototype.gabobell.api.dto


data class CreateEventRequest(
    val createEvent: CreateEvent
)

data class CreateEvent(
    val userUuid: String,
    val serviceType: String,
    val latitude: Double,
    val longitude: Double,
    /**
     * serviceType 이 MONITORING 인 경우 필요
     * dstLatitude : 목적지 위도
     * dstLongitude : 목적지 경도
     */
    val dstLatitude: Double,
    val dstLongitude: Double
)