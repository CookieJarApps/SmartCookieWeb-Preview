package com.cookiejarapps.android.smartcookieweb.di

import android.app.Application
import android.content.SharedPreferences
import dagger.Module
import dagger.Provides
import javax.inject.Qualifier

@Module
class AppModule {
    @Provides
    @UserPrefs
    fun provideUserPreferences(application: Application): SharedPreferences = application.getSharedPreferences("settings", 0)
}

@Qualifier
@Retention(AnnotationRetention.SOURCE)
annotation class UserPrefs