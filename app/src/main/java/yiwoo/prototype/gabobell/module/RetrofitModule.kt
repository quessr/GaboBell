package yiwoo.prototype.gabobell.module

import android.content.Context
import android.util.Log
import com.google.gson.GsonBuilder
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import yiwoo.prototype.gabobell.helper.UserDataStore

object RetrofitModule {

    private const val BASE_URL = "https://ansimi.withfriends.kr:8443/api/v1/"
//    private const val BASE_URL = "http://192.168.1.100:8080/api/v1/"
    private const val SEARCH_API_BASE = "https://dapi.kakao.com/"

    fun provideRetrofit(context: Context): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient(context))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private fun okHttpClient(context: Context): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(context))
            .build()
    }

    // kakao

    fun provideSearchOkHttpClient(): OkHttpClient {
        // JSON을 포맷팅하는 함수
        fun formatJson(json: String): String {
            return try {
                val gson = GsonBuilder().setPrettyPrinting().create()
                val jsonElement = gson.fromJson(json, Any::class.java)
                gson.toJson(jsonElement)
            } catch (e: Exception) {
                json  // JSON 파싱에 실패한 경우 원래 문자열 그대로 반환
            }
        }

        val loggingInterceptor = HttpLoggingInterceptor { message ->
            Log.d("HttpLogging", formatJson(message)) // "HttpLogging"이라는 태그로 로그 출력
        }.apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .build()
    }

    fun provideRetrofitSearchRegion(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .baseUrl(SEARCH_API_BASE)
            .build()
    }


    class AuthInterceptor(private val context: Context) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val authToken = UserDataStore.getToken(context)
            // TODO Token이 없는경우?
            val originalRequest = chain.request()
            val newRequest = originalRequest.newBuilder()
                .addHeader("Authorization", authToken)
                .build()
            return chain.proceed(newRequest)
        }
    }
}