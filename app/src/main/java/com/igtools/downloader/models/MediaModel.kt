package com.igtools.downloader.models

class MediaModel {
    constructor()
    constructor(
        thumbnail: String = "",
        url: String = "",
        type: Int = 1,
        source: String = "",
        title: String = "",
        content: String = ""
    )

    var thumbnail = ""
    var url = ""
    var type = 1
    var source = ""
    var title = ""
    var content = ""
}