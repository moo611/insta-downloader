package com.igtools.insta.videodownloader.api

import com.google.gson.JsonObject
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

/**
 * UrlService 接口定义了用于获取媒体数据和故事数据的方法。
 */
interface UrlService {

    /**
     * 获取媒体数据的方法。
     *
     * @param url 请求的完整URL。
     * @param headers 请求头部的键值对映射。
     * @param query_hash 查询参数的哈希值。
     * @param variables 查询变量的字符串表示。
     * @return 返回一个包含JsonObject响应体的响应对象。
     */
    @GET
    suspend fun getMediaData(
        @Url url: String,
        @HeaderMap headers: HashMap<String, String>,
        @Query("query_hash") query_hash: String,
        @Query("variables") variables: String
    ): Response<JsonObject>

    /**
     * 获取媒体数据的简化版本方法，只包含必需的URL参数。
     *
     * @param url 请求的完整URL。
     * @return 返回一个包含ResponseBody响应体的响应对象。
     */
    @GET
    suspend fun getMediaData(
        @Url url: String
    ): Response<ResponseBody>

    /**
     * 获取故事数据的方法。
     *
     * @param url 请求的完整URL。
     * @param headers 请求头部的键值对映射。
     * @return 返回一个包含JsonObject响应体的响应对象。
     */
    @GET
    suspend fun getStoryData(
        @Url url: String,
        @HeaderMap headers: HashMap<String, String>
    ): Response<JsonObject>
}
