package com.igtools.downloader.adapter

import android.content.Context
import com.igtools.downloader.models.BlogModel
import androidx.recyclerview.widget.RecyclerView
import com.igtools.downloader.adapter.BlogAdapter.BlogViewHolder
import android.view.ViewGroup
import android.view.LayoutInflater
import com.igtools.downloader.R
import com.bumptech.glide.Glide
import android.graphics.drawable.ColorDrawable
import androidx.core.content.ContextCompat
import android.content.Intent
import android.view.View
import android.widget.ImageView
import com.igtools.downloader.activities.BlogDetailsActivity
import java.util.ArrayList

class BlogAdapter(var c: Context, blogs: List<BlogModel>) : RecyclerView.Adapter<BlogViewHolder>() {
    var blogs: List<BlogModel> = ArrayList()

    inner class BlogViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var imgThumbnail: ImageView = itemView.findViewById(R.id.img_thumbnail)
        var imgCollections: ImageView = itemView.findViewById(R.id.img_collections)

    }

    init {
        this.blogs = blogs
    }

    fun setDatas(blogs: List<BlogModel>) {
        this.blogs = blogs
        notifyDataSetChanged()
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
                ).putExtra("shortCode", blogs[position].shortCode)
            )
        }
        val typename = blogs[position].typeName
        if (typename == "GraphSidecar") {
            holder.imgCollections.visibility = View.VISIBLE
        } else {
            holder.imgCollections.visibility = View.INVISIBLE
        }
    }

    override fun getItemCount(): Int {
        return blogs.size
    }
}