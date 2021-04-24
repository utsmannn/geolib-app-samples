package com.utsman.geolibsample.module

import android.content.Context
import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.utsman.geolib.location.PlacesLocation
import com.utsman.geolib.location.createPlacesLocation
import com.utsman.geolib.routes.PlacesRoute
import com.utsman.geolib.routes.createPlacesRoute
import com.utsman.geolibsample.utils.FakeTrafficLocator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import dagger.hilt.android.qualifiers.ApplicationContext

@Module
@InstallIn(ApplicationComponent::class)
object GeolibModule {

    private const val HERE_API = "rujdNo1Z-UlY47ipLzcvIkgXpiNSvdYdTxpiDRV-Z6I"

    @Provides
    fun provideFusedLocation(@ApplicationContext context: Context): FusedLocationProviderClient {
        return LocationServices.getFusedLocationProviderClient(context)
    }

    @Provides
    fun providePlaceLocation(fusedLocationProviderClient: FusedLocationProviderClient): PlacesLocation {
        return fusedLocationProviderClient.createPlacesLocation(HERE_API)
    }

    @Provides
    fun providePlaceRoute(): PlacesRoute {
        return createPlacesRoute(HERE_API)
    }

    @Provides
    fun provideFakeTrafficLocator(): FakeTrafficLocator {
        return FakeTrafficLocator()
    }
}