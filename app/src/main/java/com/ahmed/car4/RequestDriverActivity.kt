package com.ahmed.car4


import android.Manifest
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Layout
import android.util.Log
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.ahmed.car4.Common.Common
import com.ahmed.car4.Model.EventBus.SelectedPlaceEvent
import com.ahmed.car4.Remote.IGoogleAPI
import com.ahmed.car4.Remote.RetrofitClient

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.ahmed.car4.databinding.ActivityRequestDriverBinding
import com.google.android.gms.maps.model.*
import com.google.android.material.snackbar.Snackbar
import com.google.maps.android.ui.IconGenerator
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.json.JSONObject
import java.util.ArrayList

class RequestDriverActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var txt_origin:TextView
    private lateinit var txt_time:TextView


    private lateinit var binding: ActivityRequestDriverBinding

    private val compositeDisposable = CompositeDisposable()
    private var selectedPlaceEvent:SelectedPlaceEvent?=null
    private lateinit var iGoogleAPI: IGoogleAPI
    private var blackPolyline:Polyline?=null
    private var greyPolyline:Polyline?=null
    private var polylineOptions:PolylineOptions?=null
    private var blackPolylineOptions:PolylineOptions?=null
    private var polylineList:ArrayList<LatLng?>? =null
    private var originMarker:Marker?=null
    private var destinationMarker:Marker?=null
    private lateinit var  mapFragment:SupportMapFragment



    override fun onStart() {
        if (!EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().register(this)
        super.onStart()
    }

    override fun onStop() {
        compositeDisposable.clear()
        if (EventBus.getDefault().hasSubscriberForEvent(SelectedPlaceEvent::class.java))
        {
            EventBus.getDefault().removeStickyEvent(SelectedPlaceEvent::class.java)
        }
        EventBus.getDefault().unregister(this)
        super.onStop()
    }
    @Subscribe(sticky = true,threadMode = ThreadMode.MAIN)

    fun onSelectPlaceEvent(event: SelectedPlaceEvent){
        selectedPlaceEvent = event
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityRequestDriverBinding.inflate(layoutInflater)
        setContentView(binding.root)

        init()

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun init() {
        iGoogleAPI = RetrofitClient.instance!!.create(IGoogleAPI::class.java)

        val view =   layoutInflater.inflate(R.layout.layout_confirm_pickup,null)
        layoutInflater.inflate(R.layout.layout_confirm_ride,null)

        val btn_confirm_ride =  view.findViewById<View>(R.id.btn_confirm_ride) as Button
        //view.findViewById<View>(R.id.btn_confirm_pickup) as Button

        val confirm_pickup_layout =  view.findViewById<View>(R.id.confirm_pickup_layout)
        val confirm_ride_layout =  view.findViewById<View>(R.id.confirm_ride_layout)
//        //Event
        btn_confirm_ride.setOnClickListener {
            confirm_pickup_layout.visibility = View.VISIBLE
            confirm_ride_layout.visibility= View.GONE
        }
        setDataPickup()

    }

    private fun setDataPickup() {
        val view =  layoutInflater.inflate(R.layout.layout_confirm_ride,null)
        val txt_address_pickup =  view.findViewById<View>(R.id.btn_confirm_ride) as TextView
        val txt_origin= view.findViewById<View>(R.id.txt_origin) as TextView

        txt_address_pickup.text = if(txt_origin != null){
            txt_origin.text
        } else "None"
        mMap.clear()
        addPickupMarker()
    }

    private fun addPickupMarker() {
        val view =  layoutInflater.inflate(R.layout.pickup_info_window,null)
        val generator =  IconGenerator(this)
        generator.setContentView(view)
        generator.setBackground(ColorDrawable(Color.TRANSPARENT))
        val icon  = generator.makeIcon()
        originMarker = mMap.addMarker(MarkerOptions()
            .icon(BitmapDescriptorFactory.fromBitmap(icon))
            .position(selectedPlaceEvent!!.origin))

    }


    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap


        drawpath(selectedPlaceEvent!!)


        try {
            val success = googleMap.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    this,
                    R.raw.maps_style
                )
            )
            if (!success)
                Log.e("ERROR", "Style Parsing Error")
        } catch (e: Resources.NotFoundException) {
            Log.e("ERROR", "" + e.message)
        }

    }

    private fun drawpath(selectedPlaceEvent: SelectedPlaceEvent) {
        //request Api
        compositeDisposable.add(iGoogleAPI.getDirections("driving",
            "less_driving",
            selectedPlaceEvent.originString,
            selectedPlaceEvent.destinationString,
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
                        polylineList = Common.decodePoly(polyline)


                    }

                    polylineOptions = PolylineOptions()
                    polylineOptions!!.color(Color.GRAY)
                    polylineOptions!!.width(12f)
                    polylineOptions!!.startCap(SquareCap())
                    polylineOptions!!.jointType(JointType.ROUND)
                    polylineOptions!!.addAll(polylineList)
                    greyPolyline = mMap.addPolyline(polylineOptions)

                    blackPolylineOptions = PolylineOptions()
                    blackPolylineOptions!!.color(Color.BLACK)
                    blackPolylineOptions!!.width(12f)
                    blackPolylineOptions!!.startCap(SquareCap())
                    blackPolylineOptions!!.jointType(JointType.ROUND)
                    blackPolylineOptions!!.addAll(polylineList)
                    blackPolyline = mMap.addPolyline(blackPolylineOptions)

                    val valueAnimator = ValueAnimator.ofInt(0,100)
                    valueAnimator.duration = 1100
                    valueAnimator.repeatCount = ValueAnimator.INFINITE
                    valueAnimator.interpolator = LinearInterpolator()
                    valueAnimator.addUpdateListener { value ->
                        val points = greyPolyline!!.points
                        val percentValue= value.animatedValue.toString().toInt()
                        val size = points.size
                        val newpoints = (size* (percentValue/100.0f)).toInt()
                        val p=points.subList(0,newpoints)
                        blackPolyline!!.points = (p)


                    }
                    valueAnimator.start()

                    val latLngBound = LatLngBounds.builder().include(selectedPlaceEvent.origin)
                        .include(selectedPlaceEvent.destination)
                        .build()

                    //Add car icon for origin
                    val objects= jsonArray.getJSONObject(0)
                    val legs = objects.getJSONArray("legs")
                    val legsObject = legs.getJSONObject(0)

                    val time = legsObject.getJSONObject("duration")
                    val duration = time.getString("text")

                    val start_address = legsObject.getString("start_address")
                    val end_address = legsObject.getString("end_address")

                    addOriginMarker(duration,start_address)

                    addDestinationMarker(end_address)

                    mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(latLngBound,160))
                    mMap.moveCamera(CameraUpdateFactory.zoomTo(mMap.cameraPosition!!.zoom-1))





                }catch (e:java.lang.Exception){
                    Toast.makeText(this,e.message!!, Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun addDestinationMarker(endAddress: String) {
        val view =  layoutInflater.inflate(R.layout.destination_info_window,null)
        val txt_destination =  view.findViewById<View>(R.id.txt_destination) as TextView
        txt_destination.text = Common.formatAddress(endAddress)


        val generator =  IconGenerator(this)
        generator.setContentView(view)
        generator.setBackground(ColorDrawable(Color.TRANSPARENT))
        val icon  = generator.makeIcon()
        destinationMarker = mMap.addMarker(MarkerOptions().icon(BitmapDescriptorFactory.fromBitmap(icon))
            .position(selectedPlaceEvent!!.destination))

    }

    private fun addOriginMarker(duration: String, startAddress: String) {

        val view =  layoutInflater.inflate(R.layout.origin_info_window,null)

        txt_time =  view.findViewById<View>(R.id.txt_time) as TextView
        txt_origin =  view.findViewById<View>(R.id.txt_origin) as TextView


        txt_time.text = Common.formatDuration(duration)
        txt_origin.text = Common.formatAddress(startAddress)

        val generator =  IconGenerator(this)
        generator.setContentView(view)
        generator.setBackground(ColorDrawable(Color.TRANSPARENT))
        val icon  = generator.makeIcon()
        originMarker = mMap.addMarker(MarkerOptions().icon(BitmapDescriptorFactory.fromBitmap(icon))
            .position(selectedPlaceEvent!!.origin))
    }


}