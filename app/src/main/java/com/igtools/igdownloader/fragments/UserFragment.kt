package com.igtools.igdownloader.fragments

import android.app.ProgressDialog
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
import com.google.gson.JsonObject
import com.igtools.igdownloader.R
import com.igtools.igdownloader.adapter.BlogAdapter2
import com.igtools.igdownloader.api.retrofit.ApiClient
import com.igtools.igdownloader.databinding.FragmentUserBinding
import com.igtools.igdownloader.models.MediaModel
import com.igtools.igdownloader.models.ResourceModel
import com.igtools.igdownloader.utils.KeyboardUtils
import com.igtools.igdownloader.utils.getNullable
import kotlinx.coroutines.launch


class UserFragment : Fragment() {

    val TAG = "UserFragment"

    lateinit var layoutManager: GridLayoutManager
    lateinit var adapter: BlogAdapter2
    lateinit var progressDialog: ProgressDialog
    lateinit var binding: FragmentUserBinding

    var cursor = ""
    var loadingMore = false
    var isEnd = false
    var userId = ""
    var mInterstitialAd: InterstitialAd? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_user, container, false)

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
        layoutManager = GridLayoutManager(context, 3)
        binding.rv.adapter = adapter
        binding.rv.layoutManager = layoutManager
        layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return 1
            }
        }
    }


    private fun initAds() {
        val adRequest = AdRequest.Builder().build();
        //inter
        InterstitialAd.load(requireContext(), "ca-app-pub-8609866682652024/2974806950", adRequest,
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
            binding.etUsername.clearFocus()
            KeyboardUtils.closeKeybord(binding.etUsername, context)
            if (binding.etUsername.text.toString().isEmpty()){
                Toast.makeText(requireContext(),getString(R.string.empty_username),Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            refresh(binding.etUsername.text.toString())
        }

        binding.etUsername.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

                if (binding.etUsername.text.isNotEmpty()) {

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

                    loadMore(binding.etUsername.text.toString(), cursor)

                }

            }
        })

        binding.imgClear.setOnClickListener {
            binding.etUsername.setText("")
        }

    }

    private fun refresh(user: String) {

        if (loadingMore) {
            return
        }
        userId = ""
        isEnd = false
        progressDialog.show()
        lifecycleScope.launch {
            try {
                val res = ApiClient.getClient2().getUserMedias(user, "", userId)
                val code = res.code()
                val jsonObject = res.body()
                if (code ==200 && jsonObject != null) {

                    mInterstitialAd?.show(requireActivity())

                    val medias: ArrayList<MediaModel> = ArrayList()

                    val items = jsonObject["data"].asJsonArray
                    userId = jsonObject["user_id"].asString
                    for (item in items) {
                        parseData(medias, item as JsonObject)
                    }

                    if (medias.size > 0) {
                        adapter.refresh(medias)

                    }

                    cursor = jsonObject["end_cursor"].asString
                    if (cursor == "") {
                        isEnd = true
                    }
                }else{

                    if (code == 429) {
                        Toast.makeText(context, getString(R.string.too_many), Toast.LENGTH_SHORT)
                            .show()
                    } else {
                        Toast.makeText(context, getString(R.string.not_found), Toast.LENGTH_SHORT)
                            .show()
                    }

                }
                progressDialog.dismiss()

            } catch (e: Exception) {
                Log.e(TAG, e.message + "")

                progressDialog.dismiss()
                Toast.makeText(context, getString(R.string.not_found), Toast.LENGTH_SHORT).show()
            }
        }


    }


    private fun loadMore(user: String, end_cursor: String) {

        if (loadingMore || isEnd) {
            return
        }
        loadingMore = true
        binding.progressBottom.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val res = ApiClient.getClient2().getUserMediasMore(user, end_cursor, userId)
                val code = res.code()
                val jsonObject = res.body()
                if (code == 200 && jsonObject != null) {
                    val medias: ArrayList<MediaModel> = ArrayList()
                    val items = jsonObject["data"].asJsonArray
                    for (item in items) {
                        parseData(medias, item as JsonObject)
                    }

                    if (medias.size > 0) {
                        adapter.loadMore(medias)
                    }

                    cursor = jsonObject["end_cursor"].asString
                    if (cursor == "") {
                        isEnd = true
                    }
                }else{
                    if (code == 429) {
                        Toast.makeText(context, getString(R.string.too_many), Toast.LENGTH_SHORT)
                            .show()
                    } else {
                        Toast.makeText(context, getString(R.string.not_found), Toast.LENGTH_SHORT)
                            .show()
                    }
                }
                loadingMore = false
                binding.progressBottom.visibility = View.INVISIBLE
            } catch (e: Exception) {
                Log.e(TAG, e.message + "")
                loadingMore = false
                binding.progressBottom.visibility = View.INVISIBLE
                Toast.makeText(context, getString(R.string.not_found), Toast.LENGTH_SHORT).show()
            }

        }


    }

    private fun parseData(medias: ArrayList<MediaModel>, jsonObject: JsonObject) {
        val mediaInfo = MediaModel()
        val mediaType = jsonObject["media_type"].asInt
        if (mediaType == 8) {

            mediaInfo.pk = jsonObject["pk"].asString
            mediaInfo.code = jsonObject["code"].asString
            mediaInfo.mediaType = jsonObject["media_type"].asInt
            mediaInfo.videoUrl = jsonObject.getNullable("video_url")?.asString
            mediaInfo.captionText = jsonObject["caption_text"].asString
            mediaInfo.username = jsonObject["user"].asJsonObject["username"].asString
            mediaInfo.profilePicUrl =
                jsonObject["user"].asJsonObject.getNullable("profile_pic_url")?.asString

            val resources = jsonObject["resources"].asJsonArray
            mediaInfo.thumbnailUrl = resources[0].asJsonObject["thumbnail_url"].asString
            for (res in resources) {

                val resourceInfo = ResourceModel()
                resourceInfo.pk = res.asJsonObject["pk"].asString
                resourceInfo.mediaType = res.asJsonObject["media_type"].asInt
                resourceInfo.thumbnailUrl = res.asJsonObject["thumbnail_url"].asString
                resourceInfo.videoUrl = res.asJsonObject.getNullable("video_url")?.asString
                mediaInfo.resources.add(resourceInfo)
            }
            medias.add(mediaInfo)

        } else if (mediaType == 0 || mediaType == 1 || mediaType == 2) {

            mediaInfo.pk = jsonObject["pk"].asString
            mediaInfo.code = jsonObject["code"].asString
            mediaInfo.mediaType = jsonObject["media_type"].asInt
            mediaInfo.thumbnailUrl = jsonObject["thumbnail_url"].asString
            mediaInfo.videoUrl = jsonObject.getNullable("video_url")?.asString
            mediaInfo.captionText = jsonObject.getNullable("caption_text")?.asString
            mediaInfo.username = jsonObject["user"].asJsonObject["username"].asString
            mediaInfo.profilePicUrl =
                jsonObject["user"].asJsonObject.getNullable("profile_pic_url")?.asString
            medias.add(mediaInfo)
        }

    }

}