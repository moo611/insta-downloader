package com.igtools.downloader.api.retrofit

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

}