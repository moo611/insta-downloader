package com.igtools.igdownloader.activities

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
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.igtools.igdownloader.R
import com.igtools.igdownloader.adapter.HistoryAdapter
import com.igtools.igdownloader.databinding.ActivityHistoryBinding
import com.igtools.igdownloader.models.MediaModel
import com.igtools.igdownloader.models.Record
import com.igtools.igdownloader.room.RecordDB
import kotlinx.coroutines.launch


class HistoryActivity : AppCompatActivity() {

    lateinit var adapter: HistoryAdapter
    lateinit var binding: ActivityHistoryBinding
    var records: ArrayList<Record> = ArrayList()
    var thumbnails: ArrayList<String> = ArrayList()
    var titles: ArrayList<String> = ArrayList()
    var contents: ArrayList<String> = ArrayList()
    var avatars: ArrayList<String> = ArrayList()
    var usernames: ArrayList<String> = ArrayList()
    var gson = Gson()
    val TAG = "DownloadActivity"
    private var mInterstitialAd: InterstitialAd? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //沉浸式状态栏
        if (Build.VERSION.SDK_INT >= 23) {
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            window.statusBarColor = Color.TRANSPARENT
        }
        binding = DataBindingUtil.setContentView(this, R.layout.activity_history)
        //initAds()
        initViews()
        getData()
    }

    private fun initAds() {
        val adRequest = AdRequest.Builder().build();

        InterstitialAd.load(this, "ca-app-pub-3940256099942544/1033173712", adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(p0: InterstitialAd) {
                    super.onAdLoaded(p0)
                    mInterstitialAd = p0

                    mInterstitialAd?.show(this@HistoryActivity)
                }

                override fun onAdFailedToLoad(p0: LoadAdError) {
                    super.onAdFailedToLoad(p0)
                    mInterstitialAd = null;
                }
            })
    }

    private fun initViews() {


        adapter = HistoryAdapter(this)
        binding.rv.adapter = adapter
        binding.rv.layoutManager = LinearLayoutManager(this)
        adapter.onItemClickListener = object : HistoryAdapter.OnItemClickListener {
            override fun onClick(position: Int) {
                val content = contents[position]

                startActivity(
                    Intent(
                        this@HistoryActivity,
                        BlogDetailsActivity::class.java
                    ).putExtra("content", content)
                )
            }

        }

        binding.imgBack.setOnClickListener {
            finish()
        }

    }

    private fun getData() {
        records.clear()
        titles.clear()

        lifecycleScope.launch {

            records = RecordDB.getInstance().recordDao().all() as ArrayList<Record>
            Log.v(TAG, records.size.toString())
            for (record in records) {

                val mediaModels: ArrayList<MediaModel> =
                    gson.fromJson(record.content, genericType<ArrayList<MediaModel>>())

                titles.add(mediaModels[0].title ?: "")
                thumbnails.add(mediaModels[0].thumbnailUrl)
                usernames.add(mediaModels[0].username)
                avatars.add(mediaModels[0].avatar)
                contents.add(record.content)

            }
            adapter.setDatas(thumbnails, titles, usernames, avatars)

        }

    }

    inline fun <reified T> genericType() = object : TypeToken<T>() {}.type
}