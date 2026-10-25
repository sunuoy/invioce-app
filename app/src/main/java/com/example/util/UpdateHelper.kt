package com.example.util

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object UpdateHelper {
    private const val LATEST_RELEASE_URL = "https://api.github.com/repos/sunuoy/invioce-app/releases/latest"

    data class UpdateInfo(
        val isUpdateAvailable: Boolean,
        val latestVersion: String,
        val currentVersion: String,
        val releaseNotes: String,
        val downloadUrl: String,
        val htmlUrl: String
    )

    suspend fun checkForUpdates(): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val url = URL(LATEST_RELEASE_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
            connection.setRequestProperty("User-Agent", "Invoice-Easy-App")

            if (connection.responseCode == 200) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(responseText)
                val tagName = json.optString("tag_name", "")
                val htmlUrl = json.optString("html_url", "")
                val body = json.optString("body", "")

                // Find apk download url in assets matching the current build type (debug vs release)
                var downloadUrl = ""
                val assets = json.optJSONArray("assets")
                if (assets != null) {
                    val preferDebug = BuildConfig.DEBUG
                    var fallbackUrl = ""
                    var preferredUrl = ""

                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val name = asset.optString("name", "")
                        val assetUrl = asset.optString("browser_download_url", "")
                        if (name.endsWith(".apk", ignoreCase = true)) {
                            if (fallbackUrl.isEmpty()) {
                                fallbackUrl = assetUrl
                            }
                            val isDebugAsset = name.contains("debug", ignoreCase = true)
                            if (preferDebug && isDebugAsset) {
                                preferredUrl = assetUrl
                                break
                            } else if (!preferDebug && !isDebugAsset) {
                                preferredUrl = assetUrl
                                break
                            }
                        }
                    }
                    downloadUrl = if (preferredUrl.isNotEmpty()) preferredUrl else fallbackUrl
                }

                // Fallback to htmlUrl if no apk in assets
                if (downloadUrl.isEmpty()) {
                    downloadUrl = htmlUrl
                }

                val currentVersion = BuildConfig.VERSION_NAME
                val hasUpdate = isNewerVersion(currentVersion, tagName)

                UpdateInfo(
                    isUpdateAvailable = hasUpdate,
                    latestVersion = tagName,
                    currentVersion = currentVersion,
                    releaseNotes = body,
                    downloadUrl = downloadUrl,
                    htmlUrl = htmlUrl
                )
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun isNewerVersion(current: String, latest: String): Boolean {
        val currentClean = current.trim().removePrefix("v")
        val latestClean = latest.trim().removePrefix("v")
        val currentParts = currentClean.split(".").mapNotNull { it.toIntOrNull() }
        val latestParts = latestClean.split(".").mapNotNull { it.toIntOrNull() }
        for (i in 0 until minOf(currentParts.size, latestParts.size)) {
            if (latestParts[i] > currentParts[i]) return true
            if (latestParts[i] < currentParts[i]) return false
        }
        return latestParts.size > currentParts.size
    }
}
