package com.igtools.videodownloader.modules.repost

import android.app.Activity.RESULT_OK
import android.app.RecoverableSecurityException
import android.app.WallpaperManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.ads.AdRequest
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.ktx.Firebase
import com.igtools.videodownloader.R
import com.igtools.videodownloader.base.BaseFragment
import com.igtools.videodownloader.databinding.FragmentRepostBinding
import com.igtools.videodownloader.models.MediaModel
import com.igtools.videodownloader.models.Record
import com.igtools.videodownloader.modules.details.BlogDetailsActivity
import com.igtools.videodownloader.room.RecordDB
import com.igtools.videodownloader.utils.Analytics
import com.igtools.videodownloader.utils.FileUtils
import com.igtools.videodownloader.utils.PermissionUtils
import com.igtools.videodownloader.widgets.dialog.BottomDialog
import com.igtools.videodownloader.widgets.dialog.MyDialog
import kotlinx.coroutines.launch
import java.io.File
import java.io.InputStream


class RepostFragment : BaseFragment<FragmentRepostBinding>() {

    var deleteResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            //Toast.makeText(context, "Image deleted.", Toast.LENGTH_SHORT).show()
            Log.v(TAG, "deleted")

            Analytics.sendEvent("delete_success","delete_success","1")
        }
    }


    lateinit var adapter: RepostAdapter
    lateinit var bottomDialog: BottomDialog
    lateinit var selectDialog: BottomDialog
    lateinit var deleteDialog: MyDialog
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

        mBinding.imgDelete.setOnClickListener {
            deleteDialog.show()
        }

    }

    fun initDialog() {
        //deleteDialog
        deleteDialog = MyDialog(requireContext(), R.style.MyDialogTheme)
        val deleteView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_delete, null)
        val tvCancel = deleteView.findViewById<TextView>(R.id.tv_cancel)
        val tvDelete = deleteView.findViewById<TextView>(R.id.tv_delete)
        deleteDialog.setUpView(deleteView)
        tvCancel.setOnClickListener {
            deleteDialog.dismiss()
        }
        tvDelete.setOnClickListener {

            for (media in medias) {
                val index = medias.indexOf(media)
                deleteFile(media, records[index])
            }

            lifecycleScope.launch {
                RecordDB.getInstance().recordDao().deleteAll()
                medias.clear()
                records.clear()
                adapter.setDatas(medias)
            }

            deleteDialog.dismiss()
        }


        //bottomDialog
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
                val record = records[lastSelected]
                deleteFile(medias[lastSelected], record)

                //删除记录
                lifecycleScope.launch {
                    RecordDB.getInstance().recordDao().delete(record)
                    records.removeAt(lastSelected)
                    medias.removeAt(lastSelected)
                    adapter.setDatas(medias)
                }
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

    private fun addWallPaper(filePath: String?, status: Int) {

        Analytics.sendEvent("add_wallpaper","add_wallpaper","1")

        if (Build.VERSION.SDK_INT >= 24) {
            if (filePath != null) {

                val ios: InputStream = if (filePath.contains("content://")) {
                    val uri = Uri.parse(filePath)
                    activity?.contentResolver?.openInputStream(uri)!!
                } else {
                    File(filePath).inputStream()
                }

                val myWallpaperManager = WallpaperManager.getInstance(requireContext());
                when (status) {
                    0 -> {
                        myWallpaperManager.setStream(
                            ios,
                            null,
                            true,
                            WallpaperManager.FLAG_SYSTEM
                        );
                        myWallpaperManager.setStream(
                            ios,
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
                            ios,
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
                            ios,
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

    private fun addWallPaperUnder24(filePath: String?) {
        Analytics.sendEvent("add_wallpaper","add_wallpaper","1")
        if (filePath != null) {
            val intent = Intent("android.intent.action.ATTACH_DATA")
            intent.addCategory("android.intent.category.DEFAULT")
            val str = "image/*"

            val uri = if (filePath.contains("content://")) {
                Uri.parse(filePath)
            } else {
                Uri.fromFile(File(filePath))
            }

            intent.setDataAndType(uri, str)
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
        Analytics.sendEvent("repost","repost","1")
        if (filePath != null) {
            val uri: Uri = if (filePath.contains("content://")) {
                Uri.parse(filePath)
            } else {
                val file = File(filePath)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    FileProvider.getUriForFile(
                        requireContext(), "com.igtools.videodownloader.fileprovider",
                        file
                    );
                } else {
                    Uri.fromFile(file);
                }

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
                if (mediaModel != null && mediaModel.resources != null) {
                    medias.add(mediaModel)
                }

            }
            adapter.setDatas(medias)

        }

    }

    fun shareFile() {

        bottomDialog.dismiss()

        val paths = records[lastSelected].paths
        val pathMap = gson.fromJson(paths, HashMap::class.java)
        val media = medias[lastSelected]
        if (media.mediaType == 8) {
            val filePaths = ArrayList<String>()
            for (resource in media.resources) {
                if (resource.mediaType == 1) {
                    val filePath = pathMap[resource.thumbnailUrl] as? String
                    if (filePath != null) {
                        filePaths.add(filePath)
                    }
                } else if (resource.mediaType == 2) {
                    val filePath = pathMap[resource.videoUrl] as? String
                    if (filePath != null) {
                        filePaths.add(filePath)
                    }
                }
            }
            FileUtils.shareAll(requireContext(), filePaths)

        } else {
            if (media.mediaType == 1) {

                val filePath = pathMap[media.thumbnailUrl] as? String
                if (filePath != null) {
                    FileUtils.shareImage(requireContext(), filePath)
                }

            } else if (media.mediaType == 2) {
                val filePath = pathMap[media.videoUrl] as? String
                if (filePath != null) {
                    FileUtils.shareVideo(requireContext(), filePath)
                }
            }
        }

    }

    fun deleteFile(media: MediaModel, record: Record) {

        if (!PermissionUtils.checkPermissionsForReadAndRight(requireActivity())) {
            PermissionUtils.requirePermissionsReadAndWrite(requireActivity(), 1024)
            return
        }

        bottomDialog.dismiss()

        if (media.mediaType == 8) {
            val paths = record.paths
            val pathMap = gson.fromJson(paths, HashMap::class.java)
            for (resource in media.resources) {
                if (resource.mediaType == 1) {
                    val filePath = pathMap[resource.thumbnailUrl] as? String
                    if (filePath != null) {
                        deleteMedia(filePath)
                    }
                } else if (resource.mediaType == 2) {
                    val filePath = pathMap[resource.videoUrl] as? String
                    if (filePath != null) {
                        deleteMedia(filePath)
                    }
                }

            }

        } else {

            if (media.mediaType == 1) {
                val paths = record.paths
                val pathMap = gson.fromJson(paths, HashMap::class.java)
                val filePath = pathMap[media.thumbnailUrl] as? String
                if (filePath != null) {
                    deleteMedia(filePath)
                }

            } else if (media.mediaType == 2) {

                val paths = record.paths
                val pathMap = gson.fromJson(paths, HashMap::class.java)
                val filePath = pathMap[media.videoUrl] as? String
                if (filePath != null) {
                    deleteMedia(filePath)
                }
            }

        }


    }

    private fun deleteMedia(path: String) {
        //兼容
        val uri: Uri = if (path.contains("content://")) {
            Uri.parse(path)
        } else {
            val file = File(path)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(
                    requireContext(), "com.igtools.videodownloader.fileprovider",
                    file
                );
            } else {
                Uri.fromFile(file);
            }

        }
        try {
            activity?.contentResolver?.delete(uri, null, null)

        } catch (e: SecurityException) {
            Log.e(TAG, e.message + "")
            e.message?.let {
                Analytics.sendException("delete_exception",Analytics.ERROR_KEY,it)
            }

            val intentSender = when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                    MediaStore.createDeleteRequest(
                        requireActivity().contentResolver,
                        listOf(uri)
                    ).intentSender
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                    val recoverableSecurityException = e as? RecoverableSecurityException
                    recoverableSecurityException?.userAction?.actionIntent?.intentSender
                }
                else -> null
            }
            intentSender?.let {
                deleteResultLauncher.launch(IntentSenderRequest.Builder(it).build())
            }
        }

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