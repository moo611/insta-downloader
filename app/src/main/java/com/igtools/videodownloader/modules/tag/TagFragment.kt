package com.igtools.videodownloader.modules.tag

import android.app.ProgressDialog
import android.content.Intent
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
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
import com.google.gson.JsonArray
import com.igtools.videodownloader.BaseApplication
import com.igtools.videodownloader.R
import com.igtools.videodownloader.modules.web.WebActivity
import com.igtools.videodownloader.modules.home.MediaAdapter
import com.igtools.videodownloader.api.okhttp.Urls
import com.igtools.videodownloader.api.retrofit.ApiClient
import com.igtools.videodownloader.databinding.FragmentTagBinding
import com.igtools.videodownloader.models.MediaModel
import com.igtools.videodownloader.utils.KeyboardUtils
import com.igtools.videodownloader.utils.getNullable
import com.igtools.videodownloader.widgets.dialog.BottomDialog
import kotlinx.android.synthetic.main.dialog_bottom.view.*
import kotlinx.coroutines.launch
import java.lang.Exception

/**
 * @Author: desong
 * @Date: 2022/7/21
 */
class TagFragment : BaseFragment<FragmentTagBinding>() {
    lateinit var layoutManager: GridLayoutManager
    lateinit var adapter: MediaAdapter
    lateinit var firebaseAnalytics: FirebaseAnalytics
    lateinit var progressDialog: ProgressDialog
    lateinit var bottomDialog: BottomDialog

    var loadingMore = false
    var mInterstitialAd: InterstitialAd? = null
    //tag 分页信息
    var next_media_ids: ArrayList<String> = ArrayList()
    var next_max_id = ""
    var more_available = true
    var next_page = 1

    val TAG = "TagFragment"
    val LOGIN_REQ = 1000

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

