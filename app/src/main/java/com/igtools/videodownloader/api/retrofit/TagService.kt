package com.igtools.videodownloader.api.retrofit

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


    @GET
    suspend fun getTagData(@Url url:String,@HeaderMap map:HashMap<String,String>,@Query("tag_name")tag:String): Response<JsonObject>


    @GET
    suspend fun getTagNew(@Url url:String): Response<JsonObject>

    @POST
    @FormUrlEncoded
    suspend fun getMoreTagData(@Url url:String,@HeaderMap map:HashMap<String,String>,@FieldMap queries:HashMap<String,Any>): Response<JsonObject>

}