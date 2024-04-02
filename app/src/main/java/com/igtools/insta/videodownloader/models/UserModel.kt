package com.igtools.insta.videodownloader.models

/**
 * @Author:  desong
 * @Date:  2024/3/29
 */
data class UserModel(
    val username: String,
    val avatar: String,
    val post: Int = 0,
    val followers: Int = 0,
    val following: Int = 0
)
