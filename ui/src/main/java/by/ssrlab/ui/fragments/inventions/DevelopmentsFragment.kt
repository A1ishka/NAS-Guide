package by.ssrlab.ui.fragments.inventions

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import by.ssrlab.common_ui.common.ui.base.BaseFragment
import by.ssrlab.data.data.common.RepositoryData
import by.ssrlab.data.data.settings.remote.DevelopmentLocale
import by.ssrlab.data.util.ButtonAction
import by.ssrlab.domain.models.ToolbarControlObject
import by.ssrlab.domain.utils.Resource
import by.ssrlab.common_ui.common.ui.MainActivity
import by.ssrlab.ui.R
import by.ssrlab.ui.databinding.FragmentDevelopmentsBinding
import by.ssrlab.common_ui.common.ui.exhibit.fragments.exhibit.NavigationManager
import by.ssrlab.ui.rv.SectionAdapter
import by.ssrlab.ui.vm.FDevelopmentsVM
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class DevelopmentsFragment : BaseFragment() {

    private lateinit var binding: FragmentDevelopmentsBinding
    private lateinit var adapter: SectionAdapter
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    override val toolbarControlObject = ToolbarControlObject(
        isBack = true,
        isLang = false,
        isSearch = true,
        isDates = false
    )

    override val fragmentViewModel: FDevelopmentsVM by activityViewModel<FDevelopmentsVM>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fragmentViewModel.setTitle(requireContext().resources.getString(by.ssrlab.domain.R.string.folder_inventions))
        fragmentViewModel.observeLanguageChanges()

        activityVM.apply {
            setHeaderImg(by.ssrlab.common_ui.R.drawable.header_inventions)
            setButtonAction(ButtonAction.BackAction, ::onBackPressed)
            setButtonAction(ButtonAction.SearchAction, ::initSearchBar)
        }

        binding.apply {
            viewModel = this@DevelopmentsFragment.fragmentViewModel
            lifecycleOwner = viewLifecycleOwner
        }

        initAdapter()
        observeOnDataChanged()
        disableButtons()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        clearQuery()
        hideSearchBar()
    }

    override fun onResume() {
        super.onResume()

        if (fragmentViewModel.isFiltering.value == true) {
            showSearchResults()
            binding.resetFilterButton.visibility = View.VISIBLE
        }
    }

    override fun observeOnDataChanged() {
        fragmentViewModel.inventionsData.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    adapter.showLoading()
                }

                is Resource.Success -> {
                    adapter.updateData(resource.data)
                    addAvailableFilterCategories()
                    fragmentViewModel.setLoaded(true)
                }

                is Resource.Error -> {
                    adapter.showError(resource.message)
                }
            }
        }
    }

    override fun initAdapter() {
        adapter = SectionAdapter(
            emptyList(), NavigationManager
        ) { NavigationManager.handleNavigate(activity as MainActivity) }

        when (val resource = fragmentViewModel.inventionsData.value) {
            is Resource.Success -> {
                val data = resource.data
                adapter.updateData(data)
            }

            is Resource.Error -> {
                adapter.showError(resource.message)
            }

            is Resource.Loading -> {
                adapter.showLoading()
            }

            null -> {}
        }

        binding.apply {
            inventionsRv.adapter = adapter
            inventionsRv.layoutManager = LinearLayoutManager(requireContext())
        }
    }

    override fun initBinding(container: ViewGroup?): View {
        binding = DataBindingUtil.inflate(
            layoutInflater,
            R.layout.fragment_developments,
            container,
            false
        )
        return binding.root
    }

    private fun disableButtons() {
        moveToFilter()
        initResetButton()
    }

    //Navigation
    override fun onBackPressed() {
        findNavController().navigate(R.id.mainFragment)
        scope.launch {
            delay(500)
            resetFilters()
        }
    }

    override fun navigateNext(repositoryData: RepositoryData) {
        (activity as MainActivity).moveToExhibit(repositoryData)
    }

    //Search
    private var toolbarSearchView: SearchView? = null

    private fun searchBarInstance(): SearchView {
        if (toolbarSearchView == null) {
            toolbarSearchView = requireActivity().findViewById(by.ssrlab.common_ui.R.id.toolbar_search_view)
        }
        return toolbarSearchView!!
    }

    override fun filterData(query: String) {
        fragmentViewModel.filterData(query)
    }

    private fun showSearchResults() {
        fragmentViewModel.filteredDataList.value?.let { adapter.updateData(it) }
    }

    private fun initSearchBar() {
        val toolbarSearchView = searchBarInstance()

        toolbarSearchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let {
                    filterData(it)
                    showSearchResults()
                    return true
                }
                showSearchResults()
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                newText?.let {
                    filterData(it)
                    showSearchResults()
                }
                return true
            }
        })
        toolbarSearchView.visibility = View.VISIBLE
        toolbarSearchView.isIconified = false
        val searchButton: ImageButton = requireActivity().findViewById(by.ssrlab.common_ui.R.id.toolbar_search)
        searchButton.visibility = View.GONE

        toolbarSearchView.setOnCloseListener {
            toolbarSearchView.visibility = View.GONE
            searchButton.visibility = View.VISIBLE
            true
        }

        toolbarSearchView.requestFocus()
        val inputManager =
            requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputManager.showSoftInput(toolbarSearchView.findFocus(), InputMethodManager.SHOW_IMPLICIT)
    }

    override fun hideSearchBar() {
        val toolbarSearchView = searchBarInstance()
        toolbarSearchView.visibility = View.GONE
    }

    private fun clearQuery() {
        val toolbarSearchView = searchBarInstance()
        toolbarSearchView.setQuery("", true)
    }


    //Filter
    private fun addAvailableFilterCategories() {
        fragmentViewModel.setAvailableFilters()
    }

    private fun resetFilters() {
        fragmentViewModel.resetFilters()
        fragmentViewModel.setFiltering(false)
        showAllDevelopments()
        binding.resetFilterButton.visibility = View.GONE
    }

    private fun showAllDevelopments() {
        fragmentViewModel.let {
            if (it.inventionsData.value is Resource.Success) {
                val data = (it.inventionsData.value as Resource.Success<List<DevelopmentLocale>>).data
                adapter.updateData(data)
            }
        }
    }

    private fun initResetButton() {
        binding.resetFilterButton.setOnClickListener { resetFilters() }
    }

    private fun moveToFilter() {
        binding.inventionsFilterRipple.setOnClickListener {
            if (fragmentViewModel.isLoaded.value == true) {
                findNavController().navigate(R.id.inventionsFilterFragment)
            } else {
                val currentContext = requireContext()
                Toast.makeText(
                    currentContext,
                    currentContext.resources.getString(by.ssrlab.common_ui.R.string.wait_for_data_to_load),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}