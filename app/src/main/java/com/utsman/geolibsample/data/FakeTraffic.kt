package com.utsman.geolibsample.data

import com.google.android.gms.maps.model.LatLng

data class FakeTraffic(
    var id: String,
    var routes: List<LatLng>? = null
)