package hk.hkuce.sdp

import android.Manifest
import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.transition.ChangeBounds
import android.transition.Transition
import android.transition.TransitionManager
import android.view.MenuItem
import android.view.View
import android.view.animation.AnticipateOvershootInterpolator
import android.widget.*
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.app.ActivityCompat
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuth.AuthStateListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.maps.android.SphericalUtil
import java.security.KeyStore.TrustedCertificateEntry
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var currentLocation: Location
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private var permissionCode = 101
    private var fab: FloatingActionButton? = null
    private lateinit var database: DatabaseReference
    private var report: Button? = null
    private var edit: Button? = null
    private var navigate: Button? = null
    private lateinit var toggle : ActionBarDrawerToggle
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var firebaseAuth: FirebaseAuth
    private var username: String? = null

    private lateinit var constraintLayout: ConstraintLayout
    private lateinit var infoLayout: ConstraintLayout

    private lateinit var close: ImageButton
    private lateinit var typeTextView: TextView
    private lateinit var dateTextView: TextView
    private lateinit var startingTimeTextView: TextView
    private lateinit var endingTimeTextView: TextView
    private lateinit var detailsTextView: TextView
    private lateinit var usernameTextView: TextView
    private lateinit var distanceTextView: TextView
    private lateinit var warningTextView: TextView

    private var items: List<String> = listOf("Any","Dessert","Snack","Beverage","Others")
    private lateinit var autoCompleteTextView: AutoCompleteTextView
    private var itemSelected: String? = null

    private lateinit var setTime: Button
    private lateinit var resetTime: ImageButton
    private lateinit var timeEditText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        if(ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION)!=
            PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION)!=
            PackageManager.PERMISSION_GRANTED){

            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),permissionCode)
            return
        }

        else{
            init()
        }


    }

    private fun init(){
        supportActionBar!!.title = "Main activity"

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        navigate = findViewById(R.id.navigate)
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.nav_view)
        constraintLayout = findViewById(R.id.constraintLayout)
        infoLayout = findViewById(R.id.infoLayout)
        close = findViewById(R.id.close)
        typeTextView = findViewById(R.id.typeTextView)
        dateTextView = findViewById(R.id.dateTextView)
        startingTimeTextView = findViewById(R.id.startingTimeTextView)
        endingTimeTextView = findViewById(R.id.endingTimeTextView)
        detailsTextView = findViewById(R.id.detailsTextView)
        usernameTextView = findViewById(R.id.usernameTextView)
        distanceTextView = findViewById(R.id.distanceTextView)
        warningTextView = findViewById(R.id.warningTextView)

        report = findViewById(R.id.report)
        edit = findViewById(R.id.edit)

        firebaseAuth = FirebaseAuth.getInstance()

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        toggle = ActionBarDrawerToggle(this, drawerLayout, R.string.open, R.string.close)
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        navigationView.setNavigationItemSelectedListener {
            when(it.itemId){
                R.id.log_in -> {
                    startActivity(Intent(this, LogInActivity::class.java))
                }
                R.id.log_out -> {
                    val builder = AlertDialog.Builder(this)
                    builder.setTitle("Confirm")
                    builder.setMessage("Are you sure to log out?")
                    builder.setPositiveButton("Yes") { dialog, which ->
                        firebaseAuth.signOut()
                    }
                    builder.setNegativeButton("No") { dialog, _ -> dialog.cancel() }
                    builder.show()
                }
                R.id.upload -> {
                    val intent = Intent(this, ReportActivity::class.java)
                    startActivity(intent)
                }
                R.id.manage -> {
                    val intent = Intent(this, EditActivity::class.java)
                    startActivity(intent)
                }
                else -> {}
            }
            return@setNavigationItemSelectedListener true
        }

        val authStateListener = firebaseAuth.addAuthStateListener{
            if (firebaseAuth.currentUser == null){
                username = null
                navigationView.getHeaderView(0).findViewById<TextView>(R.id.display_name)
                    .text = "Guest"
                navigationView.menu.findItem(R.id.log_in).isVisible = true
                navigationView.menu.findItem(R.id.log_out).isVisible = false
                navigationView.menu.findItem(R.id.upload).isVisible = false
                navigationView.menu.findItem(R.id.manage).isVisible = false
                report!!.isVisible = false
                edit!!.isVisible = false
                setMarkers()
            }
            else{
                username = firebaseAuth.currentUser!!.email.toString()
                navigationView.getHeaderView(0).findViewById<TextView>(R.id.display_name)
                    .text = username
                navigationView.menu.findItem(R.id.log_in).isVisible = false
                navigationView.menu.findItem(R.id.log_out).isVisible = true
                navigationView.menu.findItem(R.id.upload).isVisible = true
                navigationView.menu.findItem(R.id.manage).isVisible = true
                report!!.isVisible = true
                edit!!.isVisible = true
                setMarkers()
            }
        }
        report!!.setOnClickListener {
            if (firebaseAuth.currentUser == null){
                val builder = AlertDialog.Builder(this)
                builder.setTitle("Log-in required")
                builder.setMessage("This function is for authenticated vendors only, " +
                        "proceed to log-in page?")
                builder.setPositiveButton("Yes") { dialog, which ->
                    startActivity(Intent(this, LogInActivity::class.java))
                }
                builder.setNegativeButton("No") { dialog, _ -> dialog.cancel() }
                builder.show()
            }
            else{
                val intent = Intent(this, ReportActivity::class.java)
                startActivity(intent)
            }
        }
        edit!!.setOnClickListener {
            if (firebaseAuth.currentUser == null){
                val builder = AlertDialog.Builder(this)
                builder.setTitle("Log-in required")
                builder.setMessage("This function is for authenticated vendors only, " +
                        "proceed to log-in page?")
                builder.setPositiveButton("Yes") { dialog, which ->
                    startActivity(Intent(this, LogInActivity::class.java))
                }
                builder.setNegativeButton("No") { dialog, _ -> dialog.cancel() }
                builder.show()
            }
            else{
                val intent = Intent(this, EditActivity::class.java)
                startActivity(intent)
            }
        }
        setTime = findViewById(R.id.setTime)
        timeEditText = findViewById(R.id.timeEditText)
        resetTime = findViewById(R.id.resetTime)

        val adapter = ArrayAdapter(this,R.layout.list_item,items)

        autoCompleteTextView = findViewById(R.id.autoCompleteTextView)
        autoCompleteTextView.setAdapter(adapter)
        autoCompleteTextView.onItemClickListener = AdapterView.OnItemClickListener{
                adapterView, view, i, l ->
            itemSelected = adapterView.getItemAtPosition(i).toString()
            setMarkers()
        }
        var cal = Calendar.getInstance()
        val dateSetListener = DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
            cal.set(Calendar.YEAR, year)
            cal.set(Calendar.MONTH, monthOfYear)
            cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            val timeSetListener = TimePickerDialog.OnTimeSetListener { timePicker, hour, minute->
                cal.set(Calendar.HOUR_OF_DAY, hour)
                cal.set(Calendar.MINUTE, minute)
                var currentTime = System.currentTimeMillis()
                val selectedTime = cal.time
                if (selectedTime.time < currentTime){
                    Toast.makeText(
                        this@MapsActivity,
                        "Selected time should be at least one minute after current time!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                else{
                    timeEditText.setText(SimpleDateFormat("dd/MM/yyyy-HH:mm").format(cal.time))
                    setMarkers()
                }
            }
            val timePickerDialog = TimePickerDialog(this@MapsActivity, timeSetListener,
            cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true)
            timePickerDialog.show()
        }

        setTime.setOnClickListener {
            cal = Calendar.getInstance()
            val datePickerDialog = DatePickerDialog(this@MapsActivity, dateSetListener,
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH))
            datePickerDialog.datePicker.minDate = cal.timeInMillis
            datePickerDialog.show()
        }
        resetTime.setOnClickListener {
            timeEditText.setText("Now")
            setMarkers()
        }
    }

    @SuppressLint("MissingPermission")
    private fun setMarkers(){

        mMap.clear()
        database.child("Locations").get().addOnSuccessListener {
            for (location in it.children){
                var currentTime = System.currentTimeMillis()
                var endTime = SimpleDateFormat("dd/MM/yyyy-HH:mm")
                    .parse(location.child("date").value.toString()+"-"+
                            location.child("endingTime").value.toString())
                if (endTime.time < currentTime) {
                    var data = HashMap<String, Any>()
                    data[location.key.toString()] = location.value as Any
                    database.child("Expired").setValue(data).addOnCompleteListener {
                        database.child("Locations").child(location.key.toString()).removeValue()
                    }
                    continue
                }
                if (itemSelected != null && itemSelected != "Any" && location.child("type").
                    value.toString() != itemSelected){
                    continue
                }
                var selectedTime: Long
                var startingTime = SimpleDateFormat("dd/MM/yyyy-HH:mm")
                    .parse(location.child("date").value.toString()+"-"+
                            location.child("startingTime").value.toString())
                var endingTime = SimpleDateFormat("dd/MM/yyyy-HH:mm")
                    .parse(location.child("date").value.toString()+"-"+
                            location.child("endingTime").value.toString())
                if (timeEditText.text.toString() == "Now"){
                    selectedTime = System.currentTimeMillis()
                }
                else{
                    selectedTime = SimpleDateFormat("dd/MM/yyyy-HH:mm").parse(timeEditText.text.toString()).time
                }
                if (startingTime.time > selectedTime || endingTime.time < selectedTime){
                    continue
                }
                var latLng = LatLng(
                    location.child("Lat").value.toString().toDoubleOrNull()!!,
                    location.child("Lng").value.toString().toDoubleOrNull()!!
                )
                if (location.child("type").value.toString() == "Dessert"){
                    mMap.addMarker(
                        MarkerOptions()
                            .position(latLng)
                            .title("Selected marker")
                            .snippet(location.key)
                            .icon(
                                BitmapDescriptorFactory
                                    .defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                    )
                }
                else if (location.child("type").value.toString() == "Snack"){
                    mMap.addMarker(
                        MarkerOptions()
                            .position(latLng)
                            .title("Selected marker")
                            .snippet(location.key)
                            .icon(
                                BitmapDescriptorFactory
                                    .defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                    )
                }else if (location.child("type").value.toString() == "Beverage"){
                    mMap.addMarker(
                        MarkerOptions()
                            .position(latLng)
                            .title("Selected marker")
                            .snippet(location.key)
                            .icon(
                                BitmapDescriptorFactory
                                    .defaultMarker(BitmapDescriptorFactory.HUE_RED))
                    )
                }else if (location.child("type").value.toString() == "Others"){
                    mMap.addMarker(
                        MarkerOptions()
                            .position(latLng)
                            .title("Selected marker")
                            .snippet(location.key)
                            .icon(
                                BitmapDescriptorFactory
                                    .defaultMarker(BitmapDescriptorFactory.HUE_YELLOW))
                    )
                }
            }
        }.addOnFailureListener {
            Toast.makeText(this@MapsActivity, it.message, Toast.LENGTH_SHORT).show()
        }
//        database.child("Locations").addValueEventListener(object: ValueEventListener {
//            override fun onDataChange(snapshot: DataSnapshot) {
//                setMarkers()
//            }
//            override fun onCancelled(error: DatabaseError) {
//                Toast.makeText(this@MapsActivity, error.message, Toast.LENGTH_SHORT).show()
//            }
//        })
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
            permissionCode -> if(grantResults.isNotEmpty() && grantResults[0] ==
                    PackageManager.PERMISSION_GRANTED){
                init()
            }
        }
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

        val constraintSet = ConstraintSet()

        mMap.setOnMarkerClickListener { marker ->
            marker.showInfoWindow()
            mMap.moveCamera(CameraUpdateFactory.newLatLng(LatLng(marker.position.latitude,marker.position.longitude)))

            var info: DatabaseReference = database.child("Locations").child(marker.snippet.toString())
            info.get().addOnSuccessListener {

                val getLocation= fusedLocationProviderClient.lastLocation.addOnSuccessListener(this) { location ->
                    if (location == null)
                        Toast.makeText(this, "Cannot get location.", Toast.LENGTH_SHORT).show()
                    else {
                        currentLocation = location
                        var latLng = LatLng(currentLocation.latitude, currentLocation.longitude)
                        var distance = SphericalUtil.computeDistanceBetween(latLng, marker.position)
                        typeTextView.text = "Type: " + it.child("type").value.toString()
                        dateTextView.text = "Date: " + it.child("date").value.toString()
                        startingTimeTextView.text = "Starting time: " + it.child("startingTime").value.toString()
                        endingTimeTextView.text = "Ending time: " + it.child("endingTime").value.toString()
                        detailsTextView.text = "Other details: " + it.child("details").value.toString()
                        usernameTextView.text = "Vendor's username: " + it.child("username").value.toString()
                        distanceTextView.text = "Distance: " + distance.toInt().toString() + " m"
                        var currentTime = System.currentTimeMillis()
                        if (timeEditText.text.toString() != "Now"){
                            currentTime = SimpleDateFormat("dd/MM/yyyy-HH:mm").parse(timeEditText.text.toString()).time
                        }
                        var endingTime = SimpleDateFormat("dd/MM/yyyy-HH:mm")
                            .parse(it.child("date").value.toString()+"-"+
                                    it.child("endingTime").value.toString())
                        var diff = endingTime.time - currentTime
                        var sec = diff/1000
                        var min = (sec/60).toInt()
                        if (min <= 30){
                            warningTextView.text = "Warning: the vendor may leave in about ${min.toString()} minutes"
                            warningTextView.visibility = View.VISIBLE
                        }
                        else{
                            warningTextView.visibility = View.GONE
                        }
                        constraintSet.clone(constraintLayout)
                        constraintSet.clear(infoLayout.id, ConstraintSet.TOP)
                        constraintSet.connect(infoLayout.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
                        val transition: Transition = ChangeBounds()
                        transition.interpolator = AnticipateOvershootInterpolator(1.0f)
                        transition.duration = 1200
                        TransitionManager.beginDelayedTransition(constraintLayout, transition)
                        constraintSet.applyTo(constraintLayout)

                        navigate!!.setOnClickListener {
                            if (distance > 1000){
                                val builder = AlertDialog.Builder(this)
                                builder.setTitle("Too far")
                                builder.setMessage("The distance is too far for an AR walking trip, " +
                                        "would you like to jump to Google Maps' directions instead?")
                                builder.setPositiveButton("Yes") { dialog, which ->
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(
                                        "https://www.google.com/maps/dir/?api=1&destination=" +
                                                marker.position.latitude.toString() + "," +
                                                marker.position.longitude.toString()))
                                    intent.setPackage("com.google.android.apps.maps")
                                    startActivity(intent)
                                }
                                builder.setNegativeButton("No") { dialog, _ -> dialog.cancel() }
                                builder.show()
                            }else{
                                val builder = AlertDialog.Builder(this)
                                builder.setTitle("Choose navigation type")
                                builder.setMessage("Would you like to have an AR navigation or jump to Google Maps?")
                                builder.setPositiveButton("AR navigation") { _, _ ->
                                    val intent = Intent(this, NavigationActivity::class.java).apply {
                                        putExtra("Lat", (marker.position.latitude.toString()))
                                        putExtra("Lng", (marker.position.longitude.toString()))
                                    }
                                    startActivity(intent)
                                }
                                builder.setNeutralButton("Google Maps"){ _, _ ->
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(
                                        "https://www.google.com/maps/dir/?api=1&destination=" +
                                                marker.position.latitude.toString() + "," +
                                                marker.position.longitude.toString()))
                                    intent.setPackage("com.google.android.apps.maps")
                                    startActivity(intent)
                                }
                                builder.setNegativeButton("No") { dialog, _ -> dialog.cancel() }
                                builder.show()
                            }
                        }
                    }
                }.addOnFailureListener {
                    Toast.makeText(this@MapsActivity, it.message, Toast.LENGTH_SHORT).show()
                }
                close.setOnClickListener {
                    marker.hideInfoWindow()
                }
            }.addOnFailureListener {
                Toast.makeText(this@MapsActivity, it.message, Toast.LENGTH_SHORT).show()
            }

            true
        }
        mMap.setOnInfoWindowCloseListener {
            constraintSet.clone(constraintLayout)
            constraintSet.clear(infoLayout.id, ConstraintSet.BOTTOM)
            constraintSet.connect(infoLayout.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
            val transition: Transition = ChangeBounds()
            transition.interpolator = AnticipateOvershootInterpolator(1.0f)
            transition.duration = 1200
            TransitionManager.beginDelayedTransition(constraintLayout, transition)
            constraintSet.applyTo(constraintLayout)
            constraintSet.clear(infoLayout.id, ConstraintSet.BOTTOM)
            constraintSet.connect(infoLayout.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
        }

        database = Firebase.database.reference

        setMarkers()

        val getLocation= fusedLocationProviderClient.lastLocation.addOnSuccessListener(this) { location ->
            if (location == null)
                Toast.makeText(this, "Cannot get location.", Toast.LENGTH_SHORT).show()
            else {
                currentLocation = location
                var latLng = LatLng(currentLocation.latitude,currentLocation.longitude)
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng,13f))
            }
        }

        fab = findViewById(R.id.fab)
        fab!!.setOnClickListener {
            if (timeEditText.text.toString() != "Now"){
                var currentTime = System.currentTimeMillis()
                val selectedTime = SimpleDateFormat("dd/MM/yyyy-HH:mm").parse(timeEditText.text.toString())
                if (selectedTime.time < currentTime){
                    timeEditText.setText("Now")
                }
            }
            setMarkers()
        }

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (toggle.onOptionsItemSelected(item)){
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}