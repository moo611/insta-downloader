package com.igtools.videodownloader.service.details

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.bumptech.glide.Glide
import com.fagaia.farm.base.BaseActivity
import com.igtools.videodownloader.R
import com.igtools.videodownloader.databinding.ActivityVideoBinding


class VideoActivity : BaseActivity<ActivityVideoBinding>() {

    lateinit var binding: ActivityVideoBinding
    var url: String? = null
    var thumbnailUrl: String? = null


    override fun getLayoutId(): Int {
        return R.layout.activity_video
    }

    override fun initView() {

        binding.imgBack.setOnClickListener {
            finish()
        }
    }

    override fun initData() {
        url = intent.extras?.getString("url")
        thumbnailUrl = intent.extras?.getString("thumbnailUrl")
        binding.player.setUp(url, true, null)
        binding.player.backButton.visibility = View.GONE
        binding.player.startPlayLogic()
        //增加封面
        val imageView = ImageView(this)
        imageView.scaleType = ImageView.ScaleType.CENTER_CROP
        Glide.with(this).load(thumbnailUrl).placeholder(
            ColorDrawable(
                ContextCompat.getColor(
                    this, R.color.gray_1
                )
            )
        ).thumbnail(/*sizeMultiplier=*/ 0.25f).into(imageView)
        binding.player.thumbImageView = imageView
    }

    override fun onResume() {
        super.onResume()

        binding.player.onVideoResume()
    }


    override fun onPause() {
        super.onPause()

        binding.player.onVideoPause()

    }



}