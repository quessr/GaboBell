package yiwoo.prototype.gabobell.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import yiwoo.prototype.gabobell.api.dto.CreateEventRequest
import yiwoo.prototype.gabobell.api.dto.CreateEventResponse

interface GaboAPI {
    @Headers("Content-Type: application/json")
    @POST("events/create")
    suspend fun createEvent(
        @Body
        createEventRequest: CreateEventRequest
    ): Response<CreateEventResponse>
}