package com.example.meezan360

import android.app.Activity
import android.app.Application.ActivityLifecycleCallbacks
import android.content.Context
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresApi
import com.aheaditec.talsec_security.security.api.Talsec
import com.aheaditec.talsec_security.security.api.TalsecConfig
import com.aheaditec.talsec_security.security.api.ThreatListener
import com.example.meezan360.di.appModule
import com.example.meezan360.di.dataModule
import com.example.meezan360.security.EncryptionKeyStoreImpl
import com.example.meezan360.security.logger.SecureLogger
import com.example.meezan360.utils.InternetHelper
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import timber.log.Timber
import android.app.Application
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import com.example.meezan360.datamodule.local.SharedPreferencesManager
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.GlobalContext
import www.sanju.motiontoast.MotionToast
import www.sanju.motiontoast.MotionToastStyle
import java.security.MessageDigest
import kotlin.system.exitProcess
import com.example.meezan360.BuildConfig
import com.example.meezan360.network.BaseUrlManager

object ModuleInitializer: ThreatListener.ThreatDetected  {


    lateinit var internetHelper: InternetHelper
    val encryptionKeyStore = EncryptionKeyStoreImpl.instance

    private lateinit var appContext: Context
    // Initialize Koin
    fun initKoin(context: Context) {
        startKoin {
            androidContext(context)
            modules(appModule, dataModule)
        }
    }

    fun setContext(context: Context) {
        appContext = context
    }
    fun getAppConfigVariables(baseUrl: String) {
        BaseUrlManager.setBaseUrl(baseUrl)
    }
    // Set up logging
    fun setupLogging(context: Context) {
        SecureLogger.init(context)
        try {
            SecureLogger.processLogcat(context)
        } catch (e: Exception) {
            Timber.tag("ModuleInitializer").e(e, "Error processing logcat")
        }
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(ReleaseTree(context))
        }
    }


    fun registerActivityLifecycleCallbacks(context: Context) {
        val appContext = context.applicationContext as? Application
        appContext?.registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                changeStatusBarColor(activity)
            }

            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }


    private fun changeStatusBarColor(activity: Activity) {
        val colorId = R.color.purple_bar
        try {
            activity.window.statusBarColor = activity.resources.getColor(colorId)
        } catch (e: Resources.NotFoundException) {
            Log.e("ModuleInitializer", "Resource not found: $colorId", e)
            activity.window.statusBarColor = activity.resources.getColor(android.R.color.black)
        }
    }
    fun calculateHashCertificate(signature: Signature): String {
        val algorithm:String = "SHA-256"
        val hash = MessageDigest.getInstance(algorithm).run {
            digest(signature.toByteArray())
        }
        return Base64.encodeToString(hash, Base64.NO_WRAP)
    }
    @RequiresApi(Build.VERSION_CODES.P)
    fun getApkSigningCertificatesHash(packageInfo: PackageInfo):List<String>{
        val signingHashes = mutableListOf<String>()
        packageInfo.signingInfo.apply {
            if (hasMultipleSigners()) {
                apkContentsSigners.forEach {
                    signingHashes.add(
                        calculateHashCertificate(it)
                    )
                }
            } else {
                signingCertificateHistory.forEach {
                    signingHashes.add(
                        calculateHashCertificate(it)
                    )
                }
            }
        }
        return signingHashes
    }
    private fun getPkgInfo(context: Context): PackageInfo? {
        val packageInfo = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_SIGNING_CERTIFICATES
        )
        return packageInfo
    }
     @RequiresApi(Build.VERSION_CODES.P)
     fun initTalsec(context: Context) {
        val packageInfo = getPkgInfo(context)
        var expectedSigningCertificateHashBase64 = arrayOf<String>()
        if(packageInfo!=null)
            expectedSigningCertificateHashBase64 = getApkSigningCertificatesHash(packageInfo).toTypedArray()
        val config = TalsecConfig(
            BuildConfig.LIBRARY_PACKAGE_NAME,
            expectedSigningCertificateHashBase64,
            "uhfsolution@gmail.com",
            arrayOf(
                ""
            ),
            true
        )

        ThreatListener(this).registerListener(context)
        Talsec.start(context, config)
    }

    fun saveToken(token: String) {
        val sharedPreferencesManager: SharedPreferencesManager = GlobalContext.get().get()
        sharedPreferencesManager.saveToken(token)
    }

    private fun exitApp(message:String) {

        if(!BuildConfig.DEBUG){
            MotionToast.createColorToast(appContext,
                "Failed",
                message,
                MotionToastStyle.ERROR,
                MotionToast.GRAVITY_TOP,
                MotionToast.LONG_DURATION,
                ResourcesCompat.getFont(appContext,R.font.montserrat_medium))

            Handler(Looper.myLooper()!!).postDelayed({
                exitProcess(0)
            },2000)
        }
    }


    override fun onRootDetected() {
        exitApp("Your Device is rooted")
    }

    override fun onDebuggerDetected() {
        exitApp("App is detected in debug mode")
    }

    override fun onEmulatorDetected() {
        exitApp("App is detected in emulator")
    }

    override fun onTamperDetected() {
//        exitApp("App is detected as tampered")
    }

    override fun onUntrustedInstallationSourceDetected() {
        exitApp("App is detected on untrusted installation source")
    }

    override fun onHookDetected() {
        exitApp("Hook detected")
    }

    override fun onDeviceBindingDetected() {
        exitApp("Device Binding Detected detected")
    }

    override fun onObfuscationIssuesDetected() {
        exitApp("Obfuscation Issues Detected")
    }

}
