package com.igtools.downloader.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.igtools.downloader.BaseApplication
import com.igtools.downloader.models.Record

@Database(entities = [Record::class], version = 1)
abstract class RecordDB : RoomDatabase() {

    abstract fun recordDao(): RecordDao

    companion object {
        @Volatile
        private var INSTANCE: RecordDB? = null

        fun getInstance(): RecordDB = INSTANCE ?: synchronized(this) {
            INSTANCE ?: buildDatabase(BaseApplication.mContext).also { INSTANCE = it }
        }

        private fun buildDatabase(context: Context) =
            Room.databaseBuilder(
                context.applicationContext,
                RecordDB::class.java, "mydb"
            )
                .build()
    }
}