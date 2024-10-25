package yiwoo.prototype.gabobell.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import yiwoo.prototype.gabobell.R

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
        // TODO: 앱내 서비스 토큰 저장 여부를 체크 필요 (인증화면 or 메인화면)
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}