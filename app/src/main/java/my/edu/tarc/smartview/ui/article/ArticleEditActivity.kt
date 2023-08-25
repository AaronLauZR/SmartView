package my.edu.tarc.smartview.ui.article

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.squareup.picasso.Picasso
import my.edu.tarc.smartview.R
import my.edu.tarc.smartview.databinding.ActivityEditArticleBinding
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ArticleEditActivity : AppCompatActivity() {

    //Initialize Binding
    private lateinit var binding: ActivityEditArticleBinding

    //Initialize Spinner
    private var selectCategory: Boolean = true
    private var articleCategory: String = ""
    private var chosenCategory: String = ""

    //Initialize Image
    private var imageExist: Boolean = false
    private lateinit var uri: Uri

    //Initialize Firebase
    private lateinit var database: DatabaseReference
    private lateinit var storageReference: StorageReference

    //Initialize Variable
    private var articleImage: String? = null
    private var articleTitle: String? = null
    private var articleDescription: String? = null
    private var articleUid: String? = null

    @SuppressLint("WrongViewCast")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditArticleBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        // Provide back button for top action bar
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setDisplayShowHomeEnabled(true)
        supportActionBar?.title = "Edit Local Shout"

        val image: ImageView = findViewById(R.id.article_editimage)
        val title: TextView = findViewById(R.id.article_edittitletxt)
        val description: TextView = findViewById(R.id.article_editdescriptiontxt)

        val bundle: Bundle? = intent.extras
        articleImage = bundle!!.getString("image")
        articleTitle = bundle!!.getString("title")
        articleDescription = bundle!!.getString("description")
        articleUid = bundle!!.getString("articleId")

        val categoryList = arrayOf("Select Post Category", "Event", "Entertainment", "Foodstuff", "Sport", "Tourism", "Technology")
        articleCategory = bundle!!.getString("category").toString()

        val spinner: Spinner = binding.articleEditcategory
        val arrayAdp = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categoryList)
        spinner.adapter = arrayAdp

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedCategory = categoryList[position]
                if (selectedCategory == "Select Post Category") {
                    selectCategory = false
                } else {
                    selectCategory = true
                    chosenCategory = selectedCategory
                }
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                selectCategory = false
            }
        }

        // Find the index of the selected category in the category list
        val selectedCategoryIndex = categoryList.indexOf(articleCategory)

        // Set the selected category in the spinner
        spinner.setSelection(selectedCategoryIndex)

        Picasso.get()
            .load(articleImage)
            .placeholder(R.drawable.samplenews) // Placeholder image while loading
            .error(R.drawable.no_image) // Image to show if loading fails
            .into(image)
        title.text = articleTitle
        description.text = articleDescription

        storageReference = FirebaseStorage.getInstance().getReference("Local Shout/" + articleUid.toString())

        val galleryImage = registerForActivityResult(ActivityResultContracts.GetContent(), ActivityResultCallback { it ->
            if(it != null) {
                binding.articleEditimage.setImageURI(it)
                uri = it
                imageExist = true
            } else {
                imageExist = false
            }
        })

        val confirmClickListener = DialogInterface.OnClickListener { dialog, which ->
            if (which == DialogInterface.BUTTON_POSITIVE) {
                getEditArticle()
            }
        }

        val confirmClickListener2 = DialogInterface.OnClickListener { dialog, which ->
            if (which == DialogInterface.BUTTON_POSITIVE) {
                getDeleteArticle()
            }
        }

        // Create the AlertDialog.Builder
        val builder = AlertDialog.Builder(this)

        // Set the title, message, and buttons for the dialog
        builder.setTitle("Edit")
            .setMessage("Are you sure you want to save the changes?")
            .setPositiveButton("Yes", confirmClickListener)
            .setNegativeButton("No", confirmClickListener)

        // Create the AlertDialog
        val dialog: AlertDialog = builder.create()

        // Create the AlertDialog.Builder
        val builder2 = AlertDialog.Builder(this)

        // Set the title, message, and buttons for the dialog
        builder2.setTitle("Delete")
            .setMessage("Are you sure you want to delete?")
            .setPositiveButton("Yes", confirmClickListener2)
            .setNegativeButton("No", confirmClickListener2)

        // Create the AlertDialog
        val dialog2: AlertDialog = builder2.create()

        binding.articleEditimagebtn.setOnClickListener {
            galleryImage.launch("image/*")
        }

        binding.articleSavearticle.setOnClickListener {
            dialog.show()
        }

        binding.articleDeletearticle.setOnClickListener {
            dialog2.show()
        }
    }

    private fun convertImageUrl() {

        //Convert Image Uri to Url
        if (imageExist) {
            storageReference.putFile(uri).addOnSuccessListener { task ->
                task.metadata!!.reference!!.downloadUrl.addOnSuccessListener { uri ->
                    val imageUrl = uri.toString()
                    database.child(articleUid.toString()).child("image").setValue(imageUrl)
                }
            }
        } else {
            Toast.makeText(this, "Image upload failed!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getPostDate(): String? {

        //Get Current Date and Time
        val currentDateTime = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

        return currentDateTime.format(formatter)
    }

    private fun getEditArticle() {
        //Get Values
        val title = binding.articleEdittitletxt.text.toString()
        val detail: String = binding.articleEditdescriptiontxt.text.toString()
        val time: String = getPostDate().toString()

        database = FirebaseDatabase.getInstance().getReference("Local Shout")

        if (title == articleTitle && detail == articleDescription && articleCategory.equals(chosenCategory) && !imageExist) {
            Toast.makeText(this, "No detail has been changed!", Toast.LENGTH_SHORT).show()
            onBackPressed();
        } else if (title.isEmpty() && detail.isNotEmpty() && !imageExist && selectCategory) {
            Toast.makeText(this, "Title is require!", Toast.LENGTH_SHORT).show()
        } else if (title.isNotEmpty() && detail.isEmpty() && !imageExist && selectCategory) {
            Toast.makeText(this, "Description is require!", Toast.LENGTH_SHORT).show()
        } else if (title.isNotEmpty() && detail.isNotEmpty() && !imageExist && !selectCategory) {
            Toast.makeText(this, "Category is require!", Toast.LENGTH_SHORT).show()
        } else if (title.isNotEmpty() && detail.isNotEmpty() && selectCategory) {
            if (imageExist) {
                convertImageUrl()
            }
            database.child(articleUid.toString()).child("title").setValue(title)
            database.child(articleUid.toString()).child("detail").setValue(detail)
            database.child(articleUid.toString()).child("category").setValue(chosenCategory)
            database.child(articleUid.toString()).child("time").setValue(time)
            Toast.makeText(this, "Changes successfully uploaded.", Toast.LENGTH_SHORT).show()
            onBackPressed();
        } else {
            Toast.makeText(this, "Please fill in all the details!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getDeleteArticle() {
        database = FirebaseDatabase.getInstance().getReference("Local Shout")

        database.child(articleUid.toString()).removeValue().addOnSuccessListener {
            Toast.makeText(this, "Your shout has successfully deleted.", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener {
            Toast.makeText(this, "Your shout has not deleted, please try again later!", Toast.LENGTH_SHORT).show()
        }

        onBackPressed();
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}