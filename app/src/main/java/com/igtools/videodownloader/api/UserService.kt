package com.igtools.videodownloader.api

import com.google.gson.JsonObject
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface UserService {


    @GET("/api/v2/usermedias")
    suspend fun getUserMedias(
        @Query("username") username: String,
        @Query("end_cursor") end_cursor: String,
        @Query("user_id") user_id: String
    ): Response<JsonObject>


    @GET("/api/v2/usermedias/more")
    suspend fun getUserMediasMore(
        @Query("username") username: String,
        @Query("end_cursor") end_cursor: String,
        @Query("user_id") user_id: String
    ): Response<JsonObject>


    @GET
    suspend fun getUserMedia(
        @Url url: String,
        @HeaderMap headers: HashMap<String, String>,
        @Query("username") username: String
    ): Response<JsonObject>


    @GET
    suspend fun getUserMediaNoCookie(
        @Url url: String
    ): Response<JsonObject>

    @GET
    suspend fun getUserId(@Url url: String):Response<JsonObject>

    @GET
    suspend fun getUserMediaMore(
        @Url url: String,
        @HeaderMap headers: HashMap<String, String>,
        @Query("query_hash") query_hash: String,
        @Query("variables") variables: String
    ): Response<JsonObject>

}