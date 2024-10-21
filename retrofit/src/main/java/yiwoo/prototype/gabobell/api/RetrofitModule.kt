package yiwoo.prototype.gabobell.api

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitModule {

    private const val BASE_URL = "http://192.168.1.100:8080/api/v1/"

    val authToken = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJ1c2VyVHlwZSI6IlVTRVIiLCJqdGkiOiJkNjM3N2VjODdmNDQ0ODBkYjhmZjI3MmVjZGJkYzQ4Nzk3NjgiLCJpc3MiOiJZSVdPTy1BUEliYzlkY2QzZWQ5Iiwic3ViIjoidXNlcjAxIiwiaWF0IjoxNzI5MjE2Njk4LCJleHAiOjE3MzE4MDg2OTh9.wx106Tadiy5tyAA3lquMHJKcJApPVSPPknKk_DnZIs0"

    fun provideRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor(authToken))
        .build()

    class AuthInterceptor(private val authToken: String) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val originalRequest = chain.request()
            val newRequest = originalRequest.newBuilder()
                .addHeader("Authorization", authToken)
                .build()
            return chain.proceed(newRequest)
        }
    }

}