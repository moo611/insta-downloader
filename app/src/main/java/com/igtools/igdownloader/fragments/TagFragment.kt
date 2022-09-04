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
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.igtools.igdownloader.R
import com.igtools.igdownloader.adapter.BlogAdapter
import com.igtools.igdownloader.adapter.BlogAdapter2
import com.igtools.igdownloader.api.retrofit.ApiClient
import com.igtools.igdownloader.databinding.FragmentTagBinding
import com.igtools.igdownloader.models.MediaModel
import com.igtools.igdownloader.utils.KeyboardUtils
import com.igtools.igdownloader.utils.getNullable
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
    var loadingMore = false
    //var medias: ArrayList<MediaModel> = ArrayList()
    var gson = Gson()
    var mInterstitialAd: InterstitialAd? = null

    //tag 分页信息
    var next_media_ids: ArrayList<String> = ArrayList()
    var next_max_id = ""
    var more_available = true
    var next_page = 1
    val TAG = "TagFragment"
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
        adapter.fromTag=true
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
            if (binding.etTag.text.toString().isEmpty()){
                Toast.makeText(requireContext(),getString(R.string.empty_tag),Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            refresh(binding.etTag.text.toString())
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

    private fun refresh(tag: String) {

        if (loadingMore){
            return
        }
        progressDialog.show()
        next_max_id = ""
        next_media_ids.clear()
        next_page = 1
        more_available = true
        lifecycleScope.launch {

            try {
                val res = ApiClient.getClient3().getTags(tag)

                val code = res.code()
                val jsonObject = res.body()
                if (code==200 && jsonObject != null) {
                    val medias:ArrayList<MediaModel> = ArrayList()
                    parseData(jsonObject,medias);
                    if (medias.size > 0) {
                        adapter.refresh(medias)

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
                Log.v(TAG, e.message + "")

                progressDialog.dismiss()
                Log.v(TAG, e.message + "")
                Toast.makeText(context, getString(R.string.not_found), Toast.LENGTH_SHORT).show()

            }


        }
    }


    private fun loadMore() {
        if (loadingMore || !more_available) {
            return
        }
        loadingMore = true
        binding.progressBottom.visibility = View.VISIBLE

        lifecycleScope.launch {

            try {

                val params: HashMap<String, Any> = HashMap();
                params["max_id"] = next_max_id
                params["page"] = next_page
                params["next_media_ids"] = next_media_ids
                val strEntity = gson.toJson(params);

                val body = RequestBody
                    .create(
                        "application/json;charset=UTF-8".toMediaTypeOrNull(), strEntity
                    );

                val res = ApiClient.getClient3().postMoreTags(body = body)
                val code = res.code()
                
                val jsonObject = res.body()
                if (code == 200 && jsonObject != null) {
                    val medias:ArrayList<MediaModel> = ArrayList()
                    parseData(jsonObject,medias);
                    if (medias.size > 0) {
                        adapter.loadMore(medias)

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
                Log.v(TAG,e.message+"")
                loadingMore = false
                binding.progressBottom.visibility = View.INVISIBLE
                Toast.makeText(context, getString(R.string.not_found), Toast.LENGTH_SHORT).show()
            }

        }

    }

    private fun parseData(jsonObject: JsonObject,medias:ArrayList<MediaModel>) {
        next_media_ids.clear()
        val data = jsonObject["data"].asJsonObject
        more_available = data["more_available"].asBoolean
        next_max_id = data["next_max_id"].asString
        next_page = data["next_page"].asInt
        val ids = data["next_media_ids"].asJsonArray
        if (ids.size() > 0) {

            for (id in ids) {
                next_media_ids.add(id.asString)
            }
        }
        Log.v(TAG,next_media_ids.toString())
        val items = data["sections"].asJsonArray

        for (item in items) {
            val mediaModel = MediaModel()
            Log.v(TAG, "current:" + items.indexOf(item))
            mediaModel.pk = item.asJsonObject["pk"].asString
            mediaModel.captionText = item.asJsonObject["caption"].asJsonObject["text"].asString ?: ""
            mediaModel.code = item.asJsonObject["code"].asString
            mediaModel.mediaType = item.asJsonObject["media_type"].asInt

            if (mediaModel.mediaType == 8) {

                val candidates = item.asJsonObject["carousel_media"]
                    .asJsonArray[0]
                    .asJsonObject["image_versions2"]
                    .asJsonObject["candidates"]
                    .asJsonArray
                mediaModel.thumbnailUrl = candidates[candidates.size() - 1]
                    .asJsonObject["url"]
                    .asString

            } else {

                val candidates =
                    item.asJsonObject["image_versions2"].asJsonObject["candidates"].asJsonArray
                mediaModel.thumbnailUrl =
                    candidates[candidates.size() - 1].asJsonObject["url"].asString

            }
            mediaModel.username = item.asJsonObject["user"].asJsonObject["username"].asString
            mediaModel.profilePicUrl = item.asJsonObject["user"].asJsonObject.getNullable("profile_pic_url")?.asString
            medias.add(mediaModel)
        }


    }

}