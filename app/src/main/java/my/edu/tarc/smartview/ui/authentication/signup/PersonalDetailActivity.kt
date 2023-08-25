package my.edu.tarc.smartview.ui.authentication.signup

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import android.location.LocationRequest
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import my.edu.tarc.smartview.ui.MainActivity
import my.edu.tarc.smartview.R
import my.edu.tarc.smartview.databinding.ActivityPersonalDetailBinding
import my.edu.tarc.smartview.ui.authentication.User
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class PersonalDetailActivity : AppCompatActivity() {

    //Initialize Binding
    private lateinit var binding: ActivityPersonalDetailBinding

    //Initialize Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var databaseRef: DatabaseReference
    private lateinit var database: FirebaseDatabase

    //Initialize Authentication User
    private lateinit var user: User
    private lateinit var uid: String

    //The permission id is just an int that must be unique
    private var PERMISSION_ID = 52

    lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    lateinit var locationRequest: LocationRequest

    //Initialize City and State variable
    private var city: String = ""
    private var state: String = ""

    //Initialize SharedPreference
    val MY_PREF = "MY_PREF"

    //Preferences
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_personal_detail)

        binding = ActivityPersonalDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Initialise Firebase
        auth = FirebaseAuth.getInstance()
        uid = auth.currentUser?.uid.toString()
        databaseRef = FirebaseDatabase.getInstance().getReference("users")
        database = FirebaseDatabase.getInstance()

        //Display greeting message
        val email = auth.currentUser?.email
        Toast.makeText(this, "Hello there, $email", Toast.LENGTH_SHORT).show()

        //SharedPreferences
        sharedPreferences = getSharedPreferences(MY_PREF, MODE_PRIVATE)
        editor = sharedPreferences.edit()

        //Initiate the fused.. providerClient
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        //Setting up DatePicker for Date Of Birth
        dobSelected()

        //Check the textField changes status
        focusListener()

        //Click to Sign Up
        binding.btnProceedSignUp.setOnClickListener {
            signUpUserProfile()
        }

    }

    private fun signUpUserProfile() {
        //Sign Up Progress Bar
        binding.loadingPersonalDetail.visibility = View.VISIBLE
        binding.loadingPersonalDetail.bringToFront()

        //Output of sign up input
        val firstName: String = binding.editTextFirstName.text.toString()
        val lastName: String = binding.editTextLastName.text.toString()
        val gender: String = genderSelected().toString()
        val dob: String = binding.editTextDob.text.toString()

        //Output of helperText
        binding.firstNameContainer.helperText = validFirstName()
        binding.lastNameContainer.helperText = validLastName()

        //Check if the helperText is null
        val validFirstName = binding.firstNameContainer.helperText == null
        val validLastName = binding.lastNameContainer.helperText == null


        //Sign Up Authentication
        if (validFirstName && validLastName) {
            val users: User = User(firstName, lastName, dob, gender, null)
            createProfile(users)

        }else {
            Toast.makeText(this, "Please enter valid input", Toast.LENGTH_SHORT).show()
            binding.loadingPersonalDetail.visibility = View.GONE
        }

    }

    private fun createProfile(users: User) {

        val databaseRef = database.reference.child("users").child(auth.currentUser!!.uid)

        databaseRef.setValue(users).addOnCompleteListener {
            if(it.isSuccessful) {
                binding.loadingPersonalDetail.visibility = View.GONE

                val userEmail = auth.currentUser?.email
                val userId = auth.currentUser?.uid
                Toast.makeText(this, "Welcome, $userEmail", Toast.LENGTH_SHORT).show()

                //SharedPreferences
                editor.putString("profile", "true")
                editor.putString("email", "true")
                editor.apply()

                //Store user data into sharedPreference
                getUserData(userId)

                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)

            }else {
                Toast.makeText(this, "The database is failed, please try again", Toast.LENGTH_SHORT).show()
                binding.loadingPersonalDetail.visibility = View.GONE
            }
        }
    }

    private fun getUserData(userId: String?) {
        databaseRef.child(userId.toString()).addValueEventListener(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try{
                    user = snapshot.getValue(User::class.java)!!

                    editor.putString("firstname", user.firstname)
                    editor.putString("lastname", user.lastname)
                    editor.putString("dob", user.dob)
                    editor.putString("gender", user.gender)
                    editor.putString("profile_photo", user.photo)
                    editor.apply()
                }catch (e: Exception) {
                    Toast.makeText(this@PersonalDetailActivity, "No Profile details in this account", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@PersonalDetailActivity, "Failed to get user profile data", Toast.LENGTH_SHORT).show()
            }

        })
    }

    private fun focusListener() {

        //First Name
        binding.editTextFirstName?.setOnFocusChangeListener { _, focused ->
            if(!focused) {
                binding.firstNameContainer?.helperText = validFirstName()
            }
        }

        //Last Name
        binding.editTextLastName?.setOnFocusChangeListener { _, focused ->
            if(!focused) {
                binding.lastNameContainer?.helperText = validLastName()
            }
        }

    }

    private fun dobSelected() {

        val calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kuala_Lumpur"))
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val month = calendar.get(Calendar.MONTH)
        val year = calendar.get(Calendar.YEAR)

        //Current Date
        binding.editTextDob.setText("$day/${month + 1}/$year")

        //Setting up DatePicker on EditText
        binding.editTextDob.setOnClickListener {

            //Date Picker Dialog
            val picker = DatePickerDialog(this, { view, year, monthOfYear, dayOfMonth ->
                binding.editTextDob.setText("$dayOfMonth/${monthOfYear + 1}/$year")
            }, year, month, day)
            picker.show()
        }

    }

    private fun genderSelected(): String? {
        val gender = binding.radioGroupGender.checkedRadioButtonId

        if(gender == binding.radioButtonMale.id) {
            return "Male"
        }else {
            return "Female"
        }
        return null
    }

    private fun validFirstName(): String? {
        val firstNameText = binding.editTextFirstName?.text.toString()
        if (firstNameText.isEmpty()) {
            return "Required"
        }
        return null
    }

    private fun validLastName(): String? {
        val lastNameText = binding.editTextLastName?.text.toString()
        if (lastNameText.isEmpty()) {
            return "Required"
        }
        return null
    }


    override fun onBackPressed() {
        moveTaskToBack(true)
    }

}