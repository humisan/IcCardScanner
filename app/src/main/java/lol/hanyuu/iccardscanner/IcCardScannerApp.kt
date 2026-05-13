package lol.hanyuu.iccardscanner

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import dagger.hilt.android.HiltAndroidApp
import lol.hanyuu.iccardscanner.data.alert.BalanceAlertManager
import lol.hanyuu.iccardscanner.ui.components.StationNameResolver

@HiltAndroidApp
class IcCardScannerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        Thread { StationNameResolver.preWarm(this) }.start()
    }

    private fun createNotificationChannels() {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(
                BalanceAlertManager.CHANNEL_ID,
                "残高アラート",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "残高がしきい値を下回ったときに通知します"
            }
        )
    }
}
