package com.igtools.videodownloader.modules.repost

import android.app.WallpaperManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.ads.AdRequest
import com.igtools.videodownloader.R
import com.igtools.videodownloader.base.BaseFragment
import com.igtools.videodownloader.databinding.FragmentRepostBinding
import com.igtools.videodownloader.models.MediaModel
import com.igtools.videodownloader.models.Record
import com.igtools.videodownloader.modules.details.BlogDetailsActivity
import com.igtools.videodownloader.room.RecordDB
import com.igtools.videodownloader.utils.FileUtils
import com.igtools.videodownloader.utils.PermissionUtils
import com.igtools.videodownloader.widgets.dialog.BottomDialog
import kotlinx.coroutines.launch
import java.io.File


class RepostFragment : BaseFragment<FragmentRepostBinding>() {
    lateinit var adapter: RepostAdapter
    lateinit var bottomDialog: BottomDialog
    lateinit var selectDialog: BottomDialog
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
        adapter = RepostAdapter(requireContext())
        mBinding.rv.adapter = adapter
        mBinding.rv.layoutManager = LinearLayoutManager(requireContext())
        adapter.onItemClickListener = object : RepostAdapter.OnItemClickListener {
            override fun onClick(position: Int) {
                val content = records[position].content
                val record = records[position]
                startActivity(
                    Intent(requireContext(), BlogDetailsActivity::class.java)
                        .putExtra("content", content)
                        .putExtra("flag", false)
                        .putExtra("record", gson.toJson(record))
                )
            }

        }

        adapter.onMenuClickListener = object : RepostAdapter.OnMenuClickListener {
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
        val llWall: LinearLayout = bottomView.findViewById(R.id.ll_wall)
        bottomDialog.setContent(bottomView)

        llRepost.setOnClickListener {
            if (lastSelected != -1 && lastSelected < medias.size) {

                val media = medias[lastSelected]

                if (media.mediaType == 8) {

                    val paths = records[lastSelected].paths
                    val pathMap = gson.fromJson(paths, HashMap::class.java)
                    val resource = media.resources[0]
                    if (resource.mediaType == 1) {
                        val filePath = pathMap[resource.thumbnailUrl] as? String
                        repost(filePath, false)

                    } else if (resource.mediaType == 2) {
                        val filePath = pathMap[resource.videoUrl] as? String
                        repost(filePath, true)

                    }

                } else {
                    val paths = records[lastSelected].paths
                    val pathMap = gson.fromJson(paths, HashMap::class.java)
                    if (media.mediaType == 1) {
                        val filePath = pathMap[media.thumbnailUrl] as? String
                        repost(filePath, false)

                    } else if (media.mediaType == 2) {
                        val filePath = pathMap[media.videoUrl] as? String
                        repost(filePath, true)

                    }
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

        llWall.setOnClickListener {
            if (Build.VERSION.SDK_INT >= 24) {
                selectDialog.show()

            } else {

                if (medias[lastSelected].mediaType == 8) {
                    if (medias[lastSelected].resources.size > 0 && medias[lastSelected].resources[0].mediaType == 1) {
                        val media = medias[lastSelected].resources[0]
                        val paths = records[lastSelected].paths
                        val pathMap = gson.fromJson(paths, HashMap::class.java)
                        val filePath = pathMap[media.thumbnailUrl] as? String
                        addWallPaperUnder24(filePath)

                    } else {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.unsupport),
                            Toast.LENGTH_SHORT
                        ).show()

                    }
                } else {
                    if (medias[lastSelected].mediaType == 1) {
                        val paths = records[lastSelected].paths
                        val pathMap = gson.fromJson(paths, HashMap::class.java)
                        val filePath = pathMap[medias[lastSelected].thumbnailUrl] as? String
                        addWallPaperUnder24(filePath)
                    } else {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.unsupport),
                            Toast.LENGTH_SHORT
                        ).show()

                    }
                }


            }

            bottomDialog.dismiss()
        }

