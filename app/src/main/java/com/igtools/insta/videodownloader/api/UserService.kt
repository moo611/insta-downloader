package com.igtools.insta.videodownloader.api

import com.google.gson.JsonObject
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface UserService {
    /**
     * 获取用户媒体信息。
     * @param url 请求的完整URL。
     * @param headers 请求头部的键值对。
     * @param username 查询的用户名。
     * @return 返回一个包含用户媒体信息的JsonObject的响应体。
     */
    @GET
    suspend fun getUserMedia(
        @Url url: String,
        @HeaderMap headers: HashMap<String, String>,
        @Query("username") username: String
    ): Response<JsonObject>

    /**
     * 获取更多用户媒体信息。
     * @param url 请求的完整URL。
     * @param headers 请求头部的键值对。
     * @param query_hash 用于查询的哈希值。
     * @param variables 查询变量的JSON字符串。
     * @return 返回一个包含更多用户媒体信息的JsonObject的响应体。
     */
    @GET
    suspend fun getUserMediaMore(
        @Url url: String,
        @HeaderMap headers: HashMap<String, String>,
        @Query("query_hash") query_hash: String,
        @Query("variables") variables: String
    ): Response<JsonObject>

    /**
     * 获取用户网页信息。
     * @param url 请求的完整URL。
     * @param headers 请求头部的键值对。
     * @return 返回一个包含用户网页信息的JsonObject的响应体。
     */
    @GET
    suspend fun getUserWeb(@Url url:String, @HeaderMap headers: HashMap<String, String>):Response<JsonObject>
}
