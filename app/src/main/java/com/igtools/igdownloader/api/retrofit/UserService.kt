package com.igtools.igdownloader.api.retrofit

import com.google.gson.JsonObject
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface UserService {


    @GET("/api/v2/usermedias")
    suspend fun getUserMedias(
        @Query("username") username: String,
        @Query("end_cursor") end_cursor: String,
        @Query("user_id") user_id: String
    ): Response<JsonObject>

}