package yiwoo.prototype.gabobell.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
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
    private var userName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userName = intent.getStringExtra("USERNAME") ?: ""
        initUi()
    }

    private fun initUi() {
        if (userName.isEmpty()) {
            // 일반회원
            binding.clUserPw.visibility = View.VISIBLE
            binding.tietUserId.isEnabled = true
            binding.tietUserId.setText("")

        } else {
            // 카카오
            binding.clUserPw.visibility = View.GONE
            binding.tietUserId.isEnabled = false
            binding.tietUserId.setText(userName)
        }

        binding.tvBtnRegister.setOnClickListener {
            // 카카오 경우 ID/PW 를 동일하게 설정한다. (임시방편)
            val password = if (userName.isEmpty()) {
                binding.tilUserPw.editText?.text.toString()
            } else {
                binding.tilUserId.editText?.text.toString()
            }

            // 모든 입력 필드의 값이 비어있지 않은지 확인
            val userId = binding.tilUserId.editText?.text.toString()
            val nickname = binding.tilNickname.editText?.text.toString() 
            val phoneNumber = binding.tilPhoneNumber.editText?.text.toString()
            val birthDate = binding.tilBirthDate.editText?.text.toString()
            val gender = binding.tilGender.editText?.text.toString()
            val nationality = binding.tilNationality.editText?.text.toString()
            val district = binding.tilDistrict.editText?.text.toString()

            if (userId.isEmpty() || password.isEmpty() || nickname.isEmpty() || 
                phoneNumber.isEmpty() || birthDate.isEmpty() || gender.isEmpty() ||
                nationality.isEmpty() || district.isEmpty()) {
                Toast.makeText(this@MembershipActivity, "모든 항목을 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val userDetails = mapOf(
                "username" to userId,
                "password" to password,
                "nickname" to nickname,
                "phoneNumber" to phoneNumber,
                "birthDate" to birthDate,
                "gender" to gender,
                "nationality" to nationality,
                "district" to district
            )
            // 회원가입
            signup(userDetails)
        }
    }

    private fun signup(userDetails: Map<String, String>) {
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
                                finish()
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