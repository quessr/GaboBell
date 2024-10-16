package yiwoo.prototype.gabobell.helper

import android.content.Context

object UserDeviceManager {
    private const val KEY = "ring_my_bell"

    // 기기 등록
    fun registerDevice(context: Context, deviceName: String, address: String) {
        PreferenceManager(context)
            .setString(KEY, "$deviceName|$address")
    }

    // 기기 해제
    fun deleteDevice(context: Context) {
        PreferenceManager(context)
            .setString(KEY, "")
    }

    // 기기 등록 여부
    fun isRegister(context: Context): Boolean {
        return getValues(context) != ""
    }

    // 디바이스 네임 반환
    fun getDeviceName(context: Context): String {
        val parts = getValues(context).split("|")
        if (parts.size == 2) {
            return parts[0]
        }
        return ""
    }

    // Mac address 반환
    fun getAddress(context: Context): String {
        val parts = getValues(context).split("|")
        if (parts.size == 2) {
            return parts[1]
        }
        return ""
    }

    // 저장 값 반환
    private fun getValues(context: Context): String {
        return PreferenceManager(context).getString(KEY)
    }
}