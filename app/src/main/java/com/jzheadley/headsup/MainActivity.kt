package com.jzheadley.headsup

import android.annotation.SuppressLint
import android.location.Location
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import io.nlopez.smartlocation.SmartLocation
import io.nlopez.smartlocation.location.config.LocationParams
import io.nlopez.smartlocation.location.providers.LocationGooglePlayServicesProvider
import kotlinx.android.synthetic.main.activity_main.*
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.view.View
import java.io.IOException
import java.io.InputStream
import java.util.*
import kotlin.math.abs


const val logt = "MainActivity"


val delimiter: Byte = 33
var readBufferPosition = 0

var mmSocket: BluetoothSocket? = null
var mmDevice: BluetoothDevice? = null

class MainActivity : AppCompatActivity() {

    internal inner class workerThread(private val btMsg: String) : Runnable {

        override fun run() {
            sendBtMsg(btMsg)
        }
    };


    var oldLocation: Location? = null

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
            Thread(
                workerThread(
                    (
                            "{" +
                                    "\"lat\":" + location.latitude + "," +
                                    "\"long\":" + location.longitude + "," +
                                    "\"speed\":" + abs(speedVal) + "," +
                                    "\"bearing\":" + bearingVal + "" +
                                    "}")
                )
            ).start()
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val provider = LocationGooglePlayServicesProvider()
        SmartLocation.with(this)
            .location(provider)
            .config(LocationParams.NAVIGATION)
            .continuous()
            .start {
                run {
                    locationUpdate(it)
                }
            }

        val mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        val pairedDevices = mBluetoothAdapter.bondedDevices
        if (pairedDevices.size > 0) {
            for (device in pairedDevices) {
                Log.d(logt, device.name)
                if (device.name == "raspberrypi")
                //Note, you will need to change this to match the name of your device
                {
                    Log.e(logt, device.name)
                    mmDevice = device
                    break
                }
            }
        }
        startBluetooth.setOnClickListener {
            Thread(workerThread("blah")).start()
        }

    }

    fun sendBtMsg(msg2send: String) {
//        val uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"); //Standard SerialPortService ID
        val uuid = UUID.fromString("94f39d29-7d6d-437d-973b-fba39e49d4ee") //Standard SerialPortService ID
        try {
            Log.d(logt, "Sending bluetooth message")
            mmSocket = mmDevice?.createRfcommSocketToServiceRecord(uuid)
            if (!mmSocket?.isConnected!!) {
                mmSocket!!.connect()
            }

            val mmOutputStream = mmSocket!!.getOutputStream()
            mmOutputStream.write(msg2send.toByteArray())

        } catch (e: IOException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
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

}
