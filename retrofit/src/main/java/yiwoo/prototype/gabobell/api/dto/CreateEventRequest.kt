package yiwoo.prototype.gabobell.api.dto


data class CreateEventRequest(
    val createEvent: CreateEvent
)

data class CreateEvent(
    val userUuid: String,
    val latitude: Double,
    val longitude: Double
)