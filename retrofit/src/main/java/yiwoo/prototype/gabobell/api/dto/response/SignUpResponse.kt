package yiwoo.prototype.gabobell.api.dto.response

data class SignUpResponse(
    val uuid: String,
    val user: UserInfo
)

data class UserInfo(
    val username: String,
    val userPassword: String,
    val name: String,
    val phoneNumber: String,
    val birth: String,
    val gender: String,
    val nationality: String,
    val jachigu: String,
    val terms: String,
    val younger: String,
    val userStatus: String,
)
