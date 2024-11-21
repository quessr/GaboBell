package yiwoo.prototype.gabobell.api.dto.response

data class SearchResponse(
    val documents: List<SearchDocumentsResponse>
)

data class SearchDocumentsResponse(
    val place_name: String? = "",
    val address_name: String? = "",
    val road_address_name: String? = "",
    val x: Double,
    val y: Double,
)

//data class Address(
//    val addressName: String?,
//    val x: Double?,
//    val y: Double?,
//)

