package yiwoo.prototype.gabobell.data.network

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import yiwoo.prototype.gabobell.api.dto.request.LogInRequest
import yiwoo.prototype.gabobell.helper.ApiProvider

class LogInUserClient(private val context: Context) {
    private val gaboApi = ApiProvider.provideGaboApi(context)

    suspend fun logInUser(
        username: String,
        password: String,
        onSuccess: (String, String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        try {
            val response = withContext(Dispatchers.IO) {
                gaboApi.loginInUser(
                    logInRequest = LogInRequest(
                        username = username,
                        password = password
                    )
                )
            }

            if (response.isSuccessful) {
                val uuid = response.body()?.data?.uuid ?: ""
                val token = response.body()?.data?.token ?: ""

                onSuccess(uuid, token)
            } else {
                onFailure("Error: ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            onFailure("Network Error: ${e.localizedMessage}")
        }
    }
}