package com.example.translinktrack

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Thread.sleep(1000)
        installSplashScreen() // Splashscreen API
        setContentView(R.layout.activity_main)

        // Google Maps map fragment setup
        val mapFragment: SupportMapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // btnFind and etSearch used together to search for live bus data
        val btnFind = findViewById<Button>(R.id.btnFind)

        val etSearch = findViewById<EditText>(R.id.etSearch)

        btnFind.setOnClickListener {
            // Create a request to send using OkHttp client
            // (stored in "client" val
            var userInput = etSearch.text.toString()

            // Lines 45 and 46 ensure that a 3 digit value is always used as
            // when making the HTTPS request (i.e. if the user enters 33 instead of
            // 033, the necessary "0" will be added to the front of the input)
            if(userInput.length != 3) { userInput = "0" + userInput }

            getBusData(etSearch, userInput) // send GET request through OkHttp client
        }
    }

    private fun getBusData(et: TextView, input: String) {
        // Build request
        var URL = "https://api.translink.ca/rttiapi/v1/buses?apikey="+translinkAPIKey+"&routeNo"+input
        var request = Request
            .Builder()
            .url(URL)
            .build()

        var call = client.newCall(request).enqueue(object: Callback {
            override fun onFailure(call: Call, e: IOException) {
                Toast.makeText(this@MainActivity, "Something went wrong...", Toast.LENGTH_SHORT)
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                   Log.d("iony", "${response.body!!.string()}")
                }
            }

        })
    }

    // When map ready, pan to Vancouver to easily show requested buses
    override fun onMapReady(p0: GoogleMap) {
        mMap = p0
        val vancouver = LatLng(49.2829, -123.1207)
        mMap.moveCamera(CameraUpdateFactory.newLatLng(vancouver))
        mMap.setMinZoomPreference(11.2F)
    }
}