package yiwoo.prototype.gabobell.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import yiwoo.prototype.gabobell.data.network.LogInUserClient
import yiwoo.prototype.gabobell.data.network.SignUpUserClient
import yiwoo.prototype.gabobell.databinding.ActivityMembershipBinding
import yiwoo.prototype.gabobell.helper.UserDataStore

class MembershipActivity :
    BaseActivity<ActivityMembershipBinding>(ActivityMembershipBinding::inflate) {
    private val signUpUserClient = SignUpUserClient(this)
    private val logInUpUserClient = LogInUserClient(this)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val username = intent.getStringExtra("USERNAME") ?: ""

        binding.tvUserId.text = username

        binding.tvBtnRegister.setOnClickListener {
            val userDetails = mapOf(
                "username" to username,
                "nickname" to binding.tilNickname.editText?.text.toString(),
                "phoneNumber" to binding.tilPhoneNumber.editText?.text.toString(),
                "birthDate" to binding.tilBirthDate.editText?.text.toString(),
                "gender" to binding.tilGender.editText?.text.toString(),
                "nationality" to binding.tilNationality.editText?.text.toString(),
                "district" to binding.tilDistrict.editText?.text.toString()
            )

            CoroutineScope(Dispatchers.IO).launch {
                signUpUserClient.signUpUser(
                    userDetails = userDetails,
                    onSuccess = { username, userPassword ->
                        CoroutineScope(Dispatchers.IO).launch {
                            logInUpUserClient.logInUser(
                                username = username,
                                password = userPassword,
                                onSuccess = { uuid, token ->
                                    Log.d("TOKEN@@", "uuid: $uuid, token: $token")
                                    UserDataStore.saveUUID(this@MembershipActivity, uuid)
                                    UserDataStore.saveToken(this@MembershipActivity, token)

                                    Log.d("MembershipActivity@@", "logInUpUserClient onSuccess")

                                    val intent =
                                        Intent(this@MembershipActivity, MainActivity::class.java)
                                    startActivity(intent)
                                },
                                onFailure = { error ->
                                    Log.d(
                                        "MembershipActivity@@",
                                        "logInUpUserClient onFailure: $error"
                                    )
                                }
                            )
                        }
                    },
                    onFailure = { error ->
                        Log.d("MembershipActivity@@", "onFailure error: $error")
                    })
            }
        }
    }
}