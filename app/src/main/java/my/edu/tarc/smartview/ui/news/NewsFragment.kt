package my.edu.tarc.smartview.ui.news

import android.app.AlertDialog
import android.content.ContentValues.TAG
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import dagger.hilt.android.AndroidEntryPoint
import my.edu.tarc.smartview.R
import my.edu.tarc.smartview.databinding.FragmentNewsBinding
import my.edu.tarc.smartview.ui.news.adapter.CategoryRVAdapter
import my.edu.tarc.smartview.ui.news.adapter.ICategoryRVAdapter
import my.edu.tarc.smartview.ui.news.adapter.INewsRVAdapter
import my.edu.tarc.smartview.ui.news.adapter.NewsAdapter
import my.edu.tarc.smartview.ui.news.db.ArticleDatabase
import my.edu.tarc.smartview.ui.news.models.Article
import my.edu.tarc.smartview.ui.news.repository.NewsRepository
import my.edu.tarc.smartview.ui.news.utils.Resource
import my.edu.tarc.smartview.ui.news.viewModel.NewsViewModel
import my.edu.tarc.smartview.ui.news.viewModel.NewsViewModelProviderFactory


@AndroidEntryPoint
class NewsFragment : Fragment(), ICategoryRVAdapter, INewsRVAdapter {

    //Initialize NavController
    private lateinit var navController: NavController

    //Initialize Binding
    private var _binding: FragmentNewsBinding? = null
    private val binding get() = _binding!!

    //Alert Dialog
    private lateinit var builder: AlertDialog.Builder

    //Initialise News
    private val categories = listOf("General", "My Locality", "Business", "Entertainment", "Sports", "Health", "Science", "Technology")
    private val categoryAdapter = CategoryRVAdapter(categories, this)
    private var currentCategory = "General"
    private var isScrolling: Boolean = false
    private var currentPage = 1
    private var itemsDisplayed = 0
    lateinit var viewModel: NewsViewModel
    private lateinit var newsAdapter: NewsAdapter

    //Initialize SharedPreference
    val MY_PREF = "MY_PREF"

    //Preferences
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    //Initialize City variable
    private var city: String = ""
    private var state: String = ""


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentNewsBinding.inflate(inflater, container, false)

        //SharedPreferences
        sharedPreferences = requireContext().getSharedPreferences(MY_PREF, AppCompatActivity.MODE_PRIVATE)
        editor = sharedPreferences.edit()

        city = sharedPreferences.getString("city", "").toString()
        state = sharedPreferences.getString("state", "").toString()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //Navigate back to profile page
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Handle the back button press here
                // Add your desired functionality or navigation logic
                findNavController().navigate(R.id.action_navigation_news_to_navigation_home)
            }
        })

        //initializing navController
        //navController = Navigation.findNavController(view)

        //initializing View Model
        //viewModel = (activity as MainActivity).viewModel
        //viewModel = ViewModelProvider(this).get(NewsViewModel::class.java)
        val newsRepository = NewsRepository(ArticleDatabase(requireContext()))
        val viewModelProviderFactory = NewsViewModelProviderFactory(newsRepository)
        viewModel = ViewModelProvider(this, viewModelProviderFactory).get(NewsViewModel::class.java)


        //Set up Category Option
        setUpCategoriesRecyclerView()

        //Set up news
        setUpNewsRecyclerView()

        //Navigate to the news detail
        newsAdapter.setOnItemClickListener {
            val bundle = Bundle().apply {
                putSerializable("article", it)
            }
            findNavController().navigate(
                R.id.action_navigation_news_to_navigation_read_news,
                bundle
            )
        }

        viewModel.breakingNews.observe(viewLifecycleOwner, Observer { response ->
            when(response) {
                is Resource.Success -> {
                    hideProgressBar()
                    response.data?.let { newsResponse ->
                        newsAdapter.differ.submitList(newsResponse.articles)


                    }
                }
                is Resource.Error -> {
                    hideProgressBar()
                    response.data?.let { message ->
                        Log.e("Breaking News Fragment", "An error occured: $message")
                    }
                }
                is Resource.Loading -> {
                    showProgressBar()
                }
            }
        })

    }

    private fun hideProgressBar() {
        binding.progressBar.visibility = View.INVISIBLE
    }

    private fun showProgressBar() {
        binding.progressBar.visibility = View.VISIBLE
    }

    private fun setUpNewsRecyclerView() {
        newsAdapter = NewsAdapter()

        binding?.rvNews?.apply {
            adapter = newsAdapter
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        }
    }

    private fun setUpCategoriesRecyclerView() {
        binding?.rvCategory?.apply {
            adapter = categoryAdapter
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        }
    }

    override fun onCategoryClicked(category: String) {
        currentCategory = category
        itemsDisplayed = 0
        currentPage = 1

        if(currentCategory == "My Locality") {
            if(city.equals("")) {
                Toast.makeText(context, "No location", Toast.LENGTH_SHORT).show()
            }else {
                viewModel.getSearchNews("$city", "en", "thestar.com.my,paultan.org,freemalaysiatoday.com,malaymail.com", "relevancy")
                Toast.makeText(context, "Location: $city, $state", Toast.LENGTH_SHORT).show()
            }
        } else {
            viewModel.getBreakingNews("my", category)
        }

    }


    override fun onNewsClicked(article: Article) {

    }


}