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
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.igtools.igdownloader.R
import com.igtools.igdownloader.adapter.BlogAdapter
import com.igtools.igdownloader.api.retrofit.ApiClient
import com.igtools.igdownloader.databinding.FragmentTagBinding
import com.igtools.igdownloader.models.BlogModel
import com.igtools.igdownloader.utils.KeyboardUtils
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
    lateinit var adapter: BlogAdapter
    lateinit var binding: FragmentTagBinding
    lateinit var progressDialog: ProgressDialog
    var isFetching = false
    //var blogs: ArrayList<BlogModel> = ArrayList()
    var gson = Gson()

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

        initViews()
        setListeners()
        return binding.root
    }


    private fun initViews() {

        val adRequest: AdRequest = AdRequest.Builder().build()
        binding.adView.loadAd(adRequest)

        binding.tvSearch.isEnabled = false
        binding.tvSearch.setTextColor(requireContext().resources!!.getColor(R.color.black))
        progressDialog = ProgressDialog(requireContext())
        progressDialog.setMessage(getString(R.string.searching))
        progressDialog.setCancelable(false)
        adapter = BlogAdapter(requireContext())
        layoutManager = GridLayoutManager(context, 3)
        binding.rv.adapter = adapter
        binding.rv.layoutManager = layoutManager
        layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return 1
            }
        }

    }

    private fun setListeners() {

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

        if (isFetching){
            return
        }
        progressDialog.show()
        next_max_id = ""
        next_media_ids.clear()
        next_page = 1
        more_available = true
        lifecycleScope.launch {

            try {
                val res = ApiClient.getClient().getTags(tag)

                val code = res.code()
                if (code != 200) {
                    progressDialog.dismiss()
                    Toast.makeText(context, getString(R.string.not_found), Toast.LENGTH_SHORT)
                        .show()
                    return@launch
                }

                val jsonObject = res.body()
                if (jsonObject != null) {
                    val blogs:ArrayList<BlogModel> = ArrayList()
                    parseData(jsonObject,blogs);
                    if (blogs.size > 0) {
                        adapter.refresh(blogs)

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
        if (isFetching || !more_available) {
            return
        }
        isFetching = true
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

                val res = ApiClient.getClient().postMoreTags(body = body)
                val code = res.code()
                if (code != 200) {
                    progressDialog.dismiss()
                    Toast.makeText(context, getString(R.string.not_found), Toast.LENGTH_SHORT)
                        .show()
                    return@launch
                }

                val jsonObject = res.body()
                if (jsonObject != null) {
                    val blogs:ArrayList<BlogModel> = ArrayList()
                    parseData(jsonObject,blogs);
                    if (blogs.size > 0) {
                        adapter.loadMore(blogs)

                    }
                }
                isFetching = false
                binding.progressBottom.visibility = View.INVISIBLE
            } catch (e: Exception) {
                Log.v(TAG,e.message+"")
                isFetching = false
                binding.progressBottom.visibility = View.INVISIBLE
                Toast.makeText(context, getString(R.string.not_found), Toast.LENGTH_SHORT).show()
            }

        }

    }

    private fun parseData(jsonObject: JsonObject,blogs:ArrayList<BlogModel>) {
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
            Log.v(TAG, "current:" + items.indexOf(item))
            val blogModel = BlogModel()
            blogModel.caption = item.asJsonObject["caption"].asJsonObject["text"].asString ?: ""
            //blogModel.displayUrl = item.asJsonObject["display_url"].asString
            blogModel.shortCode = item.asJsonObject["code"].asString
            //blogModel.typeName = item.asJsonObject["__typename"].asString
            val type = item.asJsonObject["media_type"].asInt
            if (type == 8) {
                blogModel.typeName = "GraphSidecar"
                val candidates = item.asJsonObject["carousel_media"]
                    .asJsonArray[0]
                    .asJsonObject["image_versions2"]
                    .asJsonObject["candidates"]
                    .asJsonArray
                blogModel.thumbnailUrl = candidates[candidates.size() - 1]
                    .asJsonObject["url"]
                    .asString

            } else {
                blogModel.typeName = "others"
                val candidates =
                    item.asJsonObject["image_versions2"].asJsonObject["candidates"].asJsonArray
                blogModel.thumbnailUrl =
                    candidates[candidates.size() - 1].asJsonObject["url"].asString

            }
            blogs.add(blogModel)
        }


    }

}