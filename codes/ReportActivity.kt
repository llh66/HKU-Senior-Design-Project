package hk.hkuce.sdp

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Location
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.firebase.database.DatabaseReference


class ReportActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var currentLocation: Location
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private var cancel: Button? = null
    private var confirm: Button? = null
    private var receiver: BroadcastReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report)

        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                finish()
            }
        }
        val filter = IntentFilter()
        filter.addAction("FINISH")
        registerReceiver(receiver, filter)

        supportActionBar!!.title = "Location"
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }
    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.setMinZoomPreference(10f)
        val hkBounds = LatLngBounds(
            LatLng(22.1, 113.8),  // SW bounds
            LatLng(22.6, 114.5) // NE bounds
        )
        mMap.setLatLngBoundsForCameraTarget(hkBounds)
//        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(hkBounds.center, 11f))

        mMap.isMyLocationEnabled = true

        val getLocation= fusedLocationProviderClient.lastLocation.addOnSuccessListener(this) { location ->
            if (location == null)
                Toast.makeText(this, "Cannot get location.", Toast.LENGTH_SHORT).show()
            else {
                currentLocation = location
                var latLng = LatLng(currentLocation.latitude,currentLocation.longitude)
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng,16f))
            }
        }

        confirm = findViewById(R.id.confirm)
        confirm!!.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Confirm")
            builder.setMessage("Are you sure to report the location?")

            builder.setPositiveButton("Yes") { dialog, which ->

                val camPos = mMap.cameraPosition
                val intent = Intent(this, ReportActivity2::class.java).apply {
                    putExtra("Lat", camPos.target.latitude)
                    putExtra("Lng", camPos.target.longitude)
                }
                startActivity(intent)
            }

            builder.setNegativeButton("No") { dialog, _ -> dialog.cancel() }
            builder.show()
        }

        cancel = findViewById(R.id.cancel)
        cancel!!.setOnClickListener {
            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
    }
}