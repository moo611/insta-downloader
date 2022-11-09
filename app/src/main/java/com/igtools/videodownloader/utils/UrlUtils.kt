package com.igtools.videodownloader.utils

import java.net.URL

object UrlUtils {

    val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"

    fun extractMedia(url:String):String?{

        val newurl = URL(url)
        if (newurl.path.split("/").size>2){
            return newurl.path.split("/")[2]
        }
        return null
    }

    fun extractStory(url:String):String?{
        val newurl = URL(url)
        if (newurl.path.split("/").size>3){
            return newurl.path.split("/")[3]
        }
        return null
    }

    fun getInstagramPostId(code: String): String {

        var mychar:Char;
        var id:Long = 0;
        val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_";
        for (element in code) {
            mychar = element;
            id = (id * 64) + alphabet.indexOf(mychar);
        }
        return id.toString();

    }

}