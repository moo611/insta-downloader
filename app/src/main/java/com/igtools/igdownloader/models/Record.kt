package com.igtools.igdownloader.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity(tableName = "record")
data class Record(
    @PrimaryKey
    var id: String,
    @ColumnInfo(name = "content")
    var content: String?,
    @ColumnInfo(name = "created_time")
    var createdTime: Long?

)




