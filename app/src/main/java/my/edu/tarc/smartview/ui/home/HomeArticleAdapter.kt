package my.edu.tarc.smartview.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import my.edu.tarc.smartview.R
import android.widget.ImageView
import com.squareup.picasso.Picasso
import my.edu.tarc.smartview.ui.article.ArticleAdapter
import my.edu.tarc.smartview.ui.article.ArticleDetails

class HomeArticleAdapter(private val articleList: ArrayList<ArticleDetails>) : RecyclerView.Adapter<HomeArticleAdapter.MyViewHolder>() {

    private lateinit var articleListener: onItemClickListener

    interface onItemClickListener {
        fun onItemClick (position: Int)
    }

    fun setOnItemClickListener(listener: onItemClickListener) {
        articleListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {

        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.list_home_article_item, parent, false)
        return MyViewHolder(itemView, articleListener)

    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {

        val currentItem = articleList[position]

        holder.tvTitle.text = currentItem.title
        holder.article_publish_time.text = currentItem.time

        Picasso.get()
            .load(currentItem.image)
            .placeholder(R.drawable.samplenews) // Set the placeholder image
            .into(holder.articleImage);

    }

    override fun getItemCount(): Int {
        //return articleList.size
        return minOf(articleList.size, 5)
    }


    class MyViewHolder(itemView: View, listener: HomeArticleAdapter.onItemClickListener): RecyclerView.ViewHolder(itemView) {

        val tvTitle : TextView = itemView.findViewById(R.id.tvTitle)
        val article_publish_time : TextView = itemView.findViewById(R.id.article_publication_time)
        val articleImage : ImageView = itemView.findViewById(R.id.articleImage)

        init {
            itemView.setOnClickListener {
                listener.onItemClick(adapterPosition)
            }
        }

    }

}