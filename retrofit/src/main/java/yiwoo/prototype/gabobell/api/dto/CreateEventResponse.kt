package yiwoo.prototype.gabobell.api.dto


data class CreateEventResponse(
    val result: ResultData,
    val data: EventData
)

data class EventData(
    val id: Int,
    val createEvent: EventDetails
)

data class EventDetails(
    val id: Int,
    val userUuid: String,
    val eventAddress: String,
    val latitude: Double,
    val longitude: Double
)

data class ResultData(
    val status: String,
    val message: String
)
