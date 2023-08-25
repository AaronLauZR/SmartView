package my.edu.tarc.smartview.ui.news.models

import my.edu.tarc.smartview.ui.news.models.Article

data class NewsResponse(
    val articles: List<Article>,
    val status: String,
    val totalResults: Int
)