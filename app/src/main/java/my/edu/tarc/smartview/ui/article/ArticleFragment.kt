package my.edu.tarc.smartview.ui.article

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import my.edu.tarc.smartview.databinding.FragmentArticleBinding
import my.edu.tarc.smartview.ui.news.adapter.ICategoryRVAdapter
import my.edu.tarc.smartview.ui.news.adapter.NewsAdapter
import my.edu.tarc.smartview.ui.news.viewModel.NewsViewModel

class ArticleFragment : Fragment(), ArticleCategoryAdapter.IArticleCategoryAdapter {

    //Initialize Binding
    private var _binding: FragmentArticleBinding? = null
    private val binding get() = _binding!!

    //Initialize Database
    private lateinit var database: DatabaseReference

    //Initialize Article Recycler View
    private lateinit var articleRecyclerView: RecyclerView
    private lateinit var articleArrayList: ArrayList<ArticleDetails>

    //Initialize Category Recycler View
    private val categories =
        listOf("All", "Event", "Entertainment", "Foodstuff", "Sport", "Tourism", "Technology")
    private val categoryAdapter = ArticleCategoryAdapter(categories, this)
    private var currentCategory = "General"

    //Initialize Shared Preferences
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private val MY_PREF = "MY_PREF"

    //Initialize Searchbar
    private lateinit var searchView: androidx.appcompat.widget.SearchView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentArticleBinding.inflate(inflater, container, false)

        //SharedPreferences
        sharedPreferences = requireContext().getSharedPreferences(MY_PREF, AppCompatActivity.MODE_PRIVATE)
        editor = sharedPreferences.edit()

        setUpCategoriesRecyclerView()

        searchView = binding.articleSearchView
        searchView.clearFocus()
        searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                // Handle query submission if needed
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterList(newText)
                return true
            }
        })

        articleRecyclerView = binding.articleRecyclerview
        articleRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        articleRecyclerView.setHasFixedSize(true)

        articleArrayList = arrayListOf<ArticleDetails>()
        articleArrayList.clear()
        getLocalShout()

        return binding.root
    }

    private fun getLocalShout() {
        val city = sharedPreferences.getString("city", "")
        database = FirebaseDatabase.getInstance().getReference("Local Shout")

        articleArrayList.clear()

        if (city == "") {
            //Alert Dialog
            val builder = AlertDialog.Builder(requireContext())

            builder.setTitle("No location")
                .setMessage("Can't detect the location. Please try again!")
                .setCancelable(false)
                .setPositiveButton("OK") { dialogInterface, it->
                    findNavController().navigateUp()
                }.show()
        }

        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                articleArrayList.clear()
                if (snapshot.exists()) {
                    for (articleSnapshot in snapshot.children) {
                        if (articleSnapshot.child("city").getValue().toString().equals(city)) {
                            val article = articleSnapshot.getValue(ArticleDetails::class.java)
                            articleArrayList.add(article!!)
                        }
                    }

                    articleArrayList.sortByDescending { it.time }

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
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "System error!", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun getCategoryShout() {
        val city = sharedPreferences.getString("city", "")
        val selectedCategory = sharedPreferences.getString("category", "")
        database = FirebaseDatabase.getInstance().getReference("Local Shout")

        articleArrayList.clear()

        if (city == "") {
            //Alert Dialog
            val builder = AlertDialog.Builder(requireContext())

            builder.setTitle("No location")
                .setMessage("Can't detect the location. Please try again!")
                .setCancelable(false)
                .setPositiveButton("OK") { dialogInterface, it->
                    findNavController().navigateUp()
                }.show()
        }

        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                articleArrayList.clear()
                if (snapshot.exists()) {
                    for (articleSnapshot in snapshot.children) {
                        if (articleSnapshot.child("city").getValue().toString().equals(city)) {
                            if (articleSnapshot.child("category").getValue().toString().equals(selectedCategory)) {
                                val article = articleSnapshot.getValue(ArticleDetails::class.java)
                                articleArrayList.add(article!!)
                            }
                        }
                    }

                    if (articleArrayList.isEmpty()) {
                        // Show a toast message if the list is empty
                        Toast.makeText(context,
                            "This area doesn't have any posted items about $selectedCategory yet.",
                            Toast.LENGTH_SHORT).show()
                    } else {
                        // Sort the articles based on the "time" field
                        articleArrayList.sortByDescending { it.time }}

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
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "System error!", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setUpCategoriesRecyclerView() {
        binding?.articleCategoryRecyclerView?.apply {
            adapter = categoryAdapter
            layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        }
    }

    override fun onArticleCategoryClicked(category: String) {
        currentCategory = category

        if (currentCategory == "All") {
            getLocalShout()
        } else if (currentCategory == "Event") {
            editor.putString("category", currentCategory)
            editor.apply()
            getCategoryShout()
        } else if (currentCategory == "Entertainment") {
            editor.putString("category", currentCategory)
            editor.apply()
            getCategoryShout()
        } else if (currentCategory == "Foodstuff") {
            editor.putString("category", currentCategory)
            editor.apply()
            getCategoryShout()
        } else if (currentCategory == "Sport") {
            editor.putString("category", currentCategory)
            editor.apply()
            getCategoryShout()
        } else if (currentCategory == "Tourism") {
            editor.putString("category", currentCategory)
            editor.apply()
            getCategoryShout()
        } else if (currentCategory == "Technology") {
            editor.putString("category", currentCategory)
            editor.apply()
            getCategoryShout()
        }
    }

    private fun filterList(query: String?) {
        val filteredList = ArrayList<ArticleDetails>()

        query?.let {
            val lowerCaseQuery = it.toLowerCase()

            for (article in articleArrayList) {
                if (article.title!!.toLowerCase().contains(lowerCaseQuery)) {
                    filteredList.add(article)
                }
            }

            val adapter = ArticleAdapter(filteredList)
            articleRecyclerView.adapter = adapter
            adapter.setOnItemClickListener(object : ArticleAdapter.onItemClickListener {
                override fun onItemClick(position: Int) {
                    val intent = Intent(requireContext(), ReadArticleActivity::class.java)
                    intent.putExtra("articleId", filteredList[position].articleID)
                    intent.putExtra("category", filteredList[position].category)
                    intent.putExtra("image", filteredList[position].image)
                    intent.putExtra("time", filteredList[position].time)
                    intent.putExtra("area", filteredList[position].city)
                    intent.putExtra("title", filteredList[position].title)
                    intent.putExtra("name", filteredList[position].name)
                    intent.putExtra("description", filteredList[position].detail)
                    startActivity(intent)
                }
            })
        }
    }
}