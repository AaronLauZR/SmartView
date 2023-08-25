package my.edu.tarc.smartview.ui.news.api

import my.edu.tarc.smartview.ui.news.models.NewsResponse
import my.edu.tarc.smartview.ui.news.utils.Constants.Companion.API_KEY
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface NewsAPI {

    @GET("v2/top-headlines")
    suspend fun getBreakingNews(
        @Query("country")
        countryCode: String = "my",
        @Query("page")
        pageNumber: Int = 1,
        @Query("category")
        category: String,
        @Query("apiKey")
        apiKey: String = API_KEY,
    ): Response<NewsResponse>

    @GET("v2/everything")
    suspend fun getSearchNews(
        @Query("q")
        searchQuery: String,
        @Query("language")
        language: String?,
        @Query("domains")
        domains: String?,
        @Query("sortBy")
        sortBy: String?,
        @Query("page")
        pageNumber: Int = 1,
        @Query("apiKey")
        apiKey: String = API_KEY,
    ): Response<NewsResponse>
}