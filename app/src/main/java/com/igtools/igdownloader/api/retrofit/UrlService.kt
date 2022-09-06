package com.igtools.igdownloader.api.retrofit

import com.google.gson.JsonObject
import okhttp3.RequestBody
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
    suspend fun getMediaData(@HeaderMap map:HashMap<String,String>, @Url url:String)

}