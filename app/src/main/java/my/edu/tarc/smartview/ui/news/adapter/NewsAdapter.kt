package my.edu.tarc.smartview.ui.news.adapter

import android.content.res.Configuration
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import my.edu.tarc.smartview.R
import my.edu.tarc.smartview.ui.news.models.Article
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class NewsAdapter : RecyclerView.Adapter<NewsAdapter.ArticleViewHolder>() {

    inner class ArticleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    private val differCallback = object : DiffUtil.ItemCallback<Article>() {
        override fun areItemsTheSame(oldItem: Article, newItem: Article): Boolean {
            return oldItem.url == newItem.url
        }

        override fun areContentsTheSame(oldItem: Article, newItem: Article): Boolean {
            return oldItem == newItem
        }
    }

    val differ = AsyncListDiffer(this, differCallback)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArticleViewHolder {
        return ArticleViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.list_news_item,
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    override fun onBindViewHolder(holder: ArticleViewHolder, position: Int) {

        val article = differ.currentList[position]
        holder.itemView.apply {
            val newsImage = findViewById<ImageView>(R.id.newsImage)
            val title = findViewById<TextView>(R.id.tvTitle)
            val desc = findViewById<TextView>(R.id.tvDes)
            val name = findViewById<TextView>(R.id.tvName)
            val publishTime = findViewById<TextView>(R.id.news_publication_time)

            //Display News Image
            Glide.with(this)
                .load(article.urlToImage)
                .placeholder(R.drawable.samplenews) // Set the default image placeholder
                .into(newsImage)

            //Display News Title
            title.text = article.title

            //Display News Description
            desc.text = article.description

            //Display News source name
            name.text = article.source.name

            //Display News publish Time
            val currentTimeInHours = Instant.now().atZone(ZoneId.of("Asia/Kolkata"))
            val newsTimeInHours = Instant.parse(article.publishedAt).atZone(ZoneId.of("Asia/Kolkata"))
            val hoursDifference = Duration.between(currentTimeInHours, newsTimeInHours)
            val hoursAgo = " " + hoursDifference.toHours().toString().substring(1) + " hour ago"

            // Get the date formatter
            val dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")

            // Format the newsTimeInHours to get only the date
            val formattedDate = dateFormatter.format(newsTimeInHours)

            publishTime.text = formattedDate

            setOnClickListener {
                onItemClickListener?.let { it(article) }
            }

        }

    }

    private var onItemClickListener: ((Article) -> Unit)? = null

    fun setOnItemClickListener(listener: (Article) -> Unit) {
        onItemClickListener = listener
    }

}

interface INewsRVAdapter{
    fun onNewsClicked(article: Article)
}