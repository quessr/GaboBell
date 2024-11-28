package yiwoo.prototype.gabobell.helper

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object UserDataStore {
    private const val TOKEN_KEY = "token"
    private const val UUID_KEY = "uuid"
    private const val PREFERENCE_NAME = "encrypted_preferences"

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

    // 토근 저장하기
    fun saveToken(context: Context, token: String) {
        val sharedPreferences = getSharedPreferences(context)
        with(sharedPreferences.edit()) {
            putString(TOKEN_KEY, token)
            commit()
        }
    }

    // UUID 저장하기
    fun saveUUID(context: Context, uuid: String) {
        val sharedPreferences = getSharedPreferences(context)
        with(sharedPreferences.edit()) {
            putString(UUID_KEY, uuid)
            commit()
        }
    }

    // 토큰 가져오기
    fun getToken(context: Context): String {
        val sharedPreferences = getSharedPreferences(context)
        val aaaa = sharedPreferences.getString(TOKEN_KEY, "") ?: ""
        Log.d("@!@", ">>tt>>> $aaaa")
        return aaaa
    }

    // UUID 가져오기
    fun getUUID(context: Context): String {
        val sharedPreferences = getSharedPreferences(context)
        return sharedPreferences.getString(UUID_KEY, "") ?: ""
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