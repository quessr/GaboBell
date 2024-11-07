package yiwoo.prototype.gabobell.data.network

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import yiwoo.prototype.gabobell.api.dto.request.SignUpRequest
import yiwoo.prototype.gabobell.api.dto.request.UserDetails
import yiwoo.prototype.gabobell.helper.ApiProvider

class SignUpUserClient(private val context: Context) {
    private val gaboApi = ApiProvider.provideGaboApi(context)

    suspend fun signUpUser(
        userDetails: Map<String, String>,
        onSuccess: (String, String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val username = "김테스트2"
        val userPassword = "김테스트1"
        val name = "김테스트1"
        val phoneNumber = "01012349992"
        val birthDate = "7609"
        val gender = "M"
        val nationality = "local"
        val district = "마포구"
        val terms = "true"
        val younger = "true"
        val userStatus = "ACTIVE"

//        val username = "김가나"
//        val userPassword = "김가나"
//        val name = "김가나"
//        val phoneNumber = userDetails["phoneNumber"] ?: "01012340001"
//        val birthDate = userDetails["birthDate"] ?: "7609"
//        val gender = userDetails["gender"] ?: "M"
//        val nationality = userDetails["nationality"] ?: "local"
//        val district = userDetails["district"] ?: "마포구"
//        val terms = "true"
//        val younger = "true"
//        val userStatus = "ACTIVE"

        Log.d(
            "MembershipActivity@@",
            "username: $username, phoneNumber: $phoneNumber, birthDate: $birthDate, gender: $gender, nationality: $nationality, district: $district"
        )
        try {
            val response = withContext(Dispatchers.IO) {
                gaboApi.signUpUser(
                    signUpRequest = SignUpRequest(
                        user = UserDetails(
                            username = username,
                            userPassword = userPassword,
                            name = name,
                            phoneNumber = phoneNumber,
                            birth = birthDate,
                            gender = gender,
                            nationality = nationality,
                            jachigu = district,
                            terms = terms,
                            younger = younger,
                            userStatus = userStatus
                        )
                    )
                )
            }

            if (response.isSuccessful) {
                onSuccess(username, userPassword)
            } else {
                onFailure("Error: ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            onFailure("Network Error: ${e.localizedMessage}")
        }

    }
}