package com.utsman.geolibsample.fragments

import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.utsman.geolibsample.R
import com.utsman.geolibsample.utils.hideKeyboard
import com.utsman.geolibsample.utils.toast
import com.utsman.paging.data.LoadStatus
import com.utsman.paging.data.PagingData
import com.utsman.paging.extensions.SimpleAdapter
import com.utsman.paging.extensions.createSimpleAdapter
import com.utsman.geolib.location.PlacesLocation
import com.utsman.geolib.location.data.PlaceData
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LocationSearchFragment : Fragment(R.layout.fragment_location_search) {

    @Inject lateinit var placeLocation: PlacesLocation
    private lateinit var searchAdapter: SimpleAdapter<PlaceData>

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val edSearchLocation = view.findViewById<TextInputEditText>(R.id.ed_search_location)
        val btnSearchLocation = view.findViewById<ImageView>(R.id.btn_search_location)
        val rvSearchResult = view.findViewById<RecyclerView>(R.id.rv_search_result)
        val prBar = view.findViewById<ProgressBar>(R.id.pr_bar)
        prBar.isVisible = false

        searchAdapter = rvSearchResult.createSimpleAdapter(R.layout.item_location_result) {
            onBindViewHolder = { v, i, _ ->
                val tvItem = v.findViewById<TextView>(R.id.tv_item_location)
                tvItem.text = i.title
                tvItem.setOnClickListener {
                    val latLngMessage = "${i.location.latitude} - ${i.location.longitude}"
                    context?.toast(latLngMessage)
                }
            }
        }

        rvSearchResult.run {
            layoutManager = LinearLayoutManager(context)
            adapter = searchAdapter
        }

        lifecycleScope.launch {
            val currentLocation = placeLocation.getLocationFlow().first()
            btnSearchLocation.setOnClickListener {
                val textSearch = edSearchLocation.text.toString()
                if (textSearch.isNotEmpty()) {
                    edSearchLocation.hideKeyboard()
                    searchLocation(textSearch, currentLocation, prBar)
                }
            }
        }
    }

    private fun searchLocation(search: String, location: Location, progressBar: ProgressBar) = lifecycleScope.launch {
        context?.toast("search location...")
        progressBar.isVisible = true
        val resultLocation = placeLocation.searchPlaces(location, search)

        resultLocation.onSuccess { places ->
            progressBar.isVisible = false
            searchAdapter.clearItems()
            searchAdapter.submitData(PagingData.fromList(places))
        }
    }
}