package com.utsman.geolibsample.data

import com.google.android.gms.maps.model.LatLng

data class GrabLocationPlace(
    val title: String,
    val address: String,
    val latLng: LatLng
)