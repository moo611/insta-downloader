package com.igtools.instagram.videodownloader.api

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Streaming
import retrofit2.http.Url

interface DownloadService {

    @Streaming
    @GET
    suspend fun downloadUrl(@Url url: String): Response<ResponseBody>


}