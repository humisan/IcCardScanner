package lol.hanyuu.iccardscanner.data.updater

import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val assetName: String,
    val assetSize: Long,
    val downloadUrl: String
)

@Singleton
class GithubUpdateChecker @Inject constructor() {

    fun checkForUpdate(currentVersionCode: Int): UpdateInfo? {
        Log.d(TAG, "checkForUpdate: current=$currentVersionCode url=$RELEASES_LATEST_URL")
        return try {
            val conn = URL(RELEASES_LATEST_URL).openConnection() as HttpURLConnection
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            val httpCode = conn.responseCode
            if (httpCode != 200) {
                val err = conn.errorStream?.bufferedReader()?.readText() ?: "(no body)"
                Log.w(TAG, "HTTP $httpCode: $err")
                conn.disconnect()
                return null
            }
            val body = conn.inputStream.bufferedReader().readText()
            conn.disconnect()
            Log.d(TAG, "response body (first 200): ${body.take(200)}")

            val json = JSONObject(body)
            val tagName = json.getString("tag_name")
            Log.d(TAG, "tag_name=$tagName")
            val remoteVersionCode = parseVersionCode(tagName)
            if (remoteVersionCode == null) {
                Log.w(TAG, "Cannot parse versionCode from tag_name=$tagName")
                return null
            }
            Log.d(TAG, "remote=$remoteVersionCode current=$currentVersionCode")
            if (remoteVersionCode <= currentVersionCode) {
                Log.d(TAG, "Already up to date.")
                return null
            }

            val asset = findApkAsset(json.getJSONArray("assets"))
            if (asset == null) {
                Log.w(TAG, "Release has no APK asset")
                return null
            }
            val assetName = asset.getString("name")
            val assetSize = asset.optLong("size", -1L)
            val downloadUrl = asset.getString("browser_download_url")
            if (!downloadUrl.startsWith("https://github.com/")) {
                Log.w(TAG, "Unexpected APK download host: $downloadUrl")
                return null
            }
            Log.d(TAG, "update available: v$remoteVersionCode asset=$assetName size=$assetSize url=$downloadUrl")
            UpdateInfo(
                versionCode = remoteVersionCode,
                versionName = tagName,
                assetName = assetName,
                assetSize = assetSize,
                downloadUrl = downloadUrl
            )
        } catch (e: Exception) {
            Log.e(TAG, "Update check failed", e)
            null
        }
    }

    private fun parseVersionCode(tagName: String): Int? =
        Regex("""\d+""").find(tagName)?.value?.toIntOrNull()

    private fun findApkAsset(assets: JSONArray): JSONObject? {
        for (i in 0 until assets.length()) {
            val asset = assets.getJSONObject(i)
            val name = asset.optString("name")
            if (name.endsWith(".apk", ignoreCase = true)) return asset
        }
        return null
    }

    private companion object {
        const val TAG = "GithubUpdateChecker"
        const val RELEASES_LATEST_URL =
            "https://api.github.com/repos/humisan/IcCardScanner/releases/latest"
    }
}
