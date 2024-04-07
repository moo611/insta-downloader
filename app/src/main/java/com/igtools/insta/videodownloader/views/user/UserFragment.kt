package com.igtools.insta.videodownloader.views.user

import android.app.Activity.RESULT_OK
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.DialogInterface
import android.content.Intent
import android.util.Log
import android.view.View
import android.widget.SearchView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.google.gson.JsonObject
import com.igtools.insta.videodownloader.BaseApplication
import com.igtools.insta.videodownloader.R
import com.igtools.insta.videodownloader.api.ApiClient
import com.igtools.insta.videodownloader.api.Urls
import com.igtools.insta.videodownloader.base.BaseFragment
import com.igtools.insta.videodownloader.databinding.FragmentUserBinding
import com.igtools.insta.videodownloader.models.MediaModel
import com.igtools.insta.videodownloader.models.UserModel
import com.igtools.insta.videodownloader.views.WebActivity
import com.igtools.insta.videodownloader.utils.getNullable
import kotlinx.coroutines.launch


class UserFragment : BaseFragment<FragmentUserBinding>() {
    lateinit var privateDialog: AlertDialog
    val TAG = "SearchFragment"
    val LOGIN_REQ = 1000
    val COUNT = 50

    lateinit var layoutManager: GridLayoutManager
    lateinit var adapter: UserAdapter
    lateinit var progressDialog: ProgressDialog

    var cursor = ""
    var loadingMore = false
    var isEnd = false
    var userId = ""
    var userInfo: UserModel? = null
    var retries = 0
    override fun getLayoutId(): Int {
        return R.layout.fragment_user
    }

    override fun initView() {

        progressDialog = ProgressDialog(requireContext())
        progressDialog.setMessage(getString(R.string.searching))
        progressDialog.setCancelable(false)
        initDialog()
        adapter = UserAdapter(requireContext())
        layoutManager = GridLayoutManager(context, 3)
        mBinding.rv.adapter = adapter
        mBinding.rv.layoutManager = layoutManager
        layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {

                return if (position == 0) {
                    layoutManager.spanCount
                } else {
                    1
                }
            }
        }



        mBinding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(p0: String?): Boolean {
                getUserData()
                return true
            }

