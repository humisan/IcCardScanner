package lol.hanyuu.iccardscanner.data.updater

import android.util.Log
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

data class UpdateInfo(val versionCode: Int, val downloadUrl: String)

@Singleton
class GithubUpdateChecker @Inject constructor() {

    fun checkForUpdate(currentVersionCode: Int): UpdateInfo? {
        return try {
            val conn = URL(RELEASES_LATEST_URL).openConnection() as HttpURLConnection
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            val body = conn.inputStream.bufferedReader().readText()
            conn.disconnect()

            val json = JSONObject(body)
            val tagName = json.getString("tag_name") // "v1042"
            val remoteVersionCode = tagName.removePrefix("v").toIntOrNull()
            if (remoteVersionCode == null || remoteVersionCode <= currentVersionCode) return null

            val assets = json.getJSONArray("assets")
            if (assets.length() == 0) return null
            val downloadUrl = assets.getJSONObject(0).getString("browser_download_url")
            UpdateInfo(remoteVersionCode, downloadUrl)
        } catch (e: Exception) {
            Log.w(TAG, "Update check failed: ${e.message}")
            null
        }
    }

    private companion object {
        const val TAG = "GithubUpdateChecker"
        const val RELEASES_LATEST_URL =
            "https://api.github.com/repos/humisan/IcCardScanner/releases/latest"
    }
}
