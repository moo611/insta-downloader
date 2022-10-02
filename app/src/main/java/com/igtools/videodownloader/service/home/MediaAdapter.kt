package com.igtools.videodownloader.service.home

import android.content.Context
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.igtools.videodownloader.R
import com.igtools.videodownloader.service.details.BlogDetailsActivity
import com.igtools.videodownloader.service.details.TagBlogDetails
import com.igtools.videodownloader.models.MediaModel

class MediaAdapter(var c: Context) : RecyclerView.Adapter<MediaAdapter.BlogViewHolder>() {

    var medias: ArrayList<MediaModel> = ArrayList()
    val gson = Gson()
    var fromTag = false

    inner class BlogViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var imgThumbnail: ImageView = itemView.findViewById(R.id.img_thumbnail)
        var imgCollections: ImageView = itemView.findViewById(R.id.img_collections)
        var imgPlayer: ImageView = itemView.findViewById(R.id.img_player)
    }

    fun refresh(medias: ArrayList<MediaModel>) {
        this.medias = medias
        notifyDataSetChanged()
    }

    fun loadMore(medias: ArrayList<MediaModel>) {
        val index = this.medias.size
        this.medias.addAll(medias)
        notifyItemRangeInserted(index, medias.size)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BlogViewHolder {
        val v = LayoutInflater.from(c).inflate(R.layout.item_media, parent, false)
        return BlogViewHolder(v)
    }

    override fun onBindViewHolder(holder: BlogViewHolder, position: Int) {
        Glide.with(c)
            .load(medias[position].thumbnailUrl)
            .placeholder(ColorDrawable(ContextCompat.getColor(c, R.color.gray_1)))
            .into(holder.imgThumbnail)

        holder.itemView.setOnClickListener {

            val mediaModel = gson.toJson(medias[position])
            if (fromTag) {
                //tag的结果没有resources，因此需要调接口重新获取子集
                c.startActivity(
                    Intent(c, TagBlogDetails::class.java)
                        .putExtra("code", medias[position].code)
                        .putExtra("flag", true)

                )
            } else {
                c.startActivity(
                    Intent(c, BlogDetailsActivity::class.java)
                        .putExtra("content", mediaModel)
                        .putExtra("flag", true)

                )
            }

        }

        val typename = medias[position].mediaType
        if (typename == 8) {
            holder.imgCollections.visibility = View.VISIBLE
            holder.imgPlayer.visibility = View.INVISIBLE
        } else {
            holder.imgCollections.visibility = View.INVISIBLE

            if (typename == 1) {
                holder.imgPlayer.visibility = View.INVISIBLE
            } else {
                holder.imgPlayer.visibility = View.VISIBLE
            }

        }
    }

    override fun getItemCount(): Int {
        return medias.size
    }

}