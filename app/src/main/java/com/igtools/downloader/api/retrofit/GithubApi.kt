package com.igtools.downloader.api.retrofit

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

interface GithubApi {
    //... 代码省略

    suspend fun downloadUrl()


    companion object {
        private const val BASE_URL = "http://192.168.0.100:3000/"

        fun createGithubApi(): GithubApi {
            val logger = HttpLoggingInterceptor()
            logger.level = HttpLoggingInterceptor.Level.BASIC

            val client = OkHttpClient.Builder()
                    .addInterceptor(logger)
//                    .sslSocketFactory(SSLSocketClient.getSSLSocketFactory(),
//                            object : X509TrustManager {
//                                override fun checkClientTrusted(
//                                        chain: Array<X509Certificate>,
//                                        authType: String
//                                ) {}
//                                override fun checkServerTrusted(
//                                        chain: Array<X509Certificate>,
//                                        authType: String
//                                ) {}
//                                override fun getAcceptedIssuers(): Array<X509Certificate> {
//                                    return arrayOf()
//                                }
//                            })
//                    .hostnameVerifier(SSLSocketClient.getHostnameVerifier())
                    .build()
            return Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(GithubApi::class.java)
        }
    }
}