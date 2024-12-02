package yiwoo.prototype.gabobell.api.dto.response

data class PoliceResponse(
    val result: PoliceResultData,
    val data: PoliceData
)

data class PoliceResultData(
    val status: String,
    val message: String
)

data class PoliceData(
    val totalElements: Long,
    val results: List<PoliceResultItem>
)

data class PoliceResultItem(
    val id: Long,
    val subStation: String,
    val division: String,
    val phoneNumber: String,
    val address: String,
    val latitude: Double,
    val longitude: Double
)
