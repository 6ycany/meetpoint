package com.example.meetpoint.di

import android.content.Context
import androidx.room.Room
import com.example.meetpoint.data.local.db.AppDatabase
import com.example.meetpoint.data.local.db.MemberDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "meetpoint.db").build()

    @Provides
    fun provideMemberDao(db: AppDatabase): MemberDao = db.memberDao()
}
