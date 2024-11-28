package yiwoo.prototype.gabobell.ui.searchAddress


import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import yiwoo.prototype.gabobell.databinding.ItemSearchAddressBinding
import yiwoo.prototype.gabobell.ui.searchAddress.model.SearchAddressModel

class SearchAddressAdapter( private val onPlaceSelected: (String) -> Unit) :
    ListAdapter<SearchAddressModel, SearchAddressViewHolder>(diffUtil) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchAddressViewHolder {
        val binding =
            ItemSearchAddressBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SearchAddressViewHolder(binding, onPlaceSelected)
    }

    override fun onBindViewHolder(holder: SearchAddressViewHolder, position: Int) {
        holder.onBind(getItem(position))
    }

    companion object {
        private val diffUtil = object : DiffUtil.ItemCallback<SearchAddressModel>() {
            override fun areItemsTheSame(
                oldItem: SearchAddressModel,
                newItem: SearchAddressModel
            ): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(
                oldItem: SearchAddressModel,
                newItem: SearchAddressModel
            ): Boolean {
                return oldItem == newItem
            }

        }
    }
}