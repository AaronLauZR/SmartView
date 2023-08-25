package my.edu.tarc.smartview.ui.article

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.squareup.picasso.Picasso
import my.edu.tarc.smartview.R
import my.edu.tarc.smartview.databinding.ActivityReadArticleBinding
import my.edu.tarc.smartview.databinding.FragmentArticleBookmarkBinding
import org.w3c.dom.Text
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ReadArticleActivity : AppCompatActivity() {

    //Initialize Binding
    private lateinit var binding: ActivityReadArticleBinding

    //Initialize Database
    private lateinit var userDatabase: DatabaseReference
    private lateinit var auth: FirebaseAuth

    //Initialize Variable
    private var uid: String = ""
    private var articleUID: String? = null
    private var articleCategory: String? = null
    private var articleImage: String? = null
    private var articleTime: String? = null
    private var articleArea: String? = null
    private var articleTitle: String? = null
    private var articleName: String? = null
    private var articleDescription: String? = null
    private var bookmarkExist: Boolean = false
    private var isChangingCheckboxState = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReadArticleBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        // Provide back button for top action bar
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setDisplayShowHomeEnabled(true)
        supportActionBar?.title = "Read Local Shout"

        val image: ImageView = findViewById(R.id.read_articleimage)
        val time: TextView = findViewById(R.id.read_articletime)
        val area: TextView = findViewById(R.id.read_articlearea)
        val title: TextView = findViewById(R.id.read_articletitle)
        val name: TextView = findViewById(R.id.read_articlename)
        val description: TextView = findViewById(R.id.read_articledescription)

        val bundle: Bundle? = intent.extras
        articleUID = bundle!!.getString("articleId")
        articleCategory = bundle!!.getString("category")
        articleImage = bundle!!.getString("image")
        articleTime = bundle!!.getString("time")
        articleArea = bundle!!.getString("area")
        articleTitle = bundle!!.getString("title")
        articleName = bundle!!.getString("name")
        articleDescription = bundle!!.getString("description")

        Picasso.get()
            .load(articleImage)
            .placeholder(R.drawable.samplenews) // Placeholder image while loading
            .error(R.drawable.no_image) // Image to show if loading fails
            .into(image)
        time.text = articleTime
        area.text = articleArea
        title.text = articleTitle
        name.text = articleName
        description.text = articleDescription

        //Firebase Authentication
        auth = FirebaseAuth.getInstance()

        //Get uid from other Fragment
        uid = auth.currentUser?.uid.toString()

        //Bookmark Icon
        val cbHeartt = binding.cbHeart

        //Connect to Favorite Path
        userDatabase = FirebaseDatabase.getInstance().getReference("users").child(uid).child("Favorite")

        //Check whether Bookmarked
        userDatabase.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var newBookmarkExist = false

                if (snapshot.exists()) {
                    for (articleSnapshot in snapshot.children) {
                        if (articleSnapshot.child("articleID").getValue().toString().equals(articleUID)) {
                            newBookmarkExist = true
                            break
                        }
                    }
                }

                // Update the bookmarkExist value
                bookmarkExist = newBookmarkExist

                // Set the checkbox state without triggering the listener
                isChangingCheckboxState = true
                cbHeartt.isChecked = bookmarkExist
                isChangingCheckboxState = false
            }

            override fun onCancelled(error: DatabaseError) {
                bookmarkExist = false
            }
        })

        cbHeartt.setOnCheckedChangeListener { checkBox, isChecked ->
            // Only proceed if the state change is not due to programmatic change
            if (!isChangingCheckboxState) {
                if (isChecked) {
                    addBookmark()
                } else {
                    removeBookmark()
                }
            }
        }
    }

    private fun getBookmarkTime(): String? {

        //Get Current Date and Time
        val currentDateTime = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

        return currentDateTime.format(formatter)
    }

    private fun addBookmark() {

        val articleBookmarkTime: String = getBookmarkTime().toString()

        //Connect Parent Path
        userDatabase = FirebaseDatabase.getInstance().getReference("users")

        //Create Object and Connect with Data Class
        val articleFavoriteDetails = ArticleFavoriteDetails(articleCategory, uid, articleImage, articleTitle, articleDescription, articleTime, articleName, articleArea, articleUID, articleBookmarkTime)

        //Add Favorite Child Path to Users Parent Path and Set Shout Out Value
        userDatabase.child(uid).child("Favorite").child(articleUID.toString()).setValue(articleFavoriteDetails).addOnSuccessListener {
            Toast.makeText(this, "Shout out successfully bookmarked.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun removeBookmark() {

        //Connect Parent Path
        userDatabase = FirebaseDatabase.getInstance().getReference("users")

        //Remove Favorite from Favorite Child Path in Users Parent Path
        userDatabase.child(uid).child("Favorite").child(articleUID.toString()).removeValue().addOnSuccessListener {
            Toast.makeText(this, "Bookmark successfully removed.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
