package com.utsman.geolibsample.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.OrientationHelper
import by.kirich1409.viewbindingdelegate.viewBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.ktx.awaitMap
import com.utsman.geolib.location.PlacesLocation
import com.utsman.geolib.marker.adapter.MarkerViewAdapter
import com.utsman.geolib.marker.config.AnchorPoint
import com.utsman.geolib.marker.config.SizeLayer
import com.utsman.geolib.marker.dp
import com.utsman.geolib.marker.moveMarker
import com.utsman.geolib.marker.toLatLng
import com.utsman.geolib.routes.PlacesRoute
import com.utsman.geolibsample.R
import com.utsman.geolibsample.data.GrabLocationPlace
import com.utsman.geolibsample.databinding.FragmentGrabPickupBinding
import com.utsman.geolibsample.utils.*
import com.utsman.geolibsample.viewmodel.GrabPickupViewModel
import com.utsman.paging.data.PagingData
import com.utsman.paging.extensions.SimpleAdapter
import com.utsman.paging.extensions.createSimpleAdapter
import com.utsman.paging.extensions.toList
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class GrabPickupFragment : Fragment(R.layout.fragment_grab_pickup) {

    @Inject
    lateinit var fakeTrafficLocator: FakeTrafficLocator

    @Inject
    lateinit var placeLocation: PlacesLocation

    @Inject
    lateinit var placesRoute: PlacesRoute

    private val binding: FragmentGrabPickupBinding by viewBinding()
    private val grabPickupViewModel: GrabPickupViewModel by activityViewModels()

    private lateinit var googleMap: GoogleMap
    private lateinit var suggestionPlaceAdapter: SimpleAdapter<GrabLocationPlace>
    private lateinit var markerViewAdapter: MarkerViewAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapsView = view.findViewById<ViewGroup>(R.id.maps_view)
        markerViewAdapter = MarkerViewAdapter(mapsView)
        initView()
    }

    @SuppressLint("MissingPermission")
    private fun initView() = binding.run {
        rvPlaceSuggestion.run {
            val divider = DividerItemDecoration(context, OrientationHelper.VERTICAL)
            addItemDecoration(divider)
        }
        suggestionPlaceAdapter = rvPlaceSuggestion.createSimpleAdapter(R.layout.item_grab_suggestion) {
            onBindViewHolder = { v, i, _ ->
                val title = v.findViewById<TextView>(R.id.tv_item_title)
                val address = v.findViewById<TextView>(R.id.tv_item_address)

                title.text = i.title
                address.text = i.address

                v.setOnClickListener {
                    grabPickupViewModel.saveWhereToLocation(i)
                    grabPickupViewModel.setReadyPickup(true)
                }
            }
        }

        prBar.isVisible = true
        grabPickupViewModel.suggestionLocation.observe(viewLifecycleOwner) { result ->
            result.onSuccess { data ->
                if (data.toList().isEmpty()) {
                    suggestionPlaceAdapter.clearItems()
                } else {
                    suggestionPlaceAdapter.submitData(PagingData.fromList(data.toList().take(3)))
                }
            }

            result.onFailure {
                context?.toast(it)
            }

            prBar.isVisible = false
        }

        val statusBarHeight = activity?.getStatusBarHeight() ?: 0.dp
        fakeStatusBar.layoutParams.height = statusBarHeight
        fakeStatusBar.requestLayout()

        val mapsFragment =
            childFragmentManager.findFragmentById(R.id.maps_view) as SupportMapFragment

        btnWhereTo.setOnClickListener {
            grabPickupViewModel.setReadyPickup(false)
            findNavController().navigate(R.id.action_grabPickupFragment_to_grabListPickerFragment)
        }

        btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        lifecycleScope.launch {
            googleMap = mapsFragment.awaitMap().apply {
                isMyLocationEnabled = true
                uiSettings.isMyLocationButtonEnabled = false
                setPadding(20.dp, 100.dp, 20.dp, 100.dp)
            }
            markerViewAdapter.bindGoogleMaps(googleMap)

            grabPickupViewModel.currentLocation.observe(viewLifecycleOwner) {
                it.toLatLng().run {
                    googleMap.moveCamera(
                        CameraUpdateFactory.newLatLngZoom(this, 16f)
                    )
                }

                lifecycleScope.launch {
                    setupFakeTraffic(it)
                }
            }

            grabPickupViewModel.hasFilled.observe(viewLifecycleOwner) { hasFilled ->
                logi("has fill --> $hasFilled")
                if (hasFilled) {
                    navigateToConfirmPicker()
                }
            }
        }
    }

    private suspend fun setupFakeTraffic(location: Location) {
        fakeTrafficLocator.bindCurrentLocation(location)
        fakeTrafficLocator.bindPlaceLocation(placeLocation)
        fakeTrafficLocator.bindPlaceRoute(placesRoute)

        fakeTrafficLocator.startFakeDriverLocator()

        if (view != null) {
            observingFakeTraffic()
        }
    }

    private fun observingFakeTraffic() {
        fakeTrafficLocator.places.observe(viewLifecycleOwner) {
            logi("result -> ${it.map { a -> "$a \n" }}")
            logi("size ------> ${it.size}")
        }

        fakeTrafficLocator.route1.observe(viewLifecycleOwner) {
            lifecycleScope.launch {
                startFakeBike(it)
            }
        }

        fakeTrafficLocator.route2.observe(viewLifecycleOwner) {
            lifecycleScope.launch {
                startFakeBike(it)
            }
        }

        fakeTrafficLocator.route3.observe(viewLifecycleOwner) {
            lifecycleScope.launch {
                startFakeBike(it)
            }
        }

        fakeTrafficLocator.route4.observe(viewLifecycleOwner) {
            lifecycleScope.launch {
                startFakeBike(it)
            }
        }

        fakeTrafficLocator.route5.observe(viewLifecycleOwner) {
            lifecycleScope.launch {
                startFakeBike(it)
            }
        }

        fakeTrafficLocator.route6.observe(viewLifecycleOwner) {
            lifecycleScope.launch {
                startFakeBike(it)
            }
        }
    }

    private suspend fun startFakeBike(latLngs: List<LatLng>) {
        val markerBikeView = requireContext().inflateLayout(R.layout.marker_grab_bike)
        val marker = markerViewAdapter.addMarkerView {
            id = UUID.randomUUID().toString()
            latLng = latLngs.first()
            view = markerBikeView
            sizeLayer = SizeLayer.Custom(30.dp, 30.dp)
            anchorPoint = AnchorPoint.NORMAL
        }

        latLngs.forEach {
            delay(1000)
            marker.moveMarker(it, googleMap, true)
        }
    }

    private fun navigateToConfirmPicker() {
        val navigateId = R.id.action_grabPickupFragment_to_grabConfirmPickerFragment
        findNavController().navigate(navigateId)
    }
}