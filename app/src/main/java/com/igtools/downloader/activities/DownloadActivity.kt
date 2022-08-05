package com.igtools.downloader.activities

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.igtools.downloader.R
import com.igtools.downloader.adapter.HistoryAdapter
import com.igtools.downloader.databinding.ActivityDownloadBinding
import com.igtools.downloader.models.MediaModel
import com.igtools.downloader.models.Record
import com.igtools.downloader.room.RecordDB
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class DownloadActivity : AppCompatActivity() {

    lateinit var adapter: HistoryAdapter
    lateinit var binding: ActivityDownloadBinding
    var records: ArrayList<Record> = ArrayList()
    var thumbnails: ArrayList<String> = ArrayList()
    var titles: ArrayList<String> = ArrayList()
    var contents: ArrayList<String> = ArrayList()
    var gson = Gson()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //沉浸式状态栏
        if (Build.VERSION.SDK_INT >= 23) {
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            window.statusBarColor = Color.TRANSPARENT
        }
        binding = DataBindingUtil.setContentView(this, R.layout.activity_download)
        initViews()
        getData()
    }

    private fun initViews() {


        adapter = HistoryAdapter(this)
        binding.rv.adapter = adapter
        binding.rv.layoutManager = LinearLayoutManager(this)
        adapter.setOnItemClickListener {
            val content = contents[it]

            startActivity(
                Intent(
                    this@DownloadActivity,
                    BlogDetailsActivity::class.java
                ).putExtra("content", content)
            )

        }

    }

    private fun getData() {
        records.clear()
        titles.clear()

        lifecycleScope.launch {
            records = withContext(Dispatchers.IO) {
                RecordDB.getInstance().recordDao().all() as ArrayList<Record>
            }
            for (record in records) {

                val mediaModels: ArrayList<MediaModel> =
                    gson.fromJson(record.content, genericType<ArrayList<MediaModel>>())
                titles.add(mediaModels[0].title ?: "")
                thumbnails.add(mediaModels[0].thumbnailUrl)
                contents.add(record.content)
            }
            adapter.setDatas(thumbnails, titles)

        }

    }

    inline fun <reified T> genericType() = object : TypeToken<T>() {}.type
}