package com.utsman.geolibsample.fragments

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.OrientationHelper
import by.kirich1409.viewbindingdelegate.viewBinding
import com.utsman.geolib.marker.dp
import com.utsman.geolib.marker.toLocation
import com.utsman.geolibsample.R
import com.utsman.geolibsample.data.GrabLocationPlace
import com.utsman.geolibsample.databinding.FragmentGrabListPickerBinding
import com.utsman.geolibsample.utils.*
import com.utsman.geolibsample.viewmodel.GrabPickupViewModel
import com.utsman.paging.extensions.SimpleAdapter
import com.utsman.paging.extensions.createSimpleAdapter
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class GrabListPickerFragment : Fragment(R.layout.fragment_grab_list_picker) {

    enum class SearchFocused {
        CURRENT, WHERE
    }

    private val binding: FragmentGrabListPickerBinding by viewBinding()
    private val grabPickupViewModel: GrabPickupViewModel by activityViewModels()
    private var searchFocused = SearchFocused.WHERE
    private lateinit var suggestionPlaceAdapter: SimpleAdapter<GrabLocationPlace>

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
    }

    private fun initView() = binding.run {
        val statusBarHeight = activity?.getStatusBarHeight() ?: 0.dp
        fakeStatusBar.layoutParams.height = statusBarHeight
        fakeStatusBar.requestLayout()

        btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        edCurrentLocation.watcher(lifecycleScope) {
            searchFocused = SearchFocused.CURRENT
            tvCurrentLocation.text = it
            prBar.isVisible = true

            if (searchFocused == SearchFocused.CURRENT) {
                grabPickupViewModel.searchLocation(it)
            }
        }

        edWhereTo.watcher(lifecycleScope) {
            searchFocused = SearchFocused.WHERE
            tvWhereTo.text = it
            prBar.isVisible = true

            if (searchFocused == SearchFocused.WHERE) {
                grabPickupViewModel.searchLocation(it)
            }
        }

        tvCurrentLocation.setOnClickListener {
            toggleCurrentView(false)
            toggleWhereToView(true)
            edCurrentLocation.showKeyboard()
        }

        tvWhereTo.setOnClickListener {
            toggleCurrentView(true)
            toggleWhereToView(false)
            edWhereTo.showKeyboard()
        }

        tvWhereTo.performClick()

        rvPlaceResult.run {
            val divider = DividerItemDecoration(context, OrientationHelper.VERTICAL)
            addItemDecoration(divider)
        }

        suggestionPlaceAdapter = rvPlaceResult.createSimpleAdapter(R.layout.item_grab_result) {
            onBindViewHolder = { v, i, _ ->
                val title = v.findViewById<TextView>(R.id.tv_item_title)
                val address = v.findViewById<TextView>(R.id.tv_item_address)

                title.text = i.title
                address.text = i.address

                v.setOnClickListener {
                    when (searchFocused) {
                        SearchFocused.WHERE -> {
                            grabPickupViewModel.saveWhereToLocation(i)
                            grabPickupViewModel.setReadyPickup(true)
                        }
                        SearchFocused.CURRENT -> {
                            grabPickupViewModel.saveCurrentToLocation(i)
                            grabPickupViewModel.setReadyPickup(true)
                        }
                    }

                    suggestionPlaceAdapter.clearItems()
                }
            }
        }

        grabPickupViewModel.suggestionLocation.observe(viewLifecycleOwner) {
            it.onSuccess { data ->
                suggestionPlaceAdapter.submitData(data)
                prBar.isVisible = false
            }
        }

        grabPickupViewModel.searchResult.observe(viewLifecycleOwner) {
            it.onSuccess { data ->
                suggestionPlaceAdapter.clearItems()
                suggestionPlaceAdapter.submitData(data)
                prBar.isVisible = false
            }
        }

        grabPickupViewModel.currentToLocation.observe(viewLifecycleOwner) {
            if (it.latLng.toLocation().distanceTo(grabPickupViewModel.currentLocation.value) > GrabPickupViewModel.DISTANCE) {
                edCurrentLocation.setText("")
                tvCurrentLocation.text = it.title

                toggleCurrentView(true)
            }
        }

        grabPickupViewModel.whereToLocation.observe(viewLifecycleOwner) {
            edWhereTo.setText("")
            tvWhereTo.text = it.title

            toggleWhereToView(true)
        }

        grabPickupViewModel.hasFilled.observe(viewLifecycleOwner) { hasFilled ->
            logi("has fill --> $hasFilled")

            if (hasFilled) {
                navigateToConfirmPicker()
            }
        }
    }

    private fun toggleCurrentView(textViewVisible: Boolean) = binding.run {
        tvCurrentLocation.isVisible = textViewVisible
        edCurrentLocation.isVisible = !textViewVisible
    }

    private fun toggleWhereToView(textViewVisible: Boolean) = binding.run {
        tvWhereTo.isVisible = textViewVisible
        edWhereTo.isVisible = !textViewVisible
    }

    private fun navigateToConfirmPicker() {
        val navigateId = R.id.action_grabListPickerFragment_to_grabConfirmPickerFragment
        findNavController().navigate(navigateId)
    }
}