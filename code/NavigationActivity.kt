package hk.hkuce.sdp

import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouterCallback
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.arrival.ArrivalObserver
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.dropin.EmptyBinder
import com.mapbox.navigation.dropin.NavigationView
import kotlinx.android.synthetic.main.activity_navigation.*
import java.util.concurrent.ExecutorService
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin


//class NavigationActivity : AppCompatActivity(), OnMapReadyCallback , SensorEventListener{
class NavigationActivity : AppCompatActivity(), SensorEventListener{

    private lateinit var mMap: GoogleMap
    private var currentLocation: Location? = null
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private var lat: Double? = null
    private var lng: Double? = null

    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService

    private lateinit var sensorManager: SensorManager
    private val accelerometerReading = FloatArray(3)
    private val magnetometerReading = FloatArray(3)
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)
    private var imageView: ImageView?= null
    private var azimuth: Float = 0f
    private var currentAzimuth: Float = 0f

    private var navigationView: NavigationView? = null

    private var stepLat: Double? = null
    private var stepLng: Double? = null
    private val cameraProviderResult = registerForActivityResult(ActivityResultContracts.RequestPermission()){ permissionGranted->
        if(permissionGranted){
            startCamera()
        }else {
            finish()
        }
    }

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_navigation)

        supportActionBar!!.title = "AR navigation"
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        cameraProviderResult.launch(android.Manifest.permission.CAMERA)

        imageView = findViewById(R.id.imageView)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        lat = intent.getDoubleExtra("Lat", 0.0)
        lng = intent.getDoubleExtra("Lng", 0.0)
        val getLocation= fusedLocationProviderClient.lastLocation.addOnSuccessListener(this) { location ->
            if (location == null){
                Toast.makeText(this, "Cannot get location.", Toast.LENGTH_SHORT).show()
                finish()
            }
            else {
                currentLocation = location
                val destination = Point.fromLngLat(lng!!.toDouble(),lat!!.toDouble())
                val originPoint = Point.fromLngLat(currentLocation!!.longitude,currentLocation!!.latitude)
                navigationView = findViewById(R.id.navigationView)
                navigationView!!.customizeViewBinders {
//                    infoPanelTripProgressBinder = EmptyBinder()
                    infoPanelEndNavigationButtonBinder = EmptyBinder()
                    maneuverBinder = EmptyBinder()
                    actionToggleAudioButtonBinder = EmptyBinder()
                    actionCameraModeButtonBinder = EmptyBinder()
                    roadNameBinder = EmptyBinder()
                }
                MapboxNavigationApp.current()!!.requestRoutes(
                    routeOptions = RouteOptions.builder()
                        .applyDefaultNavigationOptions()
                        .profile(DirectionsCriteria.PROFILE_WALKING)
                        .coordinatesList(listOf(originPoint, destination))
                        .alternatives(true)
                        .build(),
                    callback = object : NavigationRouterCallback {
                        override fun onCanceled(routeOptions: RouteOptions, routerOrigin: RouterOrigin) {
                            Toast.makeText(this@NavigationActivity, "Route request cancelled!", Toast.LENGTH_SHORT).show()
                            finish()
                        }

                        override fun onFailure(reasons: List<RouterFailure>, routeOptions: RouteOptions) {
                            Toast.makeText(this@NavigationActivity, "Route request failed!", Toast.LENGTH_SHORT).show()
                            finish()
                        }

                        override fun onRoutesReady(
                            routes: List<NavigationRoute>,
                            routerOrigin: RouterOrigin
                        ) {
                            navigationView!!.api.startActiveGuidance(routes)
                        }
                    }
                )

                val routeProgressObserver = object : RouteProgressObserver {
                    @SuppressLint("MissingPermission")
                    override fun onRouteProgressChanged(routeProgress: RouteProgress) {
//                        Toast.makeText(this@NavigationActivity, routeProgress.upcomingStepPoints.toString()+"\n"
//                            +routeProgress.currentLegProgress?.currentStepProgress?.step?.maneuver()
//                            ?.location()?.latitude().toString()+","+
//                                routeProgress.currentLegProgress?.currentStepProgress?.step?.maneuver()
//                                    ?.location()?.longitude().toString(), Toast.LENGTH_SHORT).show()
                        if (routeProgress.currentLegProgress?.currentStepProgress?.distanceRemaining!! < 10
                            && routeProgress.upcomingStepPoints!!.size > 1){
                            currentLocation!!.latitude = routeProgress.upcomingStepPoints?.get(0)?.latitude()!!
                            currentLocation!!.longitude = routeProgress.upcomingStepPoints?.get(0)?.longitude()!!
                            stepLat = routeProgress.upcomingStepPoints?.get(1)?.latitude()
                            stepLng = routeProgress.upcomingStepPoints?.get(1)?.longitude()
                        }
                        else{
                            currentLocation!!.latitude = routeProgress.currentLegProgress?.currentStepProgress?.step?.maneuver()?.location()?.latitude()!!
                            currentLocation!!.longitude = routeProgress.currentLegProgress?.currentStepProgress?.step?.maneuver()?.location()?.longitude()!!
                            stepLat = routeProgress.upcomingStepPoints?.get(0)?.latitude()
                            stepLng = routeProgress.upcomingStepPoints?.get(0)?.longitude()
                        }
                    }
                }

                val builder = AlertDialog.Builder(this)
                val arrivalObserver = object: ArrivalObserver{
                    override fun onFinalDestinationArrival(routeProgress: RouteProgress) {
                        builder.setTitle("Destination arrived")
                        builder.setMessage("You have arrived the destination, would you like to end the navigation?")
                        builder.setPositiveButton("Yes") { _, _ ->
                            finish()
                        }
                        builder.setNegativeButton("No") { dialog, _ -> dialog.cancel() }
                        builder.show()
                    }

                    override fun onNextRouteLegStart(routeLegProgress: RouteLegProgress) {
                    }

                    override fun onWaypointArrival(routeProgress: RouteProgress) {
                    }
                }

                MapboxNavigationApp.current()!!.registerRouteProgressObserver(routeProgressObserver)
                MapboxNavigationApp.current()!!.registerArrivalObserver(arrivalObserver)
            }
        }
    }
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onBackPressed() {
        finish()
    }
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener(Runnable {
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewFinder.createSurfaceProvider())
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview)
            } catch(exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
    }
    override fun onResume() {
        super.onResume()
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.also { accelerometer ->
            sensorManager.registerListener(
                this,
                accelerometer,
                SensorManager.SENSOR_DELAY_NORMAL,
                SensorManager.SENSOR_DELAY_UI
            )
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)?.also { magneticField ->
            sensorManager.registerListener(
                this,
                magneticField,
                SensorManager.SENSOR_DELAY_NORMAL,
                SensorManager.SENSOR_DELAY_UI
            )
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }
    @SuppressLint("MissingPermission")
    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, accelerometerReading, 0, accelerometerReading.size)
        } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, magnetometerReading, 0, magnetometerReading.size)
        }
        SensorManager.getRotationMatrix(
            rotationMatrix,
            null,
            accelerometerReading,
            magnetometerReading
        )
        SensorManager.getOrientation(rotationMatrix, orientationAngles)
        azimuth = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
