package yiwoo.prototype.gabobell.api.dto

data class CreateEventResponse(
    val result: CreateResultData,
    val data: CreateEventData
)

data class CreateEventData(
    val id: Int,
    val createEvent: CreateEventDetails
)

data class CreateEventDetails(
    val id: Long,
    val userUuid: String,
    val eventAddress: String,
    val latitude: Double,
    val longitude: Double
)

data class CreateResultData(
    val status: String,
    val message: String
)
