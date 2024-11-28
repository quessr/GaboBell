package yiwoo.prototype.gabobell.ui.searchAddress

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import yiwoo.prototype.gabobell.data.mapper.SearchAddressMapper.toSearchAddressModel
import yiwoo.prototype.gabobell.data.network.SearchAddressClient
import yiwoo.prototype.gabobell.databinding.ActivitySearchAddressBinding
import yiwoo.prototype.gabobell.ui.BaseActivity
import yiwoo.prototype.gabobell.ui.MonitoringActivity
import yiwoo.prototype.gabobell.ui.searchAddress.model.SearchAddressModel

class SearchAddressActivity :
    BaseActivity<ActivitySearchAddressBinding>(ActivitySearchAddressBinding::inflate) {
    private val searchAddressClient = SearchAddressClient()
    private var searchQuery: String = ""
    private var searchPlaceLat: Double = 0.0
    private var searchPlaceLon: Double = 0.0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val adapter = SearchAddressAdapter { placeName ->
            val isDeparture = intent.getBooleanExtra("is_departure", true)
            val resultIntent = Intent().apply {
                putExtra("selected_place_name", placeName)
                putExtra("is_departure", isDeparture)
                putExtra("search_place_lat", searchPlaceLat)
                putExtra("search_place_long", searchPlaceLon)
            }
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }
        binding.rvAddressSearch.adapter = adapter
        binding.tvBtnSearch.setOnClickListener {
            searchQuery = binding.etDeparture.text.toString()
            Log.d("SearchAddressActivity@@", "editText: $searchQuery")

            CoroutineScope(Dispatchers.Main).launch {
//                Log.d("SearchAddressActivity@@", "searchAddress: ${document.address}")
                val response = searchAddressClient.searchAddress(query = searchQuery)
                val documents = response?.documents ?: emptyList()
                val searchAddressModels: List<SearchAddressModel> =
                    documents.map {
                        searchPlaceLat = it.y
                        searchPlaceLon = it.x
                        it.toSearchAddressModel()
                    }

                Log.d("SearchAddressActivity", "documents: $documents")
                adapter.submitList(searchAddressModels)
            }
        }

//        val mockData = listOf(
//            SearchAddressModel("서울특별시 강남구 삼성동", "123-456", "서울특별시 강남구 삼성로 123"),
//            SearchAddressModel("서울특별시 서초구 서초동", "789-012", "서울특별시 서초구 서초대로 456"),
//            SearchAddressModel("서울특별시 종로구 청운동", "345-678", "서울특별시 종로구 자하문로 789"),
//            SearchAddressModel("서울특별시 강남구 삼성동", "123-456", "서울특별시 강남구 삼성로 123"),
//            SearchAddressModel("서울특별시 서초구 서초동", "789-012", "서울특별시 서초구 서초대로 456"),
//            SearchAddressModel("서울특별시 종로구 청운동", "345-678", "서울특별시 종로구 자하문로 789")
//        )

//        adapter.submitList(mockData)
    }
}