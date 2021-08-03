package com.ahmed.car4.ui.home

import android.Manifest
import android.animation.ValueAnimator
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.ahmed.car4.Callback.FirebaseDriverInfoListener
import com.ahmed.car4.Callback.FirebaseFailedListener
import com.ahmed.car4.Common.Common
import com.ahmed.car4.Model.AnimationModel
import com.ahmed.car4.Model.DriverGeoModel
import com.ahmed.car4.Model.DriverInfoModel
import com.ahmed.car4.Model.EventBus.SelectedPlaceEvent
import com.ahmed.car4.Model.GeoQueryModel
import com.ahmed.car4.R
import com.ahmed.car4.Remote.IGoogleAPI
import com.ahmed.car4.Remote.RetrofitClient
import com.ahmed.car4.RequestDriverActivity
import com.ahmed.car4.RiderHomeActivity
import com.ahmed.car4.databinding.FragmentHomeBinding
import com.firebase.geofire.GeoFire
import com.firebase.geofire.GeoLocation
import com.firebase.geofire.GeoQueryEventListener
import com.google.android.gms.common.api.Status
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.*
import com.google.firebase.database.core.Context
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import com.sothree.slidinguppanel.SlidingUpPanelLayout
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable.fromIterable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.greenrobot.eventbus.EventBus
import org.json.JSONObject
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList


class HomeFragment : Fragment(), OnMapReadyCallback, FirebaseDriverInfoListener {
    private lateinit var mMap: GoogleMap
    private lateinit var mapFragment: SupportMapFragment
    private lateinit var btn_inflate:Button

    //location
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    //Load Driver //
    var distance = 1.0
    val LIMIT_RANGE = 10.0
    var previousLocation: Location? = null
    var currentLocation: Location? = null
    var firsttime = true

    //Listener
    lateinit var iFirebaseDriverInfoListener: FirebaseDriverInfoListener
    lateinit var iFirebaseFailedListener: FirebaseFailedListener

    private var cityName = ""

    //Retrofit
    private val compositeDisposable = CompositeDisposable()
    private lateinit var iGoogleAPI: IGoogleAPI

    //SlidingUp
    private lateinit var slidingUpPanelLayout:SlidingUpPanelLayout
    private lateinit var txt_welcome: TextView
    private lateinit var autocompleteSupportFragment: AutocompleteSupportFragment



    override fun onDestroy() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        super.onDestroy()
    }

    private lateinit var homeViewModel: HomeViewModel
    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root


        init()
        initViews(root)
        mapFragment = childFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)


        return root
    }

    private fun initViews(root: View) {
        slidingUpPanelLayout = root.findViewById(R.id.activity_main) as SlidingUpPanelLayout
        txt_welcome = root.findViewById(R.id.txt_welcome) as TextView

        Common.setWelcomeMessage(txt_welcome)
    }

    private fun init() {


        Places.initialize(requireContext(),getString(R.string.google_maps_key))
        autocompleteSupportFragment = childFragmentManager.findFragmentById(R.id.autocomplete_fragment) as AutocompleteSupportFragment
        autocompleteSupportFragment.setPlaceFields(Arrays.asList(
            Place.Field.ID,
            Place.Field.ADDRESS,
            Place.Field.LAT_LNG,
            Place.Field.NAME))
        autocompleteSupportFragment.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(p0: Place) {
//                Snackbar.make(requireView()," "+p0.latLng, Snackbar.LENGTH_LONG).show()
                if (ActivityCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    Snackbar.make(requireView(),getString(R.string.permission_require),Snackbar.LENGTH_SHORT).show()
                    return
                }
                fusedLocationProviderClient
                    .lastLocation.addOnSuccessListener { location->
                        val origin = LatLng(location.latitude,location.longitude)
                        val destination = LatLng(p0.latLng!!.latitude,p0.latLng!!.longitude)

                        startActivity(Intent(requireContext(),RequestDriverActivity::class.java))
                        EventBus.getDefault().postSticky(SelectedPlaceEvent(origin,destination))
                    }
            }

            override fun onError(p0: Status) {
                Snackbar.make(requireView(),p0.statusMessage!!, Snackbar.LENGTH_LONG).show()
            }


        })


        iGoogleAPI = RetrofitClient.instance!!.create(IGoogleAPI::class.java)


        iFirebaseDriverInfoListener = this



        //Location
        locationRequest = LocationRequest()
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
        locationRequest.setFastestInterval(3000)
        locationRequest.interval = (5000)
        locationRequest.setSmallestDisplacement(10f)
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                val newPos = LatLng(
                    locationResult.lastLocation.latitude,
                    locationResult.lastLocation.longitude
                )
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(newPos, 18f))

