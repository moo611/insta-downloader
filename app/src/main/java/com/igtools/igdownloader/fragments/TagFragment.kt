package com.igtools.igdownloader.fragments

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.igtools.igdownloader.R
import com.igtools.igdownloader.activities.WebActivity
import com.igtools.igdownloader.adapter.BlogAdapter
import com.igtools.igdownloader.adapter.BlogAdapter2
import com.igtools.igdownloader.api.okhttp.Urls
import com.igtools.igdownloader.api.retrofit.ApiClient
import com.igtools.igdownloader.databinding.FragmentTagBinding
import com.igtools.igdownloader.models.MediaModel
import com.igtools.igdownloader.utils.KeyboardUtils
import com.igtools.igdownloader.utils.ShareUtils
import com.igtools.igdownloader.utils.getNullable
import com.igtools.igdownloader.widgets.dialog.BottomDialog
import kotlinx.android.synthetic.main.dialog_bottom.view.*
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import java.lang.Exception

/**
 * @Author: desong
 * @Date: 2022/7/21
 */
class TagFragment : Fragment() {
    lateinit var layoutManager: GridLayoutManager
    lateinit var adapter: BlogAdapter2
    lateinit var binding: FragmentTagBinding
    lateinit var progressDialog: ProgressDialog
    lateinit var bottomDialog: BottomDialog

    var loadingMore = false
    var gson = Gson()
    var mInterstitialAd: InterstitialAd? = null

    //tag 分页信息
    var next_media_ids: ArrayList<String> = ArrayList()
    var next_max_id = ""
    var more_available = true
    var next_page = 1

    val TAG = "TagFragment"
    val LOGIN_REQ = 1000
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_tag, container, false)
        initAds()
        initViews()
        setListeners()
        return binding.root
    }


    private fun initViews() {

        progressDialog = ProgressDialog(requireContext())
        progressDialog.setMessage(getString(R.string.searching))
        progressDialog.setCancelable(false)
        adapter = BlogAdapter2(requireContext())
        adapter.fromTag = true
        layoutManager = GridLayoutManager(context, 3)
        binding.rv.adapter = adapter
        binding.rv.layoutManager = layoutManager
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

    }

    private fun initAds() {
        val adRequest = AdRequest.Builder().build();
        //inter
        InterstitialAd.load(requireContext(), "ca-app-pub-8609866682652024/2208520199", adRequest,
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

    private fun setListeners() {

        binding.btnSearch.setOnClickListener {
            binding.etTag.clearFocus()
            KeyboardUtils.closeKeybord(binding.etTag, context)
            if (binding.etTag.text.toString().isEmpty()) {
                Toast.makeText(requireContext(), getString(R.string.empty_tag), Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }
            val cookie = ShareUtils.getData("cookie")
            if (cookie == null) {
                bottomDialog.show()
            } else {
                refresh(binding.etTag.text.toString())
            }

        }

        binding.etTag.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

                if (binding.etTag.text.isNotEmpty()) {

                    binding.imgClear.visibility = View.VISIBLE
                } else {

                    binding.imgClear.visibility = View.INVISIBLE
                }

            }

            override fun afterTextChanged(s: Editable?) {
                //TODO("Not yet implemented")
            }

        })

        binding.rv.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (!binding.rv.canScrollVertically(1) && dy > 0) {
                    //滑动到底部

                    loadMore()

                }

            }
        })

        binding.imgClear.setOnClickListener {

            binding.etTag.setText("")

        }

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
        progressDialog.show()
        clearData()
        lifecycleScope.launch {

//            try {

                val cookie = ShareUtils.getData("cookie")
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
                    Toast.makeText(context, getString(R.string.not_found), Toast.LENGTH_SHORT)
                        .show()

                }
                progressDialog.dismiss()

//            } catch (e: Exception) {
//                Log.v(TAG, e.message + "")
//
//                progressDialog.dismiss()
//                Toast.makeText(context, getString(R.string.not_found), Toast.LENGTH_SHORT).show()
//
//            }


        }
    }


    private fun loadMore() {
        if (loadingMore || !more_available) {
            return
        }
        loadingMore = true
        binding.progressBottom.visibility = View.VISIBLE

        lifecycleScope.launch {

//            try {

                val cookie = ShareUtils.getData("cookie")
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
                val tag = binding.etTag.text.toString()
                val url = Urls.TAG_INFO_MORE + "/tags/" + tag + "/sections/"
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
                    Toast.makeText(context, getString(R.string.not_found), Toast.LENGTH_SHORT)
                        .show()
                }

                loadingMore = false
                binding.progressBottom.visibility = View.INVISIBLE
//            } catch (e: Exception) {
//                Log.v(TAG, e.message + "")
//                loadingMore = false
//                binding.progressBottom.visibility = View.INVISIBLE
//                Toast.makeText(context, getString(R.string.not_found), Toast.LENGTH_SHORT).show()
//            }

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


        for (section in sections) {
            Log.v(TAG, "--------section:" + sections.indexOf(section))
            val items = section.asJsonObject["layout_content"].asJsonObject["medias"].asJsonArray
            for (item in items) {
                Log.v(TAG, "--------item:" + items.indexOf(item))
                val mediaModel = MediaModel()
                val media = item.asJsonObject["media"]
                mediaModel.pk = media.asJsonObject["pk"].asString
                mediaModel.captionText = media.asJsonObject["caption"]?.asJsonObject?.get("text")?.asString
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
                refresh(binding.etTag.text.toString())

            }

        }

    }

}