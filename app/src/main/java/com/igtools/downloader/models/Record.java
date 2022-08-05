package com.igtools.downloader.models;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.Date;

@Entity
public class Record {
    @PrimaryKey(autoGenerate = true)
    public int id;
    @ColumnInfo(name = "content")
    public String content;
    @ColumnInfo(name = "create_time")
    public String createTime;
}
