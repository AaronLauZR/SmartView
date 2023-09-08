package my.edu.tarc.smartview.ui.home

import android.Manifest
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Criteria
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.location.LocationRequest
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import my.edu.tarc.smartview.R
import my.edu.tarc.smartview.databinding.FragmentHomeBinding
import my.edu.tarc.smartview.ui.article.ArticleAdapter
import my.edu.tarc.smartview.ui.article.ArticleDetails
import my.edu.tarc.smartview.ui.article.ReadArticleActivity
import my.edu.tarc.smartview.ui.authentication.User
import my.edu.tarc.smartview.ui.news.db.ArticleDatabase
import my.edu.tarc.smartview.ui.news.repository.NewsRepository
import my.edu.tarc.smartview.ui.news.utils.Resource
import my.edu.tarc.smartview.ui.news.viewModel.NewsViewModel
import my.edu.tarc.smartview.ui.news.viewModel.NewsViewModelProviderFactory
import java.util.Locale


class HomeFragment : Fragment(), LocationListener {

    //Initialize Binding
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    //Alert Dialog
    private lateinit var builder: AlertDialog.Builder

    //Initialize Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var databaseRef: DatabaseReference
    private lateinit var database: FirebaseDatabase
    private lateinit var storageReference: StorageReference

    //Initialize Authentication User
    private lateinit var user: User
    private lateinit var uid: String

    //The permission id is just an int that must be unique
    private var PERMISSION_ID = 52

    lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    lateinit var locationRequest: LocationRequest

    //Initialise News
    lateinit var viewModel: NewsViewModel
    private lateinit var homeNewsAdapter: HomeNewsAdapter

    //Initialise Article
    private lateinit var articleArrayList : ArrayList<ArticleDetails>
    private lateinit var articleRecyclerView: RecyclerView

    //Initialize City and State variable
    private var city: String = ""
    private var state: String = ""

