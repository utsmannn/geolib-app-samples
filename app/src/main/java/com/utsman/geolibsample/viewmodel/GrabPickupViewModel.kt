package com.utsman.geolibsample.viewmodel

import android.location.Location
import androidx.lifecycle.*
import com.google.android.gms.maps.model.LatLng
import com.utsman.geolib.location.PlacesLocation
import com.utsman.geolib.marker.toLocation
import com.utsman.geolibsample.data.GrabLocationPlace
import com.utsman.geolibsample.utils.CombinedLiveData
import com.utsman.geolibsample.utils.Mapper
import com.utsman.geolibsample.utils.logi
import com.utsman.paging.data.PagingData
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class GrabPickupViewModel : ViewModel() {

    companion object {
        const val DISTANCE: Long = 50
    }

    private lateinit var placeLocation: PlacesLocation

    private val _currentLocation: MutableLiveData<Location> = MutableLiveData()
    private val _searchResult: MutableLiveData<Result<PagingData<GrabLocationPlace>>> = MutableLiveData()
    private val _suggestionLocation: MutableLiveData<Result<PagingData<GrabLocationPlace>>> = MutableLiveData()
    private val _reverseGeocoder: MutableLiveData<Result<GrabLocationPlace>> = MutableLiveData()

    private val _currentToLocation: MutableLiveData<GrabLocationPlace> = MutableLiveData()
    private val _whereToLocation: MutableLiveData<GrabLocationPlace> = MutableLiveData()
    private val _readyPickup: MutableLiveData<Boolean> = MutableLiveData()

    private val readyPickup: LiveData<Boolean>
            get() = _readyPickup

    val searchResult: LiveData<Result<PagingData<GrabLocationPlace>>>
            get() = _searchResult

    val currentLocation: LiveData<Location>
            get() = _currentLocation

    val suggestionLocation: LiveData<Result<PagingData<GrabLocationPlace>>>
            get() = _suggestionLocation

    val reverseGeocoder: LiveData<Result<GrabLocationPlace>>
            get() = _reverseGeocoder

    val currentToLocation: LiveData<GrabLocationPlace>
            get() =  _currentToLocation

    val whereToLocation: LiveData<GrabLocationPlace>
            get() =  _whereToLocation

    val hasFilled: MediatorLiveData<Boolean>
            get() = CombinedLiveData(
                currentToLocation,
                whereToLocation,
                readyPickup
            ) { current, whereTo, allow ->
                logi("current -> $current | where to -> $whereTo | allow -> $allow")
                (allow == true) && (current != null) && (whereTo != null)
            }

    fun bindPlaceLocation(placeLocation: PlacesLocation) {
        this.placeLocation = placeLocation
    }

    fun getCurrentLocation() = viewModelScope.launch {
        val locationFound = placeLocation.getLocationFlow().first()
        _currentLocation.postValue(locationFound)

        placeLocation.getPlacesLocation(locationFound)
            .onSuccess {
                val placeFound = it.first()
                _currentToLocation.postValue(Mapper.mapPlaceToGrabLocation(placeFound))
            }
    }

    fun saveCurrentToLocation(currentToLocation: GrabLocationPlace) {
        _currentToLocation.postValue(currentToLocation)
    }

    fun saveWhereToLocation(whereToLocation: GrabLocationPlace) {
        _whereToLocation.postValue(whereToLocation)
    }

    fun setReadyPickup(ready: Boolean) {
        _readyPickup.postValue(ready)
    }

    fun searchSuggestion() = viewModelScope.launch {
        val querySuggestionPlace = "restaurant"
        val currentLocationValue = currentLocation.value
        if (currentLocationValue != null) {
            placeLocation.searchPlaces(currentLocationValue, querySuggestionPlace)
                .onSuccess {
                    val pagingData = PagingData.fromList(Mapper.mapPlacesToGrabLocations(it))
                    _suggestionLocation.postValue(Result.success(pagingData))
                }
                .onFailure {
                    _suggestionLocation.postValue(Result.failure(it))
                }
        } else {
            _suggestionLocation.postValue(Result.failure(Throwable("Current location not found")))
        }
    }

    fun searchLocation(query: String) = viewModelScope.launch {
        val currentLocationValue = currentLocation.value
        if (currentLocationValue != null) {
            placeLocation.searchPlaces(currentLocationValue, query)
                .onSuccess {
                    val pagingData = PagingData.fromList(Mapper.mapPlacesToGrabLocations(it))
                    _searchResult.postValue(Result.success(pagingData))
                }
                .onFailure {
                    _searchResult.postValue(Result.failure(it))
                }
        } else {
            _searchResult.postValue(Result.failure(Throwable("Current location not found")))
        }
    }

    private fun reverseGeocoderFetcher(latLng: LatLng) = viewModelScope.launch {
        placeLocation.getPlacesLocation(latLng.toLocation())
            .onSuccess {
                val resultFirst = Mapper.mapPlacesToGrabLocations(it).first()
                _reverseGeocoder.postValue(Result.success(resultFirst))
            }
            .onFailure {
                _reverseGeocoder.postValue(Result.failure(it))
            }
    }

    private var tempLatLng = LatLng(0.0, 0.0)

    fun searchAddress(latLng: LatLng): Job? {
        var job: Job? = null

        viewModelScope.launch {
            if (tempLatLng == latLng)
                return@launch

            job?.cancel()
            tempLatLng = latLng
            delay(2000)
            job = reverseGeocoderFetcher(tempLatLng)
        }
        return job
    }
}