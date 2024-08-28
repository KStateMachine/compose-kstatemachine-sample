package org.example.project

import android.app.Application
import co.touchlab.kermit.Logger
import co.touchlab.kermit.LogcatWriter

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Logger.setLogWriters(LogcatWriter())
    }
}
