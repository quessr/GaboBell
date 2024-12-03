package yiwoo.prototype.gabobell.api.dto.request

data class ModifyUserRequest(
    val user: ModifyUserDetails
)

data class ModifyUserDetails(
    var username: String? = null,
    var userPassword: String? = null,
    var name: String? = null,
    var phoneNumber: String? = null,
    var birth: String? = null,
    var gender: String? = null,
    var nationality: String? = null,
    var jachigu: String? = null,
    var terms: String? = null,
    var younger: String? = null,
    var userStatus: String? = null,
    var pushToken: String? = null
)
