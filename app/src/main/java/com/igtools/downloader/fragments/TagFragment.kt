package com.igtools.downloader.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.JsonObject
import com.igtools.downloader.R
import com.igtools.downloader.adapter.BlogAdapter
import com.igtools.downloader.api.okhttp.OkhttpHelper
import com.igtools.downloader.api.okhttp.OkhttpListener
import com.igtools.downloader.api.okhttp.Urls
import com.igtools.downloader.databinding.FragmentTagBinding
import com.igtools.downloader.models.BlogModel
import com.igtools.downloader.utils.KeyboardUtils

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
    var cursor = ""
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


        adapter = BlogAdapter(context, blogs)
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

            binding.progressBar.visibility = View.VISIBLE
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

                    loadMore(binding.etTag.text.toString(), cursor)

                }

            }
        })

        binding.imgClear.setOnClickListener {

            binding.etTag.setText("")

        }

    }

    private fun refresh(tag: String) {
        blogs.clear()
        val url = Urls.USER_TAG + "?tag=$tag"
        OkhttpHelper.getInstance().getJson(url, object :
            OkhttpListener {
            override fun onSuccess(jsonObject: JsonObject) {

                parseData(jsonObject);
                if (blogs.size > 0) {
                    adapter.setDatas(blogs)

                }

                binding.progressBar.visibility = View.INVISIBLE

            }

            override fun onFail(message: String?) {
                binding.progressBar.visibility = View.INVISIBLE
                Toast.makeText(context, message + "", Toast.LENGTH_SHORT).show()
            }

        })
    }


    private fun loadMore(tag: String, end_cursor: String) {
        if (isFetching || isEnd) {
            return
        }
        isFetching = true
        binding.progressBottom.visibility = View.VISIBLE
        val url = Urls.USER_TAG + "?user=$tag&&end_cursor=$end_cursor"
        OkhttpHelper.getInstance().getJson(url, object :
            OkhttpListener {
            override fun onSuccess(jsonObject: JsonObject) {

                parseData(jsonObject);
                if (blogs.size > 0) {
                    adapter.setDatas(blogs)

                }
                isFetching = false
                binding.progressBottom.visibility = View.INVISIBLE

            }

            override fun onFail(message: String?) {
                isFetching = false
                binding.progressBottom.visibility = View.INVISIBLE
                Toast.makeText(context, message + "", Toast.LENGTH_SHORT).show()
            }

        })

    }

    private fun parseData(jsonObject: JsonObject) {
        val data = jsonObject["data"].asJsonObject
        val items = data["items"].asJsonArray
        cursor = data["end_cursor"].asString
        for (item in items) {

            val blogModel = BlogModel()
            if (item.asJsonObject["edge_media_to_caption"].asJsonObject["edges"].asJsonArray.size() > 0) {
                blogModel.caption =
                    item.asJsonObject["edge_media_to_caption"].asJsonObject["edges"].asJsonArray[0].asJsonObject["node"].asJsonObject["text"].asString
            }
            blogModel.displayUrl = item.asJsonObject["display_url"].asString
            blogModel.shortCode = item.asJsonObject["shortcode"].asString
            //blogModel.typeName = item.asJsonObject["__typename"].asString
            blogs.add(blogModel)
        }

        if (cursor == "") {
            isEnd = true
        }
    }

}