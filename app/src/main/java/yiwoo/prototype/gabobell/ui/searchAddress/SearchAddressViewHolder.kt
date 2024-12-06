package yiwoo.prototype.gabobell.ui.searchAddress

import androidx.recyclerview.widget.RecyclerView
import yiwoo.prototype.gabobell.R
import yiwoo.prototype.gabobell.databinding.ItemSearchAddressBinding
import yiwoo.prototype.gabobell.ui.searchAddress.model.SearchAddressModel

class SearchAddressViewHolder(
    private val binding: ItemSearchAddressBinding,
    private val onPlaceSelected: (String, Double, Double) -> Unit
) :
    RecyclerView.ViewHolder(binding.root) {

    fun onBind(model: SearchAddressModel) {
        with(binding) {
            tvPlaceName.text = model.placeName
            tvAddressName.text =
                itemView.context.getString(R.string.address_name, model.addressName)
            tvAddressRoadName.text =
                itemView.context.getString(R.string.address_road_name, model.addressRoadName)
            tvBtnChoose.setOnClickListener {
                onPlaceSelected(model.placeName, model.x, model.y)
            }
        }
    }
}