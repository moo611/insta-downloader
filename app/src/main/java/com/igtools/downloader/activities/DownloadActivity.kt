package com.igtools.downloader.activities

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.igtools.downloader.R
import com.igtools.downloader.adapter.HistoryAdapter
import com.igtools.downloader.databinding.ActivityDownloadBinding
import com.igtools.downloader.models.MediaModel

class DownloadActivity : AppCompatActivity() {

    lateinit var adapter:HistoryAdapter
    lateinit var binding: ActivityDownloadBinding
    var datas:ArrayList<MediaModel> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //沉浸式状态栏
        if (Build.VERSION.SDK_INT >= 23) {
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            window.statusBarColor = Color.TRANSPARENT
        }
        binding = DataBindingUtil.setContentView(this, R.layout.activity_download)

    }

    private fun initViews(){


        adapter = HistoryAdapter(this,datas)
        binding.rv.adapter = adapter
        binding.rv.layoutManager = LinearLayoutManager(this)


    }

    private fun getData(){



    }
}