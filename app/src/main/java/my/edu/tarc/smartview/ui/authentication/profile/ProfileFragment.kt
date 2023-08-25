package my.edu.tarc.smartview.ui.authentication.profile

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.location.Geocoder
import android.os.Bundle
import android.util.Patterns
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.squareup.picasso.Picasso
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import my.edu.tarc.smartview.R
import my.edu.tarc.smartview.databinding.FragmentProfileBinding
import my.edu.tarc.smartview.ui.authentication.login.LoginActivity
import java.util.Locale


class ProfileFragment : Fragment() {

    //Initialize Binding
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    //Initialize Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var databaseRef: DatabaseReference
    private lateinit var database: FirebaseDatabase

    //Initialize SharedPreference
    val MY_PREF = "MY_PREF"

    //Email Details for reset Password
    private lateinit var emailContainer: TextInputLayout
    private lateinit var email: TextInputEditText

    //Preferences
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    //Initialize City and State variable
    private var city: String = ""
    private var state: String = ""

    //Alert Dialog
    private lateinit var builder: AlertDialog.Builder

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentProfileBinding.inflate(inflater, container, false)

        //Initialise Firebase
        auth = FirebaseAuth.getInstance()

        //SharedPreferences
        sharedPreferences = requireContext().getSharedPreferences(MY_PREF, AppCompatActivity.MODE_PRIVATE)
        editor = sharedPreferences.edit()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //Get User Data
        getUserData()

        //Reset Password
        resetPassword()

        //Navigate to the settings
        binding.btnSettings.setOnClickListener {
            findNavController().navigate(R.id.action_navigation_profile_to_navigation_settings)
        }

        //Navigate to the edit profile
        binding.btnEditProfile.setOnClickListener {
            findNavController().navigate(R.id.action_navigation_profile_to_navigation_edit_profile)
        }

        //Navigate to the my shout
        binding.btnMyArticle.setOnClickListener {
            findNavController().navigate(R.id.action_navigation_profile_to_navigation_my_article)
        }

        //Navigate to the bookmark
        binding.btnBookmarkArticle.setOnClickListener {
            findNavController().navigate(R.id.action_navigation_profile_to_navigation_bookmark_article)
        }

        //Alert Dialog
        builder = AlertDialog.Builder(context)


        //Click to logout
        binding.btnLogout.setOnClickListener {

            builder.setTitle("Logout")
                .setMessage("Do you want to logout?")
                .setCancelable(true)
                .setPositiveButton("Yes") { dialogInterface, it->

                    //SharedPreferences
                    editor.putString("email", "")
                    editor.putString("firstname", "")
                    editor.putString("lastname", "")
                    editor.putString("dob", "")
                    editor.putString("gender", "")
                    editor.putString("city", "")
                    editor.putString("state", "")
                    editor.putString("profile_photo", "")
                    editor.apply()

                    //Sign out with custom user account
                    auth.signOut()

                    //Sign out with user Google account
                    val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(getString(R.string.default_web_client_id))
                        .requestEmail()
                        .build()
                    val client = GoogleSignIn.getClient(requireContext(), options)
                    client.signOut()

                    Toast.makeText(context, "You have successfully logout!", Toast.LENGTH_SHORT).show()
                    val intent = Intent(context, LoginActivity::class.java)
                    startActivity(intent)

                }
                .setNegativeButton("No") { dialogInterface, it->
                    dialogInterface.cancel()
                }
                .show()
        }
    }

    private fun getUserData() {

        //Display name
        val firstName = sharedPreferences.getString("firstname", "")
        val lastName = sharedPreferences.getString("lastname", "")
        binding.textViewName.text = firstName + " " + lastName

        //Display location
        val city = sharedPreferences.getString("city", "")
        val state = sharedPreferences.getString("state", "")

        if(!city.equals("") && !state.equals("")) {
            binding.textViewLocation.text = city + ", " + state
        }else{
            binding.textViewLocation.text = "No location"
        }


        val imageUrl = sharedPreferences.getString("profile_photo", "")

        if (imageUrl.equals("")) {
            binding.imageProfile.setImageResource(R.drawable.baseline_account_circle_24)
        }else{
            Picasso.get().load(imageUrl).into(binding.imageProfile)
        }
    }

    private fun resetPassword() {

        // Dialog to enter email for reset password
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Enter Email to reset Password")

        // Inflate the custom_dialog view
        val view = layoutInflater.inflate(R.layout.dialog_resetpassword, null)
        emailContainer = view.findViewById(R.id.emailContainerForResetPassword)
        email = view.findViewById(R.id.editTextEmailForResetPassword)
        val submit = view.findViewById<Button>(R.id.btnSubmit)

        builder.setView(view)
        val dialog = builder.create()

        // Click to display dialog
        binding.btnChangePassword.setOnClickListener {
            dialog.show()
        }

        // Click to submit email to change the password
        submit.setOnClickListener{

            //Output of email input
            val emailInput = email.text.toString()

            //Output of helperText
            emailContainer.helperText = validEmail()

            val validEmail = emailContainer.helperText == null

            if (validEmail) {

                FirebaseAuth.getInstance().sendPasswordResetEmail(emailInput).addOnSuccessListener {
                    Toast.makeText(context, "Please check your email to reset password", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }.addOnFailureListener {
                    Toast.makeText(context, "This email is invalid, please try again", Toast.LENGTH_SHORT).show()
                }

            }

        }
    }

    private fun validEmail(): String? {
        val emailText = email.text.toString()
        if (emailText.isEmpty()) {
            return "Required"
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(emailText).matches()){
            return "Invalid Email Address"
        }
        return null
    }

    private fun getCityName(lat:Double, long:Double):String {
        var CityName = ""
        var geoCoder = Geocoder(requireContext(), Locale.getDefault())
        var Address = geoCoder.getFromLocation(lat, long, 1)
        if (Address != null) {
            CityName = Address.get(0).locality
        }
        return CityName
    }

    //Function to get the country name
    private fun getStateName(lat:Double, long:Double):String {
        var StateName = ""
        var geoCoder = Geocoder(requireContext(), Locale.getDefault())
        var Address = geoCoder.getFromLocation(lat, long, 1)
        if (Address != null) {
            StateName = Address.get(0).adminArea
        }
        return StateName
    }


}