//        val getLocation= fusedLocationProviderClient.lastLocation.addOnSuccessListener {
//            if (it != null) {
//                currentLocation = it
//            }
//        }
        if (stepLat != null && stepLng != null && currentLocation!= null){
//            var currentLng = currentLocation!!.longitude
//            var currentLat = currentLocation!!.latitude
//            var dLon = stepLng!! - currentLng
//            val y = sin(dLon) * cos(stepLat!!)
//            val x = cos(currentLat) * sin(stepLat!!) - sin(currentLat) *
//                    cos(stepLat!!) * cos(dLon)
//            var brng = Math.toDegrees(atan2(y, x))
//            brng = Math.toDegrees(brng)
            var from = LatLng(currentLocation!!.latitude, currentLocation!!.longitude)
            var to = LatLng(stepLat!!, stepLng!!)
            var brng = SphericalUtil.computeHeading(from,to)
//            brng += 180
//            Toast.makeText(this@NavigationActivity, brng.toString(), Toast.LENGTH_SHORT).show()
            azimuth = (azimuth + 360) % 360
            azimuth = (brng - azimuth).toFloat()
            azimuth = (azimuth + 360) % 360
            val anim: Animation = RotateAnimation(currentAzimuth, azimuth, Animation.RELATIVE_TO_SELF,
                0.5f, Animation.RELATIVE_TO_SELF, 0.5f)
            currentAzimuth = azimuth
            anim.duration = 1000
            anim.repeatCount = 0
            anim.fillAfter = true
            imageView?.startAnimation(anim)
        }
    }
}