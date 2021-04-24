package com.utsman.geolibsample.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import by.kirich1409.viewbindingdelegate.viewBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.maps.android.ktx.awaitMap
import com.utsman.geolib.marker.dp
import com.utsman.geolib.marker.toLatLng
import com.utsman.geolibsample.R
import com.utsman.geolibsample.databinding.FragmentGrabConfirmPickerBinding
import com.utsman.geolibsample.utils.getStatusBarHeight
import com.utsman.geolibsample.utils.toast
import com.utsman.geolibsample.viewmodel.GrabPickupViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class GrabConfirmPickerFragment : Fragment(R.layout.fragment_grab_confirm_picker) {

    private val binding: FragmentGrabConfirmPickerBinding by viewBinding()
    private val grabPickupViewModel: GrabPickupViewModel by activityViewModels()

    private lateinit var googleMap: GoogleMap

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
    }

    private fun initView() = binding.run {
        val statusBarHeight = activity?.getStatusBarHeight() ?: 0.dp
        fakeStatusBar.layoutParams.height = statusBarHeight
        fakeStatusBar.requestLayout()

        btnBack.setOnClickListener {
            grabPickupViewModel.setReadyPickup(false)
            findNavController().popBackStack(R.id.grabPickupFragment, false)
        }

        val mapsFragment =
            childFragmentManager.findFragmentById(R.id.maps_view) as SupportMapFragment

        lifecycleScope.launch {
            googleMap = mapsFragment.awaitMap()

            grabPickupViewModel.currentLocation.observe(viewLifecycleOwner) {
                val latLng = it.toLatLng()
                googleMap.moveCamera(
                    CameraUpdateFactory.newLatLngZoom(latLng, 16f)
                )
            }

            grabPickupViewModel.currentToLocation.observe(viewLifecycleOwner) { place ->
                val currentLatLng = place.latLng
                googleMap.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(currentLatLng, 16f)
                )

                tvItemTitle.text = place.title
                tvItemAddress.text = place.address
            }

            grabPickupViewModel.reverseGeocoder.observe(viewLifecycleOwner) { result ->
                result.onSuccess { place ->
                    grabPickupViewModel.saveCurrentToLocation(place)
                }

                result.onFailure {
                    context?.toast(it)
                }
            }

            googleMap.setOnCameraIdleListener {
                val currentLatLng = googleMap.cameraPosition.target
                grabPickupViewModel.searchAddress(currentLatLng)
            }
        }

        btnConfirm.setOnClickListener {
            findNavController().navigate(R.id.action_grabConfirmPickerFragment_to_grabResultFragment)
        }
    }
}