            override fun onQueryTextChange(p0: String?): Boolean {
                return false
            }

        })

        mBinding.rv.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (isSlideToBottom(mBinding.rv) && dy > 0) {
                    //滑动到底部
                    getUserDataMore()

                }

            }
        })


    }


    private fun initDialog() {

        val builder2 = AlertDialog.Builder(requireContext());
        builder2.setTitle(R.string.login);
        builder2.setMessage(R.string.long_text2);
        builder2.setIcon(R.mipmap.icon);
        //点击对话框以外的区域是否让对话框消失
        builder2.setCancelable(true);
        //设置正面按钮
        builder2.setPositiveButton(R.string.ok, object : DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface?, which: Int) {
                val url = "https://www.instagram.com/accounts/login"
                startActivityForResult(
                    Intent(requireContext(), WebActivity::class.java).putExtra(
                        "url",
                        url
                    ), LOGIN_REQ
                )
                privateDialog.dismiss()
            }

        });
        //设置反面按钮
        builder2.setNegativeButton(R.string.cancel, object : DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface?, which: Int) {
                privateDialog.dismiss()
            }

        });
        privateDialog = builder2.create()
    }

    override fun initData() {

    }


    private fun clearUserData() {
        userId = ""
        cursor = ""
        isEnd = false
        userInfo = null

    }

    /**
     * 获取用户数据的方法。此方法首先清除旧的用户数据，然后通过异步任务从Instagram API获取用户的公开信息（如果用户不私有）或私有信息（如果用户已登录并授权）。
     * 使用了Coroutine和LifecycleScope来管理异步任务，并在UI线程上更新进度对话框和数据。
     */
    private fun getUserData() {
        if (retries > 1) {
            retries = 0
            return
        }
        clearUserData()
        lifecycleScope.launch {
            try {
                progressDialog.show()
                val query = mBinding.searchView.query.toString().trim().lowercase()
                val url =
                    "https://www.instagram.com/api/v1/users/web_profile_info/?username=$query"
                val headers: HashMap<String, String> = HashMap()
                headers["user-agent"] =
                    "Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Mobile Safari/537.36"
                headers["x-csrftoken"] = "IQ9sfTjzBvHWYxRUwQqKOLU3eHkOISJM"
                headers["x-ig-app-id"] = "1217981644879628"
                val res1 = ApiClient.getClient2().getUserWeb(url, headers)
                Log.v(TAG, res1.body().toString())

                val code = res1.code()

                if (code == 200 && res1.body() != null) {
                    val jsonObject = res1.body()!!

                    val user = jsonObject["data"].asJsonObject["user"].asJsonObject

                    val isPrivate = user["is_private"].asBoolean
                    if (isPrivate) {

                        if (BaseApplication.cookie == null) {
                            privateDialog.show()
                            progressDialog.dismiss()
                        } else {
                            getUserDataByCookie()
                        }

                    } else {

                        userId = user["id"].asString

                        val edge_owner_to_timeline_media =
                            user["edge_owner_to_timeline_media"].asJsonObject
                        val username = user["username"].asString
                        val avatar = user["profile_pic_url"].asString
                        val post = edge_owner_to_timeline_media["count"].asInt
                        val followers = user["edge_followed_by"].asJsonObject["count"].asInt
                        val following = user["edge_follow"].asJsonObject["count"].asInt
                        userInfo = UserModel(username, avatar, post, followers, following)


                        val edges = edge_owner_to_timeline_media["edges"].asJsonArray
                        if (edges.size() > 0) {
                            val medias: ArrayList<MediaModel> = ArrayList()
                            for (item in edges) {
                                val mediainfo = parseUserData(item.asJsonObject)
                                medias.add(mediainfo)
                            }

                            adapter.refresh(medias, userInfo!!)
                        }
                        val pageInfo = edge_owner_to_timeline_media["page_info"].asJsonObject
                        isEnd = !pageInfo["has_next_page"].asBoolean
                        cursor = pageInfo["end_cursor"].asString
                        progressDialog.dismiss()

                    }

                } else {
                    if (!isInvalidContext()) {
                        progressDialog.dismiss()
                    }
                    safeToast(R.string.failed)
                }

            } catch (e: Exception) {

                Log.e(TAG, e.message + "")

                if (retries == 0) {
                    retries++
                    getUserData()
                }

                if (!isInvalidContext()) {
                    progressDialog.dismiss()
                }
                safeToast(R.string.failed)
            }
        }

    }

    /**
     * 使用cookie获取用户数据的异步函数。
     * 该函数首先检查进度对话框是否正在显示，如果没有则显示它。
     * 然后基于提供的cookie构建请求头，向指定API发送请求以获取用户信息和媒体内容。
     * 如果请求成功，会解析响应数据，更新用户ID和头像URL（如果存在），
     * 并刷新媒体列表。如果请求失败，会显示错误信息。
     * 在任何情况下，如果上下文未失效，都会尝试关闭进度对话框。
     */
    private suspend fun getUserDataByCookie() {

        if (!progressDialog.isShowing) {
            progressDialog.show()
        }
        try {
            val cookie = BaseApplication.cookie
            val map: HashMap<String, String> = HashMap()
            map["Cookie"] = cookie!!
            map["User-Agent"] = Urls.USER_AGENT
            val res = ApiClient.getClient2()
                .getUserMedia(
                    Urls.PRIVATE_API + "/users/web_profile_info",
                    map,
                    mBinding.searchView.query.toString().trim().lowercase()
                )
            val code = res.code()
            val jsonObject = res.body()
            if (code == 200 && jsonObject != null) {

                val user = jsonObject["data"].asJsonObject["user"].asJsonObject
                userId = user["id"].asString

                Log.v(TAG, "user_id:" + userId)
                val edge_owner_to_timeline_media =
                    user["edge_owner_to_timeline_media"].asJsonObject

                val username = user["username"].asString
                val avatar = user["profile_pic_url"].asString
                val post = edge_owner_to_timeline_media["count"].asInt
                val followers = user["edge_followed_by"].asJsonObject["count"].asInt
                val following = user["edge_follow"].asJsonObject["count"].asInt
                userInfo = UserModel(username, avatar, post, followers, following)

                val edges = edge_owner_to_timeline_media["edges"].asJsonArray
                if (edges.size() > 0) {
                    val medias: ArrayList<MediaModel> = ArrayList()
                    for (item in edges) {
                        val mediainfo = parseUserData(item.asJsonObject)
                        medias.add(mediainfo)
                    }



                    adapter.refresh(medias, userInfo!!)


                }
                val pageInfo = edge_owner_to_timeline_media["page_info"].asJsonObject
                isEnd = !pageInfo["has_next_page"].asBoolean
                cursor = pageInfo["end_cursor"].asString

            } else {
                Log.e(TAG, res.errorBody()?.string() + "")
                safeToast(R.string.failed)
            }

            if (!isInvalidContext()) {
                progressDialog.dismiss()
            }

        } catch (e: Exception) {
            Log.e(TAG, e.message + "")
            if (!isInvalidContext()) {
                progressDialog.dismiss()
            }
            safeToast(R.string.failed)
        }


    }

    /**
     * 加载更多用户数据的函数。
     * 这个函数用于在当前已有数据基础上，通过网络请求获取更多用户媒体信息，并更新到UI上。
     * 该函数会检查是否已经在加载中或者数据已经加载完毕，以避免不必要的请求。
     *
     * 参数无。
     * 返回值无。
     */
    private fun getUserDataMore() {

        if (loadingMore || isEnd || BaseApplication.cookie == null) {
            return
        }

        loadingMore = true
        val cookie = BaseApplication.cookie
        val map: HashMap<String, String> = HashMap()
        map["Cookie"] = cookie!!
        map["User-Agent"] = Urls.USER_AGENT
        lifecycleScope.launch {
            try {
                mBinding.progressBottom.visibility = View.VISIBLE
                val variables: HashMap<String, Any> = HashMap()
                variables["id"] = userId
                variables["first"] = COUNT
                variables["after"] = cursor
                //Log.v(TAG,"variables:"+variables)
                val res = ApiClient.getClient2().getUserMediaMore(
                    Urls.PUBLIC_API + "/graphql/query",
                    map,
                    Urls.QUERY_HASH_USER,
                    gson.toJson(variables)
                )
                val code = res.code()
                val jsonObject = res.body()
                if (code == 200 && jsonObject != null) {
                    val user = jsonObject["data"].asJsonObject["user"].asJsonObject

                    val edge_owner_to_timeline_media =
                        user["edge_owner_to_timeline_media"].asJsonObject
                    val edges = edge_owner_to_timeline_media["edges"].asJsonArray
                    if (edges.size() > 0) {
                        val medias: ArrayList<MediaModel> = ArrayList()
                        for (item in edges) {
                            val mediainfo = parseUserData(item.asJsonObject)
                            medias.add(mediainfo)
                        }
                        adapter.loadMore(medias)
                    }
                    val pageInfo = edge_owner_to_timeline_media["page_info"].asJsonObject
                    isEnd = !pageInfo["has_next_page"].asBoolean
                    cursor = pageInfo["end_cursor"].asString
                } else {
                    Log.e(TAG, res.errorBody()?.string() + "")
                    Toast.makeText(requireContext(), getString(R.string.failed), Toast.LENGTH_SHORT)
                        .show()
                }

                loadingMore = false
                mBinding.progressBottom.visibility = View.INVISIBLE
            } catch (e: Exception) {
                Log.e(TAG, e.message + "")
                loadingMore = false
                mBinding.progressBottom.visibility = View.INVISIBLE
                safeToast(R.string.failed)
            }

        }

    }

    /**
     * 解析用户数据的函数。
     *
     * @param jsonObject 包含媒体模型数据的JsonObject对象。
     * @return 返回一个填充了媒体模型数据的MediaModel实例。
     */
    private fun parseUserData(jsonObject: JsonObject): MediaModel {
        val mediaModel = MediaModel()
        val node = jsonObject["node"].asJsonObject
        mediaModel.code = node["shortcode"].asString
        val captions = node["edge_media_to_caption"].asJsonObject["edges"].asJsonArray
        if (captions.size() > 0) {
            mediaModel.captionText = captions[0].asJsonObject["node"].asJsonObject["text"].asString
        }
        if (node.has("video_url")) {
            mediaModel.videoUrl = node["video_url"].asString
        }

        val parentType = node["__typename"].asString
        if (parentType == "GraphImage") {
            mediaModel.mediaType = 1
        } else if (parentType == "GraphVideo") {
            mediaModel.mediaType = 2
        } else {
            mediaModel.mediaType = 8
        }

        mediaModel.thumbnailUrl = node["thumbnail_src"].asString
        mediaModel.username = mBinding.searchView.query.toString().trim().lowercase()
        mediaModel.profilePicUrl = userInfo!!.avatar
        if (node.has("edge_sidecar_to_children")) {
            val children = node["edge_sidecar_to_children"].asJsonObject["edges"].asJsonArray
            if (children.size() > 0) {
                for (child in children) {
                    val resource = MediaModel()
                    resource.pk = child.asJsonObject["node"].asJsonObject["id"].asString
                    resource.thumbnailUrl =
                        child.asJsonObject["node"].asJsonObject["display_url"].asString
                    resource.videoUrl =
                        child.asJsonObject["node"].asJsonObject.getNullable("video_url")?.asString
                    val typeName = child.asJsonObject["node"].asJsonObject["__typename"].asString
                    if (typeName == "GraphImage") {
                        resource.mediaType = 1
                    } else if (typeName == "GraphVideo") {
                        resource.mediaType = 2
                    } else {
                        resource.mediaType = 8
                    }
                    mediaModel.resources.add(resource)
                }
            }
        }

        return mediaModel

    }


    fun isSlideToBottom(recyclerView: RecyclerView?): Boolean {
        if (recyclerView == null) return false
        return (recyclerView.computeVerticalScrollExtent() + recyclerView.computeVerticalScrollOffset()
                >= recyclerView.computeVerticalScrollRange() - 50)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == LOGIN_REQ && resultCode == RESULT_OK) {

            lifecycleScope.launch {
                getUserDataByCookie()
            }

        }

    }
}