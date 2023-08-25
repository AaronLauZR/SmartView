package my.edu.tarc.smartview.ui.authentication.login

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import my.edu.tarc.smartview.R
import my.edu.tarc.smartview.databinding.ActivityLoginBinding
import my.edu.tarc.smartview.ui.MainActivity
import my.edu.tarc.smartview.ui.authentication.User
import my.edu.tarc.smartview.ui.authentication.signup.PersonalDetailActivity
import my.edu.tarc.smartview.ui.authentication.signup.SignUpActivity

class LoginActivity : AppCompatActivity() {

    //Initialize Binding
    private lateinit var binding: ActivityLoginBinding

    //Initialize Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var databaseRef: DatabaseReference
    private lateinit var database: FirebaseDatabase

    private lateinit var client:GoogleSignInClient
    private val RC_SIGN_IN: Int = 10001

    //private var dataExist
    private var dataExist: String = ""

    //Initialize Authentication User
    private lateinit var user: User
    private lateinit var uid: String

    //Initialize SharedPreference
    val MY_PREF = "MY_PREF"

    //Preferences
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Initialise Firebase
        auth = FirebaseAuth.getInstance()
        databaseRef = FirebaseDatabase.getInstance().getReference("users")
        database = FirebaseDatabase.getInstance()

        //Google Sign In
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        client = GoogleSignIn.getClient(this, options)

        //SharedPreferences
        sharedPreferences = getSharedPreferences(MY_PREF, MODE_PRIVATE)
        editor = sharedPreferences.edit()

        //Check the textField changes status
        focusListener()

        //Click to login
        binding.btnLogin.setOnClickListener {
            signInUser()
        }

        //Click to login with Google
        binding.btnLoginGoogle?.setOnClickListener {
            signInWithGoogle()
        }

        //Navigate to Sign Up Page
        binding.navSignUp.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }
    }

    private fun signInUser() {

        //Sign Up Progress Bar
        binding.loadingLogin.visibility = View.VISIBLE
        binding.loadingLogin.bringToFront()

        //Output of login input
        val email = binding.editTextEmail.text.toString()
        val password = binding.editTextPassword.text.toString()

        //Output of helperText
        binding.emailContainer.helperText = validEmail()
        binding.passwordContainer.helperText = validPassword()

        //Check if the helperText is null
        val validEmail = binding.emailContainer.helperText == null
        val validPassword = binding.passwordContainer.helperText == null

        //Login Authentication
        if (validEmail && validPassword) {
            firebaseAuthWithEmailPassword(email, password)
        }else {
            binding.loadingLogin.visibility = View.GONE
            Toast.makeText(this, "Please enter valid input", Toast.LENGTH_SHORT).show()
        }

    }

    private fun firebaseAuthWithEmailPassword(email: String, password: String) {

        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener {
            if(it.isSuccessful) {

                binding.loadingLogin.visibility = View.GONE

                val userEmail = auth.currentUser?.email
                val userId = auth.currentUser?.uid
                Toast.makeText(this, "Welcome, $userEmail", Toast.LENGTH_SHORT).show()

                //SharedPreferences
                editor.putString("email", "true")
                editor.apply()

                //Store user data into sharedPreference
                getUserData(userId)

                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)

            }else {
                binding.loadingLogin.visibility = View.GONE
                Toast.makeText(this, "Failed to login, please try again", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun signInWithGoogle() {
        val intent = client.signInIntent
        startActivityForResult(intent, RC_SIGN_IN)

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try{
                val account = task.getResult(ApiException::class.java)!!
                Log.d("TAG", "firebaseAuthWithGoogle:" + account.id)
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Log.w("TAG", "Google sign in failed", e)
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential).addOnCompleteListener{
            if (it.isSuccessful) {

                editor.putString("email", "true")
                editor.apply()
                uid = auth.currentUser?.uid.toString()

                binding.loadingLogin.visibility = View.GONE

                verifyGoogleAccountProfile()

            }else {
                binding.loadingLogin.visibility = View.GONE
                Toast.makeText(this, "Failed to login, please try again", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun verifyGoogleAccountProfile() {

        val userEmail = auth.currentUser?.email
        val email = sharedPreferences.getString("email", "")

        checkProfileExist {profile ->

            if (email.equals("true") && profile.equals("true")) {

                editor.putString("profile", profile)
                editor.apply()
                Toast.makeText(this, "Welcome, $userEmail", Toast.LENGTH_SHORT).show()

                //Store user data into sharedPreference
                getUserData(uid)

                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)

            }else {
                editor.putString("profile", profile)
                editor.apply()
                val intent = Intent(this, PersonalDetailActivity::class.java)
                startActivity(intent)
            }
        }
    }


    private fun checkProfileExist(callback: (String?) -> Unit) {

        databaseRef.child(uid).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val profile = if (snapshot.exists()) {
                        "true" // Profile exists
                    } else {
                        "" // Profile does not exist
                    }
                    callback(profile)
                }catch (e: Exception) {
                    Toast.makeText(this@LoginActivity, "Profile status is invalid", Toast.LENGTH_SHORT).show()
                    callback(null)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@LoginActivity, "Failed to get user profile data", Toast.LENGTH_SHORT).show()
            }
        })

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
                    //editor.putString("city", user.city)
                    //editor.putString("state", user.state)
                    editor.putString("profile_photo", user.photo)
                    editor.apply()

                }catch (e: Exception) {
                    Toast.makeText(this@LoginActivity, "No Profile details in this account", Toast.LENGTH_SHORT).show()
                }

            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@LoginActivity, "Failed to get user profile data", Toast.LENGTH_SHORT).show()
            }

        })
    }


    private fun focusListener() {

        binding.editTextEmail?.setOnFocusChangeListener { _, focused ->
            if(!focused) {
                binding.emailContainer?.helperText = validEmail()
            }
        }

        binding.editTextPassword?.setOnFocusChangeListener { _, focused ->
            if(!focused) {
                binding.passwordContainer?.helperText = validPassword()
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

    override fun onBackPressed() {
        moveTaskToBack(true)
    }

}