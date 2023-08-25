package my.edu.tarc.smartview.ui.news

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebViewClient
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.fragment.navArgs
import my.edu.tarc.smartview.databinding.FragmentReadNewsBinding
import my.edu.tarc.smartview.ui.news.adapter.NewsAdapter
import my.edu.tarc.smartview.ui.news.db.ArticleDatabase
import my.edu.tarc.smartview.ui.news.repository.NewsRepository
import my.edu.tarc.smartview.ui.news.viewModel.NewsViewModel
import my.edu.tarc.smartview.ui.news.viewModel.NewsViewModelProviderFactory


class ReadNewsFragment : Fragment() {

    //Initialize Binding
    private var _binding: FragmentReadNewsBinding? = null
    private val binding get() = _binding!!

    private lateinit var navController: NavController
    lateinit var viewModel: NewsViewModel
    lateinit var newsAdapter: NewsAdapter
    val args: ReadNewsFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentReadNewsBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //initializing navController
        navController = Navigation.findNavController(view)

        //initializing View Model
        val newsRepository = NewsRepository(ArticleDatabase(requireContext()))
        val viewModelProviderFactory = NewsViewModelProviderFactory(newsRepository)
        viewModel = ViewModelProvider(this, viewModelProviderFactory).get(NewsViewModel::class.java)


        //Display the news content
        val article = args.article
        binding.webView.apply {
            webViewClient = WebViewClient()
            loadUrl(article.url)
        }


        //Share the news through social media
        binding?.fabShare?.setOnClickListener {
            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.type="text/plain"
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Share this article...")
            shareIntent.putExtra(Intent.EXTRA_TEXT, article.url)
            startActivity(shareIntent)
        }

    }

}