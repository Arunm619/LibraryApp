package io.arunbuilds.libraryapp.di

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.arunbuilds.libraryapp.ui.main.SessionController
import io.arunbuilds.libraryapp.utils.rx.AppSchedulerProvider
import io.arunbuilds.libraryapp.utils.rx.SchedulerProvider
import javax.inject.Qualifier
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    fun provideSchedulerProvider(): SchedulerProvider {
        return AppSchedulerProvider()
    }

    @Singleton
    @Provides
    @SessionControllerSharedPreferences
    fun provideSharedPreferences(
        @ApplicationContext app: Context
    ): SharedPreferences = app.getSharedPreferences(SessionController.PREFS_FILENAME, MODE_PRIVATE)
}

@Qualifier
annotation class SessionControllerSharedPreferences