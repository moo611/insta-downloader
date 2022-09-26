package com.igtools.videodownloader.service.home

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.igtools.videodownloader.R
import com.igtools.videodownloader.models.MediaModel
import com.igtools.videodownloader.service.history.HistoryAdapter
import java.util.ArrayList

class RecentAdapter(var c: Context) : RecyclerView.Adapter<RecentAdapter.RecentHolder>() {


    var medias: ArrayList<MediaModel> = ArrayList()
    var onItemClickListener: HistoryAdapter.OnItemClickListener? = null

    class RecentHolder (v: View):RecyclerView.ViewHolder(v){
        val avatar:ImageView = v.findViewById(R.id.img_avatar)
        val username:TextView = v.findViewById(R.id.tv_name)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecentHolder {
        val v = LayoutInflater.from(c).inflate(R.layout.item_recent, parent, false)
        return RecentHolder(v)
    }

    override fun onBindViewHolder(holder: RecentHolder, position: Int) {

        Glide.with(c)
            .load(medias[position].profilePicUrl)
            .circleCrop()
            .placeholder(ColorDrawable(ContextCompat.getColor(c, R.color.gray_1)))
            .into(holder.avatar)
        holder.username.text = medias[position].username
        holder.itemView.setOnClickListener { v: View? -> onItemClickListener!!.onClick(position) }
    }

    override fun getItemCount(): Int {
       return medias.size
    }

    public fun setDatas(medias: ArrayList<MediaModel>) {
        this.medias = medias
        notifyDataSetChanged()
    }
}