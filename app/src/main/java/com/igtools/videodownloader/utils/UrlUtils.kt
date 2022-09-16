package com.igtools.videodownloader.utils

import java.net.URL
import kotlin.math.pow

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

    fun code2pk(code:String):Long{
        val base = alphabet.length
        val strlen = code.length
        var num = 0L
        var idx =0

        for (char in code){
           val power =  strlen-(idx+1)
            num += alphabet.indexOf(char) * (base.toDouble().pow(power.toDouble())).toInt()
            idx += 1
        }
        return num
    }

}