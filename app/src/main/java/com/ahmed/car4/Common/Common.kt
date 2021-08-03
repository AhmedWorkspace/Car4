package com.ahmed.car4.Common

import android.widget.TextView
import com.ahmed.car4.Model.AnimationModel
import com.ahmed.car4.Model.DriverGeoModel
import com.ahmed.car4.Model.RiderInfoModel
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import java.util.*
import kotlin.collections.HashMap
import kotlin.collections.HashSet

object Common {
    val driversSubscribe: MutableMap<String, AnimationModel> = HashMap<String, AnimationModel>()
    val markerList: MutableMap<String, Marker> = HashMap<String, Marker>()
    const val DRIVER_INFO_REFERENCE: String= "DRIVER-INFO"
    val driversFound: MutableSet<DriverGeoModel> = HashSet<DriverGeoModel>()
    const val DRIVERS_LOCATION_PREFERENCE: String= "DRIVER-LOCATION"
    var currentRider: RiderInfoModel? = null
    const val RIDER_INFO_REFERENCE: String="RIDER-INFO"

    fun BuildWelcomeMessage(): String{
        return  StringBuilder("Welcome, ")
            .append(currentRider!!.firstName)
            .append(" ")
            .append(currentRider!!.lastName)
            .toString()
    }

    fun buildName(firstName: String?, lastName: String?): String? {
        return StringBuilder(firstName!!)
            .append(" ")
            .append(lastName)
            .toString()

    }



    //GET BEARING
    fun getBearing(begin: LatLng, end: LatLng): Float {
        //You can copy this function by link at description
        val lat = Math.abs(begin.latitude - end.latitude)
        val lng = Math.abs(begin.longitude - end.longitude)
        if (begin.latitude < end.latitude && begin.longitude < end.longitude) return Math.toDegrees(
            Math.atan(lng / lat)
        )
            .toFloat() else if (begin.latitude >= end.latitude && begin.longitude < end.longitude) return (90 - Math.toDegrees(
            Math.atan(lng / lat)
        ) + 90).toFloat() else if (begin.latitude >= end.latitude && begin.longitude >= end.longitude) return (Math.toDegrees(
            Math.atan(lng / lat)
        ) + 180).toFloat() else if (begin.latitude < end.latitude && begin.longitude >= end.longitude) return (90 - Math.toDegrees(
            Math.atan(lng / lat)
        ) + 270).toFloat()
        return (-1).toFloat()
    }


    //DECODE POLY
    fun decodePoly(encoded: String): ArrayList<LatLng?> {
        val poly: ArrayList<LatLng?> = ArrayList<LatLng?>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0
        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].toInt() - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat
            shift = 0
            result = 0
            do {
                b = encoded[index++].toInt() - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng
            val p = LatLng(
                lat.toDouble() / 1E5,
                lng.toDouble() / 1E5
            )
            poly.add(p)
        }
        return poly
    }

    fun setWelcomeMessage(txtWelcome: TextView) {

        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 1..11 -> {
                txtWelcome.setText(java.lang.StringBuilder("Good Morning"))
            }
            in 12..16 -> {
                txtWelcome.setText(java.lang.StringBuilder("Good Afternoon"))
            }
            in 17..24 -> {
                txtWelcome.text = java.lang.StringBuilder("Good Evening")
            }
        }
    }

    fun formatDuration(duration: String): CharSequence? {
        if (duration.contains("mins")){
            return duration.substring(0,duration.length-1)
        }
        else
            return  duration
    }

    fun formatAddress(startAddress: String): CharSequence? {
        val  firstIndexComma = startAddress.indexOf(",")
        return startAddress.substring(0,firstIndexComma)

    }
}