package com.igtools.videodownloader.fragments

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
import com.google.gson.JsonObject
import com.igtools.videodownloader.R
import com.igtools.videodownloader.activities.WebActivity
import com.igtools.videodownloader.adapter.BlogAdapter2
import com.igtools.videodownloader.api.okhttp.Urls
import com.igtools.videodownloader.api.retrofit.ApiClient
import com.igtools.videodownloader.databinding.FragmentUserBinding
import com.igtools.videodownloader.models.MediaModel
import com.igtools.videodownloader.models.ResourceModel
import com.igtools.videodownloader.utils.KeyboardUtils
import com.igtools.videodownloader.utils.ShareUtils
import com.igtools.videodownloader.utils.getNullable
import com.igtools.videodownloader.widgets.dialog.BottomDialog
import kotlinx.android.synthetic.main.dialog_bottom.view.*
import kotlinx.coroutines.launch


class UserFragment : Fragment() {

    val TAG = "UserFragment"
    val LOGIN_REQ=1000
    val gson = Gson()

    lateinit var layoutManager: GridLayoutManager
    lateinit var adapter: BlogAdapter2
    lateinit var progressDialog: ProgressDialog
    lateinit var binding: FragmentUserBinding
    lateinit var bottomDialog:BottomDialog
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

        bottomDialog = BottomDialog(requireContext(),R.style.MyDialogTheme)
        val bottomView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_bottom, null)
        bottomView.btn_login.setOnClickListener {

            val url = "https://www.instagram.com/accounts/login"
            startActivityForResult(Intent(requireContext(), WebActivity::class.java).putExtra("url", url),LOGIN_REQ)
            bottomDialog.dismiss()
        }
        bottomDialog.setContent(bottomView)


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
            //refresh(binding.etUsername.text.toString())
            val cookie = ShareUtils.getData("cookie")
            if (cookie == null){
                bottomDialog.show()
            }else{
                getData()
            }

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

                    getDataMore()

                }

            }
        })

        binding.imgClear.setOnClickListener {
            binding.etUsername.setText("")
        }

    }

    private fun clearData(){
        userId = ""
        cursor = ""
        isEnd = false
    }

    fun getData(){

        clearData()

        val cookie = ShareUtils.getData("cookie")
        Log.v(TAG,cookie+"")
        //Log.v(TAG,userAgent+"")
        val map:HashMap<String,String> = HashMap()
        map["Cookie"] = cookie!!
        map["User-Agent"] = Urls.USER_AGENT
        lifecycleScope.launch {
            try {
                progressDialog.show()
                val res = ApiClient.getClient2().getUserMedia(Urls.USER_INFO,map,binding.etUsername.text.toString())
                val code = res.code()
                val jsonObject = res.body()
                if (code==200 && jsonObject!=null){
                    val user = jsonObject["data"].asJsonObject["user"].asJsonObject
                    userId = user["id"].asString
                    Log.v(TAG,"user_id:"+userId)
                    val edge_owner_to_timeline_media = user["edge_owner_to_timeline_media"].asJsonObject
                    val edges = edge_owner_to_timeline_media["edges"].asJsonArray
                    if (edges.size()>0){
                        val medias:ArrayList<MediaModel> = ArrayList()
                        for (item in edges){
                            val mediainfo= parse1(item.asJsonObject)
                            medias.add(mediainfo)
                        }
                        adapter.refresh(medias)


                    }
                    val pageInfo = edge_owner_to_timeline_media["page_info"].asJsonObject
                    isEnd = !pageInfo["has_next_page"].asBoolean
                    cursor = pageInfo["end_cursor"].asString
                }else{
                    Log.e(TAG, res.errorBody()?.string()+"")
                    Toast.makeText(context, getString(R.string.not_found), Toast.LENGTH_SHORT).show()
                }

                progressDialog.dismiss()

            }catch (e:Exception){
                Log.e(TAG, e.message + "")
                progressDialog.dismiss()
                Toast.makeText(context, getString(R.string.parse_error), Toast.LENGTH_SHORT).show()
            }

        }

    }


    private fun getDataMore(){

        if (loadingMore || isEnd){
            return
        }
        loadingMore = true
        val cookie = ShareUtils.getData("cookie")
        val map:HashMap<String,String> = HashMap()
        map["Cookie"] = cookie!!
        map["User-Agent"] = Urls.USER_AGENT
        lifecycleScope.launch {
            try {
                binding.progressBottom.visibility = View.VISIBLE
                val variables:HashMap<String,Any> = HashMap()
                variables["id"] = userId
                variables["first"] = 12
                variables["after"] = cursor
                //Log.v(TAG,"variables:"+variables)
                val res = ApiClient.getClient2().getUserMediaMore(Urls.USER_INFO_MORE,map,Urls.QUERY_HASH_USER,gson.toJson(variables))
                val code = res.code()
                val jsonObject = res.body()
                if (code ==200 && jsonObject!=null){
                    val user = jsonObject["data"].asJsonObject["user"].asJsonObject

                    val edge_owner_to_timeline_media = user["edge_owner_to_timeline_media"].asJsonObject
                    val edges = edge_owner_to_timeline_media["edges"].asJsonArray
                    if (edges.size()>0){
                        val medias:ArrayList<MediaModel> = ArrayList()
                        for (item in edges){
                            val mediainfo= parse1(item.asJsonObject)
                            medias.add(mediainfo)
                        }
                        adapter.loadMore(medias)
                    }
                    val pageInfo = edge_owner_to_timeline_media["page_info"].asJsonObject
                    isEnd = !pageInfo["has_next_page"].asBoolean
                    cursor = pageInfo["end_cursor"].asString
                }else{
                    Log.e(TAG,res.errorBody()?.string()+"")
                    Toast.makeText(context, getString(R.string.not_found), Toast.LENGTH_SHORT).show()
                }

                loadingMore = false
                binding.progressBottom.visibility = View.INVISIBLE
            }catch (e:Exception){
                Log.e(TAG, e.message + "")
                loadingMore = false
                binding.progressBottom.visibility = View.INVISIBLE
                Toast.makeText(context, getString(R.string.not_found), Toast.LENGTH_SHORT).show()
            }

        }

    }


    private fun parse1(jsonObject: JsonObject):MediaModel{
        val mediaModel = MediaModel()
        val node = jsonObject["node"].asJsonObject
        mediaModel.code = node["shortcode"].asString
        val captions = node["edge_media_to_caption"].asJsonObject["edges"].asJsonArray
        if (captions.size()>0){
            mediaModel.captionText = captions[0].asJsonObject["node"].asJsonObject["text"].asString
        }
        if (node.has("video_url")){
            mediaModel.videoUrl = node["video_url"].asString
        }

        val parentType = node["__typename"].asString
        if (parentType == "GraphImage"){
            mediaModel.mediaType = 1
        }else if (parentType == "GraphVideo"){
            mediaModel.mediaType = 2
        }else{
            mediaModel.mediaType = 8
        }

        mediaModel.thumbnailUrl = node["thumbnail_src"].asString
        if (node.has("edge_sidecar_to_children")){
            val children = node["edge_sidecar_to_children"].asJsonObject["edges"].asJsonArray
            if (children.size()>0){
                for (child in children){
                    val resource = ResourceModel()
                    resource.pk = child.asJsonObject["node"].asJsonObject["id"].asString
                    resource.thumbnailUrl = child.asJsonObject["node"].asJsonObject["display_url"].asString
                    resource.videoUrl = child.asJsonObject["node"].asJsonObject.getNullable("video_url")?.asString
                    val typeName = child.asJsonObject["node"].asJsonObject["__typename"].asString
                    if (typeName == "GraphImage"){
                        resource.mediaType = 1
                    }else if (typeName == "GraphVideo"){
                        resource.mediaType = 2
                    }else{
                        resource.mediaType = 8
                    }
                    mediaModel.resources.add(resource)
                }
            }
        }

        return mediaModel

    }



    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode==LOGIN_REQ){

            if (resultCode==200){
                getData()
            }

        }

    }
}