package yiwoo.prototype.gabobell.helper

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object TokenStore {
    private val TOKEN_KEY = "token"
    private val PREFERENCE_NAME = "encrypted_preferences"

    private fun getSharedPreferences(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            PREFERENCE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    // 데이터 저장하기
    fun saveToken(context: Context, token: String) {
        val sharedPreferences = getSharedPreferences(context)
        with(sharedPreferences.edit()) {
            putString(TOKEN_KEY, token)
            commit()
        }
    }

    // 데이터 가져오기
    fun getToken(context: Context): String {
        val sharedPreferences = getSharedPreferences(context)
        return sharedPreferences.getString(TOKEN_KEY, "") ?: ""
    }

    // EncryptedSharedPreferences 제거하기
    fun removeToken(context: Context) {
        val sharedPreferences = getSharedPreferences(context)
        with(sharedPreferences.edit()) {
            remove(TOKEN_KEY)
            apply()
        }
    }
}