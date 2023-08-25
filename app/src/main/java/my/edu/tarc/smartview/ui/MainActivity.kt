package my.edu.tarc.smartview.ui

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.SharedPreferences
import android.os.Bundle
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import my.edu.tarc.smartview.R
import my.edu.tarc.smartview.databinding.ActivityMainBinding
import my.edu.tarc.smartview.ui.authentication.User
import dagger.hilt.android.AndroidEntryPoint
import my.edu.tarc.smartview.ui.network.NetworkConnection
import my.edu.tarc.smartview.ui.news.viewModel.NewsViewModel
import my.edu.tarc.smartview.ui.news.viewModel.NewsViewModelProviderFactory
import my.edu.tarc.smartview.ui.news.db.ArticleDatabase
import my.edu.tarc.smartview.ui.news.repository.NewsRepository

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private lateinit var  appBarConfiguration: AppBarConfiguration

    //Initialize Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var databaseRef: DatabaseReference
    private lateinit var database: FirebaseDatabase

    //Initialize Authentication User
    private lateinit var user: User
    private lateinit var uid: String

    //Initialize SharedPreference
    val MY_PREF = "MY_PREF"

    //Preferences
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    //Initialize Alert Dialog and ProgressBar
    private lateinit var builder: AlertDialog.Builder
    private var progressBarError: ProgressDialog? = null

    //News Repository
    lateinit var viewModel: NewsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //Check Network Connection
        val networkConnection = NetworkConnection(applicationContext)
        networkConnection.observe(this, Observer {isConnected ->
            if(isConnected) {
                progressBarError?.dismiss()
            }else {
                error()
            }
        })

        //Hide the Action Bar and App Title
        //requestWindowFeature(Window.FEATURE_NO_TITLE)
        //getSupportActionBar()?.hide()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Initialise Firebase
        auth = FirebaseAuth.getInstance()
        uid = auth.currentUser?.uid.toString()
        databaseRef = FirebaseDatabase.getInstance().getReference("users")

        //SharedPreferences
        sharedPreferences = getSharedPreferences(MY_PREF, MODE_PRIVATE)
        editor = sharedPreferences.edit()


        //Bottom Menu Navigation
        val navView: BottomNavigationView = binding.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home,
                R.id.navigation_news,
                R.id.navigation_article,
                R.id.navigation_post_article,
                R.id.navigation_profile
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        //Initialise View Model
        val newsRepository = NewsRepository(ArticleDatabase(this))
        val viewModelProviderFactory = NewsViewModelProviderFactory(newsRepository)
        viewModel = ViewModelProvider(this, viewModelProviderFactory).get(NewsViewModel::class.java)

    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    private fun error() {
        progressBarError = ProgressDialog(this)
        progressBarError?.setTitle("Connection is lost")
        progressBarError?.setCancelable(false)
        progressBarError?.setMessage("Please turn on your network...")
        progressBarError?.show()

    }

}