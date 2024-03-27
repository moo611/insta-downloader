package com.igtools.insta.videodownloader.api

import com.google.gson.JsonObject
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface UrlService {


    @GET
    suspend fun getMediaData(
        @Url url: String,
        @HeaderMap headers: HashMap<String, String>,
        @Query("query_hash") query_hash: String,
        @Query("variables") variables: String
    ): Response<JsonObject>


    @GET
    suspend fun getMediaData(
        @Url url: String
    ): Response<ResponseBody>


    @GET
    suspend fun getStoryData(
        @Url url: String,
        @HeaderMap headers: HashMap<String, String>
    ): Response<JsonObject>


}