package com.igtools.igdownloader.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "record")
class Record {
    @PrimaryKey
    var id: Int?=null

    @ColumnInfo(name = "content")
    var content: String=""

    @ColumnInfo(name = "created_time")
    var createdTime: String=""
}




