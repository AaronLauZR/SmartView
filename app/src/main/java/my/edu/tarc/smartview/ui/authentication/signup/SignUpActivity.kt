package my.edu.tarc.smartview.ui.authentication.signup

import android.app.DatePickerDialog
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import my.edu.tarc.smartview.R
import my.edu.tarc.smartview.databinding.ActivitySignUpBinding
import my.edu.tarc.smartview.ui.authentication.login.LoginActivity
import java.util.Calendar
import java.util.TimeZone

class SignUpActivity : AppCompatActivity() {

    //Initialize Binding
    private lateinit var binding: ActivitySignUpBinding

    //Initialize Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    //Initialize SharedPreference
    val MY_PREF = "MY_PREF"

    //Preferences
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Initialise Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        //SharedPreferences
        sharedPreferences = getSharedPreferences(MY_PREF, MODE_PRIVATE)
        editor = sharedPreferences.edit()

        //Check the textField changes status
        focusListener()

        //Click to sign up
        binding.btnSignUp?.setOnClickListener {
            signUpUser()
        }

        //Navigate to Login Page
        binding.navSignUp.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
    }

    private fun signUpUser() {

        //Sign Up Progress Bar
        binding.loadingSignUp?.visibility = View.VISIBLE
        binding.loadingSignUp?.bringToFront()

        //Output of sign up input
        val email: String = binding.editTextEmail.text.toString()
        val password: String = binding.editTextPassword.text.toString()

        //Output of helperText
        binding.emailContainer.helperText = validEmail()
        binding.passwordContainer.helperText = validPassword()
        binding.confirmPasswordContainer.helperText = validConfirmPassword()

        //Check if the helperText is null
        val validEmail = binding.emailContainer.helperText == null
        val validPassword = binding.passwordContainer.helperText == null
        val validConfirmPassword = binding.confirmPasswordContainer.helperText == null

        //Sign Up Authentication
        if (validEmail && validPassword && validConfirmPassword) {
            firebaseAuthCreateUser(email, password)

        }else {
            Toast.makeText(this, "Please enter valid input", Toast.LENGTH_SHORT).show()
            binding.loadingSignUp?.visibility = View.GONE
        }

    }

    private fun firebaseAuthCreateUser(email: String, password: String) {

        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { it ->

            if(it.isSuccessful) {
                binding.loadingSignUp?.visibility = View.GONE

                //SharedPreferences
                editor.putString("profile", "")
                editor.apply()

                val intent = Intent(this, PersonalDetailActivity::class.java)
                startActivity(intent)

            }else {
                Toast.makeText(this, "The database is failed, please try again", Toast.LENGTH_SHORT).show()
                binding.loadingSignUp?.visibility = View.GONE
            }
        }
    }

    private fun focusListener() {

        //Email
        binding.editTextEmail?.setOnFocusChangeListener { _, focused ->
            if(!focused) {
                binding.emailContainer?.helperText = validEmail()
            }
        }

        //Password
        binding.editTextPassword?.setOnFocusChangeListener { _, focused ->
            if(!focused) {
                binding.passwordContainer?.helperText = validPassword()
            }
        }

        //Confirm Password
        binding.editTextConfirmPassword?.setOnFocusChangeListener { _, focused ->
            if(!focused) {
                binding.confirmPasswordContainer?.helperText = validConfirmPassword()
            }
        }

    }

    private fun validEmail(): String? {
        val emailText = binding.editTextEmail?.text.toString()
        if (emailText.isEmpty()) {
            return "Required"
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(emailText).matches()){
            return "Invalid Email Address"
        }
        return null
    }

    private fun validPassword(): String? {
        val passwordText = binding.editTextPassword?.text.toString()
        if (passwordText.isEmpty()) {
            return "Required"
        }
        if(passwordText.length < 8) {
            return "Minimum 8 Character Password"
        }
        if(!passwordText.matches(".*[A-Z].*".toRegex())) {
            return "Must Contain 1 Upper-case Character"
        }
        if(!passwordText.matches(".*[a-z].*".toRegex())) {
            return "Must Contain 1 Lower-case Character"
        }
        if(!passwordText.matches(".*[@#\$%^&+=].*".toRegex())) {
            return "Must Contain 1 Special Character"
        }
        return null
    }

    private fun validConfirmPassword(): String? {
        val passwordText = binding.editTextPassword?.text.toString()
        val confirmPasswordText = binding.editTextConfirmPassword?.text.toString()
        if (confirmPasswordText.isEmpty()) {
            return "Required"
        }
        if(passwordText.length < 8) {
            return "Minimum 8 Character Password"
        }
        if(passwordText != confirmPasswordText) {
            return "Not matched with Password"
        }
        return null
    }


    override fun onBackPressed() {
        moveTaskToBack(true)
    }
}