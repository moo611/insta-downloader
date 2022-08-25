package com.igtools.igdownloader.adapter

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
import com.igtools.igdownloader.R
import com.igtools.igdownloader.activities.BlogDetailsActivity
import com.igtools.igdownloader.models.MediaModel

class BlogAdapter2(var c: Context): RecyclerView.Adapter<BlogAdapter2.BlogViewHolder>() {

    var blogs:ArrayList<MediaModel> = ArrayList()

    inner class BlogViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var imgThumbnail: ImageView = itemView.findViewById(R.id.img_thumbnail)
        var imgCollections: ImageView = itemView.findViewById(R.id.img_collections)

    }

    fun refresh(blogs: ArrayList<MediaModel>) {
        this.blogs = blogs
        notifyDataSetChanged()
    }

    fun loadMore(blogs:ArrayList<MediaModel>){
        val index = this.blogs.size
        this.blogs.addAll(blogs)
        notifyItemRangeInserted(index,blogs.size)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BlogViewHolder {
        val v = LayoutInflater.from(c).inflate(R.layout.item_blog, parent, false)
        return BlogViewHolder(v)
    }

    override fun onBindViewHolder(holder: BlogViewHolder, position: Int) {
        Glide.with(c)
            .load(blogs[position].thumbnailUrl)
            .placeholder(ColorDrawable(ContextCompat.getColor(c, R.color.gray_1)))
            .into(holder.imgThumbnail)
        holder.itemView.setOnClickListener {
            c.startActivity(
                Intent(
                    c,
                    BlogDetailsActivity::class.java
                ).putExtra("shortCode", blogs[position].code)
            )
        }
        val typename = blogs[position].mediaType
        if (typename == 8) {
            holder.imgCollections.visibility = View.VISIBLE
        } else {
            holder.imgCollections.visibility = View.INVISIBLE
        }
    }

    override fun getItemCount(): Int {
        return blogs.size
    }

}