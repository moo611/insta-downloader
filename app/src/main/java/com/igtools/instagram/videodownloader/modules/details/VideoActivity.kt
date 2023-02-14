package com.igtools.instagram.videodownloader.modules.details

import android.graphics.drawable.ColorDrawable
import android.view.View
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.igtools.instagram.videodownloader.base.BaseActivity
import com.igtools.instagram.videodownloader.R
import com.igtools.instagram.videodownloader.databinding.ActivityVideoBinding


class VideoActivity : BaseActivity<ActivityVideoBinding>() {

    
    var url: String? = null
    var thumbnailUrl: String? = null


    override fun getLayoutId(): Int {
        return R.layout.activity_video
    }

    override fun initView() {

        mBinding.imgBack.setOnClickListener {
            finish()
        }
    }

    override fun initData() {
        url = intent.extras?.getString("url")
        thumbnailUrl = intent.extras?.getString("thumbnailUrl")

        //增加封面
        val imageView = ImageView(this)
        imageView.scaleType = ImageView.ScaleType.CENTER_CROP
        Glide.with(this).load(thumbnailUrl).placeholder(
            ColorDrawable(
                ContextCompat.getColor(
                    this, R.color.gray_1
                )
            )
        ).into(imageView)
        mBinding.player.thumbImageView = imageView

        //player
        if(url != null){
            mBinding.player.setUp(url, true, null)
            mBinding.player.startPlayLogic()
        }

        mBinding.player.backButton.visibility = View.GONE

    }

    override fun onResume() {
        super.onResume()

        mBinding.player.onVideoResume()
    }


    override fun onPause() {
        super.onPause()

        mBinding.player.onVideoPause()

    }



}