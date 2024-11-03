package by.ssrlab.ui.fragments

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import by.ssrlab.common_ui.common.ui.base.BaseFragment
import by.ssrlab.data.data.common.RepositoryData
import by.ssrlab.data.util.ButtonAction
import by.ssrlab.domain.models.ToolbarControlObject
import by.ssrlab.ui.MainActivity
import by.ssrlab.ui.R
import by.ssrlab.ui.databinding.FragmentFiltersBinding
import by.ssrlab.ui.rv.FilterAdapter
import by.ssrlab.ui.vm.FOrgsVM
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class FilterFragment : BaseFragment() {

    private lateinit var binding: FragmentFiltersBinding
    private lateinit var adapter: FilterAdapter

    override val toolbarControlObject = ToolbarControlObject(
        isBack = true,
        isLang = false,
        isSearch = true,
        isDates = false
    )

    override val fragmentViewModel: FOrgsVM by activityViewModel<FOrgsVM>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //TODO: add for specific filter title (strings)
        fragmentViewModel.setTitle(requireContext().resources.getString(by.ssrlab.common_ui.R.string.button_support_filter))
        activityVM.apply {
            setButtonAction(ButtonAction.BackAction, ::onBackPressed)
        }

        binding.apply {
            viewModel = this@FilterFragment.fragmentViewModel
            lifecycleOwner = viewLifecycleOwner
        }

        initAdapter()
        observeOnDataChanged()
        disableButtons()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        val searchButton: ImageButton = requireActivity().findViewById(R.id.toolbar_search)
        searchButton.visibility = View.VISIBLE
    }

    private fun disableButtons() {
        val searchButton: ImageButton = requireActivity().findViewById(R.id.toolbar_search)
        searchButton.visibility = View.GONE
    }

    override fun observeOnDataChanged() {
        fragmentViewModel.availableFilters.observe(viewLifecycleOwner, Observer { filters ->
            filters?.let {
                adapter.updateData(filters)
            }
        })
    }

    override fun initAdapter() {
        val adapter = FilterAdapter(emptyMap()) { filterSet, isSelected ->
            filterSet.forEach { filter ->
                fragmentViewModel.onFilterSelected(filter, isSelected)
            }
        }
        val filters = fragmentViewModel.availableFilters.value
        filters?.let {
            adapter.updateData(filters)
        }

        binding.apply {
            filterOrgsRv.adapter = adapter
            filterOrgsRv.layoutManager = LinearLayoutManager(requireContext())
        }
    }

    override fun initBinding(container: ViewGroup?): View {
        binding =
            DataBindingUtil.inflate(layoutInflater, R.layout.fragment_filters, container, false)
        return binding.root
    }

    override fun onBackPressed() {
        findNavController().popBackStack()
    }

    override fun navigateNext(repositoryData: RepositoryData) {
        (activity as MainActivity).moveToExhibit(repositoryData)
    }
}