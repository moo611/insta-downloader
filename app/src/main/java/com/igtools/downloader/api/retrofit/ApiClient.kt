package com.igtools.downloader.api.retrofit

import com.igtools.downloader.api.okhttp.Urls
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {


    fun getClient(): ApiService {
        return Retrofit.Builder().baseUrl(Urls.BASE_URL_PY)
            .addConverterFactory(GsonConverterFactory.create()).build()
            .create(ApiService::class.java)
    }


}