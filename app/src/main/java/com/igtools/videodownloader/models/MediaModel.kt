package com.igtools.videodownloader.models

class MediaModel : ResourceModel() {

    var code = ""
    var captionText:String?=null
    var profilePicUrl:String?=null
    var username = ""
    var resources: ArrayList<ResourceModel> = ArrayList()

}