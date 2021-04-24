package com.utsman.geolibsample.fragments

import android.content.Context
import android.os.Bundle
import android.view.*
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.maps.android.ktx.addMarker
import com.google.maps.android.ktx.awaitMap
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import com.utsman.geolib.marker.adapter.MarkerBitmapAdapter
import com.utsman.geolib.marker.adapter.MarkerViewAdapter
import com.utsman.geolib.marker.clearAllLayers
import com.utsman.geolib.marker.config.AnchorPoint
import com.utsman.geolib.marker.config.SizeLayer
import com.utsman.geolib.marker.dp
import com.utsman.geolibsample.R
import com.utsman.geolibsample.utils.buaran
import com.utsman.geolibsample.utils.klender
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class MarkerFragment : Fragment(R.layout.fragment_marker) {

    class MarkerPicasso(private val context: Context?) : MarkerBitmapAdapter() {
        private val url = "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRKzSe_UNCiqOm1cf8IXaqome0oji9YVRZZZA&usqp=CAU"

        override suspend fun createView(): View = suspendCancellableCoroutine { task ->
            val view = LayoutInflater.from(context).inflate(R.layout.marker_bitmap, null)
            val imgView = view.findViewById<ImageView>(R.id.img_marker)

            Picasso.get()
                .load(url)
                .into(imgView, object : Callback {
                    override fun onSuccess() {
                        MainScope().launch {
                            delay(100)
                            task.resume(view)
                        }
                    }

                    override fun onError(e: Exception?) {
                        task.resume(view)
                    }

                })
        }

        override fun maxWidth(): Int {
            return 70.dp
        }

        override fun maxHeight(): Int {
            return 100.dp
        }

    }

    private lateinit var googleMap: GoogleMap
    private lateinit var markerViewAdapter: MarkerViewAdapter
    private lateinit var markerBitmapAdapter: MarkerPicasso

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val mapsFragment =
            childFragmentManager.findFragmentById(R.id.maps_view) as SupportMapFragment

        val mapsView = view.findViewById<ViewGroup>(R.id.maps_view)
        markerViewAdapter = MarkerViewAdapter(mapsView)
        markerBitmapAdapter = MarkerPicasso(context)

        lifecycleScope.launch {
            googleMap = mapsFragment.awaitMap().apply {
                uiSettings.isZoomControlsEnabled = true
            }

            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(buaran, 15f))
            markerViewAdapter.bindGoogleMaps(googleMap)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.marker_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_bitmap_marker -> setMarkerBitmap()
            R.id.action_lottie_marker -> setMarkerLottie()
            R.id.action_clear_marker -> clearAllMarker()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setMarkerLottie() {
        val lottieView = LayoutInflater.from(context).inflate(R.layout.marker_view, null)

        markerViewAdapter.addMarkerView {
            id = "marker_lottie"
            latLng = buaran
            view = lottieView
            sizeLayer = SizeLayer.Marker
            anchorPoint = AnchorPoint.NORMAL
        }
    }

    private fun setMarkerBitmap() = lifecycleScope.launch {
        val bitmapIcon = markerBitmapAdapter.getIconView()
        googleMap.addMarker {
            position(klender)
            icon(bitmapIcon)
        }
    }

    private fun clearAllMarker() {
        googleMap.clearAllLayers(markerViewAdapter)
    }
}