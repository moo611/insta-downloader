package com.igtools.videodownloader.api

import com.google.gson.JsonObject
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface UrlService {



    @GET
    suspend fun getMedia(
        @HeaderMap headers: HashMap<String, String>,
        @Url url: String
    ): Response<JsonObject>

    @GET("/api/mediastory")
    suspend fun getStory(@Query("url") code: String): Response<JsonObject>

    @GET
    suspend fun getMediaNew(@Url url: String): Response<JsonObject>


    @GET
    suspend fun getMediaData(
        @Url url: String,
        @HeaderMap headers: HashMap<String, String>,
        @Query("query_hash") query_hash: String,
        @Query("variables") variables: String
    ): Response<JsonObject>


    @GET
    suspend fun getMediaData2(
        @Url url: String,
        @HeaderMap headers: HashMap<String, String>,
        @Query("query_hash") query_hash: String,
        @Query("variables") variables: String
    ): Response<ResponseBody>


    @GET
    suspend fun getStoryData(
        @Url url: String,
        @HeaderMap headers: HashMap<String, String>
    ): Response<JsonObject>


}