package com.example.woodenpuppetdemo.platform

import android.app.Application
import com.pico.spatial.ui.foundation.dsl.launch
import com.example.woodenpuppetdemo.mainApp

class SpatialApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        launch(::mainApp)
    }
}
