package my.edu.tarc.smartview.ui.article

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import my.edu.tarc.smartview.R
import my.edu.tarc.smartview.ui.news.models.Article

class ArticleAdapter(private val articleList : ArrayList<ArticleDetails>) : RecyclerView.Adapter<ArticleAdapter.MyViewHolder>() {

    private lateinit var articleListener: onItemClickListener

    interface onItemClickListener {
        fun onItemClick (position: Int)
    }

    fun setOnItemClickListener(listener: onItemClickListener) {
        articleListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {

        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.list_news_item, parent, false)

        return MyViewHolder(itemView, articleListener)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {

        val currentItem = articleList[position]

        holder.title.text = currentItem.title
        holder.detail.text = currentItem.detail
        holder.category.text = currentItem.category
        holder.time.text = currentItem.time

        Picasso.get()
            .load(currentItem.image)
            .placeholder(R.drawable.samplenews) // Set the placeholder image
            .into(holder.image);
    }

    override fun getItemCount(): Int {

        return articleList.size
    }

    class MyViewHolder(itemView : View, listener: onItemClickListener) : RecyclerView.ViewHolder(itemView) {

        val image : ImageView = itemView.findViewById(R.id.newsImage)
        val title : TextView = itemView.findViewById(R.id.tvTitle)
        val detail : TextView = itemView.findViewById(R.id.tvDes)
        val category : TextView = itemView.findViewById(R.id.tvName)
        val time : TextView = itemView.findViewById(R.id.news_publication_time)

        init {
            itemView.setOnClickListener {
                listener.onItemClick(adapterPosition)
            }
        }
    }
}