package com.example.artravel.fragments

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.artravel.R
import com.example.artravel.constants.Constants
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
import kotlinx.android.synthetic.main.fragment_maps.*
import java.io.IOException
import java.util.*

/**
 * MapsFragment uses Google Maps Services to locate user and look for places
 *
 * @author Michael Lock
 * @date 23.02.2021
 */

@Suppress("DEPRECATION")
class MapsFragment : Fragment() {
    companion object {
        const val GOOGLE_API_KEY = Constants.GOOGLE_API_KEY
        private const val LOCATION_PERMISSION_REQUEST = 1
    }

    /*
    * Likely Places
    * */
    private lateinit var likelyPlaceNames: Array<String?>
    private lateinit var likelyPlaceAddresses: Array<String?>
    private lateinit var likelyPlaceAttributions: Array<List<*>?> //arrayOfNulls<List<*>?>(count)
    private lateinit var likelyPlaceLatLngs: Array<LatLng?>

    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback

    // Place Detection Client
    private lateinit var placesClient: PlacesClient
    private val callback = OnMapReadyCallback { googleMap ->
        map = googleMap
        getLocationAccess()
        // Use a custom info window adapter to handle multiple lines of text in the
        // info window contents.
        this.map.setInfoWindowAdapter(object : GoogleMap.InfoWindowAdapter {
            // Return null here, so that getInfoContents() is called next.
            override fun getInfoWindow(arg0: Marker): View? {
                return null
            }

            override fun getInfoContents(marker: Marker): View {
                // Inflate the layouts for the info window, title and snippet.
                val infoWindow = layoutInflater.inflate(
                    R.layout.custom_info_contents,
                    view?.findViewById<FrameLayout>(R.id.google_map), false
                )
                val title = infoWindow.findViewById<TextView>(R.id.title)
                title.text = marker.title
                val snippet = infoWindow.findViewById<TextView>(R.id.snippet)
                snippet.text = marker.snippet
                return infoWindow
            }
        })
    }

    /**
     * Calls methods that locate user and request frequent location updates
     *
     * This method is called when google maps is ready.
     * @author Michael Lock
     * @date 23.02.2021
     */

