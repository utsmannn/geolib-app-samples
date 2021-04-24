package com.utsman.geolibsample.fragments

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.maps.android.ktx.addMarker
import com.google.maps.android.ktx.awaitMap
import com.utsman.geolibsample.R
import com.utsman.geolibsample.utils.toast
import com.utsman.geolib.location.PlacesLocation
import com.utsman.geolib.marker.toLatLng
import com.utsman.geolib.marker.toLocation
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LocationFragment : Fragment(R.layout.fragment_location) {

    @Inject lateinit var placeLocation: PlacesLocation
    private lateinit var googleMap: GoogleMap

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<RelativeLayout>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val layoutBottomSheet = view.findViewById<RelativeLayout>(R.id.bottom_sheet_view)
        bottomSheetBehavior = BottomSheetBehavior.from(layoutBottomSheet)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN

        val mapsFragment =
            childFragmentManager.findFragmentById(R.id.maps_view) as SupportMapFragment

        lifecycleScope.launch {
            googleMap = mapsFragment.awaitMap().apply {
                uiSettings.isZoomControlsEnabled = true
            }
        }
    }

    private fun getCurrentLocation() = lifecycleScope.launch {
        context?.toast("Get current location")
        val currentLocation = placeLocation.getLocationFlow().first()
        val currentLatLng = currentLocation.toLatLng()
        val cameraUpdate = CameraUpdateFactory.newLatLngZoom(currentLatLng, 14f)
        googleMap.animateCamera(cameraUpdate)
        googleMap.addMarker {
            position(currentLatLng)
        }
    }

    private fun getCurrentPlaceAddress() = lifecycleScope.launch {
        context?.toast("Get current place address")
        val tvCurrentLocation = view?.findViewById<TextView>(R.id.tv_current_location)
        val prBar = view?.findViewById<ProgressBar>(R.id.pr_bar)

        prBar?.isVisible = true
        tvCurrentLocation?.isVisible = false
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED

        val currentLocation = placeLocation.getLocationFlow().first()
        val currentLatLng = currentLocation.toLatLng()
        val cameraUpdate = CameraUpdateFactory.newLatLngZoom(currentLatLng, 18f)
        googleMap.animateCamera(cameraUpdate)


        val result = placeLocation.getPlacesLocation(currentLatLng.toLocation())

        result.onSuccess { places ->
            val currentPlace = if (places.isNotEmpty()) {
                places.first()
            } else {
                null
            }

            prBar?.isVisible = false
            tvCurrentLocation?.isVisible = true

            if (currentPlace != null) {
                val address = currentPlace.address
                tvCurrentLocation?.text = address
            } else {
                tvCurrentLocation?.text = "Cannot get location"
            }
        }

        result.onFailure {
            prBar?.isVisible = false
            tvCurrentLocation?.text = "Cannot get location"
        }
    }

    private fun searchLocation() {
        findNavController().navigate(R.id.action_locationFragment_to_locationSearchFragment)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.location_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_current_location -> getCurrentLocation()
            R.id.action_get_place -> getCurrentPlaceAddress()
            R.id.action_search_location -> searchLocation()
        }
        return super.onOptionsItemSelected(item)
    }
}