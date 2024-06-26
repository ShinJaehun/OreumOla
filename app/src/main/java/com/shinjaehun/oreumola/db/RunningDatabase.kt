package com.shinjaehun.oreumola.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [Run::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class RunningDatabase: RoomDatabase() {
    abstract fun getRunDao(): RunDAO
}