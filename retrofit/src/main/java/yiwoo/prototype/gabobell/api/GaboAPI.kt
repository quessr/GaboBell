package yiwoo.prototype.gabobell.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import yiwoo.prototype.gabobell.api.dto.ApiResponse
import yiwoo.prototype.gabobell.api.dto.CreateEventRequest
import yiwoo.prototype.gabobell.api.dto.CreateEventResponse
import yiwoo.prototype.gabobell.api.dto.UpdateEventRequest
import yiwoo.prototype.gabobell.api.dto.UpdateEventResponse

interface GaboAPI {
    @Headers("Content-Type: application/json")
    @POST("events/create")
    suspend fun createEvent(
        @Body
        createEventRequest: CreateEventRequest
    ): Response<CreateEventResponse>


    @Headers("Content-Type: application/json")
    @PUT("events/update-status/{eventId}")
    suspend fun updateEvent(
        @Path("eventId")
        eventId: Long,
        @Body
        updateEventRequest: UpdateEventRequest
    ): Response<ApiResponse<UpdateEventResponse>>

//    @Headers("Content-Type: application/json")
//    @PUT("events/update-status/{eventId}")
//    suspend fun updateEvent(
//        @Path("eventId")
//        eventId: Int,
//        @Body
//        updateEventRequest: UpdateEventRequest
//    ): Response<UpdateEventResponse>
}