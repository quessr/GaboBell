package yiwoo.prototype.gabobell.ui.searchAddress

import android.os.Bundle
import yiwoo.prototype.gabobell.databinding.ActivitySearchAddressBinding
import yiwoo.prototype.gabobell.ui.BaseActivity
import yiwoo.prototype.gabobell.ui.searchAddress.model.SearchAddressModel

class SearchAddressActivity :
    BaseActivity<ActivitySearchAddressBinding>(ActivitySearchAddressBinding::inflate) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val adapter = SearchAddressAdapter()
        binding.rvAddressSearch.adapter = adapter

        val mockData = listOf(
            SearchAddressModel("서울특별시 강남구 삼성동", "123-456", "서울특별시 강남구 삼성로 123"),
            SearchAddressModel("서울특별시 서초구 서초동", "789-012", "서울특별시 서초구 서초대로 456"),
            SearchAddressModel("서울특별시 종로구 청운동", "345-678", "서울특별시 종로구 자하문로 789"),
            SearchAddressModel("서울특별시 강남구 삼성동", "123-456", "서울특별시 강남구 삼성로 123"),
            SearchAddressModel("서울특별시 서초구 서초동", "789-012", "서울특별시 서초구 서초대로 456"),
            SearchAddressModel("서울특별시 종로구 청운동", "345-678", "서울특별시 종로구 자하문로 789")
        )

        adapter.submitList(mockData)
    }
}