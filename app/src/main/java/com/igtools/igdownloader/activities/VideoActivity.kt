package com.igtools.igdownloader.activities

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
import com.igtools.igdownloader.R
import com.igtools.igdownloader.databinding.ActivityVideoBinding


class VideoActivity : AppCompatActivity() {

    lateinit var binding: ActivityVideoBinding
    var url: String? = null
    var thumbnailUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //沉浸式状态栏
        if (Build.VERSION.SDK_INT >= 23) {
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            window.statusBarColor = Color.TRANSPARENT
        }

        binding = DataBindingUtil.setContentView(this, R.layout.activity_video)

        initViews()

    }

    private fun initViews() {

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

        binding.imgBack.setOnClickListener {
            finish()
        }
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