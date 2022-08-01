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
import com.google.gson.JsonObject
import com.igtools.downloader.R
import com.igtools.downloader.adapter.BlogAdapter
import com.igtools.downloader.api.OkhttpHelper
import com.igtools.downloader.api.OkhttpListener
import com.igtools.downloader.api.Urls
import com.igtools.downloader.databinding.FragmentUserNameBinding
import com.igtools.downloader.models.BlogModel
import com.igtools.downloader.utils.KeyboardUtils

/**
 * @Author: desong
 * @Date: 2022/7/21
 */
class UserNameFragment : Fragment() {
    lateinit var layoutManager: GridLayoutManager
    lateinit var adapter: BlogAdapter
    var TAG = "UserNameFragment"
    lateinit var binding: FragmentUserNameBinding
    var blogs: ArrayList<BlogModel> = ArrayList()
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
        binding.tvDownload.isEnabled = false
        binding.tvPaste.isEnabled = false
        binding.tvDownload.setTextColor(requireContext().resources!!.getColor(R.color.black))
        binding.tvPaste.setTextColor(requireContext().resources!!.getColor(R.color.black))


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

        binding.tvDownload.setOnClickListener {


        }
        binding.tvPaste.setOnClickListener {
            binding.etUsername.clearFocus()
            KeyboardUtils.closeKeybord(binding.etUsername, context)
            binding.tvDownload.isEnabled = false
            binding.tvDownload.setTextColor(requireContext().resources!!.getColor(R.color.black))
            binding.progressBar.visibility = View.VISIBLE
            getData(binding.etUsername.text.toString())
        }

        binding.etUsername.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

                if (binding.etUsername.text.isNotEmpty()) {
                    binding.tvPaste.setTextColor(requireContext().resources!!.getColor(R.color.white))
                    binding.tvPaste.isEnabled = true
                } else {
                    binding.tvPaste.setTextColor(requireContext().resources!!.getColor(R.color.black))
                    binding.tvPaste.isEnabled = false
                }

            }

            override fun afterTextChanged(s: Editable?) {
                //TODO("Not yet implemented")
            }

        })

    }

    private fun getData(user: String) {
        val url = Urls.USER_NAME + "?user=$user"
        OkhttpHelper.getInstance().getJson(url, object : OkhttpListener {
            override fun onSuccess(jsonObject: JsonObject) {

                parseData(jsonObject);
                if (blogs.size > 0) {
                    adapter.setDatas(blogs)

                    //enable download
                    binding.tvDownload.isEnabled = true
                    binding.tvDownload.setTextColor(requireContext().resources!!.getColor(R.color.white))
                }

                binding.progressBar.visibility = View.INVISIBLE

            }

            override fun onFail(message: String?) {
                binding.progressBar.visibility = View.INVISIBLE
                Toast.makeText(context, message + "", Toast.LENGTH_SHORT).show()
            }

        })

    }


    private fun parseData(jsonObject: JsonObject) {
        blogs.clear()
        val data = jsonObject["data"].asJsonObject
        val items = data["items"].asJsonArray

        for (item in items) {

            val blogModel = BlogModel()
            if (item.asJsonObject["edge_media_to_caption"].asJsonObject["edges"].asJsonArray.size() > 0) {
                blogModel.caption =
                    item.asJsonObject["edge_media_to_caption"].asJsonObject["edges"].asJsonArray[0].asJsonObject["node"].asJsonObject["text"].asString
            }
            blogModel.displayUrl = item.asJsonObject["display_url"].asString
            blogModel.shortCode = item.asJsonObject["shortcode"].asString
            blogModel.typeName = item.asJsonObject["__typename"].asString
            blogs.add(blogModel)
        }

        adapter.setDatas(blogs)
    }


}