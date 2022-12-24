package com.igtools.videodownloader.modules.home

import android.app.ProgressDialog
import android.content.Intent
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.igtools.videodownloader.base.BaseFragment
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.ktx.Firebase
import com.google.gson.JsonObject
import com.igtools.videodownloader.BaseApplication
import com.igtools.videodownloader.R
import com.igtools.videodownloader.modules.web.WebActivity
import com.igtools.videodownloader.api.okhttp.Urls
import com.igtools.videodownloader.api.retrofit.ApiClient
import com.igtools.videodownloader.databinding.FragmentUserBinding
import com.igtools.videodownloader.models.MediaModel
import com.igtools.videodownloader.utils.KeyboardUtils
import com.igtools.videodownloader.utils.getNullable
import com.igtools.videodownloader.widgets.dialog.MyDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URLEncoder


class UserFragment : BaseFragment<FragmentUserBinding>() {
    lateinit var privateDialog: MyDialog
    val TAG = "UserFragment"
    val LOGIN_REQ = 1000
    val COUNT = 50
    lateinit var firebaseAnalytics: FirebaseAnalytics
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
        return R.layout.fragment_user
    }

    override fun initView() {
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
            mBinding.etUsername.clearFocus()
            KeyboardUtils.closeKeybord(mBinding.etUsername, context)
            if (mBinding.etUsername.text.toString().isEmpty()) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.empty_username),
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            firebaseAnalytics.logEvent("search_by_user") {
                param("search_by_user", 1)
            }

            getDataNoCookie()

        }

        mBinding.etUsername.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

                if (mBinding.etUsername.text.isNotEmpty()) {

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
                if (!mBinding.rv.canScrollVertically(1) && dy > 0) {
                    //滑动到底部
                    if (mode=="public"){
                        getDataMoreNoCookie()
                    }else{
                        getDataMore()
                    }

                }

            }
        })

        mBinding.imgClear.setOnClickListener {
            mBinding.etUsername.setText("")
        }
    }

    private fun initDialog() {

        privateDialog = MyDialog(requireContext(), R.style.MyDialogTheme)
        val privateView =
            LayoutInflater.from(requireContext()).inflate(R.layout.dialog_remind, null)
        val title = privateView.findViewById<TextView>(R.id.title)
        title.text = getString(R.string.long_text2)
        val tvLogin = privateView.findViewById<TextView>(R.id.tv_login)
        val tvCancel = privateView.findViewById<TextView>(R.id.tv_cancel)
        tvLogin.setOnClickListener {

            val url = "https://www.instagram.com/accounts/login"
            startActivityForResult(
                Intent(requireContext(), WebActivity::class.java).putExtra(
                    "url",
                    url
                ), LOGIN_REQ
            )
            privateDialog.dismiss()
        }
        tvCancel.setOnClickListener {
            privateDialog.dismiss()
        }
        privateDialog.setUpView(privateView)
    }

    override fun initData() {
        firebaseAnalytics = Firebase.analytics
        initAds()
    }


    private fun initAds() {
        val adRequest = AdRequest.Builder().build();
        //inter
        InterstitialAd.load(requireContext(), "ca-app-pub-8609866682652024/1157367794", adRequest,
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


    private fun clearData() {
        userId = ""
        cursor = ""
        isEnd = false
        profileUrl = ""
        mode = "public"
    }

    suspend fun getData() = withContext(Dispatchers.Main){
            mode = "private"
            if (!progressDialog.isShowing){
                progressDialog.show()
            }
            try {
                val cookie = BaseApplication.cookie
                val map: HashMap<String, String> = HashMap()
                map["Cookie"] = cookie!!
                map["User-Agent"] = Urls.USER_AGENT
                val res = ApiClient.getClient2()
                    .getUserMedia(Urls.USER_INFO, map, mBinding.etUsername.text.toString())
                val code = res.code()
                val jsonObject = res.body()
                if (code == 200 && jsonObject != null) {
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
                            val mediainfo = parse1(item.asJsonObject)
                            medias.add(mediainfo)
                        }
                        adapter.refresh(medias)


                    }
                    val pageInfo = edge_owner_to_timeline_media["page_info"].asJsonObject
                    isEnd = !pageInfo["has_next_page"].asBoolean
                    cursor = pageInfo["end_cursor"].asString
                    mInterstitialAd?.show(requireActivity())
                } else {
                    Log.e(TAG, res.errorBody()?.string() + "")
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.failed),
                        Toast.LENGTH_SHORT
                    )
                        .show()
                }

                progressDialog.dismiss()

            } catch (e: Exception) {
                Log.e(TAG, e.message + "")
                progressDialog.dismiss()
                Toast.makeText(
                    requireContext(),
                    getString(R.string.network),
                    Toast.LENGTH_SHORT
                ).show()
            }



    }

    fun getDataNoCookie() {
        clearData()
        lifecycleScope.launch {
            try {
                progressDialog.show()
                val username = mBinding.etUsername.text.toString()
                val url1 = "https://www.instagram.com/$username/?__a=1&__d=dis"
                val urlEncoded1 = URLEncoder.encode(url1,"utf-8")
                val api1 = "http://api.scrape.do?token=${BaseApplication.APIKEY}&url=$urlEncoded1"
                val res1 = ApiClient.getClient2().getUserId(api1)

                val code = res1.code()

                if (code == 200 && res1.body()!=null) {
                    val jsonObject = res1.body()!!

                    val user = jsonObject["graphql"].asJsonObject["user"].asJsonObject

                    val isPrivate = user["is_private"].asBoolean
                    if (isPrivate){

                        if (BaseApplication.cookie == null){
                            privateDialog.show()
                            progressDialog.dismiss()
                        }else{
                            getData()
                        }

                    }else{
                        userId = user["id"].asString
                        val edge_owner_to_timeline_media =
                            user["edge_owner_to_timeline_media"].asJsonObject
                        val edges = edge_owner_to_timeline_media["edges"].asJsonArray
                        if (edges.size() > 0) {
                            val medias: ArrayList<MediaModel> = ArrayList()
                            for (item in edges) {
                                val mediainfo = parse1(item.asJsonObject)
                                medias.add(mediainfo)
                            }
                            adapter.refresh(medias)
                        }
                        val pageInfo = edge_owner_to_timeline_media["page_info"].asJsonObject
                        isEnd = !pageInfo["has_next_page"].asBoolean
                        cursor = pageInfo["end_cursor"].asString
                        progressDialog.dismiss()
                        mInterstitialAd?.show(requireActivity())
                    }

                } else {
                    progressDialog.dismiss()
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.failed),
                        Toast.LENGTH_SHORT
                    )
                        .show()
                }


            } catch (e: Exception) {
                Log.e(TAG, e.message + "")
                progressDialog.dismiss()
                Toast.makeText(
                    requireContext(),
                    getString(R.string.network),
                    Toast.LENGTH_SHORT
                ).show()
            }

        }

    }

    private fun getDataMore() {

        if (loadingMore || isEnd) {
            return
        }
        if (adapter.medias.size>300){
            return
        }
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
                    Urls.GRAPH_QL,
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
                            val mediainfo = parse1(item.asJsonObject)
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
                Toast.makeText(requireContext(), getString(R.string.network), Toast.LENGTH_SHORT).show()
            }

        }

    }



    private fun getDataMoreNoCookie() {

        if (loadingMore || isEnd) {
            return
        }
        if (adapter.medias.size>300){
            return
        }
        loadingMore = true

        lifecycleScope.launch {
            try {
                mBinding.progressBottom.visibility = View.VISIBLE
                val variables: HashMap<String, Any> = HashMap()
                variables["id"] = userId
                variables["first"] = COUNT
                variables["after"] = cursor

                val str = Urls.GRAPH_QL + "?query_hash=${Urls.QUERY_HASH_USER}&variables=${
                    gson.toJson(variables)
                }"
                val urlEncoded = URLEncoder.encode(str, "utf-8")
                val api = "http://api.scrape.do?token=${BaseApplication.APIKEY}&url=$urlEncoded"

                //Log.v(TAG,"variables:"+variables)
                val res = ApiClient.getClient2().getUserMediaNoCookie(api)
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
                            val mediainfo = parse1(item.asJsonObject)
                            medias.add(mediainfo)
                        }
                        adapter.loadMore(medias)
                    }
                    val pageInfo = edge_owner_to_timeline_media["page_info"].asJsonObject
                    isEnd = !pageInfo["has_next_page"].asBoolean
                    cursor = pageInfo["end_cursor"].asString
                } else {
                    Log.e(TAG, res.errorBody()?.string() + "")
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.failed),
                        Toast.LENGTH_SHORT
                    )
                        .show()
                }

                loadingMore = false
                mBinding.progressBottom.visibility = View.INVISIBLE
            } catch (e: Exception) {
                Log.e(TAG, e.message + "")
                loadingMore = false
                mBinding.progressBottom.visibility = View.INVISIBLE
                Toast.makeText(requireContext(), getString(R.string.network), Toast.LENGTH_SHORT)
                    .show()
            }

        }

    }

    private fun parse1(jsonObject: JsonObject): MediaModel {
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
        mediaModel.username = mBinding.etUsername.text.toString()
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


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == LOGIN_REQ) {

            if (resultCode == 200) {
                lifecycleScope.launch {
                    getData()
                }
            }

        }

    }
}