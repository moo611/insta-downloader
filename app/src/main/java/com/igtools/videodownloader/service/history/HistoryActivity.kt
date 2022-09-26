package com.igtools.videodownloader.service.history

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.fagaia.farm.base.BaseActivity
import com.google.android.gms.ads.AdRequest
import com.google.gson.Gson
import com.igtools.videodownloader.R
import com.igtools.videodownloader.databinding.ActivityHistoryBinding
import com.igtools.videodownloader.models.MediaModel
import com.igtools.videodownloader.models.Record
import com.igtools.videodownloader.room.RecordDB
import com.igtools.videodownloader.service.details.BlogDetailsActivity
import kotlinx.coroutines.launch


class HistoryActivity : BaseActivity<ActivityHistoryBinding>() {

    lateinit var adapter: HistoryAdapter
    lateinit var binding: ActivityHistoryBinding
    var records: ArrayList<Record> = ArrayList()
    var medias: ArrayList<MediaModel> = ArrayList()


    val TAG = "DownloadActivity"


    override fun getLayoutId(): Int {
        return R.layout.activity_history
    }

    override fun initView() {
        val adRequest = AdRequest.Builder().build();
        binding.adView.loadAd(adRequest)
        adapter = HistoryAdapter(this)
        binding.rv.adapter = adapter
        binding.rv.layoutManager = LinearLayoutManager(this)
        adapter.onItemClickListener = object : HistoryAdapter.OnItemClickListener {
            override fun onClick(position: Int) {
                val content = records[position].content

                startActivity(
                    Intent(
                        this@HistoryActivity,
                        BlogDetailsActivity::class.java
                    ).putExtra("content", content).putExtra("flag",false)
                )
            }

        }

        binding.imgBack.setOnClickListener {
            finish()
        }
    }

    override fun initData() {
        records.clear()
        medias.clear()

        lifecycleScope.launch {

            records = RecordDB.getInstance().recordDao().all() as ArrayList<Record>
            Log.v(TAG, records.size.toString())
            for (record in records) {

                val mediaModel = gson.fromJson(record.content, MediaModel::class.java)
                medias.add(mediaModel)

            }
            adapter.setDatas(medias)

        }
    }

}