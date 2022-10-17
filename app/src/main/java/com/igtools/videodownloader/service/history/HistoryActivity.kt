package com.igtools.videodownloader.service.history

import android.content.Intent
import android.content.IntentSender
import android.util.Log
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.fagaia.farm.base.BaseActivity
import com.google.android.gms.ads.AdRequest
import com.igtools.videodownloader.R
import com.igtools.videodownloader.databinding.ActivityHistoryBinding
import com.igtools.videodownloader.models.MediaModel
import com.igtools.videodownloader.models.Record
import com.igtools.videodownloader.room.RecordDB
import com.igtools.videodownloader.service.details.BlogDetailsActivity
import com.igtools.videodownloader.utils.FileUtils
import com.igtools.videodownloader.widgets.dialog.BottomDialog
import kotlinx.coroutines.launch
import java.io.File


class HistoryActivity : BaseActivity<ActivityHistoryBinding>() {

    lateinit var adapter: HistoryAdapter
    lateinit var bottomDialog: BottomDialog
    var records: ArrayList<Record> = ArrayList()
    var medias: ArrayList<MediaModel> = ArrayList()
    var lastSelected = -1
    val TAG = "DownloadActivity"
    private val resolutionForResult = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { activityResult ->

        // do whatever you want with activity result...
        deleteFile()
    }
    override fun getLayoutId(): Int {
        return R.layout.activity_history
    }

    override fun initView() {
        initDialog()
        val adRequest = AdRequest.Builder().build();
        mBinding.adView.loadAd(adRequest)
        adapter = HistoryAdapter(this)
        mBinding.rv.adapter = adapter
        mBinding.rv.layoutManager = LinearLayoutManager(this)
        adapter.onItemClickListener = object : HistoryAdapter.OnItemClickListener {
            override fun onClick(position: Int) {
                val content = records[position].content

                startActivity(
                    Intent(
                        this@HistoryActivity,
                        BlogDetailsActivity::class.java
                    ).putExtra("content", content).putExtra("flag", false)
                )
            }

        }

        adapter.onMenuClickListener = object : HistoryAdapter.OnMenuClickListener {
            override fun onClick(position: Int) {

                lastSelected = position
                if (!bottomDialog.isShowing) {
                    bottomDialog.show()
                }

            }

        }

        mBinding.imgBack.setOnClickListener {
            finish()
        }
    }

    fun initDialog() {

        bottomDialog = BottomDialog(this, R.style.MyDialogTheme)
        val bottomView = LayoutInflater.from(this).inflate(R.layout.dialog_menu, null)

        val llRepost: LinearLayout = bottomView.findViewById(R.id.ll_repost)
        val llShare: LinearLayout = bottomView.findViewById(R.id.ll_share)
        val llDelete: LinearLayout = bottomView.findViewById(R.id.ll_delete)
        bottomDialog.setContent(bottomView)

        llRepost.setOnClickListener {
            if (lastSelected != -1) {

            }
        }
        llShare.setOnClickListener {
            if (lastSelected != -1) {
                shareFile()
            }
        }
        llDelete.setOnClickListener {
            if (lastSelected != -1) {
                deleteFile()
            }
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

    fun shareFile() {

        bottomDialog.dismiss()

        records[lastSelected].paths?.let {
            val newpaths = it.substring(0, it.length - 1)
            val paths = newpaths.split(",")
            if (paths.size > 1) {
                FileUtils.shareAll(this, paths)
            } else if (paths.size == 1) {

                if (paths[0].endsWith(".jpg")) {
                    FileUtils.share(this, File(paths[0]))
                } else {
                    FileUtils.shareVideo(this, File(paths[0]))
                }
            }

        }

    }

    fun deleteFile() {
        bottomDialog.dismiss()
        records[lastSelected].paths?.let {

            val newpaths = it.substring(0, it.length - 1)
            val paths = newpaths.split(",")
            if (paths.size > 1) {

                for (path in paths){

                    if (path.endsWith(".jpg")){
                        deleteImage(path)
                    }else if (path.endsWith(".mp4")){
                        deleteVideo(path)
                    }

                }

            } else if (paths.size == 1) {

                if (paths[0].endsWith(".jpg")) {

                    deleteImage(paths[0])
                } else if (paths[0].endsWith(".mp4")){
                    deleteVideo(paths[0])
                }

            }
            //删除记录
            val record = records[lastSelected]

            lifecycleScope.launch {
                RecordDB.getInstance().recordDao().delete(record)
                records.removeAt(lastSelected)
                medias.removeAt(lastSelected)
                adapter.setDatas(medias)
            }

        }
    }

    fun deleteImage(path:String){
        FileUtils.deleteImageUri(
            contentResolver,
            path,
            object : FileUtils.FileDeleteListener {
                override fun onSuccess() {
                    //Toast.makeText(this@HistoryActivity,"",Toast.LENGTH_SHORT)

                }

                override fun onFailed(intentSender: IntentSender?) {

                }
            })
    }

    fun deleteVideo(path:String){
        FileUtils.deleteVideoUri(
            contentResolver,
            path,
            object : FileUtils.FileDeleteListener {
                override fun onSuccess() {
                    //Toast.makeText(this@HistoryActivity,"",Toast.LENGTH_SHORT)

                }

                override fun onFailed(intentSender: IntentSender?) {

                }
            })
    }

}