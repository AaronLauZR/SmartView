package my.edu.tarc.smartview.ui.locality

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import my.edu.tarc.smartview.databinding.FragmentLocalityBinding
import my.edu.tarc.smartview.ui.locality.adapter.LocalityAdapter
import my.edu.tarc.smartview.ui.locality.model.ModelLocality
import my.edu.tarc.smartview.ui.locality.network.ApiEndpoint
import my.edu.tarc.smartview.ui.locality.utils.OnItemClickCallback
import org.json.JSONException
import com.android.volley.Request
import com.android.volley.Response
import my.edu.tarc.smartview.ui.MainActivity
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import org.json.JSONObject
import java.io.IOException


class LocalityFragment : Fragment() {

    //Initialize Binding
    private var _binding: FragmentLocalityBinding? = null
    private val binding get() = _binding!!

    //Initialize progress bar and timer
    private var progressBar: ProgressDialog? = null
    private var loadingTimeout = 10000L // 10 seconds timeout
    private lateinit var handler: Handler

    private var localityAdapter: LocalityAdapter? = null
    private var modelLocality: MutableList<ModelLocality> = ArrayList()


    //Initialize latitude and longitude variable
    private var latitude: String? = ""
    private var longitude: String? = ""
    private var city: String? = ""
    private var state: String? = ""

    //Initialize eateries variable
    private var locality: String? = ""

    //Initialize SharedPreference
    val MY_PREF = "MY_PREF"

    //Preferences
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    //Alert Dialog
    private lateinit var builder: AlertDialog.Builder

    //Fragment Manager
    private lateinit var fragmentManager: FragmentManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentLocalityBinding.inflate(inflater, container, false)

        //SharedPreferences
        sharedPreferences = requireContext().getSharedPreferences(MY_PREF, AppCompatActivity.MODE_PRIVATE)
        editor = sharedPreferences.edit()

        //Current Location SharedPreferences
        latitude = sharedPreferences.getString("lat", "")
        longitude = sharedPreferences.getString("long", "")

        city = sharedPreferences.getString("city", "")
        state = sharedPreferences.getString("state", "")

        //Alert Dialog
        builder = AlertDialog.Builder(context)

        //Eateries SharedPreferences
        locality = sharedPreferences.getString("locality", "")

        fragmentManager = requireActivity().supportFragmentManager

        return binding.root
    }

    //@RequiresApi(Build.VERSION_CODES.M)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //Handler Delay
        handler = Handler(Looper.getMainLooper())

        if (city.equals("") && state.equals("")) {
            error()
        }else {
            //Progress Bar
            progressBar()

            //Search query
            searchQueryBusiness(locality.toString())

            //Display Locality
            displayNearestBusiness(locality.toString())
        }

    }

    private fun showRecyclerLocality() {
        localityAdapter = LocalityAdapter(requireContext(), modelLocality)

        binding.rvRestaurantsNearby.setLayoutManager(LinearLayoutManager(context))
        binding.rvRestaurantsNearby.setHasFixedSize(true)
        binding.rvRestaurantsNearby.setAdapter(localityAdapter)

        localityAdapter?.setOnItemClickCallback(object : OnItemClickCallback {
            override fun onItemMainClicked(modelLocality: ModelLocality?) {
                val intent = Intent(requireActivity(), ReadLocalityActivity::class.java)
                intent.putExtra(ReadLocalityActivity.DETAIL_LOCALITY, modelLocality)
                startActivity(intent)
            }
        })
    }

    private fun searchQueryBusiness(query: String) {

        binding.searchResto.setQueryHint("Finding Locality")
        binding.searchResto.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            //Insert locality name
            override fun onQueryTextSubmit(query: String): Boolean {
                displayNearestBusiness(query)
                return false
            }
            //Check if the restaurant input is empty, then display nearest restaurant list
            override fun onQueryTextChange(newText: String): Boolean {
                if (newText == "") displayNearestBusiness(query)
                return false
            }
        })

        val searchPlateId = binding.searchResto.getContext()
            .resources.getIdentifier("android:id/search_plate", null, null)
        val searchPlate = binding.searchResto.findViewById<View>(searchPlateId)
        searchPlate?.setBackgroundColor(Color.TRANSPARENT)
    }

    private fun displayNearestBusiness(query: String) {
        progressBar?.show()

        val url = ApiEndpoint.BASEURL + ApiEndpoint.SearchBusiness + "latitude=$latitude&longitude=$longitude&term=$query&limit=50"

        val requestQueue: RequestQueue = Volley.newRequestQueue(requireContext())
        val request = object : JsonObjectRequest(Method.GET, url, null,
            Response.Listener { response ->
                // Handle the successful response here
                try {
                    progressBar?.dismiss()
                    modelLocality.clear()
                    val jsonArray = response.getJSONArray("businesses")

                    for (i in 0 until jsonArray.length()) {
                        val jsonObject = jsonArray.getJSONObject(i)
                        val dataApi = ModelLocality()

                        // Extract necessary restaurant information from the Yelp API response
                        dataApi.id = jsonObject.getString("id")
                        dataApi.nameLocality = jsonObject.getString("name")
                        dataApi.thumbLocality = jsonObject.getString("image_url")
                        dataApi.ratingText = jsonObject.getDouble("rating").toString()
                        dataApi.addressLocality = jsonObject.getJSONObject("location").getString("address1")
                        dataApi.aggregateRating = jsonObject.getDouble("rating")

                        modelLocality.add(dataApi)
                    }
                    showRecyclerLocality()
                    localityAdapter?.notifyDataSetChanged()
                } catch (e: JSONException) {
                    e.printStackTrace()
                    progressBarError()
                }
            },
            Response.ErrorListener { err ->
                // Handle the error response here
                progressBarError()
            }) {
            // Override the getHeaders() function to include the API Key in the request
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Authorization"] = ApiEndpoint.api
                return headers
            }
        }
        requestQueue.add(request)
    }

    private fun progressBar() {

        progressBar = ProgressDialog(context)
        progressBar?.setTitle("Search Locality")
        progressBar?.setCancelable(false)
        progressBar?.setMessage("Retrieving data...")
        progressBar?.show()

    }

    private fun progressBarError() {
        handler.postDelayed({
            progressBar?.dismiss()
            Toast.makeText(context, "Can't find locality. Please try again!", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
        }, 5000)
    }

    private fun error() {

        //Alert Dialog
        val builder = AlertDialog.Builder(context)

        builder.setTitle("No location")
            .setMessage("Can't detect the location. Please try again!")
            .setCancelable(false)
            .setPositiveButton("OK") { dialogInterface, it->
                findNavController().navigateUp()
            }
            .show()
    }

    companion object {
        fun setWindowFlag(activity: Activity, bits: Int, on: Boolean) {
            val window = activity.window
            val layoutParams = window.attributes
            if (on) {
                layoutParams.flags = layoutParams.flags or bits
            } else {
                layoutParams.flags = layoutParams.flags and bits.inv()
            }
            window.attributes = layoutParams
        }
    }



}
