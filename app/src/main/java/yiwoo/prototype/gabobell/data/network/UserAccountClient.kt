package yiwoo.prototype.gabobell.data.network

import android.content.Context
import android.provider.SyncStateContract.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import yiwoo.prototype.gabobell.constants.CheckAccountConstants
import yiwoo.prototype.gabobell.helper.ApiProvider

class UserAccountClient(private val context: Context) {
    private val gaboApi = ApiProvider.provideGaboApi(context)

    suspend fun checkUserAccountDuplicate(
        username: String,
        onSuccess: (String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        try {
            val response = withContext(Dispatchers.IO) {
                gaboApi.checkUserAccountDuplicate(username)
            }
            if (response.isSuccessful) {
                val resultStatus = response.body()?.result?.status ?: ""
                val message = when (resultStatus) {
                    "0000" -> CheckAccountConstants.NO_ACCOUNT_REDUNDANCY
                    "0001" -> CheckAccountConstants.ACCOUNT_REDUNDANCY
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