package com.igtools.igdownloader.api.retrofit

import com.google.gson.JsonObject
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

interface ApiService {

    @GET
    suspend fun downloadUrl(@Url url: String): Response<ResponseBody>


    @GET("api/userinfo")
    suspend fun getUserInfo(
        @Query("user") user: String,
        @Query("end_cursor") end_cursor: String
    ): Response<JsonObject>


    @GET("api/mediainfo")
    suspend fun getShortCode(@Query("url") url: String): Response<JsonObject>

    @GET("api/taginfo")
    suspend fun getTags(
        @Query("tag") tag: String
    ): Response<JsonObject>

    @GET("api/taginfo/more")
    suspend fun getMoreTags(
        @Query("max_id") max_id: String,
        @Query("page") page: Int,
        @Query("next_media_ids") next_media_ids: String
    ): Response<JsonObject>

}