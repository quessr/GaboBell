package yiwoo.prototype.gabobell.ui.searchAddress

import androidx.recyclerview.widget.RecyclerView
import yiwoo.prototype.gabobell.databinding.ItemSearchAddressBinding
import yiwoo.prototype.gabobell.ui.searchAddress.model.SearchAddressModel

class SearchAddressViewHolder(
    private val binding: ItemSearchAddressBinding,
    private val onPlaceSelected: (String) -> Unit
) :
    RecyclerView.ViewHolder(binding.root) {

    fun onBind(model: SearchAddressModel) {
        with(binding) {
            tvPlaceName.text = model.placeName
            tvAddressName.text = model.addressName
            tvAddressRoadName.text = model.addressRoadName
            tvBtnChoose.setOnClickListener {
                onPlaceSelected(model.placeName)
            }
        }
    }
}