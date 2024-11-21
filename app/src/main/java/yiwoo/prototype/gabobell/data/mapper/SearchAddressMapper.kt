package yiwoo.prototype.gabobell.data.mapper

import yiwoo.prototype.gabobell.api.dto.response.SearchDocumentsResponse
import yiwoo.prototype.gabobell.ui.searchAddress.model.SearchAddressModel

object SearchAddressMapper {
    fun SearchDocumentsResponse.toSearchAddressModel(): SearchAddressModel {
        return SearchAddressModel(
            placeName = this.place_name ?: "Unknown Place",
            addressName = this.address_name ?: "Unknown Address",
            addressRoadName = this.road_address_name ?: "Unknown Road"
        )
    }
}