        selectDialog = BottomDialog(requireContext(), R.style.MyDialogTheme)
        val selectView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_select, null)

        val llBoth: LinearLayout = selectView.findViewById(R.id.ll_both)
        val llLock: LinearLayout = selectView.findViewById(R.id.ll_lockscreen)
        val llWallPaper: LinearLayout = selectView.findViewById(R.id.ll_wallpaper)

        selectDialog.setContent(selectView)

        llBoth.setOnClickListener {
            if (medias[lastSelected].mediaType == 8) {
                if (medias[lastSelected].resources.size > 0 && medias[lastSelected].resources[0].mediaType == 1) {
                    val media = medias[lastSelected].resources[0]
                    val paths = records[lastSelected].paths
                    val pathMap = gson.fromJson(paths, HashMap::class.java)
                    val filePath = pathMap[media.thumbnailUrl] as? String
                    addWallPaper(filePath, 0)

                } else {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.unsupport),
                        Toast.LENGTH_SHORT
                    ).show()

                }
            } else {
                if (medias[lastSelected].mediaType == 1) {
                    val paths = records[lastSelected].paths
                    val pathMap = gson.fromJson(paths, HashMap::class.java)
                    val filePath = pathMap[medias[lastSelected].thumbnailUrl] as? String
                    addWallPaper(filePath, 0)
                } else {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.unsupport),
                        Toast.LENGTH_SHORT
                    ).show()

                }
            }

            selectDialog.dismiss()
        }

        llWallPaper.setOnClickListener {
            if (medias[lastSelected].mediaType == 8) {
                if (medias[lastSelected].resources.size > 0 && medias[lastSelected].resources[0].mediaType == 1) {
                    val media = medias[lastSelected].resources[0]
                    val paths = records[lastSelected].paths
                    val pathMap = gson.fromJson(paths, HashMap::class.java)
                    val filePath = pathMap[media.thumbnailUrl] as? String
                    addWallPaper(filePath, 1)

                } else {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.unsupport),
                        Toast.LENGTH_SHORT
                    ).show()

                }
            } else {
                if (medias[lastSelected].mediaType == 1) {
                    val paths = records[lastSelected].paths
                    val pathMap = gson.fromJson(paths, HashMap::class.java)
                    val filePath = pathMap[medias[lastSelected].thumbnailUrl] as? String
                    addWallPaper(filePath, 1)
                } else {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.unsupport),
                        Toast.LENGTH_SHORT
                    ).show()

                }
            }
            selectDialog.dismiss()
        }
        llLock.setOnClickListener {

            if (medias[lastSelected].mediaType == 8) {

                if (medias[lastSelected].resources.size > 0 && medias[lastSelected].resources[0].mediaType == 1) {
                    val media = medias[lastSelected].resources[0]
                    val paths = records[lastSelected].paths
                    val pathMap = gson.fromJson(paths, HashMap::class.java)
                    val filePath = pathMap[media.thumbnailUrl] as? String
                    addWallPaper(filePath, 2)

                } else {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.unsupport),
                        Toast.LENGTH_SHORT
                    ).show()

                }

            } else {

                if (medias[lastSelected].mediaType == 1) {
                    val paths = records[lastSelected].paths
                    val pathMap = gson.fromJson(paths, HashMap::class.java)
                    val filePath = pathMap[medias[lastSelected].thumbnailUrl] as? String
                    addWallPaper(filePath, 2)
                } else {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.unsupport),
                        Toast.LENGTH_SHORT
                    ).show()

                }

            }

            selectDialog.dismiss()
        }

    }

    override fun initData() {
        getDatas()
    }


    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)

        if (!isHidden) {
            getDatas()
        }
    }

    fun addWallPaper(filePath: String?, status: Int) {
        if (Build.VERSION.SDK_INT >= 24) {
            if (filePath != null) {
                val file = File(filePath)
                val myWallpaperManager = WallpaperManager.getInstance(requireContext());
                when (status) {
                    0 -> {
                        myWallpaperManager.setStream(
                            file.inputStream(),
                            null,
                            true,
                            WallpaperManager.FLAG_SYSTEM
                        );
                        myWallpaperManager.setStream(
                            file.inputStream(),
                            null,
                            true,
                            WallpaperManager.FLAG_LOCK
                        );
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.add_successfully),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    1 -> {
                        myWallpaperManager.setStream(
                            file.inputStream(),
                            null,
                            true,
                            WallpaperManager.FLAG_SYSTEM
                        );
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.add_successfully),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    else -> {
                        myWallpaperManager.setStream(
                            file.inputStream(),
                            null,
                            true,
                            WallpaperManager.FLAG_LOCK
                        );
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.add_successfully),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

            } else {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.file_not_found),
                    Toast.LENGTH_SHORT
                ).show()

            }
        }

    }

    fun addWallPaperUnder24(filePath: String?) {

        if (filePath != null) {
            val intent = Intent("android.intent.action.ATTACH_DATA")
            intent.addCategory("android.intent.category.DEFAULT")
            val str = "image/*"
            intent.setDataAndType(Uri.fromFile(File(filePath)), str)
            intent.putExtra("mimeType", str)
            startActivity(Intent.createChooser(intent, "Set As:"))
        } else {
            Toast.makeText(
                requireContext(),
                getString(R.string.file_not_found),
                Toast.LENGTH_SHORT
            ).show()
        }

    }

    fun repost(filePath: String?, isVideo: Boolean) {

        if (filePath != null) {
            val file = File(filePath)
            val uri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(
                    requireContext(), "com.igtools.videodownloader.fileprovider",
                    file
                );
            } else {
                Uri.fromFile(file);
            }
            shareFileToInstagram(uri, isVideo)
            bottomDialog.dismiss()
        } else {
            Toast.makeText(
                requireContext(),
                getString(R.string.file_not_found),
                Toast.LENGTH_SHORT
            ).show()
            bottomDialog.dismiss()
        }


    }


    fun getDatas() {

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