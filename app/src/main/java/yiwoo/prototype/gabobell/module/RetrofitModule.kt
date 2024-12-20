package yiwoo.prototype.gabobell.module

import android.content.Context
import android.util.Log
import com.google.gson.GsonBuilder
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import yiwoo.prototype.gabobell.BuildConfig
import yiwoo.prototype.gabobell.api.dto.request.LogInRequest
import yiwoo.prototype.gabobell.helper.ApiProvider
import yiwoo.prototype.gabobell.helper.UserDataStore

object RetrofitModule {

    private const val BASE_URL = "https://ansimi.withfriends.kr:8443/api/v1/"
    private const val BASE_DEV_URL = "https://ansimi-dev.withfriends.kr:8443/api/v1/"
//    private const val BASE_DEV_URL = BASE_URL

    //    private const val BASE_URL = "http://192.168.1.100:8080/api/v1/"
    private const val SEARCH_API_BASE = "https://dapi.kakao.com/"

    private fun getBaseUrl() = if (BuildConfig.DEBUG_MODE) BASE_DEV_URL else BASE_URL

    fun provideRetrofit(context: Context): Retrofit {
        return Retrofit.Builder()
            .baseUrl(getBaseUrl())
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
            val originalRequest = chain.request()
            val newRequest = originalRequest.newBuilder()
                .addHeader("Authorization", "Bearer $authToken")
                .build()

            var response = chain.proceed(newRequest)

            if (response.code == 403) {
                Log.d("Retrofit@@", response.code.toString())
                response.close() // 기존 응답 닫기
                synchronized(this) {
                    // 토큰 재발급 요청
                    refreshToken { newToken ->
                        // 새 토큰을 사용해 요청을 재시도
                        val retryRequest = originalRequest.newBuilder()
                            .addHeader("Authorization", "Bearer $newToken")
                            .build()
                        Log.d("Retrofit@@-------->", newToken)
                        response = chain.proceed(retryRequest)  // 새 요청 보내기
                    }
                }
            }
            return response
        }
        private fun refreshToken(onTokenRefreshed: (String) -> Unit) {
            val gaboApi = ApiProvider.provideGaboApi(context)

            Log.d("Retrofit@@", "refreshToken()-------->")
            val response = runBlocking {
                try {
                    gaboApi.loginInUser(
                        logInRequest = LogInRequest(
                            username = UserDataStore.getUserId(context),
                            password = UserDataStore.getUserPassWord(context)
                        )
                    )
                } catch (e: Exception) {
                    throw RuntimeException("토큰 갱신 실패", e)
                }
            }
            if (response.isSuccessful) {
                val newToken = response.body()?.data?.token ?: ""
                UserDataStore.saveToken(context, newToken)
                Log.d("Retrofit@@: ", "response.isSuccessful: $newToken")
                onTokenRefreshed(newToken)

                Log.d("Retrofit@@: ", "newToken: $newToken")
            } else {
                Log.d("Retrofit@@: ", "error : ${response.errorBody()?.string()}")
            }

        }
    }
}