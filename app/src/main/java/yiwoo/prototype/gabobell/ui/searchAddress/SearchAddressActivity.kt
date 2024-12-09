package yiwoo.prototype.gabobell.ui.searchAddress

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import yiwoo.prototype.gabobell.data.mapper.SearchAddressMapper.toSearchAddressModel
import yiwoo.prototype.gabobell.data.network.SearchAddressClient
import yiwoo.prototype.gabobell.databinding.ActivitySearchAddressBinding
import yiwoo.prototype.gabobell.ui.BaseActivity
import yiwoo.prototype.gabobell.ui.searchAddress.model.SearchAddressModel

class SearchAddressActivity :
    BaseActivity<ActivitySearchAddressBinding>(ActivitySearchAddressBinding::inflate) {
    private val searchAddressClient = SearchAddressClient()
    private lateinit var searchAddressAdapter: SearchAddressAdapter
    private var searchQuery: String = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initUi()
    }

    private fun initUi() {
        searchAddressAdapter = SearchAddressAdapter { placeName, longitude, latitude ->
            val isDeparture = intent.getBooleanExtra("is_departure", true)
            val resultIntent = Intent().apply {
                putExtra("selected_place_name", placeName)
                putExtra("is_departure", isDeparture)
                putExtra("search_place_longitude", longitude)
                putExtra("search_place_latitude", latitude)
            }
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }

        with(binding) {
            rvAddressSearch.run {
                adapter = searchAddressAdapter
                addItemDecoration(SearchAddressItemDecoration())
            }

            tvBtnSearch.setOnClickListener {
                searchAddress()
            }

            btnClose.setOnClickListener {
                finish()
            }

            etSearchAddress.apply {
                requestFocus()
                setOnKeyListener { _, keyCode, event ->
                    if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN) {
                        searchAddress()
                        return@setOnKeyListener true
                    } else false
                }
            }
        }
    }

    private fun searchAddress() {
        searchQuery = binding.etSearchAddress.text.toString()
        Log.d("SearchAddressActivity@@", "editText: $searchQuery")

        CoroutineScope(Dispatchers.Main).launch {
            val response = searchAddressClient.searchAddress(query = searchQuery)
            val documents = response?.documents ?: emptyList()
            val searchAddressModels: List<SearchAddressModel> =
                documents.map {
                    it.toSearchAddressModel()
                }

            Log.d("SearchAddressActivity", "documents: $documents")
            searchAddressAdapter.submitList(searchAddressModels)
        }

    }
}