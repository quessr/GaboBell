package yiwoo.prototype.gabobell.api

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query
import yiwoo.prototype.gabobell.api.dto.response.SearchResponse

interface KakaoAPI {
    @GET("v2/local/search/keyword.json")
    suspend fun getSearch(
        @Header("Authorization") authorization: String,
        @Query("query") query: String,
    ): SearchResponse
}