package com.igtools.insta.videodownloader.room

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.igtools.insta.videodownloader.models.Record

@Dao
interface RecordDao {

    @Query("SELECT * FROM Record ORDER BY created_time DESC")
    suspend fun all(): List<Record>

    @Query("SELECT * FROM Record ORDER BY created_time DESC limit 8")
    suspend fun recent(): List<Record>

    @Insert
    suspend fun insert(record: Record?)

    @Delete
    suspend fun delete(record: Record?)

//    @Query("SELECT * FROM Record WHERE id = :code")
//    suspend fun findById(code: String): Record?

    @Query("SELECT * FROM Record WHERE url = :url")
    suspend fun findByUrl(url: String): Record?

    @Query("SELECT * FROM Record WHERE code = :code")
    suspend fun findByCode(code: String): Record?


    @Query("DELETE FROM Record")
    suspend fun deleteAll()

}