    private fun getLocationAccess() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION
            )
            == PackageManager.PERMISSION_GRANTED
        ) {
            map.isMyLocationEnabled = true
            getLocationUpdates()
            startLocationUpdates()
            Toast.makeText(
                view?.context,
                R.string.user_granted_permission,
                Toast.LENGTH_LONG
            )
                .show()

        } else
            ActivityCompat.requestPermissions(
                requireActivity(), arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST
            )
    }

    @SuppressLint("MissingPermission")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {

        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.contains(PackageManager.PERMISSION_GRANTED)) {
                map.isMyLocationEnabled = true
            } else {
                Toast.makeText(
                    view?.context,
                    R.string.user_not_grant_permission,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    /**
     * getLocationUpdates method requests user location every 3 seconds and
     * updates the drawn route from user to destination.
     *
     * This method is called when user has accepted the location permissions
     * @author Michael Lock
     * @date 23.02.2021
     */

    private fun getLocationUpdates() {
        locationRequest = LocationRequest()
        locationRequest.interval = 30000
        locationRequest.fastestInterval = 20000
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY



        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                // Geocode user location address
                activity ?: return
                if (locationResult.locations.isNotEmpty()) {
                    val location = locationResult.lastLocation
                    val latLng = LatLng(location.latitude, location.longitude)
//                    map.addMarker(
//                        MarkerOptions().position(latLng).title("Current Location")
//                    )
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {

        fusedLocationClient.lastLocation
            .addOnCompleteListener { task ->
                if (task.isSuccessful && task.result != null) {
                    val mLastLocation = task.result

                    var address = "No known address"

                    val gcd = Geocoder(activity, Locale.getDefault())
                    val addresses: List<Address>
                    try {
                        addresses = gcd.getFromLocation(
                            mLastLocation!!.latitude,
                            mLastLocation.longitude, 1
                        )
                        if (addresses.isNotEmpty()) {
                            address = addresses[0].getAddressLine(0)
                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }

                    map.addMarker(
                        MarkerOptions()
                            .position(
                                LatLng(
                                    mLastLocation!!.latitude,
                                    mLastLocation.longitude
                                )
                            )
                            .title("Current Location")
                            .snippet(address)
                    )

                    val cameraPosition = CameraPosition.Builder()
                        .target(
                            LatLng(
                                mLastLocation.latitude,
                                mLastLocation.longitude
                            )
                        )
                        .zoom(17f)
                        .build()
                    map.moveCamera(
                        CameraUpdateFactory.newCameraPosition(
                            cameraPosition
                        )
                    )
                } else {
                    Toast.makeText(
                        activity, "No current location found",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            null
        )
    }

    private val cancellationTokenSource by lazy {
        CancellationTokenSource()
    }

    override fun onStop() {
        super.onStop()

        // onStop or whenever you want to cancel the request
        cancellationTokenSource.cancel()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.current_place_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.option_get_place) {
            showCurrentPlace()
        }
        return true
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_maps, container, false)
        setHasOptionsMenu(true)

        return view
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment =
            childFragmentManager.findFragmentById(R.id.google_map) as SupportMapFragment?
        mapFragment?.getMapAsync(callback)

        // For locating and updating user location
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

        // Construct a PlacesClient
        Places.initialize(requireContext(), GOOGLE_API_KEY)
        placesClient = Places.createClient(requireContext())

        take_ar_image_handler.setOnClickListener {
            val intent = Intent(context, ArTakeImage::class.java)

            startActivity(intent)
        }
    }

    /**
     * Creates user AlertDialog of 5 places near to user
     *
     * This method is called when user pressed menu button in
     * right corner of the action bar
     *
     * This method is called when user location has been found.
     * @author Michael Lock
     * @date 23.02.2021
     */

    @SuppressLint("MissingPermission")
    private fun showCurrentPlace() {
        // Use fields to define the data types to return.
        val placeFields = listOf(Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG)
        // Use the builder to create a FindCurrentPlaceRequest.
        val request = FindCurrentPlaceRequest.newInstance(placeFields)
        // Get the likely places - that is, the businesses and other points of interest that
        // are the best match for the device's current location.
        val placeResult = placesClient.findCurrentPlace(request)
        placeResult.addOnCompleteListener { task ->
            if (task.isSuccessful && task.result != null) {
                val likelyPlaces = task.result
                // Set the count, handling cases where less than 5 entries are returned.
                val count = if (likelyPlaces != null && likelyPlaces.placeLikelihoods.size < 5) {
                    likelyPlaces.placeLikelihoods.size
                } else {
                    5
                }
                var i = 0
                likelyPlaceNames = arrayOfNulls(count)
                likelyPlaceAddresses = arrayOfNulls(count)
                likelyPlaceAttributions = arrayOfNulls<List<*>?>(count)
                likelyPlaceLatLngs = arrayOfNulls(count)
                for (placeLikelihood in likelyPlaces?.placeLikelihoods ?: emptyList()) {
                    // Build a list of likely places to show the user.
                    likelyPlaceNames[i] = placeLikelihood.place.name
                    likelyPlaceAddresses[i] = placeLikelihood.place.address
                    likelyPlaceAttributions[i] = placeLikelihood.place.attributions
                    likelyPlaceLatLngs[i] = placeLikelihood.place.latLng
                    i++
                    if (i > count - 1) {
                        break
                    }
                }
                // Show a dialog offering the user the list of likely places, and add a
                // marker at the selected place.
                openPlacesDialog()
            } else {
                Log.e("DBG", "Exception: %s", task.exception)
            }
        }
    }

    /**
     * openPlacesDialog draws AlertDialog screen for user to choose where
     * wants to set Marker on Map
     *
     * This method is called at end of showCurrentPlace method.
     *
     * @author Michael Lock
     * @date 23.02.2021
     */

    private fun openPlacesDialog() {
        // Ask the user to choose the place where they are now.
        val listener =
            DialogInterface.OnClickListener { _, which -> // The "which" argument contains the position of the selected item.
                val markerLatLng = likelyPlaceLatLngs[which]
                var markerSnippet = likelyPlaceAddresses[which]
                if (likelyPlaceAttributions[which] != null) {
                    markerSnippet = """
                $markerSnippet
                ${likelyPlaceAttributions[which]}
                """.trimIndent()
                }
                // Add a marker for the selected place, with an info window
                // showing information about that place.
                map.addMarker(
                    MarkerOptions()
                        .title(likelyPlaceNames[which])
                        .position(markerLatLng!!)
                        .snippet(markerSnippet)
                )
                // Position the map's camera at the location of the marker.
                map.moveCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        markerLatLng,
                        15.toFloat()
                    )
                )
            }
        // Display the dialog.
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.pick_place)
            .setItems(likelyPlaceNames, listener)
            .show()
    }
}