//Load All Drivers In City
                if (firsttime) {
                    previousLocation = locationResult.lastLocation
                    currentLocation = locationResult.lastLocation

                    setRestrictPlacesInCountry(locationResult.lastLocation)
                    firsttime = false
                } else {
                    previousLocation = currentLocation
                    currentLocation = locationResult.lastLocation
                }
                if (previousLocation!!.distanceTo(currentLocation) / 1000 <= LIMIT_RANGE) {
                    loadAvailableDrivers()
                }

            }


        }

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireContext())
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            //Snackbar.make(requireView(),getString(R.string.permission_require),Snackbar.LENGTH_SHORT).show()
            return
        }
        fusedLocationProviderClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.myLooper()
        )
        loadAvailableDrivers();


    }

    private fun setRestrictPlacesInCountry(location: Location?) {
        try{
            val geocoder = Geocoder(requireContext(), Locale.getDefault())
            var addressList= geocoder.getFromLocation(location!!.latitude, location.longitude, 1)
            if (addressList.size > 0){

                autocompleteSupportFragment.setCountry(addressList[0].countryCode)
            }



        }catch (e:IOException){
            e.printStackTrace()
        }


    }

    private fun loadAvailableDrivers() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Snackbar.make(
                requireView(),
                getString(R.string.permission_require),
                Snackbar.LENGTH_SHORT
            ).show()
            return
        }
        fusedLocationProviderClient.lastLocation
            .addOnFailureListener { e ->
                Snackbar.make(requireView(), e.message!!, Snackbar.LENGTH_SHORT).show()

            }.addOnSuccessListener { location ->
//Load Available Drivers in city
                val geocoder = Geocoder(requireContext(), Locale.getDefault())
                var addressList: List<Address> = ArrayList()
                try {
                    addressList = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                    if (addressList.size > 0)
                        cityName = addressList[0].locality

                    //Query
                    if (!TextUtils.isEmpty(cityName)) {
                        val driver_location_ref = FirebaseDatabase.getInstance()
                            .getReference(Common.DRIVERS_LOCATION_PREFERENCE)
                            .child(cityName)
                        val gf = GeoFire(driver_location_ref)
                        val geoQuery = gf.queryAtLocation(
                            GeoLocation(location.latitude, location.longitude),
                            distance
                        )
                        geoQuery.removeAllListeners()

                        geoQuery.addGeoQueryEventListener(object : GeoQueryEventListener {
                            override fun onKeyEntered(key: String?, location: GeoLocation?) {
                                Common.driversFound.add(DriverGeoModel(key!!, location!!))
                            }

                            override fun onKeyExited(key: String?) {

                            }

                            override fun onKeyMoved(key: String?, location: GeoLocation?) {

                            }

                            override fun onGeoQueryReady() {
                                if (distance <= LIMIT_RANGE) {
                                    distance++
                                    loadAvailableDrivers()
                                } else {
                                    distance = 0.0
                                    addDriverMarker()
                                }
                            }

                            override fun onGeoQueryError(error: DatabaseError?) {
                                Snackbar.make(
                                    requireView(),
                                    error!!.message,
                                    Snackbar.LENGTH_SHORT
                                ).show()

                            }


                        })

                        driver_location_ref.addChildEventListener(object : ChildEventListener {
                            override fun onChildAdded(
                                snapshot: DataSnapshot,
                                previousChildName: String?
                            ) {
                                //New Driver
                                val geoQueryModel = snapshot.getValue(GeoQueryModel::class.java)
                                val geolocation =
                                    GeoLocation(geoQueryModel!!.l!![0], geoQueryModel!!.l!![1])
                                val driverGeoModel = DriverGeoModel(snapshot.key, geolocation)
                                val newDriverLocation = Location("")
                                newDriverLocation.latitude = geolocation.latitude
                                newDriverLocation.longitude = geolocation.longitude

                                val newDistance = location.distanceTo(newDriverLocation) / 1000
                                if (newDistance <= LIMIT_RANGE) findDriverByKey(driverGeoModel)
                            }

                            override fun onChildRemoved(snapshot: DataSnapshot) {
                            }

                            override fun onChildChanged(
                                snapshot: DataSnapshot,
                                previousChildName: String?
                            ) {
                            }

                            override fun onChildMoved(
                                snapshot: DataSnapshot,
                                previousChildName: String?
                            ) {
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Snackbar.make(
                                    requireView(),
                                    error.message,
                                    Snackbar.LENGTH_SHORT
                                ).show()
                            }

                        })

                    }
                    else
                    {
                        Snackbar.make(
                            requireView(),
                            getString(R.string.city_name_not_found),
                            Snackbar.LENGTH_SHORT
                        ).show()
                    }

                } catch (e: IOException) {
                    Snackbar.make(
                        requireView(),
                        getString(R.string.permission_require),
                        Snackbar.LENGTH_SHORT
                    ).show()

                }
            }

    }

    private fun addDriverMarker() {
        if (Common.driversFound.size>0 ){
            fromIterable(Common.driversFound)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    {

                            driverGeoModel: DriverGeoModel? ->
                        findDriverByKey(driverGeoModel)
                    },
                    {
                            t: Throwable? ->
                        Snackbar.make(requireView(),t!!.message!!,Snackbar.LENGTH_SHORT).show()
                    }


                )
        }
        else{
            Snackbar.make(requireView(),getString(R.string.driver_not_found),Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun findDriverByKey(driverGeoModel: DriverGeoModel?) {
        FirebaseDatabase.getInstance().getReference(Common.DRIVER_INFO_REFERENCE)
            .child(driverGeoModel!!.key!!)
            .addListenerForSingleValueEvent(object: ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    if(snapshot.hasChildren()){
                        driverGeoModel.driverInfoModel= (snapshot.getValue(DriverInfoModel::class.java))
                        iFirebaseDriverInfoListener.onDriverInfoLoadSuccess(driverGeoModel)

                    }
                    else{
                        iFirebaseFailedListener.onFirebaseFailed(getString(R.string.key_not_found)+driverGeoModel.key)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    iFirebaseFailedListener.onFirebaseFailed(error.message)
                }

            })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        //Request Permission
        Dexter.withContext(context)
            .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            .withListener(object : PermissionListener {

                override fun onPermissionGranted(p0: PermissionGrantedResponse?) {

                    //Enable Button
                    if (ActivityCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {

                        return
                    }

                    mMap.isMyLocationEnabled = true
                    mMap.uiSettings.isMyLocationButtonEnabled = true
                    mMap.setOnMapLongClickListener {
                        fusedLocationProviderClient.lastLocation
                            .addOnFailureListener { e ->
                                Toast.makeText(context!!, "" + e.message, Toast.LENGTH_SHORT).show()
                            }.addOnSuccessListener { location ->
                                val userLatLng = LatLng(location.latitude, location.longitude)
                                mMap.animateCamera(
                                    CameraUpdateFactory.newLatLngZoom(
                                        userLatLng,
                                        18f
                                    )
                                )
                            }
                        true
                    }

                    //layout
                    val view = mapFragment.requireView()
                        .findViewById<View>("1".toInt())!!
                        .parent as View
                    val locationButton = view.findViewById<View>("2".toInt())
                    val params = locationButton.layoutParams as RelativeLayout.LayoutParams
                    params.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0)
                    params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE)
                    params.bottomMargin = 350


                }

                override fun onPermissionDenied(p0: PermissionDeniedResponse?) {
                    Toast.makeText(
                        context!!,
                        "Permission " + p0!!.permissionName + "was denied",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                override fun onPermissionRationaleShouldBeShown(
                    p0: PermissionRequest?,
                    p1: PermissionToken?
                ) {

                }

            }).check()

//Enable Zoom
        mMap.uiSettings.isZoomControlsEnabled= true
        try {
            val success = googleMap.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    context,
                    R.raw.maps_style
                )
            )
            if (!success)
                Log.e("ERROR", "Style Parsing Error")
        } catch (e: Resources.NotFoundException) {
            Log.e("ERROR", "" + e.message)
        }

    }

    override fun onDriverInfoLoadSuccess(driverGeoModel: DriverGeoModel?) {
        if(!Common.markerList.containsKey(driverGeoModel!!.key))
        {
            Common.markerList.put(driverGeoModel!!.key!!,
                mMap.addMarker(MarkerOptions()
                    .position(LatLng(driverGeoModel.geoLocation!!.latitude,
                        driverGeoModel.geoLocation!!.longitude))
                    .flat(true)
                    .title(Common.buildName(driverGeoModel.driverInfoModel!!.firstName,
                        driverGeoModel.driverInfoModel!!.lastName   ))
                    .snippet(driverGeoModel.driverInfoModel!!.phoneNumber)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.car))))

            if(!TextUtils.isEmpty(cityName)){
                val driverLocation = FirebaseDatabase.getInstance()
                    .getReference(Common.DRIVERS_LOCATION_PREFERENCE)
                    .child(cityName)
                    .child(driverGeoModel!!.key!!)
                driverLocation.addValueEventListener(object : ValueEventListener{
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (!snapshot.hasChildren())
                        {
                            if (Common.markerList[driverGeoModel!!.key!!] != null)
                            {
                                val marker= Common.markerList[driverGeoModel!!.key!!]
                                marker!!.remove()//Remove marker
                                Common.markerList.remove(
                                    driverGeoModel!!.key!!
                                )//remove marker Information

                                Common.driversSubscribe.remove(driverGeoModel.key!!)//Retrofit-Remove driver Information
                                driverLocation.removeEventListener(this)

                            }
                        }
                        else{
                            if (Common.markerList[driverGeoModel!!.key!!] != null){
                                val geoQueryModel = snapshot!!.getValue(GeoQueryModel::class.java)
                                val animationModel = AnimationModel(false,geoQueryModel!!)
                                if (Common.driversSubscribe[driverGeoModel.key!!] != null)
                                {
                                    val marker = Common.markerList[driverGeoModel.key!!]
                                    val  oldPosition = Common.driversSubscribe[driverGeoModel.key!!]

                                    val from = StringBuilder()
                                        .append(oldPosition!!.geoQueryModel!!.l?.get(0))
                                        .append(" , ")
                                        .append(oldPosition!!.geoQueryModel!!.l?.get(1))
                                        .toString()
                                    val to = StringBuilder()
                                        .append(animationModel.geoQueryModel!!.l?.get(0))
                                        .append(" , ")
                                        .append(animationModel.geoQueryModel!!.l?.get(1))
                                        .toString()

                                    moveMarkerAnimation(driverGeoModel.key!!,animationModel,marker,from
                                        ,to)
                                }
                                else{
                                    Common.driversSubscribe.put(driverGeoModel.key!!,animationModel)
                                }

                            }


                        }

                    }

                    override fun onCancelled(error: DatabaseError) {
                        Snackbar.make( requireView(),error.message,Snackbar.LENGTH_SHORT).show()
                    }

                })
            }

        }
    }

    private fun moveMarkerAnimation(
        key: String,
        newData: AnimationModel,
        marker: Marker?,
        from: String,
        to: String) {
        if (newData.isRun){

            //request Api
            compositeDisposable.add(iGoogleAPI.getDirections("driving",
                "less_driving",
                from,to,
                getString(R.string.google_maps_key))!!
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { returnResult->
                    Log.d("API_RETURN", returnResult)
                    try {
                        val jsonObject = JSONObject(returnResult)
                        val jsonArray = jsonObject.getJSONArray("routes")
                        for (i in 0 until jsonArray.length()) {
                            val route = jsonArray.getJSONObject(i)
                            val poly = route.getJSONObject("overview_polyline")
                            val polyline = poly.getString("points")
                            newData.polylineList = Common.decodePoly(polyline)

                        }
                        //handler = Handler()
                        newData.index = -1
                        newData.next = -1
                        val runnable = object:Runnable{
                            override fun run() {
                                if (newData.polylineList != null && newData.polylineList!!.size>1){

                                    if (newData.index < newData.polylineList!!.size -2){
                                        newData.index++
                                        newData.next= newData.index+1
                                        newData.start = newData.polylineList!![newData.index]!!
                                        newData.end = newData.polylineList!![newData.next]!!
                                    }
                                    val valueAnimator = ValueAnimator.ofInt(0,1)
                                    valueAnimator.duration= 3000
                                    valueAnimator.interpolator = LinearInterpolator()
                                    valueAnimator.addUpdateListener { value ->
                                        newData.v = value.animatedFraction
                                        newData.lat = newData.v*newData.end!!.latitude + (1-newData.v)* newData.start!!.latitude
                                        newData.lng = newData.v*newData.end!!.longitude + (1-newData.v)* newData.start!!.longitude
                                        val newPos = LatLng(newData.lat,newData.lng)
                                        marker!!.position = newPos
                                        marker!!.setAnchor(0.5f,0.5f)
                                        marker.rotation = Common.getBearing(newData.start!!,newPos)
                                    }
                                    valueAnimator.start()
                                    if (newData.index<newData.polylineList!!.size-2)
                                        newData.handler!!.postDelayed(this,1500)
                                    else if (newData.index<newData.polylineList!!.size-1){
                                        newData.isRun = false
                                        Common.driversSubscribe.put(key,newData)//update

                                    }

                                }
                            }
                        }
                        newData.handler!!.postDelayed(runnable, 1500)


                    }catch (e:java.lang.Exception){
                        Snackbar.make(requireView(), e.message!!, Snackbar.LENGTH_LONG).show()
                    }
                })
        }

    }
}

