package com.igtools.videodownloader.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "record")
data class Record(
    @PrimaryKey
    var id: String,
    @ColumnInfo(name = "content")
    var content: String?,
    @ColumnInfo(name = "created_time")
    var createdTime: Long?

)




