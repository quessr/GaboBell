package yiwoo.prototype.gabobell.module

import android.content.Context
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import yiwoo.prototype.gabobell.helper.Logger
import yiwoo.prototype.gabobell.helper.UserSettingsManager
import yiwoo.prototype.gabobell.module.RetrofitModule.okHttpClient

object RetrofitModule {

    private const val BASE_URL = "http://192.168.1.100:8080/api/v1/"

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


    class AuthInterceptor(private val context: Context) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val authToken = UserSettingsManager.getToken(context)
            val originalRequest = chain.request()
            val newRequest = originalRequest.newBuilder()
                .addHeader("Authorization", authToken)
                .build()
            Logger.d(authToken)
            return chain.proceed(newRequest)
        }
    }
}