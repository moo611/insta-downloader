package com.igtools.downloader.fragments

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
import com.google.gson.JsonObject
import com.igtools.downloader.R
import com.igtools.downloader.adapter.BlogAdapter
import com.igtools.downloader.api.okhttp.OkhttpHelper
import com.igtools.downloader.api.okhttp.OkhttpListener
import com.igtools.downloader.api.okhttp.Urls
import com.igtools.downloader.api.retrofit.ApiClient
import com.igtools.downloader.databinding.FragmentTagBinding
import com.igtools.downloader.models.BlogModel
import com.igtools.downloader.utils.KeyboardUtils
import kotlinx.coroutines.launch
import java.lang.Exception

/**
 * @Author: desong
 * @Date: 2022/7/21
 */
class TagFragment : Fragment() {
    lateinit var layoutManager: GridLayoutManager
    lateinit var adapter: BlogAdapter
    lateinit var binding: FragmentTagBinding
    var isFetching = false
    var isEnd = false
    var blogs: ArrayList<BlogModel> = ArrayList()

    //tag 分页信息
    var next_media_ids = ""
    var next_max_id = ""
    var more_available = true
    var next_page = 1
    val TAG="TagFragment"
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

        binding.tvSearch.isEnabled = false
        binding.tvSearch.setTextColor(requireContext().resources!!.getColor(R.color.black))


        adapter = BlogAdapter(requireContext(), blogs)
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
                if (!binding.rv.canScrollVertically(1)) {
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
        binding.progressBar.visibility = View.VISIBLE
        blogs.clear()
        lifecycleScope.launch {

            try {
                val res = ApiClient.getClient().getTags(tag)
                val jsonObject = res.body()
                if (jsonObject != null) {
                    parseData(jsonObject);
                    if (blogs.size > 0) {
                        adapter.setDatas(blogs)

                    }
                }
                binding.progressBar.visibility = View.INVISIBLE
            } catch (e: Exception) {
                Log.v(TAG,e.message+"")
                binding.progressBar.visibility = View.INVISIBLE
                Toast.makeText(context, "media not found", Toast.LENGTH_SHORT).show()

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
                val res = ApiClient.getClient().getMoreTags(
                    max_id = next_max_id,
                    page = next_page,
                    next_media_ids = next_media_ids
                )
                val jsonObject = res.body()
                if (jsonObject != null) {
                    parseData(jsonObject);
                    if (blogs.size > 0) {
                        adapter.setDatas(blogs)

                    }
                    isFetching = false

                }
                binding.progressBottom.visibility = View.INVISIBLE
            } catch (e: Exception) {

                isFetching = false
                binding.progressBottom.visibility = View.INVISIBLE
                Toast.makeText(context, "media not found", Toast.LENGTH_SHORT).show()
            }

        }

    }

    private fun parseData(jsonObject: JsonObject) {
        val data = jsonObject["data"].asJsonObject

        more_available = data["more_available"].asBoolean
        next_max_id = data["next_max_id"].asString
        next_page = data["next_page"].asInt
        val ids = data["next_media_ids"].asJsonArray
        if (ids.size() > 0) {
            next_media_ids = ids[0].asString
        }

        val items = data["sections"].asJsonArray

        for (item in items) {

            val blogModel = BlogModel()
            blogModel.caption = item.asJsonObject["caption"].asJsonObject["text"].asString ?: ""
            //blogModel.displayUrl = item.asJsonObject["display_url"].asString
            blogModel.shortCode = item.asJsonObject["code"].asString
            //blogModel.typeName = item.asJsonObject["__typename"].asString
            val type = item.asJsonObject["media_type"].asInt
            if (type == 8) {
                blogModel.typeName = "GraphSidecar"
                blogModel.displayUrl = item.asJsonObject["carousel_media"]
                    .asJsonArray[0]
                    .asJsonObject["image_versions2"]
                    .asJsonObject["candidates"]
                    .asJsonArray[0]
                    .asJsonObject["url"]
                    .asString
            } else {
                blogModel.typeName = "others"
                blogModel.displayUrl =
                    item.asJsonObject["image_versions2"].asJsonObject["candidates"].asJsonArray[0].asJsonObject["url"].asString
            }
            blogs.add(blogModel)
        }


    }

}