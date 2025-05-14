package com.uhfsolutions.app360.main

import android.app.Application
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.meezan360.ModuleInitializer
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import com.uhfsolutions.app360.BuildConfig
import timber.log.Timber
import kotlin.math.log

class MainApplication : Application() {

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate() {
        super.onCreate()
        // Initialize modules
        ModuleInitializer.setContext(applicationContext)
        ModuleInitializer.initKoin(applicationContext)
        ModuleInitializer.setupLogging(applicationContext)
        ModuleInitializer.registerActivityLifecycleCallbacks(applicationContext)
        ModuleInitializer.initTalsec(applicationContext)
        // Initialize firebase
        configureFirebase()




    }

    private fun configureFirebase() {
        val remoteConfig = Firebase.remoteConfig


        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = 0
        }
        remoteConfig.setConfigSettingsAsync(configSettings)
        fetchRemoteConfigData(remoteConfig)
    }

    private fun fetchRemoteConfigData(remoteConfig: FirebaseRemoteConfig) {
        remoteConfig.fetchAndActivate()
            .addOnCompleteListener { task ->

                if (task.isSuccessful) {
                    // Retrieve values
                    val androidAppVersion = remoteConfig.getString("android_app_version")
                    Timber.tag("fetchRemot ").d(androidAppVersion.toString())
                    Timber.tag("fetchRemot ").d(BuildConfig.VERSION_NAME.toString())
                    var baseApiUrl = ""
                    if (BuildConfig.VERSION_NAME == androidAppVersion) {
                        baseApiUrl = remoteConfig.getString("base_api_url")
                        Timber.tag("fetchRemot2 ").d(baseApiUrl.toString())
                        ModuleInitializer.getAppConfigVariables(baseApiUrl)
                    }
                } else {
                    Log.e("FirebaseRC", "Fetch failed")
                }
            }
    }
}
