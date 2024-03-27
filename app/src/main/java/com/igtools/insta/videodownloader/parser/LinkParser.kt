package com.igtools.insta.videodownloader.parser

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.igtools.insta.videodownloader.models.MediaModel
import com.igtools.insta.videodownloader.utils.getNullable
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import java.util.regex.Pattern

/**
 * @Author:  desong
 * @Date:  2024/3/27
 */
class LinkParser:InstaParser {
    override fun parse(html: String): MediaModel {
        val soup = Jsoup.parse(html)
        val embed: Element? = soup.selectFirst(".Embed")
        val mediaModel = MediaModel()

        val mediaType = embed?.attr("data-media-type")
        println("media_type is: $mediaType")

        when (mediaType) {
            "GraphImage" -> {
                mediaModel.mediaType = 1
                parseUserInfo(soup, mediaModel)
                parseCaption(soup, mediaModel)
                parseThumbnail(soup, mediaModel)
            }
            "GraphSidecar" -> {
                mediaModel.mediaType = 8
                val scripts: Elements = soup.select("script")
                println(scripts.size)
                scripts.forEach { script ->
                    if (script.data().contains("gql_data")) {
                        val scriptStr = script.data().replace("\\", "")
                        val pattern = "\"gql_data\":"
                        val matcher = Pattern.compile(pattern).matcher(scriptStr)
                        if (matcher.find()) {
                            val startIndex = matcher.end()
                            var endIndex = 1
                            var cnt = 0
                            var i = startIndex
                            while (i < scriptStr.length) {
                                when (scriptStr[i]) {
                                    '{' -> cnt++
                                    '}' -> cnt--
                                }
                                if (i != startIndex && cnt == 0) {
                                    endIndex = i
                                    break
                                }
                                i++
                            }
                            val gqlData = scriptStr.substring(startIndex, endIndex + 1)
                            val jsonObject = JsonParser.parseString(gqlData).asJsonObject
                            parseMedia(jsonObject.getAsJsonObject("shortcode_media"), mediaModel)
                        }
                    }
                }
            }
            "GraphVideo" -> {
                mediaModel.mediaType = 2
                parseUserInfo(soup, mediaModel)
                parseCaption(soup, mediaModel)
                parseThumbnail(soup, mediaModel)
                parseVideo(html, mediaModel)
            }
        }
        return mediaModel
    }

    override fun parse(json: JsonObject): MediaModel {
        val mediaModel = MediaModel()

        when (json["__typename"].asString) {
            "GraphImage" -> {
                mediaModel.mediaType = 1
            }
            "GraphVideo" -> {
                mediaModel.mediaType = 2
            }
            else -> {
                mediaModel.mediaType = 8
            }
        }

        mediaModel.code = json["shortcode"].asString
        mediaModel.pk = json["id"].asString
        val edgeMediaToCaption = json["edge_media_to_caption"].asJsonObject
        val edges = edgeMediaToCaption["edges"].asJsonArray
        if (edges.size() > 0) {
            mediaModel.captionText = edges[0].asJsonObject["node"].asJsonObject["text"].asString
        }
        mediaModel.videoUrl = json.getNullable("video_url")?.asString
        mediaModel.thumbnailUrl = json["display_url"].asString
        val owner = json["owner"].asJsonObject
        mediaModel.profilePicUrl = owner["profile_pic_url"].asString
        mediaModel.username = owner["username"].asString
        if (json.has("edge_sidecar_to_children")) {
            val edgeSidecarToChildren = json["edge_sidecar_to_children"].asJsonObject
            val children = edgeSidecarToChildren["edges"].asJsonArray
            if (children.size() > 0) {
                for (child in children) {
                    val resource = MediaModel()
                    resource.pk = child.asJsonObject["node"].asJsonObject["id"].asString
                    resource.thumbnailUrl =
                        child.asJsonObject["node"].asJsonObject["display_url"].asString
                    resource.videoUrl =
                        child.asJsonObject["node"].asJsonObject.getNullable("video_url")?.asString
                    when (child.asJsonObject["node"].asJsonObject["__typename"].asString) {
                        "GraphImage" -> {
                            resource.mediaType = 1
                        }
                        "GraphVideo" -> {
                            resource.mediaType = 2
                        }
                        else -> {
                            resource.mediaType = 8
                        }
                    }
                    mediaModel.resources.add(resource)
                }
            }
        }

        return mediaModel
    }


