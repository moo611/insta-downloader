package com.igtools.downloader.fragments

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
import com.google.android.exoplayer2.metadata.id3.ApicFrame
import com.google.gson.JsonObject
import com.igtools.downloader.R
import com.igtools.downloader.adapter.BlogAdapter
import com.igtools.downloader.api.okhttp.OkhttpHelper
import com.igtools.downloader.api.okhttp.OkhttpListener
import com.igtools.downloader.api.okhttp.Urls
import com.igtools.downloader.api.retrofit.ApiClient
import com.igtools.downloader.api.retrofit.ApiService
import com.igtools.downloader.databinding.FragmentUserNameBinding
import com.igtools.downloader.models.BlogModel
import com.igtools.downloader.utils.KeyboardUtils
import kotlinx.coroutines.launch

/**
 * @Author: desong
 * @Date: 2022/7/21
 */
class UserNameFragment : Fragment() {
    lateinit var layoutManager: GridLayoutManager
    lateinit var adapter: BlogAdapter
    lateinit var progressDialog:ProgressDialog
    var TAG = "UserNameFragment"
    lateinit var binding: FragmentUserNameBinding
    var blogs: ArrayList<BlogModel> = ArrayList()
    var cursor = ""
    var isFetching = false
    var isEnd = false
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_user_name, container, false)


        initViews()
        setListeners()
        return binding.root
    }


    private fun initViews() {

        binding.tvSearch.isEnabled = false
        binding.tvSearch.setTextColor(requireContext().resources!!.getColor(R.color.black))

        progressDialog = ProgressDialog(requireContext())
        progressDialog.setMessage(getString(R.string.searching))
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
            binding.etUsername.clearFocus()
            KeyboardUtils.closeKeybord(binding.etUsername, context)
            refresh(binding.etUsername.text.toString())
        }

        binding.etUsername.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

                if (binding.etUsername.text.isNotEmpty()) {
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
                if (!binding.rv.canScrollVertically(1) && dy>0) {
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
        Log.v(TAG,"refresh")
        blogs.clear()
        progressDialog.show()
        lifecycleScope.launch {
            try {
                val res = ApiClient.getClient().getUserInfo(user, "")
                val jsonObject = res.body()
                if (jsonObject != null) {
                    parseData(jsonObject);
                    if (blogs.size > 0) {
                        adapter.setDatas(blogs)

                    }

                }
                progressDialog.dismiss()

            } catch (e: Exception) {
                Log.e(TAG,e.message+"")
                progressDialog.dismiss()
                Toast.makeText(context, "user not found", Toast.LENGTH_SHORT).show()
            }
        }


    }


    private fun loadMore(user: String, end_cursor: String) {
        Log.v(TAG,"loadmore")
        if (isFetching || isEnd) {
            return
        }
        isFetching = true
        binding.progressBottom.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val res = ApiClient.getClient().getUserInfo(user, end_cursor)
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
                Toast.makeText(context, "user not found", Toast.LENGTH_SHORT).show()
            }

        }


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
            //blogModel.displayUrl = item.asJsonObject["display_url"].asString
            blogModel.shortCode = item.asJsonObject["shortcode"].asString
            blogModel.typeName = item.asJsonObject["__typename"].asString
            blogModel.thumbnailUrl = item.asJsonObject["thumbnail_resources"].asJsonArray[0].asJsonObject["src"].asString
            blogs.add(blogModel)
        }

        if (cursor == "") {
            isEnd = true
        }

    }


}