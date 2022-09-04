package com.igtools.igdownloader.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.igtools.igdownloader.BaseApplication
import com.igtools.igdownloader.models.Record

@Database(entities = [Record::class], version = 4, exportSchema = true)
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
            ).fallbackToDestructiveMigration().build()
    }
}