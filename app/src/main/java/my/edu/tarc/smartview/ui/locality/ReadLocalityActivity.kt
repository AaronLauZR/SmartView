package my.edu.tarc.smartview.ui.locality

import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import my.edu.tarc.smartview.R
import my.edu.tarc.smartview.databinding.ActivityReadLocalityBinding
import my.edu.tarc.smartview.ui.locality.adapter.HighlightsAdapter
import my.edu.tarc.smartview.ui.locality.model.ModelHighlights
import my.edu.tarc.smartview.ui.locality.model.ModelLocality
import my.edu.tarc.smartview.ui.locality.network.ApiEndpoint
import my.edu.tarc.smartview.ui.network.NetworkConnection
import org.json.JSONException


class ReadLocalityActivity : AppCompatActivity() {

    //Initialize Binding
    private lateinit var binding: ActivityReadLocalityBinding

    private var progressBar: ProgressDialog? = null
    private var progressBarError: ProgressDialog? = null
    private var highlightsAdapter: HighlightsAdapter? = null
    private val modelHighlights: MutableList<ModelHighlights> = ArrayList()

    var RatingResto = 0.0
    var Id: String? = null
    var ImageCover: String? = null
    var Title: String? = null
    var Rating: String? = null
    var Name: String? = null
    var modelLocality: ModelLocality? = null

    //Alert Dialog
    private lateinit var builder: AlertDialog.Builder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_read_locality)

        binding = ActivityReadLocalityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Alert Dialog
        builder = AlertDialog.Builder(this)

        //Progress Bar
        progressBar = ProgressDialog(this)
        progressBar?.setTitle("Display Locality Detail")
        progressBar?.setCancelable(false)
        progressBar?.setMessage("Retrieving data...")

        // Provide back button for top action bar
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setDisplayShowHomeEnabled(true)
        supportActionBar?.title = "Locality Detail"

        //Check Network Connection
        val networkConnection = NetworkConnection(applicationContext)
        networkConnection.observe(this, Observer {isConnected ->
            if(isConnected) {
                progressBarError?.dismiss()
            }else {
                error()
            }
        })

        //Call Model Locality to retrieve the restaurant data
        modelLocality = intent.getSerializableExtra(DETAIL_LOCALITY) as ModelLocality

        if (modelLocality != null) {
            Id = modelLocality?.id
            ImageCover = modelLocality?.thumbLocality
            RatingResto = modelLocality!!.aggregateRating
            Title = modelLocality?.nameLocality
            Rating = modelLocality?.ratingText
            Name = modelLocality?.nameLocality

            binding.tvRestoName.setText(Name)
            binding.tvRating.setText("$RatingResto")
            val newValue = RatingResto.toFloat()

            binding.ratingResto.setNumStars(5)
            binding.ratingResto.setStepSize(0.5.toFloat())
            binding.ratingResto.setRating(newValue)

            Glide.with(this)
                .load(ImageCover)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(binding.imgCover)

            //method get Highlight
            showRecyclerViewList()

            // Use the Yelp ID to fetch details from the Yelp API
            if (Id != null) {
                getDetailResto()
            }
        }

    }

    private fun showRecyclerViewList() {
        highlightsAdapter = HighlightsAdapter(modelHighlights)

        binding.rvHighlights.setLayoutManager(LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false))
        binding.rvHighlights.setHasFixedSize(true)
        binding.rvHighlights.setAdapter(highlightsAdapter)
    }

    private fun getDetailResto() {

        val url = ApiEndpoint.BASEURL + ApiEndpoint.DetailBusiness + Id

        val requestQueue: RequestQueue = Volley.newRequestQueue(this)
        val request = object : JsonObjectRequest(
            Request.Method.GET, url, null,
            Response.Listener { response ->
                // Handle the successful response here
                try {
                    progressBar?.dismiss()

                    // Extract details from the Yelp API response
                    val jsonArrayCategory = response.getJSONArray("categories")

                    //Display Highlight
                    for (i in 0 until jsonArrayCategory.length()) {
                        val dataApi = ModelHighlights()
                        val categoryObject = jsonArrayCategory.getJSONObject(i)
                        val highlights = categoryObject.getString("title")
                        dataApi.highlights = highlights
                        modelHighlights.add(dataApi)
                    }

                    val jsonObjectData = response.getJSONObject("location")
                    val jsonArrayCoordinate = response.getJSONObject("coordinates")
                    val localityVerbose = jsonObjectData.getString("address1")
                    val address = jsonObjectData.getString("display_address")
                    val phone = response.getString("phone")
                    val displayPhone = response.getString("display_phone")
                    val latitude = jsonArrayCoordinate.getDouble("latitude")
                    val longitude = jsonArrayCoordinate.getDouble("longitude")

                    binding.tvLocalityVerbose.text = localityVerbose
                    binding.tvAddress.text = address.toString()
                    binding.tvContact.text = displayPhone

                    binding.btnRoute.setOnClickListener {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("http://maps.google.com/maps?daddr=$latitude,$longitude"))
                        startActivity(intent)
                    }

                    binding.btnContact.setOnClickListener {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                        startActivity(intent)
                    }
                    highlightsAdapter?.notifyDataSetChanged()
                } catch (e: JSONException) {
                    e.printStackTrace()
                    alertErrorDialog()
                }
            },
            Response.ErrorListener { err ->
                // Handle the error response here
                alertErrorDialog()
            }) {
            // Override the getHeaders() function to include the API Key in the request
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Authorization"] = ApiEndpoint.api
                return headers
            }
        }
        Log.d("RequestQueue_output", requestQueue.toString())
        requestQueue.add(request)

    }

    private fun alertErrorDialog() {
        builder.setTitle("Connection lost")
            .setMessage("Connection is lost. Please try again!")
            .setPositiveButton("OK") { dialogInterface, it->
                moveTaskToBack(true)
            }
            .show()

        val alertDialog = builder.create()
        alertDialog.setCanceledOnTouchOutside(false) // Prevent dismissing on outside touch
        alertDialog.show()
    }

    private fun error() {
        progressBarError = ProgressDialog(this)
        progressBarError?.setTitle("Connection is lost")
        progressBarError?.setCancelable(false)
        progressBarError?.setMessage("Please turn on your network...")
        progressBarError?.show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        const val DETAIL_LOCALITY = "detailResto"
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
