// © Ion Basha 2023

package com.example.translinktrack

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.IOException

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap

    private val translinkAPIKey = "cN3DIyLQj6vKvcIv4s1b"

    private val client = OkHttpClient()

    private var markerList = ArrayList<Marker>()

    private lateinit var btnFind: Button

    private lateinit var etSearch: EditText

    private var prevInput = ""

    var busList = ArrayList<Bus>()

    private val vancouver = LatLng(49.2829, -123.1207)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Thread.sleep(1000)
        installSplashScreen() // Splashscreen API
        setContentView(R.layout.activity_main)

        // Google Maps map fragment setup
        val mapFragment: SupportMapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // btnFind and etSearch used together to search for live bus data
        btnFind = findViewById<Button>(R.id.btnFind)

        etSearch = findViewById<EditText>(R.id.etSearch)

        btnFind.setOnClickListener {
            // LINES 63 TO 73 ARE COURTESY OF GEEKSFORGEEKS:
            // https://www.geeksforgeeks.org/how-to-close-or-hide-android-soft-keyboard-with-kotlin/ ***

            val view: View? = this.currentFocus

            // on below line checking if view is not null.
            if (view != null) {
                // on below line we are creating a variable
                // for input manager and initializing it.
                val inputMethodManager =
                    getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager

                // on below line hiding our keyboard.
                inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0)
            }

            // Create a request to send using OkHttp client
            // (stored in "client" val
            var userInput = etSearch.text.toString()

            // In case the user enters 33 instead of
            // 033 for example, the necessary "0" will be added to the front of the input)
            if(userInput.length != 3 && userInput.get(0) != 'R') { userInput = "0" + userInput }

            getBusData(etSearch, userInput) // send GET request through OkHttp client
        }
    }

    private fun getBusData(et: TextView, input: String) {
        // Build request
        var URL = "https://api.translink.ca/rttiapi/v1/buses?apikey="+translinkAPIKey+"&routeNo="+input
        var request = Request
            .Builder()
            .url(URL)
            .build()

        var call = client.newCall(request).enqueue(object: Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                   parseXML(response)
                }
            }

        })
    }

    private fun parseXML(xmlFile: Response) {
        var parser = XmlPullParserFactory.newInstance().newPullParser()
        var event = parser.eventType
        parser.setInput(xmlFile.body?.byteStream(), "UTF-8")

        // Flags to determine if upcoming TEXT event is a lat, long, destination or direction
        var latIsNext = false
        var longIsNext = false
        var destIsNext = false
        var directionIsNext = false

        // Store longitude and latitude values which will be used to highlight buses on map fragment

        // Dummy object
        var bus = Bus(0.0, 0.0, null, null)

        // Loop through XML documents and pull lat/long values
        while(event != XmlPullParser.END_DOCUMENT) {

            if(event == XmlPullParser.START_TAG && parser.name == "Latitude") {
                latIsNext = true
            }
            else if(event == XmlPullParser.START_TAG && parser.name == "Longitude") {
                longIsNext = true
            }
            else if(event == XmlPullParser.START_TAG && parser.name == "Destination") {
                destIsNext = true
            }
            else if(event == XmlPullParser.START_TAG && parser.name == "Direction") {
                directionIsNext = true
            }
            else if(event == XmlPullParser.TEXT && latIsNext) {
                bus.lat = parser.text.toDouble()
                latIsNext = false
            }
            else if(event == XmlPullParser.TEXT && longIsNext) {
                bus.long = parser.text.toDouble()
                longIsNext = false
                // Add Bus object to list since the Longitude is the last
                // value we want to parse
                busList += bus
                bus = Bus(0.0, 0.0, null, null)
                Log.d("issue", "${busList.size}")
            }
            else if(event == XmlPullParser.TEXT && destIsNext) {
                bus.dest = parser.text
                destIsNext = false
            }
            else if(event == XmlPullParser.TEXT && directionIsNext) {
                bus.direction = parser.text
                directionIsNext = false
            }
            event = parser.next()
        }
        Log.d("issue", "${busList.size}")

        // Update map with bus locations
        runOnUiThread(object: Runnable {
            override fun run() {
                Log.d("issue", "${busList.size}")
                // Clear map prev map markers before adding new ones
                if(markerList.isNotEmpty()) {
                    for(m in markerList) { m.remove() }
                }
                mMap.clear()

                for(i in busList.indices) {
                    var coordinate = LatLng(busList[i].lat, busList[i].long)
                    var markerOptions = MarkerOptions()
                        .position(coordinate)
                        .icon(BitmapFromVector(applicationContext, R.drawable.bus_marker))
                        .title(busList[i].dest)
                        .snippet(busList[i].direction)
                    var marker: Marker = mMap.addMarker(markerOptions)!!
                    markerList += marker
                }

                if(!prevIsEqual(vancouver)) {
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(vancouver, 11.2F))
                }

                busList.clear()
                prevInput = etSearch.text.toString()
            }
        })

    }

    // If searching for new bus, readjust map zoom level: else leave as is
    private fun prevIsEqual(location: LatLng) : Boolean {
        return prevInput == etSearch.text.toString()
    }

    // *** COURTESY OF GEEKSFORGEEKS:
    // https://www.geeksforgeeks.org/how-to-add-custom-marker-to-google-maps-in-android/ ***
    private fun BitmapFromVector(context: Context, vectorResId:Int): BitmapDescriptor? {
        //drawable generator
        var vectorDrawable: Drawable
        vectorDrawable = ContextCompat.getDrawable(context,vectorResId)!!
        vectorDrawable.setBounds(0,0,vectorDrawable
            .intrinsicWidth,vectorDrawable.intrinsicHeight)
        //bitmap generator
        var bitmap: Bitmap
        bitmap = Bitmap.createBitmap(vectorDrawable
            .intrinsicWidth,vectorDrawable
            .intrinsicHeight,Bitmap
            .Config.ARGB_8888)
        //canvas genaret
        var canvas:Canvas
        //pass bitmap in canvas constructor
        canvas = Canvas(bitmap)
        //pass canvas in drawable
        vectorDrawable.draw(canvas)
        //return BitmapDescriptorFactory
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    // Removes all markers from the map
    private fun removeMarkers() {
        mMap.clear()
    }

    // When map ready, pan to Vancouver to show requested buses
    override fun onMapReady(p0: GoogleMap) {
        mMap = p0

        mMap.moveCamera(CameraUpdateFactory.newLatLng(vancouver))
        mMap.setMinZoomPreference(11.2F)
    }


}