    var lat: Double? = null
    var lng: Double? = null
    var permissionArrays = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION)

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
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

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

        //Alert Dialog
        builder = AlertDialog.Builder(context)

        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val MyVersion = Build.VERSION.SDK_INT
        if (MyVersion > Build.VERSION_CODES.LOLLIPOP_MR1) {
            if (checkFineLocationPermission() && checkCoarseLocationPermission()) {
            } else {
                requestPermissions(permissionArrays, 101)
            }
        }

        // Handle the back button press
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                showExitConfirmationDialog()
            }
        })

        //Refresh new current location
        getCurrentLocation()

        //Set up eateries & restaurant
        setUpEateries()

        //Set up trending news
        setUpTrendingNews()

        //Set up article
        setUpArticleRecyclerView()

        //Refresh new current location
        binding.btnRefreshLocation.setOnClickListener {
            getCurrentLocation()
        }

        //Navigate to news page
        binding.btnSeeAllNews.setOnClickListener {
            findNavController().navigate(R.id.action_navigation_home_to_navigation_news)
        }

        //Navigate to article page
        binding.btnSeeAllArticle.setOnClickListener {
            findNavController().navigate(R.id.action_navigation_home_to_navigation_article)
        }

    }

    private fun setUpEateries() {

        //Navigate to the locality eateries page
        binding.btnRestaurant.setOnClickListener {
            findNavController().navigate(R.id.action_navigation_home_to_navigation_locality)
            editor.putString("locality", "restaurants")
            editor.apply()
        }

        binding.btnHotel.setOnClickListener {
            findNavController().navigate(R.id.action_navigation_home_to_navigation_locality)
            editor.putString("locality", "hotels")
            editor.apply()
        }

        binding.btnMall.setOnClickListener {
            findNavController().navigate(R.id.action_navigation_home_to_navigation_locality)
            editor.putString("locality", "shopping")
            editor.apply()
        }

        binding.btnBank.setOnClickListener {
            findNavController().navigate(R.id.action_navigation_home_to_navigation_locality)
            editor.putString("locality", "banks")
            editor.apply()
        }

        binding.btnSport.setOnClickListener {
            findNavController().navigate(R.id.action_navigation_home_to_navigation_locality)
            editor.putString("locality", "sports")
            editor.apply()
        }

        binding.btnHospital.setOnClickListener {
            findNavController().navigate(R.id.action_navigation_home_to_navigation_locality)
            editor.putString("locality", "hospitals")
            editor.apply()
        }

    }

    private fun setUpTrendingNews() {
        val newsRepository = NewsRepository(ArticleDatabase(requireContext()))
        val viewModelProviderFactory = NewsViewModelProviderFactory(newsRepository)
        viewModel = ViewModelProvider(this, viewModelProviderFactory).get(NewsViewModel::class.java)

        //Set up news
        setUpNewsRecyclerView()

        //Navigate to the news detail
        homeNewsAdapter.setOnItemClickListener {
            val bundle = Bundle().apply {
                putSerializable("article", it)
            }
            findNavController().navigate(
                R.id.action_navigation_home_to_navigation_read_news,
                bundle
            )
        }

        viewModel.breakingNews.observe(viewLifecycleOwner, Observer { response ->
            when(response) {
                is Resource.Success -> {
                    response.data?.let { newsResponse ->
                        homeNewsAdapter.differ.submitList(newsResponse.articles)
                    }
                }
                is Resource.Error -> {
                    response.data?.let { message ->
                        Log.e("Breaking News Fragment", "An error occured: $message")
                    }
                }
                is Resource.Loading -> {

                }
            }
        })
    }

    private fun setUpNewsRecyclerView() {
        homeNewsAdapter = HomeNewsAdapter()

        binding?.rvHomeNews?.apply {
            adapter = homeNewsAdapter
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        }
    }

    private fun setUpArticleRecyclerView() {

        articleRecyclerView = binding.rvHomeArticle
        articleRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

        articleArrayList = arrayListOf<ArticleDetails>()
        getArticleData()

    }

    private fun getArticleData() {

        databaseRef = FirebaseDatabase.getInstance().getReference("Local Shout")

        databaseRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                articleArrayList.clear()
                if (snapshot.exists()) {
                    for (articleSnapshot in snapshot.children) {
                        if (articleSnapshot.child("city").getValue().toString().equals(city)) {
                            val article = articleSnapshot.getValue(ArticleDetails::class.java)
                            articleArrayList.add(article!!)
                        }
                    }

                    // Sort the articles based on the "time" field
                    articleArrayList.sortByDescending { it.time }

                    var adapter = HomeArticleAdapter(articleArrayList)
                    articleRecyclerView.adapter = adapter


                    adapter.setOnItemClickListener(object : HomeArticleAdapter.onItemClickListener {
                        override fun onItemClick(position: Int) {
                            val intent = Intent(requireContext(), ReadArticleActivity::class.java)
                            intent.putExtra("articleId", articleArrayList[position].articleID)
                            intent.putExtra("category", articleArrayList[position].category)
                            intent.putExtra("image", articleArrayList[position].image)
                            intent.putExtra("time", articleArrayList[position].time)
                            intent.putExtra("area", articleArrayList[position].city)
                            intent.putExtra("title", articleArrayList[position].title)
                            intent.putExtra("name", articleArrayList[position].name)
                            intent.putExtra("description", articleArrayList[position].detail)
                            startActivity(intent)
                        }
                    })
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "System error!", Toast.LENGTH_SHORT).show()
            }
        })

    }

    //Function to allow user to get user permission
    private fun RequestPermission() {
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION),
            PERMISSION_ID
        )
    }


    //Function to get the city name
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

    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 115)
            return
        }
        val locationManager = context?.getSystemService(AppCompatActivity.LOCATION_SERVICE) as LocationManager
        val criteria = Criteria()
        val provider = locationManager.getBestProvider(criteria, true)
        val location = locationManager.getLastKnownLocation(provider.toString())

        if (location != null) {
            onLocationChanged(location)

            city = getCityName(location.latitude, location.longitude)
            state = getStateName(location.latitude, location.longitude)
            editor.putString("city", city)
            editor.putString("state", state)

            editor.putString("lat", lat.toString())
            editor.putString("long", lng.toString())

            editor.apply()

            //Output the current location text
            binding.address.text = city + ", " + state

        } else {

            editor.putString("city", "")
            editor.putString("state", "")

            editor.putString("lat", "")
            editor.putString("long", "")

            editor.apply()

            //Output no location text
            binding.address.text = "No location"

            RequestPermission()

            try {
                locationManager.requestLocationUpdates(provider.toString(), 20000, 0f, this)
            }catch (e: Exception) {
                Log.d("Location Tag:" , e.toString())
            }
        }
    }

    override fun onLocationChanged(location: Location) {
        lng = location.longitude
        lat = location.latitude
    }

    private fun checkFineLocationPermission(): Boolean {
        val result = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
        return result == PackageManager.PERMISSION_GRANTED
    }

    private fun checkCoarseLocationPermission(): Boolean {
        val result = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
        return result == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        for (grantResult in grantResults) {
            if (grantResult == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(context, "No internet!", Toast.LENGTH_SHORT).show()
                builder.setTitle("Network Connection")
                    .setMessage("Connection lost. Please try again!")
                    .setPositiveButton("OK") { dialogInterface, it->
                        //moveTaskToBack(true)
                    }
                    .show()

                val alertDialog = builder.create()
                alertDialog.setCanceledOnTouchOutside(false) // Prevent dismissing on outside touch
                alertDialog.show()
            } else {
                getCurrentLocation()
            }
        }
    }

    private fun showExitConfirmationDialog() {
        builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Exit")
            .setMessage("Do you want to quit SmartView app?")
            .setCancelable(true)
            .setPositiveButton("Yes") { dialogInterface: DialogInterface, _: Int ->
                requireActivity().finish()
            }
            .setNegativeButton("No") { dialogInterface: DialogInterface, _: Int ->
                dialogInterface.cancel()
            }
            .show()
    }

    override fun onStatusChanged(s: String, i: Int, bundle: Bundle) {}
    override fun onProviderEnabled(s: String) {}
    override fun onProviderDisabled(s: String) {}



}