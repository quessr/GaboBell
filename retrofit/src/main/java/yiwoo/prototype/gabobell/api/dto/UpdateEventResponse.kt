package yiwoo.prototype.gabobell.api.dto

//data class UpdateEventResponse(
//    val result: UpdateResultData,
//    val data: UpdateEventData
//)
//
//data class UpdateEventData(
//    val id: Int,
//    val updateEventStatus: UpdateEventStatus
//)
//
//data class UpdateEventStatus(
//    val id: Long,
//    val serviceState: String,
//    val closureType: String,
//    val monitorUuid: String?
//)
//
//data class UpdateResultData(
//    val status: String,
//    val message: String
//)


data class UpdateEventResponse(
    val id: Int,
    val eventStatus: UpdateEventStatus
)

data class UpdateEventStatus(
    val id: Long,
    val serviceState: String,
    val closureType: String,
    val monitorUuid: String?
)

data class UpdateResultData(
    val status: String,
    val message: String
)


data class ApiResponse<T>(
    val result: UpdateResultData,
    val data: T?
)
