package my.edu.tarc.smartview.ui.news.repository

import androidx.lifecycle.ViewModel
import my.edu.tarc.smartview.ui.news.api.RetrofitInstance
import my.edu.tarc.smartview.ui.news.db.ArticleDatabase

class NewsRepository(
    val db: ArticleDatabase
) {
    suspend fun getBreakingNews(countryCode: String, pageNumber: Int, category: String) =
        RetrofitInstance.api.getBreakingNews(countryCode, pageNumber, category)

    suspend fun getSearchNews(query: String, language: String, domains: String, sortBy: String, pageNumber: Int) =
        RetrofitInstance.api.getSearchNews(query, language, domains, sortBy, pageNumber)
}