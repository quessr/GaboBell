package yiwoo.prototype.gabobell.data.network

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import yiwoo.prototype.gabobell.api.GaboAPI
import yiwoo.prototype.gabobell.api.dto.response.PoliceResultItem
import yiwoo.prototype.gabobell.module.RetrofitModule

object PoliceClient {

    fun getBoundsPolice(
        context: Context,
        swLat: Double,
        swLng: Double,
        neLat: Double,
        neLng: Double,
        callback: (List<PoliceResultItem>?) -> Unit // API 응답 데이터를 전달하는 콜백
    ) {
        val retrofit: Retrofit = RetrofitModule.provideRetrofit(context)
        val gaboAPI = retrofit.create(GaboAPI::class.java)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = gaboAPI.boundsPolice(swLat, swLng, neLat, neLng)
                Log.d("!@!@", "responseBody: ${response.body()}")
                if (response.isSuccessful) {
                    response.body()?.let {
                        val policeStatus = it.result.status
                        val policeMessage = it.result.message
                        val totalElements = it.data.totalElements
                        Log.d("!@!@", "status: $policeStatus \n" +
                                "message: $policeMessage")
                        Log.d("!@!@", "totalElements: $totalElements")
                        for (item in it.data.results) {
                            Log.d("!@!@", "ID: ${item.id} \n" +
                                    "SubStation: ${item.subStation} \n" +
                                    "Division: ${item.division} \n" +
                                    "PhoneNumber: ${item.phoneNumber} \n" +
                                    "Address: ${item.address} \n" +
                                    "Lat: ${item.latitude} \n" +
                                    "Lng: ${item.longitude}")
                        }
                    }
                    val policeDataList = response.body()?.data?.results
                    callback(policeDataList)
                } else {
                    Log.e("!@!@",
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