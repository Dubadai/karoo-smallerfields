package com.daviddokakis.smallerfields

import com.dubadai.karoo.common.*
import android.util.Log
import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.extension.DataTypeImpl
import io.hammerhead.karooext.extension.KarooExtension
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import timber.log.Timber

/** Entry point the Karoo system binds to: SmallerFields 1–4. */
class SmallerFieldsExtension : KarooExtension(EXTENSION_ID, "1") {

    private lateinit var karoo: KarooSystemService
    private lateinit var model: SmallerFieldsSession
    private val updateScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val types: List<DataTypeImpl> by lazy {
        (0 until Settings.SLOTS).map { SmallerFieldsDataType(extension, it, model) }
    }

    override fun onCreate() {
        super.onCreate()
        Log.i("BuildStamp", "$packageName ${BuildConfig.VERSION_NAME} build ${BuildConfig.BUILD_STAMP}")
        if (Timber.treeCount == 0) Timber.plant(Timber.DebugTree())
        karoo = KarooSystemService(this)
        model = SmallerFieldsSession(karoo)
        karoo.connect { connected -> Timber.i("Karoo system connected: $connected") }
        model.start()
        // Not riding: tell the rider when a newer release is on GitHub (see UpdateCheck).
        UpdateCheck(this, karoo, appName = "SmallerFields").start(updateScope)
    }

    override fun onDestroy() {
        updateScope.cancel()
        model.stop()
        karoo.disconnect()
        super.onDestroy()
    }

    companion object {
        const val EXTENSION_ID = "smallerfields"
    }
}
