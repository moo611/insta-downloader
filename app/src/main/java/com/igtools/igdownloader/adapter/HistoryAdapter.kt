package com.igtools.igdownloader.adapter

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import com.igtools.igdownloader.adapter.HistoryAdapter.HistoryHolder
import android.view.ViewGroup
import android.view.LayoutInflater
import com.igtools.igdownloader.R
import com.bumptech.glide.Glide
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.view.View
import android.widget.ImageView
import androidx.core.content.ContextCompat
import android.widget.TextView
import com.igtools.igdownloader.models.MediaModel
import java.io.File
import java.util.ArrayList

/**
 * @Author: desong
 * @Date: 2022/8/4
 */
class HistoryAdapter(var c: Context) : RecyclerView.Adapter<HistoryHolder>() {

    var medias: List<MediaModel> = ArrayList()
    var onItemClickListener: OnItemClickListener? = null
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryHolder {
        val v = LayoutInflater.from(c).inflate(R.layout.item_history, parent, false)
        return HistoryHolder(v)
    }

    override fun onBindViewHolder(holder: HistoryHolder, position: Int) {
        Glide.with(c).load(medias[position].thumbnailUrl)
            .placeholder(ColorDrawable(ContextCompat.getColor(c, R.color.gray_1)))
            .into(holder.thumbnail)
        holder.caption.text = medias[position].captionText
        holder.itemView.setOnClickListener { v: View? -> onItemClickListener!!.onClick(position) }
        Glide.with(c)
            .load(medias[position].profilePicUrl)
            .circleCrop()
            .placeholder(ColorDrawable(ContextCompat.getColor(c, R.color.gray_1)))
            .into(holder.avatar)
        holder.username.text = medias[position].username
    }

    override fun getItemCount(): Int {
        return medias.size
    }

    fun setDatas(medias: ArrayList<MediaModel>) {
        this.medias = medias
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