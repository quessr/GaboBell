
package yiwoo.prototype.gabobell.api.dto.response

data class ModifyUserResponse(
    val uuid: String,
    val user: ModifyUserInfo
)

data class ModifyUserInfo(
    val username: String?,
    val userPassword: String?,
    val name: String?,
    val phoneNumber: String?,
    val birth: String?,
    val gender: String?,
    val nationality: String?,
    val jachigu: String?,
    val terms: String?,
    val younger: String?,
    val userStatus: String?,
    val pushToken: String?
)
