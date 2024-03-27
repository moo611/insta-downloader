package com.igtools.insta.videodownloader.views.search

import android.app.Activity.RESULT_OK
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.DialogInterface
import android.content.Intent
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

import com.google.gson.JsonObject
import com.igtools.insta.videodownloader.BaseApplication
import com.igtools.insta.videodownloader.R
import com.igtools.insta.videodownloader.api.ApiClient
import com.igtools.insta.videodownloader.api.Urls
import com.igtools.insta.videodownloader.base.BaseFragment
import com.igtools.insta.videodownloader.databinding.FragmentSearchBinding
import com.igtools.insta.videodownloader.models.MediaModel
import com.igtools.insta.videodownloader.views.web.WebActivity
import com.igtools.insta.videodownloader.utils.KeyboardUtils
import com.igtools.insta.videodownloader.utils.getNullable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class SearchFragment : BaseFragment<FragmentSearchBinding>() {
    lateinit var privateDialog: AlertDialog
    val TAG = "SearchFragment"
    val LOGIN_REQ = 1000
    val COUNT = 50

    lateinit var layoutManager: GridLayoutManager
    lateinit var adapter: MediaAdapter
    lateinit var progressDialog: ProgressDialog

    var profileUrl = ""
    var cursor = ""
    var loadingMore = false
    var isEnd = false
    var userId = ""
    var mInterstitialAd: InterstitialAd? = null
    var mode = "public"

    override fun getLayoutId(): Int {
        return R.layout.fragment_search
    }

    override fun initView() {
        val adRequest = AdRequest.Builder().build();
        mBinding.adView.loadAd(adRequest)

        progressDialog = ProgressDialog(requireContext())
        progressDialog.setMessage(getString(R.string.searching))
        progressDialog.setCancelable(false)
        initDialog()
        adapter = MediaAdapter(requireContext())
        layoutManager = GridLayoutManager(context, 3)
        mBinding.rv.adapter = adapter
        mBinding.rv.layoutManager = layoutManager
        layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return 1
            }
        }

        mBinding.btnSearch.setOnClickListener {

            mBinding.etSearch.clearFocus()
            mBinding.flParent.requestFocus()
            KeyboardUtils.closeKeybord(mBinding.etSearch, context)
            if (mBinding.etSearch.text.toString().isEmpty()) {
//                Toast.makeText(
//                    requireContext(),
//                    getString(R.string.empty_username),
//                    Toast.LENGTH_SHORT
//                ).show()
                return@setOnClickListener
            }

            //getDataNoCookie()
            if (isSearchUser()) {
                adapter.isUser = true

                getUserDataWeb()
            }

        }

        mBinding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

                if (mBinding.etSearch.text.isNotEmpty()) {

                    mBinding.imgClear.visibility = View.VISIBLE
                } else {

                    mBinding.imgClear.visibility = View.INVISIBLE
                }

            }

            override fun afterTextChanged(s: Editable?) {
                //TODO("Not yet implemented")
            }

        })

        mBinding.rv.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (isSlideToBottom(mBinding.rv) && dy > 0) {
                    //滑动到底部
                    getUserDataMore()

                }

            }
        })

        mBinding.imgClear.setOnClickListener {
            mBinding.etSearch.setText("")
        }
    }

    private fun isSearchUser(): Boolean {

        if (mBinding.etSearch.text.toString().contains("#")) {
            Log.v(TAG, "is search hash tag")
            return false
        }
        Log.v(TAG, "is search user")
        return true
    }

    private fun initDialog() {

        val builder2 = AlertDialog.Builder(requireContext());
        builder2.setTitle(R.string.login);
        builder2.setMessage(R.string.long_text2);
        builder2.setIcon(R.mipmap.icon);
        //点击对话框以外的区域是否让对话框消失
        builder2.setCancelable(true);
        //设置正面按钮
        builder2.setPositiveButton(R.string.ok, object : DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface?, which: Int) {
                val url = "https://www.instagram.com/accounts/login"
                startActivityForResult(
                    Intent(requireContext(), WebActivity::class.java).putExtra(
                        "url",
                        url
                    ), LOGIN_REQ
                )
                privateDialog.dismiss()
            }

        });
        //设置反面按钮
        builder2.setNegativeButton(R.string.cancel, object : DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface?, which: Int) {
                privateDialog.dismiss()
            }

        });
        privateDialog = builder2.create()
    }

    override fun initData() {

        initAds()
    }


    private fun initAds() {
        val adRequest = AdRequest.Builder().build();
        //inter
        InterstitialAd.load(requireContext(), "ca-app-pub-8609866682652024/7172043276", adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(p0: InterstitialAd) {
                    super.onAdLoaded(p0)
                    mInterstitialAd = p0
                }

                override fun onAdFailedToLoad(p0: LoadAdError) {
                    super.onAdFailedToLoad(p0)
                    mInterstitialAd = null;
                }
            })

    }


    private fun clearUserData() {
        userId = ""
        cursor = ""
        isEnd = false
        profileUrl = ""
        mode = "public"
    }

    private fun clearTagData() {
        isEnd = false
        cursor = ""
        profileUrl = ""
    }

    suspend fun getUserData() = withContext(Dispatchers.Main) {
        mode = "private"
        if (!progressDialog.isShowing) {
            progressDialog.show()
        }
        try {
            val cookie = BaseApplication.cookie
            val map: HashMap<String, String> = HashMap()
            map["Cookie"] = cookie!!
            map["User-Agent"] = Urls.USER_AGENT
            val res = ApiClient.getClient2()
                .getUserMedia(
                    Urls.PRIVATE_API+"/users/web_profile_info",
                    map,
                    mBinding.etSearch.text.toString().trim().lowercase()
                )
            val code = res.code()
            val jsonObject = res.body()
            if (code == 200 && jsonObject != null) {
                mInterstitialAd?.show(requireActivity())
                val user = jsonObject["data"].asJsonObject["user"].asJsonObject
                userId = user["id"].asString
                if (user.has("profile_pic_url") && !user.get("profile_pic_url").isJsonNull) {
                    profileUrl = user["profile_pic_url"].asString
                }

                Log.v(TAG, "user_id:" + userId)
                val edge_owner_to_timeline_media =
                    user["edge_owner_to_timeline_media"].asJsonObject
                val edges = edge_owner_to_timeline_media["edges"].asJsonArray
                if (edges.size() > 0) {
                    val medias: ArrayList<MediaModel> = ArrayList()
                    for (item in edges) {
                        val mediainfo = parseUserData(item.asJsonObject)
                        medias.add(mediainfo)
                    }
                    adapter.refresh(medias)


                }
                val pageInfo = edge_owner_to_timeline_media["page_info"].asJsonObject
                isEnd = !pageInfo["has_next_page"].asBoolean
                cursor = pageInfo["end_cursor"].asString

            } else {
                Log.e(TAG, res.errorBody()?.string() + "")
                safeToast(R.string.failed)
            }

            if (!isInvalidContext()) {
                progressDialog.dismiss()
            }

        } catch (e: Exception) {
            Log.e(TAG, e.message + "")
            if (!isInvalidContext()) {
                progressDialog.dismiss()
            }
            safeToast(R.string.failed)
        }


    }


    private fun getUserDataWeb() {
        clearUserData()
        lifecycleScope.launch {
            try {
                progressDialog.show()
                val username = mBinding.etSearch.text.toString().trim().lowercase()
                val url =
                    "https://www.instagram.com/api/v1/users/web_profile_info/?username=$username"
                val headers: HashMap<String, String> = HashMap()
                headers["user-agent"] =
                    "Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Mobile Safari/537.36"
                headers["x-csrftoken"] = "IQ9sfTjzBvHWYxRUwQqKOLU3eHkOISJM"
                headers["x-ig-app-id"] = "1217981644879628"
                val res1 = ApiClient.getClient2().getUserWeb(url, headers)
                Log.v(TAG, res1.body().toString())

                val code = res1.code()

                if (code == 200 && res1.body() != null) {
                    val jsonObject = res1.body()!!

                    val user = jsonObject["data"].asJsonObject["user"].asJsonObject

                    val isPrivate = user["is_private"].asBoolean
                    if (isPrivate) {

                        if (BaseApplication.cookie == null) {
                            privateDialog.show()
                            progressDialog.dismiss()
                        } else {
                            getUserData()
                        }

                    } else {
                        mInterstitialAd?.show(requireActivity())
                        userId = user["id"].asString
                        if (user.has("profile_pic_url") && !user["profile_pic_url"].isJsonNull) {
                            profileUrl = user["profile_pic_url"].asString
                        }
                        val edge_owner_to_timeline_media =
                            user["edge_owner_to_timeline_media"].asJsonObject
                        val edges = edge_owner_to_timeline_media["edges"].asJsonArray
                        if (edges.size() > 0) {
                            val medias: ArrayList<MediaModel> = ArrayList()
                            for (item in edges) {
                                val mediainfo = parseUserData(item.asJsonObject)
                                medias.add(mediainfo)
                            }
                            adapter.refresh(medias)
                        }
                        val pageInfo = edge_owner_to_timeline_media["page_info"].asJsonObject
                        isEnd = !pageInfo["has_next_page"].asBoolean
                        cursor = pageInfo["end_cursor"].asString
                        progressDialog.dismiss()

                    }

                } else {
                    if (!isInvalidContext()) {
                        progressDialog.dismiss()
                    }
                    safeToast(R.string.failed)
                }

            } catch (e: Exception) {
                if (!isInvalidContext()) {
                    progressDialog.dismiss()
                }
                safeToast(R.string.failed)
            }
        }

    }

    private fun getUserDataMore() {

        if (loadingMore || isEnd) {
            return
        }
//        if (adapter.medias.size>300){
//            return
//        }
        loadingMore = true
        val cookie = BaseApplication.cookie
        val map: HashMap<String, String> = HashMap()
        map["Cookie"] = cookie!!
        map["User-Agent"] = Urls.USER_AGENT
        lifecycleScope.launch {
            try {
                mBinding.progressBottom.visibility = View.VISIBLE
                val variables: HashMap<String, Any> = HashMap()
                variables["id"] = userId
                variables["first"] = COUNT
                variables["after"] = cursor
                //Log.v(TAG,"variables:"+variables)
                val res = ApiClient.getClient2().getUserMediaMore(
                    Urls.PUBLIC_API+"/graphql/query",
                    map,
                    Urls.QUERY_HASH_USER,
                    gson.toJson(variables)
                )
                val code = res.code()
                val jsonObject = res.body()
                if (code == 200 && jsonObject != null) {
                    val user = jsonObject["data"].asJsonObject["user"].asJsonObject

                    val edge_owner_to_timeline_media =
                        user["edge_owner_to_timeline_media"].asJsonObject
                    val edges = edge_owner_to_timeline_media["edges"].asJsonArray
                    if (edges.size() > 0) {
                        val medias: ArrayList<MediaModel> = ArrayList()
                        for (item in edges) {
                            val mediainfo = parseUserData(item.asJsonObject)
                            medias.add(mediainfo)
                        }
                        adapter.loadMore(medias)
                    }
                    val pageInfo = edge_owner_to_timeline_media["page_info"].asJsonObject
                    isEnd = !pageInfo["has_next_page"].asBoolean
                    cursor = pageInfo["end_cursor"].asString
                } else {
                    Log.e(TAG, res.errorBody()?.string() + "")
                    Toast.makeText(requireContext(), getString(R.string.failed), Toast.LENGTH_SHORT)
                        .show()
                }

                loadingMore = false
                mBinding.progressBottom.visibility = View.INVISIBLE
            } catch (e: Exception) {
                Log.e(TAG, e.message + "")
                loadingMore = false
                mBinding.progressBottom.visibility = View.INVISIBLE
                safeToast(R.string.failed)
            }

        }

    }

    private fun parseUserData(jsonObject: JsonObject): MediaModel {
        val mediaModel = MediaModel()
        val node = jsonObject["node"].asJsonObject
        mediaModel.code = node["shortcode"].asString
        val captions = node["edge_media_to_caption"].asJsonObject["edges"].asJsonArray
        if (captions.size() > 0) {
            mediaModel.captionText = captions[0].asJsonObject["node"].asJsonObject["text"].asString
        }
        if (node.has("video_url")) {
            mediaModel.videoUrl = node["video_url"].asString
        }

        val parentType = node["__typename"].asString
        if (parentType == "GraphImage") {
            mediaModel.mediaType = 1
        } else if (parentType == "GraphVideo") {
            mediaModel.mediaType = 2
        } else {
            mediaModel.mediaType = 8
        }

        mediaModel.thumbnailUrl = node["thumbnail_src"].asString
        mediaModel.username = mBinding.etSearch.text.toString().trim().lowercase()
        mediaModel.profilePicUrl = profileUrl
        if (node.has("edge_sidecar_to_children")) {
            val children = node["edge_sidecar_to_children"].asJsonObject["edges"].asJsonArray
            if (children.size() > 0) {
                for (child in children) {
                    val resource = MediaModel()
                    resource.pk = child.asJsonObject["node"].asJsonObject["id"].asString
                    resource.thumbnailUrl =
                        child.asJsonObject["node"].asJsonObject["display_url"].asString
                    resource.videoUrl =
                        child.asJsonObject["node"].asJsonObject.getNullable("video_url")?.asString
                    val typeName = child.asJsonObject["node"].asJsonObject["__typename"].asString
                    if (typeName == "GraphImage") {
                        resource.mediaType = 1
                    } else if (typeName == "GraphVideo") {
                        resource.mediaType = 2
                    } else {
                        resource.mediaType = 8
                    }
                    mediaModel.resources.add(resource)
                }
            }
        }

        return mediaModel

    }


    fun isSlideToBottom(recyclerView: RecyclerView?): Boolean {
        if (recyclerView == null) return false
        return (recyclerView.computeVerticalScrollExtent() + recyclerView.computeVerticalScrollOffset()
                >= recyclerView.computeVerticalScrollRange() - 50)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == LOGIN_REQ && resultCode == RESULT_OK) {

            lifecycleScope.launch {
                getUserData()
            }

        }

    }
}