package com.jzheadley.headsup

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.location.Location
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import io.nlopez.smartlocation.SmartLocation
import io.nlopez.smartlocation.location.config.LocationAccuracy
import io.nlopez.smartlocation.location.config.LocationParams
import io.nlopez.smartlocation.location.providers.LocationGooglePlayServicesProvider
import kotlinx.android.synthetic.main.activity_main.*
import java.io.InputStream
import java.io.OutputStream
import java.util.*
import kotlin.math.abs


const val logt = "MainActivity"


class MainActivity : AppCompatActivity() {

    var mmSocket: BluetoothSocket? = null
    var mmDevice: BluetoothDevice? = null
    var mBluetoothAdapter: BluetoothAdapter? = null

    val provider = LocationGooglePlayServicesProvider()
    var params: LocationParams =
        LocationParams.Builder().setDistance(0f).setInterval(100L).setAccuracy(LocationAccuracy.HIGH)
            .build()

    val MY_UUID = UUID.fromString("94f39d29-7d6d-437d-973b-fba39e49d4ee") //Standard SerialPortService ID
    private var outputStream: OutputStream? = null
    private var inStream: InputStream? = null

    var oldLocation: Location? = null


    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        mBluetoothAdapter?.cancelDiscovery()
        mmDevice = mBluetoothAdapter?.getRemoteDevice("00:15:83:D2:50:D7")
        mmSocket = mmDevice?.createRfcommSocketToServiceRecord(MY_UUID);
        mmSocket?.connect()

        outputStream = mmSocket?.outputStream;
        inStream = mmSocket?.inputStream;

    }

    override fun onDestroy() {
        super.onDestroy()
        mmSocket?.close()
    }
    override fun onStart() {
        super.onStart()
        SmartLocation.with(this)
            .location(provider)
            .config(params)
            .continuous()
            .start {
                run {
                    locationUpdate(it)
                }
            }

    }

    fun getSpeed(currentLocation: Location, oldLocation: Location): Double {
        val newLat = currentLocation.latitude
        val newLon = currentLocation.longitude
        val oldLat = oldLocation.latitude
        val oldLon = oldLocation.longitude

        if (currentLocation.hasSpeed()) {
            return currentLocation.speed.toDouble()
        } else {
            val radius = 6371000.0
            val dLat = Math.toRadians(newLat - oldLat)
            val dLon = Math.toRadians(newLon - oldLon)
            val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(Math.toRadians(newLat)) * Math.cos(
                Math.toRadians(oldLat)
            ) *
                    Math.sin(dLon / 2) * Math.sin(dLon / 2)
            val c = 2 * Math.asin(Math.sqrt(a))
            val distance = Math.round(radius * c).toDouble()

            val timeDifferent = (currentLocation.time - oldLocation.time).toDouble()
            return distance / timeDifferent * 1000
        }
    }


    @SuppressLint("SetTextI18n")
    fun locationUpdate(location: Location) {
        val speedVal = oldLocation?.let { getSpeed(it, location) }
        val bearingVal = oldLocation?.bearingTo(location)
        Log.d(
            logt,
            "bearing is " + location.hasBearing() + " " + location.bearing + " Altitude is " + location.hasAltitude() + " " + location.altitude + " Latitude is " + location.latitude + " Longitude is " + location.longitude + " speed is " + location.hasSpeed() + " " + speedVal
        )
        oldLocation = location
        speed.text = "Speed ${speedVal.toString()}"
        bearing.text = "Bearing ${bearingVal.toString()}"
        if (speedVal != null && bearingVal != null) {
            var message =
                "{\"lat\":" + location.latitude + "," +
                        "\"long\":" + location.longitude + "," +
                        "\"speed\":" + abs(speedVal) + "," +
                        "\"bearing\":" + bearingVal + "" +
                        "}"
            outputStream?.write(message.toByteArray())
        }
    }

}
