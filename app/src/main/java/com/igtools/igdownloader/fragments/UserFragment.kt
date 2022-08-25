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
    var isFetching = false
    var isEnd = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_user, container, false)


        initViews()
        setListeners()
        return binding.root
    }

    private fun initViews() {
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

        if (isFetching) {
            return
        }
        isEnd = false
        progressDialog.show()
        lifecycleScope.launch {
            try {
                val res = ApiClient.getClient().getUserInfo(user, "")

                val code = res.code()
                if (code != 200) {
                    progressDialog.dismiss()
                    Toast.makeText(context, getString(R.string.not_found), Toast.LENGTH_SHORT)
                        .show()
                    return@launch
                }

                val jsonObject = res.body()
                if (jsonObject != null) {
                    val medias: ArrayList<MediaModel> = ArrayList()

                    cursor = jsonObject["end_cursor"].asString
                    if (cursor == "") {
                        isEnd = true
                    }

                    val items = jsonObject["items"].asJsonArray
                    for (item in items) {
                        parseData(medias, jsonObject)
                    }

                    if (medias.size > 0) {
                        adapter.refresh(medias)

                    }

                }
                isFetching = false
                progressDialog.dismiss()

            } catch (e: Exception) {
                Log.e(TAG, e.message + "")
                isFetching = false
                progressDialog.dismiss()
                Toast.makeText(context, getString(R.string.not_found), Toast.LENGTH_SHORT).show()
            }
        }


    }


    private fun loadMore(user: String, end_cursor: String) {
        Log.v(TAG, "loadmore")
        if (isFetching || isEnd) {
            return
        }
        isFetching = true
        binding.progressBottom.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val res = ApiClient.getClient().getUserInfo(user, end_cursor)
                val code = res.code()
                if (code != 200) {
                    binding.progressBottom.visibility = View.VISIBLE
                    Toast.makeText(context, getString(R.string.not_found), Toast.LENGTH_SHORT)
                        .show()
                    return@launch
                }

                val jsonObject = res.body()
                if (jsonObject != null) {
                    val medias: ArrayList<MediaModel> = ArrayList()
                    val items = jsonObject["items"].asJsonArray

                    cursor = jsonObject["end_cursor"].asString
                    if (cursor == "") {
                        isEnd = true
                    }

                    for (item in items) {
                        parseData(medias, jsonObject)
                    }

                    if (medias.size > 0) {
                        adapter.loadMore(medias)
                    }
                }
                isFetching = false
                binding.progressBottom.visibility = View.INVISIBLE
            } catch (e: Exception) {
                isFetching = false
                binding.progressBottom.visibility = View.INVISIBLE
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
            mediaInfo.profilePicUrl = jsonObject["user"].asJsonObject["profile_pic_url"].asString

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
            mediaInfo.profilePicUrl = jsonObject["user"].asJsonObject["profile_pic_url"].asString
            medias.add(mediaInfo)
        }

    }

}