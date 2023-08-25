package my.edu.tarc.smartview.ui.article

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import my.edu.tarc.smartview.databinding.FragmentArticleBookmarkBinding

class ArticleBookmarkFragment : Fragment() {

    //Initialize Binding
    private var _binding: FragmentArticleBookmarkBinding? = null
    private val binding get() = _binding!!

    //Initialize Database
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth

    //Initialize Recycler View
    private lateinit var articleRecyclerView: RecyclerView
    private lateinit var articleArrayList: ArrayList<ArticleDetails>

    //Initialize Database Path
    private var uid: String = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        _binding = FragmentArticleBookmarkBinding.inflate(inflater, container, false)

        //Get uid of CurrentUser
        auth = FirebaseAuth.getInstance()
        uid = auth.currentUser?.uid.toString()

        //Set RecyclerView at Correct Layout Section
        articleRecyclerView = binding.bookmarkRecyclerview
        articleRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        articleRecyclerView.setHasFixedSize(true)

        articleArrayList = arrayListOf<ArticleDetails>()
        articleArrayList.clear()
        getBookmarkShout()

        return binding.root
    }

    private fun getBookmarkShout() {
        database = FirebaseDatabase.getInstance().getReference("users").child(uid).child("Favorite")

        articleArrayList.clear()

        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                articleArrayList.clear()
                if (snapshot.exists()) {
                    for (articleSnapshot in snapshot.children) {
                        val article = articleSnapshot.getValue(ArticleDetails::class.java)
                        articleArrayList.add(article!!)
                    }

                    // Sort the articles based on the "time" field
                    articleArrayList.sortByDescending { it.bookmarkTime }

                    var adapter = ArticleAdapter(articleArrayList)
                    articleRecyclerView.adapter = adapter
                    adapter.setOnItemClickListener(object : ArticleAdapter.onItemClickListener {
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
                } else {
                    val builder = AlertDialog.Builder(requireContext())

                    builder.setTitle("No Bookmarks")
                        .setMessage("You have no bookmarked items.")
                        .setCancelable(false)
                        .setPositiveButton("OK") { dialogInterface, it ->
                            findNavController().navigateUp()
                        }.show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "System error!", Toast.LENGTH_SHORT).show()
            }
        })
    }
}