package com.utsman.geolibsample.utils

import android.location.Location
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.maps.model.LatLng
import com.utsman.geolib.location.PlacesLocation
import com.utsman.geolib.location.data.PlaceData
import com.utsman.geolib.routes.PlacesRoute
import com.utsman.geolibsample.data.FakeTraffic


class FakeTrafficLocator {

    private lateinit var currentLocation: Location
    private lateinit var placeLocation: PlacesLocation
    private lateinit var placeRoute: PlacesRoute
    private val trafficRange = 0..8
    private val _loader: MutableLiveData<Boolean> = MutableLiveData()
    private val _places: MutableLiveData<List<PlaceData>> = MutableLiveData()

    private val _route1: MutableLiveData<List<LatLng>> = MutableLiveData()
    private val _route2: MutableLiveData<List<LatLng>> = MutableLiveData()
    private val _route3: MutableLiveData<List<LatLng>> = MutableLiveData()
    private val _route4: MutableLiveData<List<LatLng>> = MutableLiveData()
    private val _route5: MutableLiveData<List<LatLng>> = MutableLiveData()
    private val _route6: MutableLiveData<List<LatLng>> = MutableLiveData()

    val loader: LiveData<Boolean>
        get() = _loader

    val places: LiveData<List<PlaceData>>
        get() = _places

    val route1: LiveData<List<LatLng>>
        get() = _route1

    val route2: LiveData<List<LatLng>>
        get() = _route2

    val route3: LiveData<List<LatLng>>
        get() = _route3

    val route4: LiveData<List<LatLng>>
        get() = _route4

    val route5: LiveData<List<LatLng>>
        get() = _route5

    val route6: LiveData<List<LatLng>>
        get() = _route6

    fun bindCurrentLocation(currentLocation: Location) {
        this.currentLocation = currentLocation
    }

    fun bindPlaceLocation(placeLocation: PlacesLocation) {
        this.placeLocation = placeLocation
    }

    fun bindPlaceRoute(placeRoute: PlacesRoute) {
        this.placeRoute = placeRoute
    }

    suspend fun startFakeDriverLocator() {
        val query = "coffe"
        _loader.postValue(true)
        val placesFound = placeLocation.searchPlaces(currentLocation, query)
            .getOrNull()
            ?: emptyList()

        _loader.postValue(false)
        _places.postValue(placesFound)

        if (placesFound.size > 6) {
            val startRoute1 = placesFound.random()
            val endRoute1 = placesFound.random()

            val startRoute2 = placesFound.random()
            val endRoute2 = placesFound.random()

            val startRoute3 = placesFound.random()
            val endRoute3 = placesFound.random()

            val startRoute4 = placesFound.random()
            val endRoute4 = placesFound.random()

            val startRoute5 = placesFound.random()
            val endRoute5 = placesFound.random()

            val startRoute6 = placesFound.random()
            val endRoute6 = placesFound.random()

            searchRoute(1, startRoute1, endRoute1)
            searchRoute(2, startRoute2, endRoute2)
            searchRoute(3, startRoute3, endRoute3)
            searchRoute(4, startRoute4, endRoute4)
            searchRoute(5, startRoute5, endRoute5)
            searchRoute(6, startRoute6, endRoute6)
        }
    }

    private suspend fun searchRoute(
        routeId: Int,
        start: PlaceData,
        end: PlaceData
    ) {
        placeRoute.searchRoute {
            startLocation = start.location
            endLocation = end.location
        }.mapCatching {
            it.geometries
        }.onSuccess {
            when (routeId) {
                1 -> _route1.postValue(it)
                2 -> _route2.postValue(it)
                3 -> _route3.postValue(it)
                4 -> _route4.postValue(it)
                5 -> _route5.postValue(it)
                6 -> _route6.postValue(it)
            }
        }.onFailure {
            it.printStackTrace()
        }
    }

}