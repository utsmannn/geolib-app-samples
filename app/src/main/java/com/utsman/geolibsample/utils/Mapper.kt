package com.utsman.geolibsample.utils

import com.utsman.geolib.location.data.PlaceData
import com.utsman.geolib.marker.toLatLng
import com.utsman.geolibsample.data.GrabLocationPlace

object Mapper {

    fun mapPlaceToGrabLocation(place: PlaceData): GrabLocationPlace {
        return GrabLocationPlace(
            title = place.title,
            address = place.address,
            latLng = place.location.toLatLng()
        )
    }

    fun mapPlacesToGrabLocations(places: List<PlaceData>): List<GrabLocationPlace> {
        return places.map { o ->
            GrabLocationPlace(
                title = o.title,
                address = o.address,
                latLng = o.location.toLatLng()
            )
        }
    }
}