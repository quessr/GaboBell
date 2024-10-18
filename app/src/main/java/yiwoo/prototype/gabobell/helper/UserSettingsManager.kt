package yiwoo.prototype.gabobell.helper

import android.content.Context

object UserSettingsManager {

    private const val KEY_USER = "user"
    private const val KEY_EMERGENCY_FORMAT = "emergency_format"

    private val apiInfo: Array<Pair<String, String>> = arrayOf(
        "JS's uuid" to "JS's token",
        "SR's uuid" to "SR's token",
        "KB's uuid" to "KB's token"
    )

    enum class EmergencyFormatType(val value: Int) {
        NONE(0), PHOTO(1), VIDEO(2)
    }

    fun setUser(context: Context, user: Int) {
        if (user < 0 || user >= apiInfo.size) {
            return
        }
        PreferenceManager(context).setInt(KEY_USER, user)
    }

    fun getUuid(context: Context): String {
        return apiInfo[getUser(context)].first
    }

    fun getToken(context: Context): String {
        return apiInfo[getUser(context)].second
    }

    private fun getUser(context: Context): Int {
        return PreferenceManager(context).getInt(KEY_USER)
    }

    fun getEmergencyFormat(context: Context): EmergencyFormatType {
        val type = PreferenceManager(context).getInt(KEY_EMERGENCY_FORMAT)
        return when (type) {
            EmergencyFormatType.PHOTO.value -> EmergencyFormatType.PHOTO
            EmergencyFormatType.VIDEO.value -> EmergencyFormatType.VIDEO
            else -> EmergencyFormatType.NONE
        }
    }

    fun setEmergencyFormat(context: Context, type: Int) {
        if (type < 0 || type >= EmergencyFormatType.values().size) {
            return
        }
        PreferenceManager(context).setInt(KEY_EMERGENCY_FORMAT, type)
    }
}