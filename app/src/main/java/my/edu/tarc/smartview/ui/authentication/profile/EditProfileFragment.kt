package my.edu.tarc.smartview.ui.authentication.profile

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import android.location.LocationRequest
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.NavigationUI
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import my.edu.tarc.smartview.R
import my.edu.tarc.smartview.databinding.FragmentEditProfileBinding
import my.edu.tarc.smartview.ui.MainActivity
import my.edu.tarc.smartview.ui.authentication.User
import java.io.FileNotFoundException
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import com.squareup.picasso.Picasso


class EditProfileFragment : Fragment() {

    //Initialize NavController
    private lateinit var navController: NavController

    //Initialize Binding
    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!

    //Initialize Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var databaseRef: DatabaseReference
    private lateinit var database: FirebaseDatabase
    private lateinit var storageReference: StorageReference

    //Initialize Authentication User
    private lateinit var user: User
    private lateinit var uid: String

    private lateinit var uri: Uri
    private var imageExist: Boolean = false
    private var downloadUrl: String? = ""


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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentEditProfileBinding.inflate(inflater, container, false)

        //Initialise Firebase
        auth = FirebaseAuth.getInstance()
        uid = auth.currentUser?.uid.toString()
        storageReference = FirebaseStorage.getInstance().getReference("Profile/"+auth.currentUser?.uid)
        databaseRef = FirebaseDatabase.getInstance().getReference("users")

        //SharedPreferences
        sharedPreferences = requireContext().getSharedPreferences(MY_PREF, AppCompatActivity.MODE_PRIVATE)
        editor = sharedPreferences.edit()

        //Initiate the fused.. providerClient
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireContext())

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val galleryImage = registerForActivityResult(
            ActivityResultContracts.GetContent(),
            ActivityResultCallback { it ->
                if(it != null) {
                    binding.imageProfile.setImageURI(it)
                    uri = it
                    imageExist = true
                }else{
                    imageExist = false
                }

            }
        )

        //Retrieve user data
        getUserData()

        //Select photo from gallery
        binding.imageProfile.setOnClickListener {
            galleryImage.launch("image/*")
        }
        binding.textViewUploadPhoto.setOnClickListener {
            galleryImage.launch("image/*")
        }

        //Setting up DatePicker for Date Of Birth
        dobSelected()

        //Check the textField changes status
        focusListener()

        //Click to reset edit profile
        binding.btnReset.setOnClickListener {
            getUserData()
        }

        //Click to Sign Up
        binding.btnSave.setOnClickListener {
            editProfile()
        }

    }

    private fun editProfile() {
        //Sign Up Progress Bar
        binding.loadingEditProfile.visibility = View.VISIBLE
        binding.loadingEditProfile.bringToFront()

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

        //Edit Profile Authentication
        if (validFirstName && validLastName) {

            //Update profile picture to Firebase storage
            uploadProfilePhoto()

            //Update profile info to Real-time Firebase
            updateProfile(firstName, lastName, dob, gender)

        }else {
            Toast.makeText(context, "Please enter valid input", Toast.LENGTH_SHORT).show()
            binding.loadingEditProfile.visibility = View.GONE
        }

    }

    private fun updateProfile(firstName: String, lastName: String, dob: String, gender: String) {

        val user = mapOf<String, String?>(
            "firstname" to firstName,
            "lastname" to lastName,
            "dob" to dob,
            "gender" to gender,
        )

        databaseRef.child(uid).updateChildren(user).addOnSuccessListener {

            binding.loadingEditProfile.visibility = View.GONE
            Toast.makeText(context, "Your profile is updated", Toast.LENGTH_SHORT).show()

            findNavController().popBackStack() //Go to the profile page

            editor.putString("firstname", firstName)
            editor.putString("lastname", lastName)
            editor.putString("dob", dob)
            editor.putString("gender", gender)
            editor.apply()

        }.addOnFailureListener{

            binding.loadingEditProfile.visibility = View.GONE
            Toast.makeText(context, "Database is failed. Please try again", Toast.LENGTH_SHORT).show()

        }
    }

    private fun getUserData() {

        //First Name
        binding.editTextFirstName.setText(sharedPreferences.getString("firstname", ""))

        //Last Name
        binding.editTextLastName.setText(sharedPreferences.getString("lastname", ""))

        //DOB
        binding.editTextDob.setText(sharedPreferences.getString("dob", ""))

        //Gender
        val gender = sharedPreferences.getString("gender", "")
        if (gender.equals("Male")) {
            binding.radioButtonMale.isChecked = true
        }
        if (gender.equals("Female")) {
            binding.radioButtonFemale.isChecked = true
        }


        //Profile
        val imageUrl = sharedPreferences.getString("profile_photo", "")

        if (imageUrl.equals("")) {
            binding.imageProfile.setImageResource(R.drawable.baseline_account_circle_24)
        }else{
            Picasso.get().load(imageUrl).into(binding.imageProfile)
        }

    }

    private fun uploadProfilePhoto() {
        if (imageExist) {
            try{
                storageReference.putFile(uri).addOnSuccessListener {task ->
                    task.metadata!!.reference!!.downloadUrl
                        .addOnSuccessListener { uri ->
                            val profileUrl = uri.toString()
                            updateProfilePhotoUrl(profileUrl)
                        }
                }
            }catch (e: FileNotFoundException){
                e.printStackTrace()
                Toast.makeText(context, "The image is not supported", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateProfilePhotoUrl(profileUrl: String) {

        databaseRef.child(uid).child("photo").setValue(profileUrl)
            .addOnSuccessListener {
                editor.putString("profile_photo", profileUrl)
                editor.apply()
            }.addOnFailureListener {
                Toast.makeText(context, "Fail to update photo, please try again", Toast.LENGTH_SHORT).show()
            }
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

        //Setting up DatePicker on EditText
        binding.editTextDob.setOnClickListener {

            //Date Picker Dialog
            val picker = DatePickerDialog(requireContext(), { view, year, monthOfYear, dayOfMonth ->
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

}