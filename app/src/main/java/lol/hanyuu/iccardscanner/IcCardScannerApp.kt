package lol.hanyuu.iccardscanner

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import lol.hanyuu.iccardscanner.ui.components.StationNameResolver

@HiltAndroidApp
class IcCardScannerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Thread { StationNameResolver.preWarm(this) }.start()
    }
}
