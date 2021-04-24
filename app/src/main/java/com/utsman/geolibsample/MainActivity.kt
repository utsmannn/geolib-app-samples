package com.utsman.geolibsample

import android.Manifest
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.material.appbar.AppBarLayout
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem
import com.mikepenz.materialdrawer.model.SectionDrawerItem
import com.mikepenz.materialdrawer.model.interfaces.iconRes
import com.mikepenz.materialdrawer.model.interfaces.nameText
import com.mikepenz.materialdrawer.util.addItems
import com.mikepenz.materialdrawer.widget.MaterialDrawerSliderView
import com.utsman.geolib.location.PlacesLocation
import com.utsman.geolib.marker.toLatLng
import com.utsman.geolibsample.utils.FakeTrafficLocator
import com.utsman.geolibsample.utils.getStatusBarHeight
import com.utsman.geolibsample.utils.toast
import com.utsman.geolibsample.viewmodel.GrabPickupViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val grabPickupViewModel: GrabPickupViewModel by viewModels()
    private val fakeStatusBar by lazy { findViewById<View>(R.id.fake_status_bar) }
    private val toolbar by lazy { findViewById<Toolbar>(R.id.toolbar) }
    private val appBar by lazy { findViewById<AppBarLayout>(R.id.appbar) }
    private val drawerLayout by lazy { findViewById<DrawerLayout>(R.id.drawer_layout) }
    private val drawerView by lazy { findViewById<MaterialDrawerSliderView>(R.id.nav_view) }
    private lateinit var navController: NavController

    @Inject
    lateinit var placeLocation: PlacesLocation

    fun openDrawer() {
        drawerLayout.openDrawer(drawerView)
    }

    private val destinationChangedListener =
        NavController.OnDestinationChangedListener { _, destination, _ ->
            lifecycleScope.launch {
                delay(500)
                when (destination.id) {
                    R.id.grabPickupFragment,
                    R.id.grabListPickerFragment,
                    R.id.grabConfirmPickerFragment,
                    R.id.grabResultFragment -> {
                        appBar.isVisible = false
                    }
                    else -> {
                        appBar.isVisible = true
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val statusBarHeight = getStatusBarHeight()
        fakeStatusBar.layoutParams.height = statusBarHeight
        fakeStatusBar.requestLayout()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.apply {
                clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
                addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    decorView.systemUiVisibility =
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or 0 or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                } else {
                    decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                }
                statusBarColor = Color.TRANSPARENT
            }
        }

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
        navController.addOnDestinationChangedListener(destinationChangedListener)


        toolbar.run {
            setSupportActionBar(this)
            navigationIcon =
                ContextCompat.getDrawable(this@MainActivity, R.drawable.ic_round_menu_24)
            setNavigationOnClickListener {
                openDrawer()
            }
        }

        grabPickupViewModel.bindPlaceLocation(placeLocation)
        grabPickupViewModel.getCurrentLocation()
        grabPickupViewModel.currentLocation.observe(this) { location ->
            grabPickupViewModel.searchSuggestion()
        }

        val icHome = R.drawable.ic_round_home_24
        val icLocation = R.drawable.ic_round_beenhere_24
        val icRoute = R.drawable.ic_round_directions_24
        val icPolyline = R.drawable.ic_round_polyline_24
        val icMarker = R.drawable.ic_round_marker_24
        val icGrab = R.drawable.ic_grab_icon

        val itemHome = createMenu("Home", 1, icHome) {
            navController.navigate(R.id.homeFragment)
        }

        // ------ start library

        val itemLocation = createMenu("Location", 2, icLocation) {
            navController.navigate(R.id.locationFragment)
        }

        val itemRoutes = createMenu("Routes", 3, icRoute) {
            navController.navigate(R.id.routesListFragment)
        }

        val itemPolyline = createMenu("Polyline", 4, icPolyline) {
            navController.navigate(R.id.polylineFragment)
        }

        val itemMarker = createMenu("Marker", 5, icMarker) {
            navController.navigate(R.id.markerFragment)
        }

        // ------ end library


        // ------ start sample
        val itemGrabLocation = createMenu("Grab Pickup", 6, icGrab) {
            navController.navigate(R.id.grabPickupFragment)
        }


        val itemSectionLibraries = SectionDrawerItem().apply {
            nameText = "Libraries"
            divider = false
        }

        val itemSectionUsecase = SectionDrawerItem().apply {
            nameText = "Sample Usecase"
            divider = false
        }

        val header = LayoutInflater.from(this).inflate(R.layout.nav_header, null)
        drawerView.run {
            headerView = header
            addItems(
                itemHome,
                itemSectionLibraries,
                itemLocation,
                itemRoutes,
                itemPolyline,
                itemMarker,
                itemSectionUsecase,
                itemGrabLocation
            )
            setSelection(1)
        }

        Dexter.withContext(this)
            .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            .withListener(object : PermissionListener {
                override fun onPermissionGranted(p0: PermissionGrantedResponse?) {
                    toast("Permission granted, application ready")
                }

                override fun onPermissionDenied(p0: PermissionDeniedResponse?) {
                    toast("Permission denied, application closed")
                    finish()
                }

                override fun onPermissionRationaleShouldBeShown(
                    p0: PermissionRequest?,
                    p1: PermissionToken?
                ) {
                }

            })
            .check()
    }

    private fun createMenu(
        title: String,
        id: Long,
        iconResource: Int,
        action: () -> Unit
    ): PrimaryDrawerItem {
        val colorList =
            ColorStateList.valueOf(ContextCompat.getColor(this@MainActivity, R.color.primary))
        return PrimaryDrawerItem().apply {
            isIconTinted = true
            nameText = title
            identifier = id
            iconRes = iconResource
            iconColor = colorList
            onDrawerItemClickListener = { _, _, _ ->
                lifecycleScope.launch {
                    delay(700)
                    supportActionBar?.title = title
                    action.invoke()
                }
                false
            }
            textColor = colorList
        }
    }

    override fun onBackPressed() {
        grabPickupViewModel.setReadyPickup(false)
        if (navController.currentDestination?.id == R.id.grabConfirmPickerFragment) {
            navController.popBackStack(R.id.grabPickupFragment, false)
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        navController.removeOnDestinationChangedListener(destinationChangedListener)
    }
}