    private fun parseUserInfo(soup: Document, mediaModel: MediaModel) {
        if (soup.selectFirst(".Avatar") != null) {
            println("--------find avatar")
            val img: Element? = soup.selectFirst(".Avatar img")
            mediaModel.profilePicUrl = img?.attr("src")
        } else if (soup.selectFirst(".CollabAvatar") != null) {
            println("-----find CollabAvatar")
            val img: Element? = soup.selectFirst(".CollabAvatar img")
            mediaModel.profilePicUrl = img?.attr("src")
        }
        println("avatar:${mediaModel.profilePicUrl}")

        val userDiv: Element? = soup.selectFirst(".HeaderText")
        val span: Element? = userDiv?.selectFirst("span")
        mediaModel.username = span?.text() ?: ""
        println("username:${mediaModel.username}")
    }

    private fun parseCaption(soup: Document, mediaModel: MediaModel) {
        println("caption")
        val captionDiv: Element? = soup.selectFirst(".Caption")
        captionDiv?.let {
            it.selectFirst("div")?.remove()
            mediaModel.captionText = it.text()
        }
        println(mediaModel.captionText)
    }

    private fun parseThumbnail(soup: Document, mediaModel: MediaModel) {
        println("thumbnail")
        val mediaImg: Element? = soup.selectFirst(".EmbeddedMediaImage")
        mediaModel.thumbnailUrl = mediaImg?.attr("src") ?: ""
        println("thumbnail is:${mediaModel.thumbnailUrl}")
    }

    private fun parseVideo(html: String, mediaModel: MediaModel) {
        println("video")
        val replacedHtml = html.replace("\\", "")
        println(replacedHtml)
        val pattern =
            "\"video_url\":\"https://.*?\\.mp4?.*?\","
        val matcher = Pattern.compile(pattern).matcher(replacedHtml)
        if (matcher.find()) {
            var str1 = replacedHtml.substring(matcher.start(), matcher.end())
            str1 = str1.split(",")[0].replace("\"video_url\":\"", "").replace("\"", "")
            mediaModel.videoUrl = str1
        }
        println(mediaModel.videoUrl)
    }

    private fun parseMedia(shortcodeMedia: JsonObject, mediaModel: MediaModel) {
        val typeName = shortcodeMedia.get("__typename").asString
        mediaModel.mediaType = when (typeName) {
            "GraphImage" -> 1
            "GraphVideo" -> 2
            else -> 8
        }
        val edgeMediaToCaption = shortcodeMedia.getAsJsonObject("edge_media_to_caption")
        val edges = edgeMediaToCaption.getAsJsonArray("edges")
        if (edges.size() > 0) {
            mediaModel.captionText =
                edges[0].asJsonObject.getAsJsonObject("node").get("text").asString
        }
        if (shortcodeMedia.has("video_url")) {
            mediaModel.videoUrl = shortcodeMedia.get("video_url").asString
        }
        mediaModel.thumbnailUrl = shortcodeMedia.get("display_url").asString
        val owner = shortcodeMedia.getAsJsonObject("owner")
        mediaModel.profilePicUrl = owner.get("profile_pic_url").asString
        mediaModel.username = owner.get("username").asString
        val edgeSidecarToChildren = shortcodeMedia.getAsJsonObject("edge_sidecar_to_children")
        val children = edgeSidecarToChildren.getAsJsonArray("edges")
        val list = mutableListOf<MediaModel>()
        children.forEach { edge ->
            val node = edge.asJsonObject.getAsJsonObject("node")
            val resource = MediaModel()
            resource.thumbnailUrl = node.get("display_url").asString
            resource.videoUrl = node.get("video_url").asString
            resource.mediaType = when (node.get("__typename").asString) {
                "GraphImage" -> 1
                "GraphVideo" -> 2
                else -> 8
            }
            list.add(resource)
        }
        mediaModel.resources = list as ArrayList<MediaModel>
    }
}