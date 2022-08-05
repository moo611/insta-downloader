package com.igtools.downloader.room;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import com.igtools.downloader.models.MediaModel;
import com.igtools.downloader.models.Record;

import java.util.ArrayList;
import java.util.List;

@Dao
public interface RecordDao {

    @Query("SELECT * FROM Record")
    List<Record> getAll();


    @Insert
    void insertAll(Record... records);

    @Insert
    void insert(Record record);

    @Delete
    void delete(Record record);
}
