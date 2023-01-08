package com.igtools.videodownloader.modules.tag

import android.app.ProgressDialog
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
import com.igtools.videodownloader.api.Urls
import com.igtools.videodownloader.api.ApiClient
import com.igtools.videodownloader.base.BaseFragment
import com.igtools.videodownloader.databinding.FragmentTagNewBinding
import com.igtools.videodownloader.models.MediaModel
import com.igtools.videodownloader.modules.search.MediaAdapter
import com.igtools.videodownloader.utils.KeyboardUtils
import com.igtools.videodownloader.utils.getNullable
import kotlinx.coroutines.launch
import java.net.URLEncoder

/**
 * @Author: desong
 * @Date: 2022/7/21
 */
class TagFragmentNew : BaseFragment<FragmentTagNewBinding>() {
    lateinit var layoutManager: GridLayoutManager
    lateinit var adapter: MediaAdapter
    lateinit var firebaseAnalytics: FirebaseAnalytics
    lateinit var progressDialog: ProgressDialog

    var loadingMore = false
    var mInterstitialAd: InterstitialAd? = null

    val TAG = "TagFragmentNew"
    val COUNT = 50
    var cursor = ""
    var profileUrl = ""
    var isEnd = false
    override fun initView() {
        initAds()
        progressDialog = ProgressDialog(requireContext())
        progressDialog.setMessage(getString(R.string.searching))
        progressDialog.setCancelable(false)
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

            mBinding.etTag.clearFocus()
            mBinding.flParent.requestFocus()
            KeyboardUtils.closeKeybord(mBinding.etTag, context)
            if (mBinding.etTag.text.toString().isEmpty()) {
                Toast.makeText(requireContext(), getString(R.string.empty_tag), Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }
            firebaseAnalytics.logEvent("search_by_tag") {
                param("search_by_tag", 1)
            }
            //refreshNoCookie()
            getTagData()
        }

        mBinding.etTag.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

                if (mBinding.etTag.text.isNotEmpty()) {

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
                    getTagDataMoreNoCookie()

                }

            }
        })

        mBinding.imgClear.setOnClickListener {

            mBinding.etTag.setText("")

        }
    }

    override fun initData() {
        firebaseAnalytics = Firebase.analytics
    }

    private fun initAds() {
        val adRequest = AdRequest.Builder().build();
        //inter
        InterstitialAd.load(requireContext(), "ca-app-pub-8609866682652024/3446573494", adRequest,
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
        isEnd = false
        cursor = ""
        profileUrl = ""
    }

    private fun getTagData() {
        clearData()
        lifecycleScope.launch {
            progressDialog.show()
            try {
                var tagname = mBinding.etTag.text.toString().trim().lowercase()
                if (tagname.startsWith("#")) {
                    tagname = tagname.replace("#", "")
                }
                val url =
                    "https://www.instagram.com/api/v1/tags/logged_out_web_info/?tag_name=$tagname"
                val headers: HashMap<String, String> = HashMap()
                headers["user-agent"] =
                    "Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Mobile Safari/537.36"
                headers["x-ig-app-id"] = "1217981644879628"
                val res1 = ApiClient.getClient3().getTagWeb(url, headers)
                val jsonObject = res1.body()
                val code = res1.code()
                if (code == 200 && jsonObject != null) {
                    mInterstitialAd?.show(requireActivity())
                    val tag = jsonObject["data"].asJsonObject["hashtag"].asJsonObject
                    profileUrl = tag["profile_pic_url"].asString
                    val edge_hashtag_to_media =
                        tag["edge_hashtag_to_media"].asJsonObject
                    val edges = edge_hashtag_to_media["edges"].asJsonArray
                    if (edges.size() > 0) {
                        val medias: ArrayList<MediaModel> = ArrayList()
                        for (item in edges) {
                            val mediainfo = parseTagData(item.asJsonObject)
                            medias.add(mediainfo)
                        }
                        adapter.refresh(medias)
                    }
                    val pageInfo = edge_hashtag_to_media["page_info"].asJsonObject
                    isEnd = !pageInfo["has_next_page"].asBoolean
                    cursor = pageInfo["end_cursor"].asString

                } else {

                    safeToast(R.string.failed)
                }

                if (!isInvalidContext()) {
                    progressDialog.dismiss()
                }


            } catch (e: Exception) {

                safeToast(R.string.network)
                if (!isInvalidContext()) {
                    progressDialog.dismiss()
                }
            }

        }


    }


    private fun getTagDataMoreNoCookie() {

        if (loadingMore || isEnd) {
            return
        }
        if (adapter.medias.size > 300) {
            return
        }
        loadingMore = true
        mBinding.progressBottom.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                var tagname = mBinding.etTag.text.toString().trim().lowercase()
                if (tagname.startsWith("#")) {
                    tagname = tagname.replace("#", "")
                }
                val variables: HashMap<String, Any> = HashMap()
                variables["tag_name"] = tagname
                variables["first"] = COUNT
                variables["after"] = cursor
                val str = Urls.GRAPH_QL + "?query_hash=${Urls.QUERY_HASH_TAG}&variables=${
                    gson.toJson(variables)
                }"

                val urlEncoded1 = URLEncoder.encode(str, "utf-8")
                val api1 = "https://api.scrape.do?token=${BaseApplication.APIKEY}&url=$urlEncoded1"
                val res1 = ApiClient.getClient3().getTagNew(api1)
                val jsonObject = res1.body()
                val code = res1.code()
                if (code == 200 && jsonObject != null) {
                    val tag = jsonObject["data"].asJsonObject["hashtag"].asJsonObject

                    val edge_hashtag_to_media =
                        tag["edge_hashtag_to_media"].asJsonObject
                    val edges = edge_hashtag_to_media["edges"].asJsonArray
                    if (edges.size() > 0) {
                        val medias: ArrayList<MediaModel> = ArrayList()
                        for (item in edges) {
                            val mediainfo = parseTagData(item.asJsonObject)
                            medias.add(mediainfo)
                        }
                        adapter.loadMore(medias)
                    }
                    val pageInfo = edge_hashtag_to_media["page_info"].asJsonObject
                    isEnd = !pageInfo["has_next_page"].asBoolean
                    cursor = pageInfo["end_cursor"].asString

                } else {

                    safeToast(R.string.failed)
                }
                loadingMore = false
                mBinding.progressBottom.visibility = View.INVISIBLE
            } catch (e: Exception) {
                safeToast(R.string.network)
                loadingMore = false
                mBinding.progressBottom.visibility = View.INVISIBLE
            }


        }


    }


    private fun parseTagData(jsonObject: JsonObject): MediaModel {
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
        var tagname = mBinding.etTag.text.toString().trim().lowercase()
        if (tagname.startsWith("#")) {
            tagname = tagname.replace("#", "")
        }
        mediaModel.username = "#$tagname"
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


    override fun getLayoutId(): Int {
        return R.layout.fragment_tag_new
    }

}