package com.cookiejarapps.android.smartcookieweb

import android.app.Application
import com.cookiejarapps.android.smartcookieweb.di.AppComponent
import com.cookiejarapps.android.smartcookieweb.di.DaggerAppComponent
import com.cookiejarapps.android.smartcookieweb.di.injector

class App: Application() {

    lateinit var applicationComponent: AppComponent

    override fun onCreate(){
        super.onCreate()
        applicationComponent = DaggerAppComponent.builder()
            .application(this)
            .build()
        injector.inject(this)
    }
}
