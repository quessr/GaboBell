package yiwoo.prototype.gabobell.ui.searchAddress

import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import yiwoo.prototype.gabobell.databinding.ItemSearchAddressBinding
import yiwoo.prototype.gabobell.ui.searchAddress.model.SearchAddressModel

class SearchAddressViewHolder(private val binding: ItemSearchAddressBinding) :
    RecyclerView.ViewHolder(binding.root) {

    fun onBind(model: SearchAddressModel) {
        with(binding) {
            tvAddressMain.text = model.addressMain
            tvAddressLotNumber.text = model.addressLotNumber
            tvAddressRoad.text = model.addressRoad
        }
    }
}