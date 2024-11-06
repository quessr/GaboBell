package yiwoo.prototype.gabobell.helper

import android.content.Context
import retrofit2.Retrofit
import yiwoo.prototype.gabobell.api.GaboAPI
import yiwoo.prototype.gabobell.module.RetrofitModule

object ApiProvider {
    private var retrofit: Retrofit? = null
    private var gaboApi: GaboAPI? = null

    private fun provideRetrofit(context: Context): Retrofit {
        return retrofit ?: RetrofitModule.provideRetrofit(context).also { retrofit = it }
    }

    fun provideGaboApi(context: Context): GaboAPI {
        return gaboApi ?: provideRetrofit(context).create(GaboAPI::class.java)
            .also { gaboApi = it }
    }
}