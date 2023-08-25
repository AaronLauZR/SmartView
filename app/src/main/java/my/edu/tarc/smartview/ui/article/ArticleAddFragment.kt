package my.edu.tarc.smartview.ui.article

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.findNavController
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import my.edu.tarc.smartview.R
import my.edu.tarc.smartview.databinding.FragmentArticleAddBinding
import java.io.FileNotFoundException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

class ArticleAddFragment : Fragment() {

    //Initialize Binding
    private var _binding: FragmentArticleAddBinding? = null
    private val binding get() = _binding!!

    //Initialize Firebase
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var storageReference: StorageReference

    //Initialize Shared Preferences
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private val MY_PREF = "MY_PREF"

    //Initialize Variable
    private lateinit var title: String
    private var uid: String = ""
    private var articleID: String? = null

    //Initialize Image
    private var imageExist: Boolean = false
    private lateinit var uri: Uri

    //Initialize Spinner
    private var selectCategory: Boolean = false
    private var chooseCategory: String = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment

        //Set Up Binding
        _binding = FragmentArticleAddBinding.inflate(inflater, container, false)

        //SharedPreferences
        sharedPreferences = requireContext().getSharedPreferences(MY_PREF, AppCompatActivity.MODE_PRIVATE)
        editor = sharedPreferences.edit()

        //Firebase Authentication
        auth = FirebaseAuth.getInstance()

        //Get uid from other Fragment
        uid = auth.currentUser?.uid.toString()

        //Create Article Unique ID
        articleID = UUID.randomUUID().toString()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        storageReference = FirebaseStorage.getInstance().getReference("Local Shout/" + articleID.toString())

        val galleryImage = registerForActivityResult(ActivityResultContracts.GetContent(), ActivityResultCallback { it ->
            if(it != null) {
                binding.imageArticle.setImageURI(it)
                uri = it
                imageExist = true
            } else {
                imageExist = false
            }
        })

        val spinner: Spinner = binding.categoryList
        val categorylist = arrayOf("Select Post Category", "Event", "Entertainment", "Foodstuff", "Sport", "Tourism", "Technology")
        val arrayAdp = ArrayAdapter(requireContext(),android.R.layout.simple_spinner_dropdown_item, categorylist)
        spinner.adapter = arrayAdp

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            @SuppressLint("SuspiciousIndentation")
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                var selectedCategory = categorylist[position]
                if (selectedCategory == "Select Post Category") {
                    selectCategory = false
                } else {
                    selectCategory = true
                    chooseCategory = selectedCategory
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectCategory = false;
            }
        }

        val confirmClickListener = DialogInterface.OnClickListener { dialog, which ->
            if (which == DialogInterface.BUTTON_POSITIVE) {
                getArticle()
            }
        }

        val builder = context?.let { AlertDialog.Builder(it) }

        // Set the title, message, and buttons for the dialog
        builder?.setTitle("Post")?.setMessage("Are you sure you want to post the shout out?")
            ?.setPositiveButton("Yes", confirmClickListener)
            ?.setNegativeButton("No", confirmClickListener)

        val dialog: AlertDialog = builder!!.create()

        binding.articleAddimagebtn.setOnClickListener {
            galleryImage.launch("image/*")
        }

        binding.articleSubmitbtn.setOnClickListener {
            dialog.show()
        }


    }

    private fun convertImageUrl() {

        //Convert Image Uri to Url
        if (imageExist) {
            storageReference.putFile(uri).addOnSuccessListener { task ->
                task.metadata!!.reference!!.downloadUrl.addOnSuccessListener { uri ->
                    val imageUrl = uri.toString()
                    database.child(articleID.toString()).child("image").setValue(imageUrl)
                }
            }
        } else {
            Toast.makeText(context, "Image upload failed!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getPostDate(): String? {

        //Get Current Date and Time
        val currentDateTime = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

        return currentDateTime.format(formatter)
    }

    private fun getArticle() {

        //Get Values
        title = binding.articleTitletxt.text.toString()
        val detail: String = binding.articleDescriptiontxt.text.toString()
        val image: String? = null
        val time: String = getPostDate().toString()
        val firstname = sharedPreferences.getString("firstname", "")
        val lastname = sharedPreferences.getString("lastname", "")
        val fullname: String? = "$firstname $lastname"
        val state = sharedPreferences.getString("state", "")
        val city = sharedPreferences.getString("city", "")

        if (title.isNotEmpty() && detail.isNotEmpty() && imageExist && selectCategory && state != "" && city != "") {

            //Add Firebase Path
            database = FirebaseDatabase.getInstance().getReference("Local Shout")

            //Create Object and Connect with Data Class
            val articleDetails = ArticleDetails(chooseCategory, uid, image, title, detail, time, fullname, city, state, articleID)

            //Set Child Path Title and Upload Details to Firebase
            database.child(articleID.toString()).setValue(articleDetails).addOnSuccessListener {
                convertImageUrl()
                binding.articleTitletxt.text.clear()
                binding.articleDescriptiontxt.text.clear()
                selectCategory = false
                imageExist = false
                Toast.makeText(context, "Local shout successfully posted.", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.action_navigation_post_article_to_navigation_article)
            }.addOnFailureListener {
                Toast.makeText(context, "System error!", Toast.LENGTH_SHORT).show()
            }

        } else if (title.isNotEmpty() && detail.isNotEmpty() && !imageExist && selectCategory) {
            Toast.makeText(context, "Image is require!", Toast.LENGTH_SHORT).show()
        } else if (title.isEmpty() && detail.isNotEmpty() && imageExist && selectCategory) {
            Toast.makeText(context, "Title is require!", Toast.LENGTH_SHORT).show()
        } else if (title.isNotEmpty() && detail.isEmpty() && imageExist && selectCategory) {
            Toast.makeText(context, "Description is require!", Toast.LENGTH_SHORT).show()
        } else if (title.isNotEmpty() && detail.isNotEmpty() && imageExist && !selectCategory) {
            Toast.makeText(context, "Category is require!", Toast.LENGTH_SHORT).show()
        } else if (title.isNotEmpty() && detail.isNotEmpty() && imageExist && selectCategory || state == "" || city == "") {
            Toast.makeText(context, "Can't detect location, please try again!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Please fill in all the details!", Toast.LENGTH_SHORT).show()
        }
    }
}