package yiwoo.prototype.gabobell.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import yiwoo.prototype.gabobell.R
import yiwoo.prototype.gabobell.helper.NetworkUtil
import yiwoo.prototype.gabobell.helper.UserDataStore

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        setContentView(R.layout.activity_splash)

        // 버전 네임 표기
        findViewById<TextView>(R.id.tv_version).text = getVersionName()

        if (!NetworkUtil.isAvailable(this@SplashActivity)) {
            // 네트워크 에러
            showNetworkError()
        } else {
            // 화면 이동
            Handler(Looper.getMainLooper()).postDelayed({
                moveToScreen()
            }, 3_000)
        }
    }

    private fun moveToScreen() {
        // 서비스 토큰 여부에 따라 화면 분기 (로그인 or 메인)
        val token = UserDataStore.getToken(this@SplashActivity)
        val target = if (token.isEmpty()) {
            SignInActivity::class.java
        } else {
            MainActivity::class.java
        }
        startActivity(Intent(this@SplashActivity, target))
        finish()
    }

    private fun getVersionName(): String {
        return try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            packageInfo.versionName
        } catch (e: Exception) {
            ""
        }
    }
    private fun showNetworkError() {
        AlertDialog.Builder(this)
            .setTitle(R.string.pop_network_error_title)
            .setMessage(R.string.pop_network_error_description)
            .setCancelable(false)
            .setPositiveButton(R.string.pop_btn_confirm) { _, _ ->
                finish()
            }
            .show()
    }
}