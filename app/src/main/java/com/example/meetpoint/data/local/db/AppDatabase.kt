package com.example.meetpoint.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.meetpoint.data.local.entity.MemberEntity

@Database(entities = [MemberEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun memberDao(): MemberDao
}
