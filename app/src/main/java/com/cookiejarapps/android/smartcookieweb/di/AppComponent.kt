package com.cookiejarapps.android.smartcookieweb.di

import android.app.Application
import com.cookiejarapps.android.smartcookieweb.BrowserActivity
import com.cookiejarapps.android.smartcookieweb.BrowserApp
import com.cookiejarapps.android.smartcookieweb.Components
import com.cookiejarapps.android.smartcookieweb.settings.activity.SettingsActivity
import com.cookiejarapps.android.smartcookieweb.settings.fragment.AboutSettingsFragment
import com.cookiejarapps.android.smartcookieweb.settings.fragment.GeneralSettingsFragment
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [(AppModule::class)])
interface AppComponent {

    @Component.Builder
    interface Builder {

        @BindsInstance
        fun application(application: Application): Builder

        fun build(): AppComponent
    }

    fun inject(activity: BrowserActivity)

    fun inject(activity: SettingsActivity)

    fun inject(generalSettingsFragment: GeneralSettingsFragment)

    fun inject(aboutSettingsFragment: AboutSettingsFragment)

    fun inject(app: BrowserApp)

    fun inject(components: Components)
}
