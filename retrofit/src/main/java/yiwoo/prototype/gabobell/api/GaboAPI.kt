package yiwoo.prototype.gabobell.api

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import yiwoo.prototype.gabobell.api.dto.ApiResponse
import yiwoo.prototype.gabobell.api.dto.CreateEventRequest
import yiwoo.prototype.gabobell.api.dto.CreateEventResponse
import yiwoo.prototype.gabobell.api.dto.UpdateEventRequest
import yiwoo.prototype.gabobell.api.dto.UpdateEventResponse
import yiwoo.prototype.gabobell.api.dto.request.CheckUserAccountRequest
import yiwoo.prototype.gabobell.api.dto.request.LogInRequest
import yiwoo.prototype.gabobell.api.dto.request.SignUpRequest
import yiwoo.prototype.gabobell.api.dto.response.CheckUserAccountResponse
import yiwoo.prototype.gabobell.api.dto.response.LogInResponse
import yiwoo.prototype.gabobell.api.dto.response.SignUpResponse

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

    @Multipart
    @POST("file/upload")
    suspend fun uploadFiles(
        @Part("eventId") eventId: RequestBody,
        @Part videoFile: MultipartBody.Part?,
        @Part imageFiles: List<MultipartBody.Part>?
    ): Response<ApiResponse<UpdateEventResponse>>

    @POST("users/checkusername")
    suspend fun checkUserAccountDuplicate(
        @Body checkUserAccountRequest: CheckUserAccountRequest
    ): Response<ApiResponse<CheckUserAccountResponse>>

    @POST("users")
    suspend fun signUpUser(
        @Body signUpRequest: SignUpRequest
    ): Response<ApiResponse<SignUpResponse>>

    @POST("auth/user/login")
    suspend fun loginInUser(
        @Body logInRequest: LogInRequest
    ): Response<ApiResponse<LogInResponse>>


//    @Headers("Content-Type: application/json")
//    @PUT("events/update-status/{eventId}")
//    suspend fun updateEvent(
//        @Path("eventId")
//        eventId: Int,
//        @Body
//        updateEventRequest: UpdateEventRequest
//    ): Response<UpdateEventResponse>
}