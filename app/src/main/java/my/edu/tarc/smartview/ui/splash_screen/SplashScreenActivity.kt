package my.edu.tarc.smartview.ui.splash_screen

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.location.LocationRequest
import android.net.ConnectivityManager
import android.net.NetworkInfo
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.location.LocationManagerCompat.isLocationEnabled
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch
import my.edu.tarc.smartview.R
import my.edu.tarc.smartview.databinding.ActivitySplashScreenBinding
import my.edu.tarc.smartview.ui.MainActivity
import my.edu.tarc.smartview.ui.authentication.User
import my.edu.tarc.smartview.ui.authentication.login.LoginActivity
import my.edu.tarc.smartview.ui.authentication.signup.PersonalDetailActivity
import java.util.Locale


class SplashScreenActivity : AppCompatActivity() {

    //Initialize Binding
    private lateinit var binding: ActivitySplashScreenBinding

    //Initialize Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var databaseRef: DatabaseReference
    private lateinit var database: FirebaseDatabase

    private lateinit var client: GoogleSignInClient
    private val RC_SIGN_IN: Int = 10001

    //private var dataExist
    private var dataExist: String = ""

    //Initialize Authentication User
    private lateinit var user: User
    private lateinit var uid: String

    //The permission id is just an int that must be unique
    private var PERMISSION_ID = 52

    lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    lateinit var locationRequest: LocationRequest

    var locationManager: LocationManager? = null
    var locationListener: LocationListener? = null

    //Initialize City and State variable
    private var city: String = ""
    private var state: String = ""

    //Initialize SharedPreference
    val MY_PREF = "MY_PREF"

    //Preferences
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    //Alert Dialog
    private lateinit var builder: AlertDialog.Builder


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        binding = ActivitySplashScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Initialise Firebase
        auth = FirebaseAuth.getInstance()
        uid = auth.currentUser?.uid.toString()
        databaseRef = FirebaseDatabase.getInstance().getReference("users")
        database = FirebaseDatabase.getInstance()

        //SharedPreferences
        sharedPreferences = getSharedPreferences(MY_PREF, MODE_PRIVATE)
        editor = sharedPreferences.edit()

        //Initiate the fused.. providerClient
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)


        //Check Dark Light Theme
        checkDarkLightTheme()

        //Alert Dialog
        builder = AlertDialog.Builder(this)

        //Remove Action Bar
        supportActionBar?.hide()

        //Handler Delay
        val handler = Handler(Looper.getMainLooper())

        //Connection Manager
        val connectionManager: ConnectivityManager = this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork: NetworkInfo? = connectionManager.activeNetworkInfo
        val isConnected: Boolean = activeNetwork?.isConnectedOrConnecting == true

        handler.postDelayed({
            if (isConnected) {
                keepUserLoggedIn()
            } else {
                builder.setTitle("Network Connection")
                    .setMessage("Connection lost. Please try again!")
                    .setPositiveButton("OK") { dialogInterface, it->
                        //moveTaskToBack(true)
                        finish()
                    }
                    .show()

                val alertDialog = builder.create()
                alertDialog.setCanceledOnTouchOutside(false) // Prevent dismissing on outside touch
                alertDialog.show()
            }
        }, 1000)

    }

    private fun keepUserLoggedIn() {

        val profile = sharedPreferences.getString("profile", "")

        if (profile.equals("") && auth.currentUser != null) {
            val intent = Intent(this, PersonalDetailActivity::class.java)
            startActivity(intent)
            finish()
        }
        else if (auth.currentUser != null) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()

            //getCurrentLocation()
        }
        else {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }

    }

    //Get the current location
    private fun getCurrentLocation() {

        //first we check permission
        if (checkPermission()){
            //Check the location service is enabled
            if (isLocationEnabled()) {
                //Get the location
                fusedLocationProviderClient.lastLocation.addOnCompleteListener {
                    var location = it.result
                    if(location == null){
                        //if the location is null
                        Toast.makeText(this, "The location is empty", Toast.LENGTH_SHORT).show()
                    }else it.apply{
                        //Location.latitude will return the latitude coordinates
                        //Location.longitude will return the longitude coordinates
                        city = getCityName(location.latitude, location.longitude)
                        state = getStateName(location.latitude, location.longitude)

                        //Update latest location into firebase
                        updateProfile(city, state)
                    }
                }
            }else{
                Toast.makeText(this, "Please enabled your location service", Toast.LENGTH_SHORT).show()
            }
        }else{
            RequestPermission()
        }
    }

    private fun updateProfile(city: String, state: String) {

        val user = mapOf<String, String?>(
            "city" to city,
            "state" to state,
        )

        databaseRef.child(uid).updateChildren(user).addOnSuccessListener {

            Toast.makeText(this, "Your current location is updated", Toast.LENGTH_SHORT).show()

            editor.putString("city", city)
            editor.putString("state", state)
            editor.apply()

        }.addOnFailureListener{

            //Toast.makeText(this, "Database is failed. Please try again", Toast.LENGTH_SHORT).show()

        }
    }

    //Function to check the user permission
    private fun checkPermission():Boolean {
        if(
            ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        ) {
            return true
        }
        return false
    }

    //Function to allow user to get user permission
    private fun RequestPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION),
            PERMISSION_ID
        )
    }

    //Function to check if the location service of the device is enabled
    private fun isLocationEnabled(): Boolean {
        val locationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }





    //Function to get the city name
    private fun getCityName(lat:Double, long:Double):String {
        var CityName = ""
        var geoCoder = Geocoder(this, Locale.getDefault())
        var Address = geoCoder.getFromLocation(lat, long, 1)
        if (Address != null) {
            CityName = Address.get(0).locality
        }
        return CityName
    }

    //Function to get the country name
    private fun getStateName(lat:Double, long:Double):String {
        var StateName = ""
        var geoCoder = Geocoder(this, Locale.getDefault())
        var Address = geoCoder.getFromLocation(lat, long, 1)
        if (Address != null) {
            StateName = Address.get(0).adminArea
        }
        return StateName
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        //Build in function that check the permission result
        if (requestCode == PERMISSION_ID) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("Debug:", "You have the Permission")
            }
        }
    }

    private fun checkDarkLightTheme() {
        val nightMode = sharedPreferences.getString("night", "") //Light mode is the default mode

        lifecycleScope.launch {

            if (nightMode.equals("true")) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }else{
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }
    }

}