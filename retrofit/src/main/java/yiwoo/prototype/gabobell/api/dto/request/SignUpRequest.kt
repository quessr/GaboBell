package yiwoo.prototype.gabobell.api.dto.request

data class SignUpRequest(
    val user: UserDetails
)

data class UserDetails(
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
