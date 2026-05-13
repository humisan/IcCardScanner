package lol.hanyuu.iccardscanner.data.alert

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import lol.hanyuu.iccardscanner.R
import lol.hanyuu.iccardscanner.data.repository.HistoryRepository
import java.text.NumberFormat
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BalanceAlertManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val historyRepository: HistoryRepository
) {
    suspend fun checkAndNotify(cardIdm: String) {
        val threshold = getThreshold()
        if (threshold < 0) return
        val balance = historyRepository.getLatestBalance(cardIdm) ?: return
        if (balance >= threshold) return
        postNotification(cardIdm, balance)
    }

    fun getThreshold(): Int =
        context.getSharedPreferences(PREFS_ALERT, Context.MODE_PRIVATE)
            .getInt(KEY_THRESHOLD, -1)

    fun setThreshold(value: Int) {
        context.getSharedPreferences(PREFS_ALERT, Context.MODE_PRIVATE)
            .edit().putInt(KEY_THRESHOLD, value).apply()
    }

    private fun postNotification(cardIdm: String, balance: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
        ) return

        val formatted = "¥${NumberFormat.getNumberInstance(Locale.JAPAN).format(balance)}"
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_nfc)
            .setContentTitle("残高が少なくなっています")
            .setContentText("現在の残高: $formatted")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(cardIdm.hashCode(), notification)
    }

    companion object {
        const val PREFS_ALERT = "balance_alert"
        const val KEY_THRESHOLD = "threshold"
        const val CHANNEL_ID = "balance_alert"
    }
}
