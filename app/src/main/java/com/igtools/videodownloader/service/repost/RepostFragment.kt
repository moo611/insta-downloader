package com.igtools.videodownloader.service.repost

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.ads.AdRequest
import com.igtools.videodownloader.R
import com.igtools.videodownloader.base.BaseFragment
import com.igtools.videodownloader.databinding.FragmentRepostBinding
import com.igtools.videodownloader.models.MediaModel
import com.igtools.videodownloader.models.Record
import com.igtools.videodownloader.room.RecordDB
import com.igtools.videodownloader.service.details.BlogDetailsActivity
import com.igtools.videodownloader.service.history.HistoryAdapter
import com.igtools.videodownloader.utils.FileUtils
import com.igtools.videodownloader.utils.PermissionUtils
import com.igtools.videodownloader.widgets.dialog.BottomDialog
import kotlinx.coroutines.launch
import java.io.File


class RepostFragment : BaseFragment<FragmentRepostBinding>() {
    lateinit var adapter: HistoryAdapter
    lateinit var bottomDialog: BottomDialog
    var records: ArrayList<Record> = ArrayList()
    var medias: ArrayList<MediaModel> = ArrayList()
    var lastSelected = -1
    val TAG = "RepostFragment"
    override fun getLayoutId(): Int {
        return R.layout.fragment_repost
    }

    override fun initView() {
        initDialog()
        val adRequest = AdRequest.Builder().build();
        mBinding.adView.loadAd(adRequest)
        adapter = HistoryAdapter(requireContext())
        mBinding.rv.adapter = adapter
        mBinding.rv.layoutManager = LinearLayoutManager(requireContext())
        adapter.onItemClickListener = object : HistoryAdapter.OnItemClickListener {
            override fun onClick(position: Int) {
                val content = records[position].content

                startActivity(
                    Intent(
                        requireContext(),
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


    }

    fun initDialog() {

        bottomDialog = BottomDialog(requireContext(), R.style.MyDialogTheme)
        val bottomView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_menu, null)

        val llRepost: LinearLayout = bottomView.findViewById(R.id.ll_repost)
        val llShare: LinearLayout = bottomView.findViewById(R.id.ll_share)
        val llDelete: LinearLayout = bottomView.findViewById(R.id.ll_delete)
        bottomDialog.setContent(bottomView)

        llRepost.setOnClickListener {
            if (lastSelected != -1 && records[lastSelected].paths != null) {
                val size = records[lastSelected].paths!!.length
                if (size == 0) {
                    return@setOnClickListener
                }
                val newpaths = records[lastSelected].paths!!.substring(0, size - 1)
                val paths = newpaths.split(",")
                val path = paths[0]
                val file = File(path)
                val uri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    FileProvider.getUriForFile(
                        requireContext(), "com.igtools.videodownloader.fileprovider",
                        file
                    );
                } else {
                    Uri.fromFile(file);
                }

                if (path.endsWith(".jpg")) {
                    shareFileToInstagram(uri, false)
                } else if (path.endsWith(".mp4")) {
                    shareFileToInstagram(uri, true)
                }

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

    }

    override fun onResume() {
        super.onResume()
        getDatas()
    }

    fun getDatas(){

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
            if (it.isNotEmpty()) {
                val newpaths = it.substring(0, it.length - 1)
                val paths = newpaths.split(",")
                if (paths.size > 1) {
                    FileUtils.shareAll(requireContext(), paths)
                } else if (paths.size == 1) {

                    if (paths[0].endsWith(".jpg")) {
                        FileUtils.share(requireContext(), File(paths[0]))
                    } else {
                        FileUtils.shareVideo(requireContext(), File(paths[0]))
                    }
                }
            }

        }

    }

    fun deleteFile() {

        if (!PermissionUtils.checkPermissionsForReadAndRight(requireActivity())) {
            PermissionUtils.requirePermissionsReadAndWrite(requireActivity(), 1024)
            return
        }

        bottomDialog.dismiss()

        records[lastSelected].paths?.let {
            if (it.isNotEmpty()) {
                val newpaths = it.substring(0, it.length - 1)
                val paths = newpaths.split(",")
                if (paths.size > 1) {

                    for (path in paths) {

                        if (path.endsWith(".jpg")) {
                            deleteImage(path)
                        } else if (path.endsWith(".mp4")) {
                            deleteVideo(path)
                        }

                    }

                } else if (paths.size == 1) {

                    if (paths[0].endsWith(".jpg")) {

                        deleteImage(paths[0])
                    } else if (paths[0].endsWith(".mp4")) {
                        deleteVideo(paths[0])
                    }

                }
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

    fun deleteImage(path: String) {

        FileUtils.deleteImageUri(
            requireActivity().contentResolver,
            path,
        )
    }

    fun deleteVideo(path: String) {

        FileUtils.deleteVideoUri(
            requireActivity().contentResolver,
            path,
        )
    }


    private fun shareFileToInstagram(uri: Uri?, isVideo: Boolean) {
        if (uri == null) {
            return
        }
        val feedIntent = Intent(Intent.ACTION_SEND)
        feedIntent.type = if (isVideo) "video/*" else "image/*"
        feedIntent.putExtra(Intent.EXTRA_STREAM, uri)
        feedIntent.setPackage("com.instagram.android")
        val storiesIntent = Intent("com.instagram.share.ADD_TO_STORY")
        storiesIntent.setDataAndType(uri, if (isVideo) "mp4" else "jpg")
        storiesIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        storiesIntent.setPackage("com.instagram.android")
        requireContext().grantUriPermission(
            "com.instagram.android", uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
        val chooserIntent = Intent.createChooser(feedIntent, "share to")
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(storiesIntent))
        startActivity(chooserIntent)
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 1024) {
            for (result in grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    return
                }
            }
            Log.v(TAG, "all permission granted")
        }

    }
}