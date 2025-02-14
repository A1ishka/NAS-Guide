package by.ssrlab.ui.vm

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import by.ssrlab.common_ui.common.ui.base.vm.BaseFragmentVM
import by.ssrlab.data.data.settings.remote.PersonLocale
import by.ssrlab.domain.repository.network.PersonsRepository
import by.ssrlab.ui.states.PersonsState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.util.Locale


class FPersonsVM(personsRepository: PersonsRepository) :
    BaseFragmentVM<PersonLocale>(personsRepository) {

    private val _state = MutableStateFlow(PersonsState())
    val personState: StateFlow<PersonsState> get() = _state

    private val _personsData = MutableLiveData<List<PersonLocale>>()
    val personsData: LiveData<List<PersonLocale>> get() = _personsData

    fun setTitle(value: String) {
        _state.update { currentState ->
            currentState.copy(title = value)
        }
    }

    init {
        getData { personList ->
            _state.update { currentState ->
                currentState.copy(personList = personList.toMutableList())
            }
            _personsData.value = personState.value.personList?.toList()
        }
    }


    private val _filteredData = MutableLiveData<List<PersonLocale>>()
    val filteredDataList: LiveData<List<PersonLocale>> get() = _filteredData

    private fun setFilteredList(value: List<PersonLocale>) {
        _filteredData.value = value
    }

    fun filterData(query: String) {
        val queryLowered = query.lowercase(Locale.getDefault())
        _personsData.value?.let { data ->
            val filteredList = data.filter {
                it.name.contains(queryLowered, ignoreCase = true)
            }
            setFilteredList(filteredList)
        }
    }
}