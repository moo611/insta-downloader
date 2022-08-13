package com.igtools.igdownloader.room

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.igtools.igdownloader.models.Record

@Dao
interface RecordDao {

    @Query("SELECT * FROM Record ORDER BY id DESC")
    suspend fun all(): List<Record>

    @Insert
    suspend fun insert(record: Record?)

    @Delete
    suspend fun delete(record: Record?)
}