package my.edu.tarc.smartview.ui.locality.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import my.edu.tarc.smartview.ui.locality.model.ModelLocality
import my.edu.tarc.smartview.ui.locality.utils.OnItemClickCallback
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import my.edu.tarc.smartview.R

class LocalityAdapter (

    private val mContext: Context, private val items: List<ModelLocality>) : RecyclerView.Adapter<LocalityAdapter.ViewHolder>() {

    private var Rating = 0.0

    private var onItemClickCallback: OnItemClickCallback? = null

    fun setOnItemClickCallback(onItemClickCallback: OnItemClickCallback?) {
        this.onItemClickCallback = onItemClickCallback
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_restaurant_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val data = items[position]

        Rating = data.aggregateRating

        Glide.with(mContext)
            .load(data.thumbLocality)
            .transform(CenterCrop(), RoundedCorners(25))
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(holder.imgResto)

        val newValue = Rating.toFloat()
        holder.ratingResto.numStars = 5
        holder.ratingResto.stepSize = 0.5.toFloat()
        holder.ratingResto.rating = newValue

        holder.tvNameResto.text = data.nameLocality
        holder.tvAddress.text = data.addressLocality
        holder.tvRating.text = " |  " + data.aggregateRating
        holder.cvListMain.setOnClickListener {
            onItemClickCallback?.onItemMainClicked(data)
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    //Class Holder
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var cvListMain: CardView
        var imgResto: ImageView
        var tvNameResto: TextView
        var tvAddress: TextView
        var tvRating: TextView
        var ratingResto: RatingBar

        init {
            cvListMain = itemView.findViewById(R.id.cvListMain)
            imgResto = itemView.findViewById(R.id.imgResto)
            tvNameResto = itemView.findViewById(R.id.tvNameResto)
            tvRating = itemView.findViewById(R.id.tvRating)
            tvAddress = itemView.findViewById(R.id.tvAddress)
            ratingResto = itemView.findViewById(R.id.ratingResto)
        }
    }
}