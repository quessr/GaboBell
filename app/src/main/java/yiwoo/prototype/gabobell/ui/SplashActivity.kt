package yiwoo.prototype.gabobell.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import yiwoo.prototype.gabobell.R
import yiwoo.prototype.gabobell.helper.UserDataStore

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        setContentView(R.layout.activity_splash)
        Handler(Looper.getMainLooper()).postDelayed({
            moveToScreen()
        }, 3_000)
    }

    private fun moveToScreen() {
        // 앱내 서비스 토큰 저장 여부를 체크 후 그에따른 화면 이동 (로그그인화면 or 메인화면)
        val token = UserDataStore.getToken(this)

        val intent: Intent = if (token == "") {
            // 토큰 없는 경우 (회원가입을 한 적 없는 유저) -> 회원가입 화면 이동
            Intent(this, SignInActivity::class.java)
        } else {
            // 토큰 있는 경우 (기존에 회원가입을 한 유저) -> 메인 화면 이동
            // 추후 token에 유효시간이 추가되면, 로직이 추가 될 예정
            Intent(this, MainActivity::class.java)
        }
        startActivity(intent)
        finish()
    }
}