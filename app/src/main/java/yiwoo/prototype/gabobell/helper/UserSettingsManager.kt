package yiwoo.prototype.gabobell.helper

import android.content.Context

object UserSettingsManager {

    // private const val KEY_USER = "user"
    private const val KEY_EMERGENCY_FORMAT = "emergency_format"

    /*
    private val apiInfo: Array<Pair<String, String>> = arrayOf(
        "283697cc-8e00-40da-947f-058ece8af8aa" to "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJ1c2VyVHlwZSI6IlVTRVIiLCJqdGkiOiJkNjM3N2VjODdmNDQ0ODBkYjhmZjI3MmVjZGJkYzQ4Nzk3NjgiLCJpc3MiOiJZSVdPTy1BUEliYzlkY2QzZWQ5Iiwic3ViIjoidXNlcjAxIiwiaWF0IjoxNzI5MjE2Njk4LCJleHAiOjE3MzE4MDg2OTh9.wx106Tadiy5tyAA3lquMHJKcJApPVSPPknKk_DnZIs0",
        "0df40874-482b-41ee-9d4a-dcd75f1233e6" to "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJ1c2VyVHlwZSI6IlVTRVIiLCJqdGkiOiIzMjFiODQ4MmRmMjU0NzQ1OGQ4OTJkYmE1ZTcyMDZjMDEwMDQ2IiwiaXNzIjoiWUlXT08tQVBJNzU1NTE3N2NhYSIsInN1YiI6InVzZXIwMiIsImlhdCI6MTcyOTIxNzA1NSwiZXhwIjoxNzMxODA5MDU1fQ.a3fRtxmpc4RmU7xJO1hf4dUSoqkyjritB2f7yjqqwqc",
        "619e4ad1-3868-47a5-9b41-b039bcac434c" to "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJ1c2VyVHlwZSI6IlVTRVIiLCJqdGkiOiJhODJjMWMwMjAyY2I0ZDc2ODM5MTY5MWY5ODRiOTVlMjIwNzEiLCJpc3MiOiJZSVdPTy1BUEkxMzk2MjcwNTcyIiwic3ViIjoidXNlcjAzIiwiaWF0IjoxNzI5MjE3MTkxLCJleHAiOjE3MzE4MDkxOTF9.iXt6m-3du0pnw9OvPLT_MrrwbUEoIBsNO4lDWU5a5bo"
    )
    */

    enum class EmergencyFormatType(val value: Int) {
        NONE(0), PHOTO(1), VIDEO(2)
    }

    /*
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
    */

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