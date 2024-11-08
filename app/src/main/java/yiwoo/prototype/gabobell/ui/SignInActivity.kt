package yiwoo.prototype.gabobell.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.util.Utility
import com.kakao.sdk.user.UserApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import yiwoo.prototype.gabobell.constants.CheckAccountConstants
import yiwoo.prototype.gabobell.data.network.LogInUserClient
import yiwoo.prototype.gabobell.data.network.UserAccountCheckClient
import yiwoo.prototype.gabobell.databinding.ActivitySignInBinding
import yiwoo.prototype.gabobell.helper.UserDataStore

class SignInActivity : BaseActivity<ActivitySignInBinding>(ActivitySignInBinding::inflate) {
    private val userAccountClient = UserAccountCheckClient(this)
    private val logInUpUserClient = LogInUserClient(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.btnKakaoLogIn.setOnClickListener() {
            kakaoLogin()
        }

        val keyHash = Utility.getKeyHash(this)
        Log.d("MainActivity@@", "keyHash: $keyHash")
    }

    private fun kakaoLogin() {
        // 카카오톡이 설치되어 있는 경우 카카오톡으로 로그인, 그렇지 않은 경우 카카오 계정으로 로그인
        if (UserApiClient.instance.isKakaoTalkLoginAvailable(this)) {
            UserApiClient.instance.loginWithKakaoTalk(this) { token, error ->
                CoroutineScope(Dispatchers.IO).launch {
                    handleLoginResult(token, error)
                }
            }
        } else {
            UserApiClient.instance.loginWithKakaoAccount(this) { token, error ->
                CoroutineScope(Dispatchers.IO).launch {
                    handleLoginResult(token, error)
                }
            }
        }
    }

    private suspend fun handleLoginResult(token: OAuthToken?, error: Throwable?) {
        if (error != null) {
            Log.d("SignInActivity", "카카오 로그인 실패: $error")
            Toast.makeText(this, "카카오 로그인 실패: ${error.message}", Toast.LENGTH_SHORT).show()
        } else if (token != null) {
            Log.d("SignInActivity", "카카오 로그인 성공: ${token.accessToken}")
            fetchKakaoUserInfo()
        }
    }

    private suspend fun fetchKakaoUserInfo() {
        UserApiClient.instance.me { user, error ->
            if (error != null) {
                Log.d("SignInActivity", "사용자 정보 요청 실패", error)
            } else if (user != null) {
                Log.d("SignInActivity", "사용자 정보 요청 성공: ${user.id}")
                // 사용자 정보를 활용한 코드 추가
                val username = "K_${user.id}"
                Log.d("SignInActivity", "username: $username")

                CoroutineScope(Dispatchers.IO).launch {
                    userAccountClient.checkUserAccountDuplicate(
                        username = username,
                        onSuccess = { message ->
                            Log.d("SignInActivity", "userAccountClient message: $message")

                            val intent = when (message) {
                                CheckAccountConstants.NO_ACCOUNT_REDUNDANCY ->
                                    Intent(
                                        this@SignInActivity,
                                        MembershipActivity::class.java
                                    ).apply {
                                        putExtra("USERNAME", username)
                                    }

                                CheckAccountConstants.ACCOUNT_REDUNDANCY -> {

                                    CoroutineScope(Dispatchers.IO).launch {
                                        logInUpUserClient.logInUser(
                                            username = username,
                                            password = username,
                                            onSuccess = { uuid, token ->
                                                Log.d(
                                                    "TOKEN@@",
                                                    "ACCOUNT_REDUNDANCY uuid: $uuid, token: $token"
                                                )
                                                UserDataStore.saveUUID(this@SignInActivity, uuid)
                                                UserDataStore.saveToken(this@SignInActivity, token)

                                                Log.d(
                                                    "SignInActivity@@",
                                                    "ACCOUNT_REDUNDANCY logInUpUserClient onSuccess"
                                                )
                                            },
                                            onFailure = { error ->
                                                Log.d(
                                                    "SignInActivity@@",
                                                    "ACCOUNT_REDUNDANCY logInUpUserClient onFailure: $error"
                                                )
                                            }
                                        )
                                    }
                                    Intent(this@SignInActivity, MainActivity::class.java)
                                }

                                else -> null
                            }
                            intent?.let {
                                startActivity(it)
                            }
                            Log.d("SignInActivity", "userAccountClient onSuccess")
                        },
                        onFailure = { error ->
                            Log.d("SignInActivity", "userAccountClient onFailure: $error")
                        })
                }
            }
        }
    }


}