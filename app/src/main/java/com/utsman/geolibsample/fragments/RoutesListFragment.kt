package com.utsman.geolibsample.fragments

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.PolylineOptions
import com.google.maps.android.ktx.awaitMap
import com.utsman.geolibsample.R
import com.utsman.geolibsample.utils.buaran
import com.utsman.geolibsample.utils.depok
import com.utsman.geolibsample.utils.toast
import com.utsman.paging.data.PagingData
import com.utsman.paging.extensions.SimpleAdapter
import com.utsman.paging.extensions.createSimpleAdapter
import com.utsman.geolib.marker.dp
import com.utsman.geolib.marker.toLocation
import com.utsman.geolib.routes.PlacesRoute
import com.utsman.geolib.routes.data.TransportMode
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class RoutesListFragment : Fragment(R.layout.fragment_route_list) {

    @Inject
    lateinit var placeRoute: PlacesRoute

    private lateinit var routeAdapter: SimpleAdapter<String>
    private lateinit var googleMap: GoogleMap

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnRoute = view.findViewById<Button>(R.id.btn_route)
        val btnSwitchMap = view.findViewById<Button>(R.id.btn_switch_map)

        val rvRoute = view.findViewById<RecyclerView>(R.id.rv_route_result)
        val tvFromPlace = view.findViewById<TextView>(R.id.tv_from_place)
        val tvToPlace = view.findViewById<TextView>(R.id.tv_to_place)
        val prBar = view.findViewById<ProgressBar>(R.id.pr_bar)

        tvFromPlace.text = "From: Buaran (${buaran.latitude} - ${buaran.longitude})"
        tvToPlace.text = "To: Depok (${depok.latitude} - ${depok.longitude})"
        rvRoute.isVisible = false
        prBar.isVisible = false

        routeAdapter = rvRoute.createSimpleAdapter(R.layout.item_location_result) {
            onBindViewHolder = { v, i, _ ->
                val tvItem = v.findViewById<TextView>(R.id.tv_item_location)
                tvItem.text = i
                tvItem.setOnClickListener {
                    context?.toast(i)
                }
            }
        }

        val mapsViewContainer = view.findViewById<RelativeLayout>(R.id.maps_view_container)
        val mapsFragment =
            childFragmentManager.findFragmentById(R.id.maps_view) as SupportMapFragment

        lifecycleScope.launch {
            googleMap = mapsFragment.awaitMap().apply {
                uiSettings.isZoomControlsEnabled = true
            }
        }

        btnRoute.setOnClickListener {
            routeAdapter.clearItems()
            googleMap.clear()
            searchRoute(prBar)
        }

        btnSwitchMap.text = textSwitchMap(rvRoute, mapsViewContainer)
        btnSwitchMap.setOnClickListener {
            mapsViewContainer.isVisible = !mapsViewContainer.isVisible
            rvRoute.isVisible = !rvRoute.isVisible

            btnSwitchMap.text = textSwitchMap(rvRoute, mapsViewContainer)
        }
    }

    private fun textSwitchMap(rvRoute: RecyclerView, mapsViewContainer: RelativeLayout): String {
        return when {
            rvRoute.isVisible -> {
                "Display maps"
            }
            mapsViewContainer.isVisible -> {
                "Display coordinate"
            }
            else -> {
                "Switch view"
            }
        }
    }

    private fun searchRoute(progressBar: ProgressBar) = lifecycleScope.launch {
        progressBar.isVisible = true
        placeRoute.searchRoute {
            startLocation = buaran.toLocation()
            endLocation = depok.toLocation()
            transportMode = TransportMode.CAR
        }.onSuccess {
            progressBar.isVisible = false
            val listResult = it.geometries.map { g -> "${g.latitude} | ${g.longitude}" }
            routeAdapter.submitData(PagingData.fromList(listResult))
            setupRouteInMaps(it.geometries)
        }.onFailure {
            progressBar.isVisible = false
            context?.toast(it.localizedMessage ?: "Error")
        }.let {

        }
    }

    private fun setupRouteInMaps(geometries: List<LatLng>) {
        val bound = LatLngBounds.Builder()
            .include(geometries.first())
            .include(geometries.last())
            .build()

        val cameraUpdate = CameraUpdateFactory.newLatLngBounds(bound, 40.dp)
        val polylineOptions = PolylineOptions()
            .addAll(geometries)

        googleMap.animateCamera(cameraUpdate)
        googleMap.addPolyline(polylineOptions)
    }
}