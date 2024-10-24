package yiwoo.prototype.gabobell.api.dto

data class UpdateEventRequest(
    val eventStatus: EventStatus
)

data class EventStatus(
    val serviceState: String,
    val closureType: String
)