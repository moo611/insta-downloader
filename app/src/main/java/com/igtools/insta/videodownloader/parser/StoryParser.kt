package com.igtools.insta.videodownloader.parser

import com.google.gson.JsonObject
import com.igtools.insta.videodownloader.models.MediaModel

/**
 * @Author:  desong
 * @Date:  2024/3/27
 */
class StoryParser :InstaParser{
    override fun parse(html: String): MediaModel? {
        return null
    }

    override fun parse(json: JsonObject): MediaModel? {
        val items = json.getAsJsonArray("items")
        if (items.size() > 0) {
            val mediaModel = MediaModel()
            val item = items[0].asJsonObject
            mediaModel.mediaType = item.getAsJsonPrimitive("media_type").asInt

            val user = item.getAsJsonObject("user")
            mediaModel.username = user.getAsJsonPrimitive("username").asString
            mediaModel.profilePicUrl = user.getAsJsonPrimitive("profile_pic_url").asString

            val imageVersions2 = item.getAsJsonObject("image_versions2")
            val candidates = imageVersions2.getAsJsonArray("candidates")
            if (candidates.size() > 0) {
                var size = 0
                for (candidate in candidates) {
                    val candidateObj = candidate.asJsonObject
                    val w = candidateObj.getAsJsonPrimitive("width").asInt
                    val h = candidateObj.getAsJsonPrimitive("height").asInt
                    val currentSize = w * h
                    if (currentSize > size) {
                        size = currentSize
                        mediaModel.thumbnailUrl = candidateObj.getAsJsonPrimitive("url").asString
                    }
                }
            }

            if (mediaModel.mediaType == 2) {
                val videoVersions = item.getAsJsonArray("video_versions")
                if (videoVersions.size() > 0) {
                    mediaModel.videoUrl = videoVersions[0].asJsonObject.getAsJsonPrimitive("url").asString
                }
            }
            if (!item["caption"].isJsonNull){
                mediaModel.captionText = item.getAsJsonObject("caption").getAsJsonPrimitive("text").asString
            }

            return mediaModel
        }
        return null
    }




}