        bottomDialog = BottomDialog(requireContext(), R.style.MyDialogTheme)
        val bottomView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_bottom, null)
        bottomView.btn_login.setOnClickListener {

            val url = "https://www.instagram.com/accounts/login"
            startActivityForResult(
                Intent(requireContext(), WebActivity::class.java).putExtra(
                    "url",
                    url
                ), LOGIN_REQ
            )
            bottomDialog.dismiss()
        }
        bottomDialog.setContent(bottomView)

        mBinding.btnSearch.setOnClickListener {
            mBinding.etTag.clearFocus()
            KeyboardUtils.closeKeybord(mBinding.etTag, context)
            if (mBinding.etTag.text.toString().isEmpty()) {
                Toast.makeText(requireContext(), getString(R.string.empty_tag), Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }
            val cookie = BaseApplication.cookie
            if (cookie == null) {
                bottomDialog.show()
                firebaseAnalytics.logEvent("dialog_show"){
                    param("flag", "1")
                }
            } else {
                refresh(mBinding.etTag.text.toString())
            }

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

                    loadMore()

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
        next_max_id = ""
        next_media_ids.clear()
        next_page = 1
        more_available = true
    }

    private fun refresh(tag: String) {

        if (loadingMore) {
            return
        }
        mInterstitialAd?.show(requireActivity())
        progressDialog.show()
        clearData()
        lifecycleScope.launch {

            try {

                val cookie = BaseApplication.cookie
                Log.v(TAG, cookie + "")
                //Log.v(TAG,userAgent+"")
                val map: HashMap<String, String> = HashMap()
                map["Cookie"] = cookie!!
                map["User-Agent"] = Urls.USER_AGENT

                val res = ApiClient.getClient3().getTagData(Urls.TAG_INFO, map, tag)

                val code = res.code()
                val jsonObject = res.body()
                if (code == 200 && jsonObject != null) {
                    val medias: ArrayList<MediaModel> = ArrayList()

                    next_media_ids.clear()
                    val data = jsonObject["data"].asJsonObject
                    val recent = data["recent"].asJsonObject
                    more_available = recent["more_available"].asBoolean
                    next_max_id = recent["next_max_id"].asString
                    next_page = recent["next_page"].asInt
                    val ids = recent["next_media_ids"].asJsonArray
                    if (ids.size() > 0) {

                        for (id in ids) {
                            next_media_ids.add(id.asString)
                        }
                    }
                    Log.v(TAG, next_media_ids.toString())
                    val sections = recent["sections"].asJsonArray
                    parseData(sections, medias);
                    if (medias.size > 0) {
                        adapter.refresh(medias)

                    }
                } else {
                    Toast.makeText(requireContext(), getString(R.string.not_found), Toast.LENGTH_SHORT)
                        .show()

                }
                progressDialog.dismiss()

            } catch (e: Exception) {
                Log.v(TAG, e.message + "")

                progressDialog.dismiss()
                Toast.makeText(requireContext(), getString(R.string.parse_error), Toast.LENGTH_SHORT).show()

            }


        }
    }


    private fun loadMore() {
        if (loadingMore || !more_available) {
            return
        }
        loadingMore = true
        mBinding.progressBottom.visibility = View.VISIBLE

        lifecycleScope.launch {

            try {

                val cookie = BaseApplication.cookie
                Log.v(TAG, cookie + "")
                //Log.v(TAG,userAgent+"")
                val map: HashMap<String, String> = HashMap()
                map["Cookie"] = cookie!!
                map["User-Agent"] = Urls.USER_AGENT
                map["X-CSRFToken"] = extractToken(cookie)!!
                Log.v(TAG,extractToken(cookie)!!)
                val queries: HashMap<String, Any> = HashMap();
                queries["max_id"] = next_max_id
                queries["page"] = next_page
                queries["next_media_ids"] = next_media_ids
                queries["include_persistent"] = 0
                queries["surface"] = "grid"
                queries["tab"] = "recent"
                val tag = mBinding.etTag.text.toString()
                val url = Urls.PRIVATE_API + "/tags/" + tag + "/sections/"
                val res = ApiClient.getClient3().getMoreTagData(url, map, queries)
                val code = res.code()

                val jsonObject = res.body()
                if (code == 200 && jsonObject != null) {
                    val medias: ArrayList<MediaModel> = ArrayList()
                    next_media_ids.clear()
                    more_available = jsonObject["more_available"].asBoolean
                    next_max_id = jsonObject["next_max_id"].asString
                    next_page = jsonObject["next_page"].asInt
                    val ids = jsonObject["next_media_ids"].asJsonArray
                    if (ids.size() > 0) {

                        for (id in ids) {
                            next_media_ids.add(id.asString)
                        }
                    }
                    Log.v(TAG, next_media_ids.toString())
                    val sections = jsonObject["sections"].asJsonArray
                    parseData(sections, medias);
                    if (medias.size > 0) {
                        adapter.loadMore(medias)

                    }
                } else {
                    Log.e(TAG, res.errorBody()?.string() + "")
                    Toast.makeText(requireContext(), getString(R.string.not_found), Toast.LENGTH_SHORT)
                        .show()
                }

                loadingMore = false
                mBinding.progressBottom.visibility = View.INVISIBLE
            } catch (e: Exception) {
                Log.v(TAG, e.message + "")
                loadingMore = false
                mBinding.progressBottom.visibility = View.INVISIBLE
                Toast.makeText(requireContext(), getString(R.string.parse_error), Toast.LENGTH_SHORT).show()
            }

        }

    }

    private fun extractToken(cookie: String): String? {

        val strings = cookie.split(";")
        for (str  in strings){
            val str2 =  str.trim()
            if (str2.startsWith("csrftoken=")){
                return str2.substring(10,str2.length)
            }
        }
        return null
    }

    private fun parseData(sections: JsonArray, medias: ArrayList<MediaModel>) {
        Log.v(TAG,sections.toString())

        for (section in sections) {
            Log.v(TAG, "--------section:" + sections.indexOf(section))
            val items = section.asJsonObject["layout_content"].asJsonObject["medias"].asJsonArray
            for (item in items) {
                Log.v(TAG, "--------item:" + items.indexOf(item))
                val mediaModel = MediaModel()
                val media = item.asJsonObject["media"]
                mediaModel.pk = media.asJsonObject["pk"].asLong.toString()
                mediaModel.captionText = media.asJsonObject.getNullable("caption")?.asJsonObject?.get("text")?.asString
                mediaModel.code = media.asJsonObject["code"].asString
                mediaModel.mediaType = media.asJsonObject["media_type"].asInt

                if (mediaModel.mediaType == 8) {

                    val candidates = media.asJsonObject["carousel_media"]
                        .asJsonArray[0]
                        .asJsonObject["image_versions2"]
                        .asJsonObject["candidates"]
                        .asJsonArray
                    mediaModel.thumbnailUrl = candidates[candidates.size() - 1]
                        .asJsonObject["url"]
                        .asString

                } else {

                    val candidates =
                        media.asJsonObject["image_versions2"].asJsonObject["candidates"].asJsonArray
                    mediaModel.thumbnailUrl =
                        candidates[candidates.size() - 1].asJsonObject["url"].asString

                }
                mediaModel.username = media.asJsonObject["user"].asJsonObject["username"].asString
                mediaModel.profilePicUrl =
                    media.asJsonObject["user"].asJsonObject.getNullable("profile_pic_url")?.asString
                medias.add(mediaModel)
            }


        }


    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == LOGIN_REQ) {

            if (resultCode == 200) {
                refresh(mBinding.etTag.text.toString())
                firebaseAnalytics.logEvent("user_login"){
                    param("flag", "2")
                }
            }

        }

    }

    override fun getLayoutId(): Int {
        return R.layout.fragment_tag
    }

}