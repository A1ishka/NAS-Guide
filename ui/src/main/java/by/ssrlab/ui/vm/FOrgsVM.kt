package by.ssrlab.ui.vm

import android.annotation.SuppressLint
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import by.ssrlab.common_ui.common.ui.base.vm.BaseFragmentVM
import by.ssrlab.data.data.common.DescriptionData
import by.ssrlab.data.data.remote.DepartmentFilter
import by.ssrlab.data.data.settings.remote.OrganizationLocale
import by.ssrlab.domain.repository.network.OrgsRepository
import by.ssrlab.domain.utils.Resource

class FOrgsVM(orgsRepository: OrgsRepository) : BaseFragmentVM<OrganizationLocale>(orgsRepository) {

    //Appearance
    private val _title = MutableLiveData("")
    val title: LiveData<String>
        get() = _title

    fun setTitle(value: String) {
        _title.value = value
    }

    //Data
    private val _orgsData = MutableLiveData<Resource<List<OrganizationLocale>>>()
    val orgsData: LiveData<Resource<List<OrganizationLocale>>> get() = _orgsData

    fun getDescriptionArray(): ArrayList<DescriptionData> {
        val array = arrayListOf<DescriptionData>()

        val resource = _orgsData.value
        if (resource is Resource.Success) {
            for (place in resource.data) {
                array.add(place.description)
            }
        }

        return array
    }

    private fun loadData() {
        getResourceData(
            onSuccess = { data ->
                _orgsData.value = Resource.Success(data)
            },
            onError = { errorMessage ->
                _orgsData.value = Resource.Error(errorMessage)
            },
            onLoading = {
                _orgsData.value = Resource.Loading
            }
        )
    }

    init {
        loadData()
    }

    //Filter and Search
    private val _filteredData = MutableLiveData<List<OrganizationLocale>>()
    val filteredDataList: LiveData<List<OrganizationLocale>> get() = _filteredData

    private fun setFilteredList(value: List<OrganizationLocale>) {
        _filteredData.value = value
    }

    //Search
    fun filterData(query: String) {
        val resource = _orgsData.value
        if (resource is Resource.Success) {
            val filteredList = resource.data.filter {
                it.name.contains(query, ignoreCase = true)
            }
            setFilteredList(filteredList)
        }
    }

    //Filter
    private val _isFiltering = MutableLiveData(false)
    val isFiltering: LiveData<Boolean> = _isFiltering

    private val _availableFilters = MutableLiveData<Map<DepartmentFilter, Int>>(emptyMap())
    val availableFilters: LiveData<Map<DepartmentFilter, Int>> = _availableFilters

    private val _filterList = MutableLiveData<List<DepartmentFilter>>(emptyList())
    val filterList: LiveData<List<DepartmentFilter>> = _filterList

    private val _selectedFilters = MutableLiveData<Set<DepartmentFilter>>(emptySet())
    val selectedFilters: LiveData<Set<DepartmentFilter>> = _selectedFilters

    fun setFiltering(value: Boolean) {
        _isFiltering.value = value
    }

    fun setAvailableFilters() {
        val uniqueDepartmentFilters: Set<DepartmentFilter> =
            if (_orgsData.value is Resource.Success) {
                (_orgsData.value as Resource.Success<List<OrganizationLocale>>).data
                    .map { it.description.departmentFilter }
                    .toSet()
            } else {
                emptySet()
            }

        _filterList.value = uniqueDepartmentFilters.toList()

        val departmentFilterCounts: Map<DepartmentFilter, Int> =
            uniqueDepartmentFilters.associateWith { filter ->
                _orgsData.value?.let {
                    if (it is Resource.Success) {
                        it.data.count { org -> org.description.departmentFilter == filter }
                    } else {
                        0
                    }
                } ?: 0
            }

        _availableFilters.value = departmentFilterCounts
    }

    @SuppressLint("NullSafeMutableLiveData")
    fun onFilterSelected(filter: DepartmentFilter, isSelected: Boolean) {
        val currentFilters = _selectedFilters.value?.toMutableSet()

        if (isSelected) {
            currentFilters?.add(filter)
        } else {
            currentFilters?.remove(filter)
        }
        currentFilters?.let {
            _selectedFilters.value = currentFilters
        }
    }

    fun applyFilters() {
        val filterDataByChoices =
            if (_orgsData.value is Resource.Success) {
                val currentSelectedFilters = _selectedFilters.value
                (_orgsData.value as Resource.Success<List<OrganizationLocale>>).data
                    .filter { element ->
                        element.description.departmentFilter.let {
                            currentSelectedFilters?.contains(
                                it
                            )
                        } ?: false
                    }
            } else {
                emptyList()
            }

        setFilteredList(filterDataByChoices)
    }

    fun resetFilters() {
        _selectedFilters.value = emptySet()

        if (_orgsData.value is Resource.Success) {
            val data = (_orgsData.value as Resource.Success<List<OrganizationLocale>>).data
            setFilteredList(data)
        }
    }
}