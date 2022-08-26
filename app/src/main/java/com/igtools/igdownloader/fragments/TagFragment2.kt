package com.igtools.igdownloader.fragments

import android.app.ProgressDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.ads.AdRequest
import com.google.gson.JsonObject
import com.igtools.igdownloader.R
import com.igtools.igdownloader.adapter.BlogAdapter
import com.igtools.igdownloader.adapter.BlogAdapter2
import com.igtools.igdownloader.api.retrofit.ApiClient
import com.igtools.igdownloader.databinding.FragmentTag2Binding
import com.igtools.igdownloader.models.MediaModel
import com.igtools.igdownloader.models.ResourceModel
import com.igtools.igdownloader.utils.KeyboardUtils
import com.igtools.igdownloader.utils.getNullable
import kotlinx.coroutines.launch
import java.lang.Exception


class TagFragment2 : Fragment() {
    lateinit var layoutManager: GridLayoutManager
    lateinit var adapter: BlogAdapter2
    lateinit var binding: FragmentTag2Binding
    lateinit var progressDialog: ProgressDialog
    val TAG = "TagFragment2"
    var loadingMore = false
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_tag2, container, false)

        initViews()
        setListeners()
        return binding.root
        
        
    }
    
    
    fun initViews(){
        val adRequest: AdRequest = AdRequest.Builder().build()
        binding.adView.loadAd(adRequest)

        binding.tvSearch.isEnabled = false
        binding.tvSearch.setTextColor(requireContext().resources!!.getColor(R.color.black))
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
    
    
    private fun setListeners(){
        binding.tvSearch.setOnClickListener {
            binding.etTag.clearFocus()
            KeyboardUtils.closeKeybord(binding.etTag, context)
            refresh(binding.etTag.text.toString())
        }

        binding.etTag.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

                if (binding.etTag.text.isNotEmpty()) {
                    binding.tvSearch.setTextColor(requireContext().resources!!.getColor(R.color.white))
                    binding.tvSearch.isEnabled = true
                    binding.imgClear.visibility = View.VISIBLE
                } else {
                    binding.tvSearch.setTextColor(requireContext().resources!!.getColor(R.color.home_unselect_color))
                    binding.tvSearch.isEnabled = false
                    binding.imgClear.visibility = View.INVISIBLE
                }

            }

            override fun afterTextChanged(s: Editable?) {
                //TODO("Not yet implemented")
            }

        })
        
//        binding.btnMore.setOnClickListener {
//
//            loadMore(binding.etTag.text.toString())
//
//        }

//        binding.rv.addOnScrollListener(object : RecyclerView.OnScrollListener() {
//            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
//                super.onScrolled(recyclerView, dx, dy)
//                if (!binding.rv.canScrollVertically(1) && dy > 0) {
//                    //滑动到底部
//
//                    loadMore()
//
//                }
//
//            }
//        })

        binding.imgClear.setOnClickListener {

            binding.etTag.setText("")

        }
        
    }

    private fun refresh(tag: String) {
        if (loadingMore){
            return
        }
        progressDialog.show()
        
        lifecycleScope.launch {

            try {
                val res = ApiClient.getClient().getTagMedias(tag,"2")
                progressDialog.dismiss()
                val code = res.code()
                if (code != 200) {

                    Toast.makeText(context, getString(R.string.not_found), Toast.LENGTH_SHORT)
                        .show()
                    return@launch
                }

                val jsonObject = res.body()
                if (jsonObject != null) {
                    val medias:ArrayList<MediaModel> = ArrayList()
                    val items = jsonObject["data"].asJsonArray
                    for (item in items){
                        parseData(medias,item as JsonObject);
                    }

                    if (medias.size > 0) {
                        adapter.refresh(medias)

                    }
                }

            } catch (e: Exception) {
                Log.v(TAG, e.message + "")
                progressDialog.dismiss()
                Toast.makeText(context, getString(R.string.not_found), Toast.LENGTH_SHORT).show()

            }


        }
    }
    
    private fun loadMore(tag: String){
        if (loadingMore){
            return
        }
        loadingMore = true
        progressDialog.show()

        lifecycleScope.launch {

            try {
                val res = ApiClient.getClient().getTagMedias(tag,"2")
                
                loadingMore = false
                progressDialog.dismiss()
                val code = res.code()
                if (code != 200) {
                    Toast.makeText(context, getString(R.string.not_found), Toast.LENGTH_SHORT)
                        .show()
                    return@launch
                }

                val jsonObject = res.body()
                if (jsonObject != null) {
                    val medias:ArrayList<MediaModel> = ArrayList()
                    val items = jsonObject["data"].asJsonArray
                    for (item in items){
                        parseData(medias,item as JsonObject);
                    }

                    if (medias.size > 0) {
                        adapter.loadMore(medias)

                    }
                }
                
            } catch (e: Exception) {
                loadingMore = false
                progressDialog.dismiss()
                Log.v(TAG, e.message + "")
                Toast.makeText(context, getString(R.string.not_found), Toast.LENGTH_SHORT).show()

            }


        }
        
    }

    private fun parseData(medias:ArrayList<MediaModel>, jsonObject: JsonObject) {
        val mediaInfo = MediaModel()
        val mediaType = jsonObject["media_type"].asInt
        if (mediaType == 8) {

            mediaInfo.pk = jsonObject["pk"].asString
            mediaInfo.code = jsonObject["code"].asString
            mediaInfo.mediaType = jsonObject["media_type"].asInt
            mediaInfo.videoUrl = jsonObject.getNullable("video_url")?.asString
            mediaInfo.captionText = jsonObject["caption_text"].asString
            mediaInfo.username = jsonObject["user"].asJsonObject["username"].asString
            mediaInfo.profilePicUrl = jsonObject["user"].asJsonObject.getNullable("profile_pic_url")?.asString

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
            mediaInfo.profilePicUrl = jsonObject["user"].asJsonObject.getNullable("profile_pic_url")?.asString
            medias.add(mediaInfo)
        }

    }
    
}