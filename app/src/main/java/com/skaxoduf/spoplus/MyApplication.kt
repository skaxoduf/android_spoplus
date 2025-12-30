package com.skaxoduf.spoplus

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import android.content.Context

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val mode = prefs.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        AppCompatDelegate.setDefaultNightMode(mode)
    }
}
