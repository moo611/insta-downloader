package com.igtools.insta.videodownloader.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "record")
data class Record(
    @PrimaryKey(autoGenerate = true)
    var id: Int?,
    @ColumnInfo(name = "content")
    var content: String?,
    @ColumnInfo(name = "created_time")
    var createdTime: Long?,
    @ColumnInfo(name="url")
    var url:String?,
    @ColumnInfo(name="code")
    var code:String?,
    @ColumnInfo(name="paths")
    var paths:String?
)




