package com.igtools.igdownloader.adapter

import android.content.Context
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.igtools.igdownloader.R
import com.igtools.igdownloader.activities.VideoActivity
import com.igtools.igdownloader.models.MediaModel
import com.youth.banner.adapter.BannerAdapter
import com.youth.banner.util.BannerUtils

/**
 * 自定义布局,多个不同UI切换
 */
class MultiTypeAdapter(private val context: Context, mDatas: List<MediaModel?>?) :
    BannerAdapter<MediaModel?, RecyclerView.ViewHolder>(mDatas) {
    var TAG = "MultiTypeAdapter"
    override fun onCreateHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        when (viewType) {
            1 -> return ImageHolder(BannerUtils.getView(parent, R.layout.banner_image))
            2 -> return VideoHolder(BannerUtils.getView(parent, R.layout.banner_video))
        }
        return ImageHolder(BannerUtils.getView(parent, R.layout.banner_image))
    }

    override fun onBindView(
        holder: RecyclerView.ViewHolder?,
        data: MediaModel?,
        position: Int,
        size: Int
    ) {

        when (holder?.itemViewType) {
            1 -> {
                val imageHolder = holder as ImageHolder
                Glide.with(context)
                    .load(data?.thumbnailUrl)
                    .thumbnail(/*sizeMultiplier=*/ 0.25f)
                    .placeholder(ColorDrawable(ContextCompat.getColor(context, R.color.gray_1)))
                    .into(imageHolder.imageView)
            }
            2 -> {
                val videoHolder = holder as VideoHolder
                Glide.with(context)
                    .load(data?.thumbnailUrl)
                    .thumbnail(/*sizeMultiplier=*/ 0.25f)
                    .placeholder(ColorDrawable(ContextCompat.getColor(context, R.color.gray_1)))
                    .into(videoHolder.imageView)

                videoHolder.imageView.setOnClickListener {

                    context.startActivity(
                        Intent(context, VideoActivity::class.java)
                            .putExtra("url", data?.videoUrl)
                            .putExtra("thumbnailUrl", data?.thumbnailUrl)
                    )

                }
            }
        }

    }

    override fun getItemViewType(position: Int): Int {
        //直接获取真实的实体
        return getRealData(position)!!.mediaType
    }

    internal inner class ImageHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var imageView: ImageView = itemView.findViewById(R.id.imageView)

    }

    internal inner class VideoHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var imageView: ImageView = itemView.findViewById(R.id.imageView)

    }

}