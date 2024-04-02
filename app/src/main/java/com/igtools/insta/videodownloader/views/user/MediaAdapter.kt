package com.igtools.insta.videodownloader.views.user

import android.content.Context
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.media.Image
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.igtools.insta.videodownloader.R
import com.igtools.insta.videodownloader.views.details.DetailsActivity
import com.igtools.insta.videodownloader.models.MediaModel
import com.igtools.insta.videodownloader.models.UserModel


class MediaAdapter(var c: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var medias: ArrayList<MediaModel> = ArrayList()
    var userInfo: UserModel? = null
    val gson = Gson()

    val TYPE_USER = 0
    val TYPE_MEDIA = 1

    inner class MediaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var imgThumbnail: ImageView = itemView.findViewById(R.id.img_thumbnail)
        var imgCollections: ImageView = itemView.findViewById(R.id.img_collections)
        var imgPlayer: ImageView = itemView.findViewById(R.id.img_player)
    }

    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var imgAvatar = itemView.findViewById<ImageView>(R.id.img_avatar)
        var tvUsername = itemView.findViewById<TextView>(R.id.tv_username)
        var tvPost = itemView.findViewById<TextView>(R.id.tv_post)
        var tvFollowers = itemView.findViewById<TextView>(R.id.tv_followers)
        var tvFollowing = itemView.findViewById<TextView>(R.id.tv_following)
    }

    fun refresh(medias: ArrayList<MediaModel>, userInfo: UserModel) {
        this.medias = medias
        this.userInfo = userInfo
        notifyItemRangeChanged(0, medias.size + 1)
    }

    fun loadMore(medias: ArrayList<MediaModel>) {
        val index = this.medias.size + 1
        this.medias.addAll(medias)
        notifyItemRangeInserted(index, medias.size)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_USER) {
            val v = LayoutInflater.from(c).inflate(R.layout.item_profile, parent, false)
            UserViewHolder(v)
        } else {
            val v = LayoutInflater.from(c).inflate(R.layout.item_media, parent, false)
            MediaViewHolder(v)
        }

    }

    override fun getItemViewType(position: Int): Int {
        if (position == 0) {
            return TYPE_USER
        }
        return TYPE_MEDIA
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {


        if (holder is UserViewHolder) {
            userInfo?.let {
                with(holder) {
                    Glide.with(c)
                        .load(it.avatar)
                        .placeholder(ColorDrawable(ContextCompat.getColor(c, R.color.gray_1)))
                        .into(imgAvatar)


                    tvUsername.text = it.username
                    tvPost.text = it.post.toString()
                    tvFollowers.text = it.followers.toString()
                    tvFollowing.text = it.following.toString()
                }

            }

        } else if (holder is MediaViewHolder) {
            val realPos = position - 1
            val mediaModel = medias[realPos]
            with(holder) {
                Glide.with(c)
                    .load(mediaModel.thumbnailUrl)
                    .placeholder(ColorDrawable(ContextCompat.getColor(c, R.color.gray_1)))
                    .into(imgThumbnail)

                itemView.setOnClickListener {
                    val content = gson.toJson(mediaModel)
                    c.startActivity(
                        Intent(c, DetailsActivity::class.java)
                            .putExtra("content", content)
                            .putExtra("need_download", true)
                    )


                }

                val typename = mediaModel.mediaType
                if (typename == 8) {
                    imgCollections.visibility = View.VISIBLE
                    imgPlayer.visibility = View.INVISIBLE
                } else {
                    imgCollections.visibility = View.INVISIBLE

                    if (typename == 1) {
                        imgPlayer.visibility = View.INVISIBLE
                    } else {
                        imgPlayer.visibility = View.VISIBLE
                    }

                }
            }


        }

    }

    override fun getItemCount(): Int {
        return if (userInfo == null) {
            0
        } else {
            medias.size + 1
        }

    }

}