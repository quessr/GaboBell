package yiwoo.prototype.gabobell.helper

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import yiwoo.prototype.gabobell.GaboApplication
import yiwoo.prototype.gabobell.api.GaboAPI
import yiwoo.prototype.gabobell.api.dto.CreateEvent
import yiwoo.prototype.gabobell.api.dto.CreateEventRequest
import yiwoo.prototype.gabobell.api.dto.EventStatus
import yiwoo.prototype.gabobell.api.dto.UpdateEventRequest
import yiwoo.prototype.gabobell.module.RetrofitModule

object ApiSender {

    fun reportEmergency(
        context: Context,
        uuid: String = UserSettingsManager.getUuid(context),
        latitude: Double = 37.585057,
        longitude: Double = 126.885347) {

        val retrofit: Retrofit = RetrofitModule.provideRetrofit(context)
        val gaboApi = retrofit.create(GaboAPI::class.java)

        val requestBody = CreateEventRequest(
            CreateEvent(userUuid = uuid, latitude = latitude, longitude = longitude)
        )

        Logger.d("Request Body: $requestBody") // 요청 데이터 로그 출력
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = gaboApi.createEvent(requestBody)
                Logger.d("responseBody: ${response.body()}")
                if (response.isSuccessful) {
                    response.body()?.let {
                        val eventId = it.data.createEvent.id
                        val eventAddress = it.data.createEvent.eventAddress
                        val eventStatus = it.result.status
                        val eventMessage = it.result.message
                        val latitude = it.data.createEvent.latitude
                        val longitude = it.data.createEvent.longitude

                        (context.applicationContext as GaboApplication).eventId = eventId

                        Logger.d(
                            "eventStatus: $eventStatus \n eventMessage: $eventMessage \n " +
                                    "eventId: $eventId \n eventAddress: $eventAddress \n " +
                                    "latitude: $latitude \n evelongitudentAddress: $longitude"
                        )
                    }
                } else {
                    Logger.e(
                        "Request failed with code: ${response.code()} \n " +
                                "errorBody: ${
                                    response.errorBody()?.string()
                                }"
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


    fun cancelEmergency(context: Context, eventId: Long) {
        if (eventId < 1) {
            return
        }

        val retrofit: Retrofit = RetrofitModule.provideRetrofit(context)
        val gaboApi = retrofit.create(GaboAPI::class.java)

        val requestBody = UpdateEventRequest(
            EventStatus(
                serviceState = "종료",
                closureType = "자동 종료"
            )
        )
        Logger.d("Request Body: $requestBody") // 요청 데이터 로그 출력
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = gaboApi.updateEvent(eventId, requestBody)
                Logger.d("Response Body: ${response.body()}")
                if (response.isSuccessful) {
                    response.body()?.let {
                        val eventStatus = it.result.status
                        val eventMessage = it.result.message
                        val eventId = eventId
                        val serviceState = it.data?.eventStatus?.serviceState
                        val closureType = it.data?.eventStatus?.closureType
                        val monitorUuid = it.data?.eventStatus?.monitorUuid

                        (context.applicationContext as GaboApplication).eventId = -1

                        Logger.d(
                            "eventStatus: $eventStatus \n eventMessage: $eventMessage \n " +
                                    "id: $eventId \n serviceState: $serviceState \n closureType: $closureType \n " +
                                    "monitorUuid: $monitorUuid"
                        )
                    }
                } else {
                    Logger.e(
                        "Request failed with code: ${response.code()} \n " +
                                "errorBody: ${
                                    response.errorBody()?.string()
                                }"
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


}