package com.igtools.igdownloader.adapter

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import com.igtools.igdownloader.adapter.HistoryAdapter.HistoryHolder
import android.view.ViewGroup
import android.view.LayoutInflater
import com.igtools.igdownloader.R
import com.bumptech.glide.Glide
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.widget.ImageView
import androidx.core.content.ContextCompat
import android.widget.TextView
import java.util.ArrayList

/**
 * @Author: desong
 * @Date: 2022/8/4
 */
class HistoryAdapter(var c: Context) : RecyclerView.Adapter<HistoryHolder>() {
    var thumbnails: List<String> = ArrayList()
    var titles: List<String> = ArrayList()
    var usernames: List<String> = ArrayList()
    var avatars: List<String> = ArrayList()
    var onItemClickListener: OnItemClickListener? = null
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryHolder {
        val v = LayoutInflater.from(c).inflate(R.layout.item_history, parent, false)
        return HistoryHolder(v)
    }

    override fun onBindViewHolder(holder: HistoryHolder, position: Int) {
        Glide.with(c).load(thumbnails[position])
            .placeholder(ColorDrawable(ContextCompat.getColor(c, R.color.gray_1)))
            .into(holder.thumbnail)
        holder.caption.text = titles[position]
        holder.itemView.setOnClickListener { v: View? -> onItemClickListener!!.onClick(position) }
        Glide.with(c)
            .load(avatars[position])
            .circleCrop()
            .placeholder(ColorDrawable(ContextCompat.getColor(c, R.color.gray_1)))
            .into(holder.avatar)
        holder.username.text = usernames[position]
    }

    override fun getItemCount(): Int {
        return thumbnails.size
    }

    fun setDatas(
        thumbnails: List<String>,
        titles: List<String>,
        usernames: List<String>,
        avatars: List<String>
    ) {
        this.thumbnails = thumbnails
        this.titles = titles
        this.usernames = usernames
        this.avatars = avatars
        notifyDataSetChanged()
    }

    inner class HistoryHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var thumbnail: ImageView = itemView.findViewById(R.id.img_thumbnail)
        var caption: TextView = itemView.findViewById(R.id.tv_caption)
        var avatar: ImageView = itemView.findViewById(R.id.img_avatar)
        var username: TextView = itemView.findViewById(R.id.tv_username)

    }



    interface OnItemClickListener {
        fun onClick(position: Int)
    }
}