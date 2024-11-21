package yiwoo.prototype.gabobell.helper

import android.content.Context
import retrofit2.Retrofit
import yiwoo.prototype.gabobell.api.GaboAPI
import yiwoo.prototype.gabobell.api.KakaoAPI
import yiwoo.prototype.gabobell.module.RetrofitModule

object ApiProvider {
    private var gaboRetrofit: Retrofit? = null
    private var kakaoRetrofit: Retrofit? = null
    private var gaboApi: GaboAPI? = null
    private var kakaoAPI: KakaoAPI? = null

    private fun provideRetrofit(context: Context): Retrofit {
        return gaboRetrofit ?: RetrofitModule.provideRetrofit(context).also { gaboRetrofit = it }
    }
    private fun provideKakaoRetrofit(): Retrofit {
        val okHttpClient = RetrofitModule.provideSearchOkHttpClient()
        return kakaoRetrofit ?: RetrofitModule.provideRetrofitSearchRegion(okHttpClient)
            .also { kakaoRetrofit = it }
    }

    fun provideGaboApi(context: Context): GaboAPI {
        return gaboApi ?: provideRetrofit(context).create(GaboAPI::class.java)
            .also { gaboApi = it }
    }

    fun provideKakaoApi(): KakaoAPI {
        return kakaoAPI ?: provideKakaoRetrofit().create(KakaoAPI::class.java)
            .also { kakaoAPI = it }
    }
}