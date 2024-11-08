package yiwoo.prototype.gabobell.data.network

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import yiwoo.prototype.gabobell.api.dto.request.CheckUserAccountRequest
import yiwoo.prototype.gabobell.constants.CheckAccountConstants
import yiwoo.prototype.gabobell.helper.ApiProvider

class UserAccountCheckClient(private val context: Context) {
    private val gaboApi = ApiProvider.provideGaboApi(context)

    suspend fun checkUserAccountDuplicate(
        username: String,
        onSuccess: (String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        try {
            val response = withContext(Dispatchers.IO) {
                gaboApi.checkUserAccountDuplicate(
                    checkUserAccountRequest = CheckUserAccountRequest(
                        username
                    )
                )
            }
            if (response.isSuccessful) {
                val resultStatus = response.body()?.data?.code ?: ""
                val message = when (resultStatus) {
                    "0000" -> CheckAccountConstants.NO_ACCOUNT_REDUNDANCY // 신규
                    "1000" -> CheckAccountConstants.ACCOUNT_REDUNDANCY // 기존
                    else -> "Unknown status"
                }
                onSuccess(message)
            } else {
                onFailure("Error: ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            onFailure("Network Error: ${e.localizedMessage}")
        }
    }
}