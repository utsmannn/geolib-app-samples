package com.utsman.geolibsample.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import by.kirich1409.viewbindingdelegate.viewBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.RoundCap
import com.google.maps.android.ktx.awaitMap
import com.utsman.geolib.marker.adapter.MarkerViewAdapter
import com.utsman.geolib.marker.config.AnchorPoint
import com.utsman.geolib.marker.config.SizeLayer
import com.utsman.geolib.marker.dp
import com.utsman.geolib.marker.toLatLng
import com.utsman.geolib.marker.toLocation
import com.utsman.geolib.polyline.data.StackAnimationMode
import com.utsman.geolib.polyline.polyline.PolylineAnimator
import com.utsman.geolib.polyline.utils.createPolylineAnimatorBuilder
import com.utsman.geolib.polyline.utils.enableBorder
import com.utsman.geolib.polyline.utils.withPrimaryPolyline
import com.utsman.geolib.routes.PlacesRoute
import com.utsman.geolib.routes.data.TransportMode
import com.utsman.geolibsample.R
import com.utsman.geolibsample.data.GrabLocationPlace
import com.utsman.geolibsample.databinding.FragmentGrabResultBinding
import com.utsman.geolibsample.utils.getStatusBarHeight
import com.utsman.geolibsample.utils.inflateLayout
import com.utsman.geolibsample.utils.toast
import com.utsman.geolibsample.viewmodel.GrabPickupViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class GrabResultFragment : Fragment(R.layout.fragment_grab_result) {

    @Inject
    lateinit var placeRoute: PlacesRoute

    private val binding: FragmentGrabResultBinding by viewBinding()
    private val grabPickupViewModel: GrabPickupViewModel by activityViewModels()

    private lateinit var markerViewAdapter: MarkerViewAdapter
    private lateinit var googleMap: GoogleMap
    private lateinit var polylineAnimator: PolylineAnimator

    private lateinit var markerCurrent: View
    private lateinit var markerWhereTo: View

    private lateinit var windowCurrent: View
    private lateinit var windowWhereTo: View

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapsView = view.findViewById<ViewGroup>(R.id.maps_view)
        markerViewAdapter = MarkerViewAdapter(mapsView)
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
            val paddingValue = 20.dp
            googleMap = mapsFragment.awaitMap()
            googleMap.setPadding(paddingValue, paddingValue, paddingValue, paddingValue)
            markerViewAdapter.bindGoogleMaps(googleMap)
            preparePolyline()

            grabPickupViewModel.currentLocation.observe(viewLifecycleOwner) {
                val latLng = it.toLatLng()
                googleMap.moveCamera(
                    CameraUpdateFactory.newLatLngZoom(latLng, 16f)
                )
            }

            grabPickupViewModel.hasFilled.observe(viewLifecycleOwner) { hasFilled ->
                if (hasFilled) {
                    val currentPlace = grabPickupViewModel.currentToLocation.value
                    val whereToPlace = grabPickupViewModel.whereToLocation.value

                    val currentLocation = currentPlace?.latLng?.toLocation()
                    val whereToLocation = whereToPlace?.latLng?.toLocation()

                    if (currentLocation != null && whereToLocation != null) {
                        val distance = currentLocation.distanceTo(whereToLocation)
                        if (distance > 20f) {
                            setupHasFilled(currentPlace, whereToPlace)
                        } else {
                            grabPickupViewModel.setReadyPickup(false)
                            findNavController().popBackStack(R.id.grabPickupFragment, false)
                            context?.toast("Location too close!")
                        }
                    }
                }
            }
        }
    }

    private fun preparePolyline() {
        val animatorBuilder = googleMap.createPolylineAnimatorBuilder()
        polylineAnimator = animatorBuilder.createPolylineAnimator()

        markerCurrent = requireContext().inflateLayout(R.layout.marker_grab_current)
        markerWhereTo = requireContext().inflateLayout(R.layout.marker_grab_where_to)

        windowCurrent = requireContext().inflateLayout(R.layout.marker_window_grab)
        windowWhereTo = requireContext().inflateLayout(R.layout.marker_window_grab)
    }

    private fun setupHasFilled(
        currentLocation: GrabLocationPlace,
        whereToLocation: GrabLocationPlace
    ) = binding.run {
        val bound = LatLngBounds.Builder()
            .include(currentLocation.latLng)
            .include(whereToLocation.latLng)
            .build()

        val cameraUpdate = CameraUpdateFactory.newLatLngBounds(bound, 60.dp)
        googleMap.moveCamera(cameraUpdate)

        lifecycleScope.launch {
            placeRoute.searchRoute {
                startLocation = currentLocation.latLng.toLocation()
                endLocation = whereToLocation.latLng.toLocation()
                transportMode = TransportMode.BIKE
            }.onSuccess { data ->

                val geometries = data.geometries
                polylineAnimator.startAnimate(geometries) {
                    withPrimaryPolyline {
                        color(Color.parseColor("#528C55"))
                        endCap(RoundCap())
                        startCap(RoundCap())
                    }
                    enableBorder(true, color = Color.BLACK)
                    stackAnimationMode = StackAnimationMode.BlockStackAnimation

                    val cameraUpdatePolyline = CameraUpdateFactory.newLatLngBounds(bound, 20.dp)
                    googleMap.animateCamera(cameraUpdatePolyline)
                }

            }.onFailure {
                it.printStackTrace()
            }
        }

        windowCurrent.findViewById<TextView>(R.id.tv_item_title).text = currentLocation.title
        windowWhereTo.findViewById<TextView>(R.id.tv_item_title).text = whereToLocation.title

        markerViewAdapter.addMarkerView {
            id = "id_of_${currentLocation.title}"
            latLng = currentLocation.latLng
            view = markerCurrent
            sizeLayer = SizeLayer.Custom(30.dp, 50.dp)
            anchorPoint = AnchorPoint.NORMAL
            windowView = {
                height = 30.dp
                width = 150.dp
                view = windowCurrent
            }
        }

        markerViewAdapter.addMarkerView {
            id = "id_of_${whereToLocation.title}"
            latLng = whereToLocation.latLng
            view = markerWhereTo
            sizeLayer = SizeLayer.Custom(40.dp, 40.dp)
            anchorPoint = AnchorPoint.NORMAL
            windowView = {
                height = 30.dp
                width = 150.dp
                view = windowWhereTo
            }
        }
    }
}