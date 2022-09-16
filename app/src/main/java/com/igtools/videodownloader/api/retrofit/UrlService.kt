package com.igtools.videodownloader.api.retrofit

import com.google.gson.JsonObject
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface UrlService {

    @GET
    suspend fun downloadUrl(@Url url: String): Response<ResponseBody>


    @GET("/api/mediainfo")
    suspend fun getMedia(@Query("url") url: String): Response<JsonObject>

    @GET("/api/mediastory")
    suspend fun getStory(@Query("url") code: String): Response<JsonObject>


    @GET
    suspend fun getMediaData(
        @Url url: String,
        @HeaderMap headers: HashMap<String, String>,
        @Query("query_hash") query_hash: String,
        @Query("variables") variables: String
    ): Response<JsonObject>


    @GET
    suspend fun getStoryData(@Url url: String,@HeaderMap headers: HashMap<String, String>): Response<JsonObject>


}