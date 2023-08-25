package my.edu.tarc.smartview.ui.news.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import my.edu.tarc.smartview.ui.news.repository.NewsRepository

class NewsViewModelProviderFactory(
    val newsRepository: NewsRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return NewsViewModel(newsRepository) as T
    }

}