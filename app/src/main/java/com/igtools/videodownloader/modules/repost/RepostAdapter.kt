package com.igtools.videodownloader.modules.repost

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import com.igtools.videodownloader.modules.repost.RepostAdapter.RepostHolder
import android.view.ViewGroup
import android.view.LayoutInflater
import com.igtools.videodownloader.R
import com.bumptech.glide.Glide
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.widget.ImageView
import androidx.core.content.ContextCompat
import android.widget.TextView
import com.igtools.videodownloader.models.MediaModel
import java.util.ArrayList

/**
 * @Author: desong
 * @Date: 2022/8/4
 */
class RepostAdapter(var c: Context) : RecyclerView.Adapter<RepostHolder>() {

    var medias: List<MediaModel> = ArrayList()
    var onItemClickListener: OnItemClickListener? = null
    var onMenuClickListener: OnMenuClickListener?=null
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RepostHolder {
        val v = LayoutInflater.from(c).inflate(R.layout.item_repost, parent, false)
        return RepostHolder(v)
    }

    override fun onBindViewHolder(holder: RepostHolder, position: Int) {
        val url:String
        if (medias[position].resources.size>0){
            url = medias[position].resources[0].thumbnailUrl
            holder.flag.visibility = View.VISIBLE
            holder.player.visibility = View.GONE
        }else{
            url = medias[position].thumbnailUrl

            holder.flag.visibility = View.GONE
            if (medias[position].mediaType == 1){
                holder.player.visibility = View.GONE
            }else{
                holder.player.visibility = View.VISIBLE
            }
        }
        Glide.with(c).load(url)
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
        holder.menu.setOnClickListener {
            onMenuClickListener?.onClick(position)

        }
    }

    override fun getItemCount(): Int {
        return medias.size
    }

    fun setDatas(medias: ArrayList<MediaModel>) {
        this.medias = medias
        notifyDataSetChanged()
    }

    inner class RepostHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var thumbnail: ImageView = itemView.findViewById(R.id.img_thumbnail)
        var caption: TextView = itemView.findViewById(R.id.tv_caption)
        var avatar: ImageView = itemView.findViewById(R.id.img_avatar)
        var username: TextView = itemView.findViewById(R.id.tv_username)
        var menu:ImageView = itemView.findViewById(R.id.menu)
        var flag:ImageView = itemView.findViewById(R.id.img_flag)
        val player:ImageView = itemView.findViewById(R.id.img_player)
    }


    interface OnItemClickListener {
        fun onClick(position: Int)
    }

    interface OnMenuClickListener{
        fun onClick(position: Int)
    }
}