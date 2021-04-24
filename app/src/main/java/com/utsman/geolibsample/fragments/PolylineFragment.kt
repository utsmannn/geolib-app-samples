package com.utsman.geolibsample.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RadioGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.material.textfield.TextInputEditText
import com.google.maps.android.ktx.awaitMap
import com.utsman.geolib.marker.dp
import com.utsman.geolib.marker.toLocation
import com.utsman.geolib.polyline.data.PolylineDrawMode
import com.utsman.geolib.polyline.data.StackAnimationMode
import com.utsman.geolib.polyline.utils.createPolylineAnimatorBuilder
import com.utsman.geolib.routes.PlacesRoute
import com.utsman.geolib.routes.data.TransportMode
import com.utsman.geolibsample.R
import com.utsman.geolibsample.utils.buaran
import com.utsman.geolibsample.utils.depok
import com.utsman.geolibsample.utils.toast
import dagger.hilt.android.AndroidEntryPoint
import dev.sasikanth.colorsheet.ColorSheet
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class PolylineFragment : Fragment(R.layout.fragment_polyline) {

    enum class AnimationMode {
        OFF, BLOCK, WAIT
    }

    enum class DrawMode {
        Normal, Curved, Lank
    }

    @Inject
    lateinit var placeRoute: PlacesRoute
    private lateinit var googleMap: GoogleMap

    private var geometries: List<LatLng>? = null

    private var animationMode: AnimationMode = AnimationMode.OFF
    private var drawMode: DrawMode = DrawMode.Normal
    private var animationDuration: Long = 5000L
    private var primaryColor: Int = Color.BLUE
    private var secondaryColor: Int = Color.GRAY

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val mapsFragment =
            childFragmentManager.findFragmentById(R.id.maps_view) as SupportMapFragment

        val btnStartPolyline = view.findViewById<Button>(R.id.btn_start_polyline)
        val radioGroupAnimation = view.findViewById<RadioGroup>(R.id.radio_group_stack)
        val radioGroupDraw = view.findViewById<RadioGroup>(R.id.radio_group_draw)
        val containerPrimaryColor = view.findViewById<LinearLayout>(R.id.container_color_primary)
        val containerSecondaryColor = view.findViewById<LinearLayout>(R.id.container_color_secondary)
        val inputDuration = view.findViewById<TextInputEditText>(R.id.input_duration)

        val viewPrimaryColor = view.findViewById<View>(R.id.view_color_primary)
        val viewSecondaryColor = view.findViewById<View>(R.id.view_color_secondary)

        viewPrimaryColor.setBackgroundColor(primaryColor)
        viewSecondaryColor.setBackgroundColor(secondaryColor)

        val colors = resources.getIntArray(R.array.colors)

        inputDuration.setText(animationDuration.toString())
        inputDuration.addTextChangedListener {
            val string = it.toString()
            try {
                animationDuration = string.toLong()
            } catch (e: NumberFormatException) {
            }
        }

        containerPrimaryColor.setOnClickListener {
            ColorSheet().colorPicker(
                colors = colors,
                selectedColor = primaryColor,
                listener = { color ->
                    primaryColor = color
                    viewPrimaryColor.setBackgroundColor(primaryColor)
                }
            ).show(childFragmentManager)
        }

        containerSecondaryColor.setOnClickListener {
            ColorSheet().colorPicker(
                colors = colors,
                selectedColor = secondaryColor,
                listener = { color ->
                    secondaryColor = color
                    viewSecondaryColor.setBackgroundColor(secondaryColor)
                }
            ).show(childFragmentManager)
        }

        lifecycleScope.launch {
            googleMap = mapsFragment.awaitMap().apply {
                uiSettings.isZoomControlsEnabled = true
            }

            val bound = LatLngBounds.Builder()
                .include(buaran)
                .include(depok)
                .build()

            val cameraUpdate = CameraUpdateFactory.newLatLngBounds(bound, 40.dp)
            googleMap.moveCamera(cameraUpdate)

            radioGroupAnimation.check(R.id.radio_stack_off)
            radioGroupAnimation.setOnCheckedChangeListener { group, checkedId ->
                when (checkedId) {
                    R.id.radio_stack_off -> animationMode = AnimationMode.OFF
                    R.id.radio_stack_block -> animationMode = AnimationMode.BLOCK
                    R.id.radio_stack_wait -> animationMode = AnimationMode.WAIT
                }
            }

            radioGroupDraw.check(R.id.radio_draw_normal)
            radioGroupDraw.setOnCheckedChangeListener { group, checkedId ->
                when (checkedId) {
                    R.id.radio_draw_normal -> drawMode = DrawMode.Normal
                    R.id.radio_draw_curved -> drawMode = DrawMode.Curved
                    R.id.radio_draw_lank -> drawMode = DrawMode.Lank
                }
            }

            btnStartPolyline.setOnClickListener {
                googleMap.clear()
                startAnimation()
            }
        }
    }

    private fun startAnimation() = lifecycleScope.launch {
        if (geometries == null) {
            placeRoute.searchRoute {
                startLocation = buaran.toLocation()
                endLocation = depok.toLocation()
                transportMode = TransportMode.CAR
            }.onSuccess {
                geometries = it.geometries
                createPolyline()
            }.onFailure {
                context?.toast(it.localizedMessage ?: "Error")
            }
        } else {
            createPolyline()
        }
    }

    private fun createPolyline() {
        val stackAnimatorMode = when (animationMode) {
            AnimationMode.OFF -> StackAnimationMode.OffStackAnimation
            AnimationMode.BLOCK -> StackAnimationMode.BlockStackAnimation
            AnimationMode.WAIT -> StackAnimationMode.WaitStackEndAnimation
        }

        val drawAnimationMode = when (drawMode) {
            DrawMode.Normal -> PolylineDrawMode.Normal
            DrawMode.Curved -> PolylineDrawMode.Curved
            DrawMode.Lank -> PolylineDrawMode.Lank
        }

        geometries?.let { geo ->
            googleMap.createPolylineAnimatorBuilder()
                .withPrimaryPolyline {
                    color(primaryColor)
                }
                .withAccentPolyline {
                    color(secondaryColor)
                }
                .createPolylineAnimator()
                .startAnimate(geo) {
                    stackAnimationMode = stackAnimatorMode
                    polylineDrawMode = drawAnimationMode
                    duration = animationDuration
                }
        }
    }
}