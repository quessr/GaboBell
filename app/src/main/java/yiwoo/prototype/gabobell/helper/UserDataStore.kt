package yiwoo.prototype.gabobell.helper

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object UserDataStore {
    private const val TOKEN_KEY = "token"
    private const val UUID_KEY = "uuid"
    private const val PREFERENCE_NAME = "encrypted_preferences"
    private const val PUSH_TOKEN_KEY = "push"
    private const val USER_ID_KEY = "user_id"
    private const val USER_PW_KEY = "user_pw"

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

    //아이디 저장
    fun saveUserID(context: Context, userID: String) {
        val sharedPreferences = getSharedPreferences(context)
        with(sharedPreferences.edit()) {
            putString(USER_ID_KEY, userID)
            commit()
        }
    }
    //패스워드 저장
    fun saveUserPassWord(context: Context, userPW: String) {
        val sharedPreferences = getSharedPreferences(context)
        with(sharedPreferences.edit()) {
            putString(USER_PW_KEY, userPW)
            commit()
        }
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

    // FCM(Push) 토큰 저장하기
    fun savePushToken(context: Context, token: String) {
        val sharedPreferences = getSharedPreferences(context)
        with(sharedPreferences.edit()) {
            putString(PUSH_TOKEN_KEY, token)
            commit()
        }
    }

    // 토큰 가져오기
    fun getToken(context: Context): String {
        val sharedPreferences = getSharedPreferences(context)
        return sharedPreferences.getString(TOKEN_KEY, "") ?: ""
    }

    // UUID 가져오기
    fun getUUID(context: Context): String {
        val sharedPreferences = getSharedPreferences(context)
        return sharedPreferences.getString(UUID_KEY, "") ?: ""
    }

    // FCM(Push) 토큰 가져오기
    fun getPushToken(context: Context): String {
        val sharedPreferences = getSharedPreferences(context)
        return sharedPreferences.getString(PUSH_TOKEN_KEY, "") ?: ""
    }


    //아이디 가져오기
    fun getUserId(context: Context): String {
        val sharedPreferences = getSharedPreferences(context)
        return sharedPreferences.getString(USER_ID_KEY, "") ?:""
    }

    //패스워드 가져오기
    fun getUserPassWord(context: Context): String {
        val sharedPreferences = getSharedPreferences(context)
        return sharedPreferences.getString(USER_PW_KEY, "") ?:""
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