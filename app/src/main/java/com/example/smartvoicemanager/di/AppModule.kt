package com.example.smartvoicemanager.di

import android.content.Context
import androidx.room.Room
import com.example.smartvoicemanager.data.local.AppDatabase
import com.example.smartvoicemanager.data.local.dao.TaskDao
import com.example.smartvoicemanager.data.prefs.SettingsManager
import com.example.smartvoicemanager.data.repository.TaskRepositoryImpl
import com.example.smartvoicemanager.data.util.AlarmSchedulerImpl
import com.example.smartvoicemanager.domain.repository.TaskRepository
import com.example.smartvoicemanager.domain.usecase.*
import com.example.smartvoicemanager.domain.util.AlarmScheduler
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
        .fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    @Singleton
    fun provideTaskDao(db: AppDatabase): TaskDao {
        return db.taskDao
    }

    @Provides
    @Singleton
    fun provideTaskRepository(dao: TaskDao): TaskRepository {
        return TaskRepositoryImpl(dao)
    }

    @Provides
    @Singleton
    fun provideAlarmScheduler(@ApplicationContext context: Context): AlarmScheduler {
        return AlarmSchedulerImpl(context)
    }

    @Provides
    @Singleton
    fun provideSettingsManager(@ApplicationContext context: Context): SettingsManager {
        return SettingsManager(context)
    }

    @Provides
    @Singleton
    fun provideGetActiveTasksUseCase(repository: TaskRepository): GetActiveTasksUseCase {
        return GetActiveTasksUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideScheduleTaskUseCase(
        repository: TaskRepository,
        alarmScheduler: AlarmScheduler
    ): ScheduleTaskUseCase {
        return ScheduleTaskUseCase(repository, alarmScheduler)
    }

    @Provides
    @Singleton
    fun provideCompleteTaskUseCase(repository: TaskRepository): CompleteTaskUseCase {
        return CompleteTaskUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideDeleteTaskUseCase(repository: TaskRepository): DeleteTaskUseCase {
        return DeleteTaskUseCase(repository)
    }
}
