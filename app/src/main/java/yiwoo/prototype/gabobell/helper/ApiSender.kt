package yiwoo.prototype.gabobell.helper

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import yiwoo.prototype.gabobell.GaboApplication
import yiwoo.prototype.gabobell.api.GaboAPI
import yiwoo.prototype.gabobell.api.dto.CreateEvent
import yiwoo.prototype.gabobell.api.dto.CreateEventRequest
import yiwoo.prototype.gabobell.api.dto.EventStatus
import yiwoo.prototype.gabobell.api.dto.UpdateEventRequest
import yiwoo.prototype.gabobell.module.RetrofitModule

object  ApiSender {
    enum class Event(val serviceType: String) {
        EMERGENCY("EMERGENCY"),
        BELL_EMERGENCY("BELL_EMERGENCY"),
        MONITORING("MONITORING")
    }

    fun createEvent(
        context: Context,
        uuid: String = UserDataStore.getUUID(context),
        serviceType: String = Event.EMERGENCY.serviceType,
        // latitude, dstLatitude : 신고 - 현재위치, 모니터링 - 출발지 위치
//        latitude: Double = 37.585057,
        latitude: Double,
//        longitude: Double = 126.885347,
        longitude: Double,
        // dstLatitude, dstLongitude : 모니터링만 사용하는 항목 - 도착지 위치
        dstLatitude: Double = 0.0,
        dstLongitude: Double = 0.0,
        eventIdCallback: ((Long) -> Unit)? = null
    ) {

        val retrofit: Retrofit = RetrofitModule.provideRetrofit(context)
        val gaboApi = retrofit.create(GaboAPI::class.java)

        val requestBody = CreateEventRequest(
            CreateEvent(
                userUuid = uuid,
                serviceType = serviceType,
                latitude = latitude,
                longitude = longitude,
                dstLatitude = dstLatitude,
                dstLongitude = dstLongitude
            )
        )

        Logger.d("Request Body: $requestBody") // 요청 데이터 로그 출력
        Logger.d("Request Body ==>  ${UserDataStore.getToken(context)}") // 요청 데이터 로그 출력

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = gaboApi.createEvent(requestBody)
                Logger.d("responseBody: ${response.body()}")
                if (response.isSuccessful) {
                    response.body()?.let {
                        val eventId = it.data.createEvent.id
                        val serviceType = it.data.createEvent.serviceType
                        val eventAddress = it.data.createEvent.eventAddress
                        val eventStatus = it.result.status
                        val eventMessage = it.result.message
                        val latitude = it.data.createEvent.latitude
                        val longitude = it.data.createEvent.longitude

                        val app = (context.applicationContext as GaboApplication)
                        if (serviceType == Event.MONITORING.serviceType) {
                            app.monitorId = eventId
                        } else {
                            app.eventId = eventId
                            app.isEmergency = true
                        }
                        withContext(Dispatchers.Main) {
                            eventIdCallback?.invoke(eventId)
                        }

                        Logger.d(
                            "eventStatus: $eventStatus \n eventMessage: $eventMessage \n " +
                                    "eventId: $eventId \n serviceType: $serviceType\n" +
                                    "eventAddress: $eventAddress \n " +
                                    "latitude: $latitude \n longitude: $longitude"
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


    fun cancelEvent(context: Context, eventId: Long) {
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

                        // (context.applicationContext as GaboApplication).eventId = -1
                        val app = (context.applicationContext as GaboApplication)
                        if (eventId == app.eventId) {
                            app.eventId = -1
                            app.isEmergency = false
                        } else if (eventId == app.monitorId) {
                            app.monitorId = -1
                        }

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