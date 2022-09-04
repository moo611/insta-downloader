package com.igtools.igdownloader.api.retrofit

import com.google.gson.JsonObject
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface TagService {


    @GET("/api/taginfo")
    suspend fun getTags(
        @Query("tag") tag: String
    ): Response<JsonObject>


    @POST("/api/taginfo/more")
    suspend fun postMoreTags(@Body body: RequestBody): Response<JsonObject>



}