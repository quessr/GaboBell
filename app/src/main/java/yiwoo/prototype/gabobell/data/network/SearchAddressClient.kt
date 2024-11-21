package yiwoo.prototype.gabobell.data.network

import android.content.Context
import yiwoo.prototype.gabobell.BuildConfig
import yiwoo.prototype.gabobell.api.dto.response.SearchResponse
import yiwoo.prototype.gabobell.helper.ApiProvider

class SearchAddressClient {
    private val kakaoAPI = ApiProvider.provideKakaoApi()

    suspend fun searchAddress(
        query: String
    ): SearchResponse? {
        return try {
            kakaoAPI.getSearch(
                authorization = "KakaoAK ${BuildConfig.SEARCH_REST_API_KEY}",
                query = query
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}