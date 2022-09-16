package com.igtools.videodownloader.room

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.igtools.videodownloader.models.Record

@Dao
interface RecordDao {

    @Query("SELECT * FROM Record ORDER BY created_time DESC")
    suspend fun all(): List<Record>

    @Insert
    suspend fun insert(record: Record?)

    @Delete
    suspend fun delete(record: Record?)

    @Query("SELECT * FROM Record WHERE id = :code")
    suspend fun findById(code: String): Record?

}