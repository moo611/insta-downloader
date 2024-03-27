package com.igtools.insta.videodownloader.parser

import android.provider.MediaStore.Audio.Media
import com.google.gson.JsonObject
import com.igtools.insta.videodownloader.models.MediaModel

/**
 * @Author:  desong
 * @Date:  2024/3/27
 */
interface InstaParser {


    fun parse(html:String):MediaModel?


    fun parse(json:JsonObject):MediaModel?
}