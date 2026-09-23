package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val releaseNotes: String,
    val downloadUrl: String,
    val isMandatory: Boolean = false
)

sealed class UpdateState {
    object Idle : UpdateState()
    object Checking : UpdateState()
    data class UpdateAvailable(val info: UpdateInfo) : UpdateState()
    data class Downloading(val progress: Int) : UpdateState()
    data class Downloaded(val file: File) : UpdateState()
    object Installing : UpdateState()
    object UpToDate : UpdateState()
    data class Error(val message: String) : UpdateState()
}

class ApkUpdateManager(private val context: Context) {

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    companion object {
        const val GITHUB_RELEASES_API = "https://api.github.com/repos/aqeelawan687-ux/Awang/releases/latest"
        private const val PREFS_NAME = "apk_update_prefs"
        private const val KEY_LAST_CHECK = "last_background_check_time"
        private const val THROTTLE_INTERVAL_MS = 6 * 60 * 60 * 1000L // 6 hours throttle for auto check
    }

    /**
     * Checks for app updates from GitHub Releases API.
     * @param isManual If true, bypasses 6-hour throttle and shows toast/dialog even if up to date or error occurs.
     */
    suspend fun checkLatestUpdate(
        isManual: Boolean = false,
        currentVersionCode: Int = BuildConfig.VERSION_CODE
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()

        if (!isManual) {
            val lastCheck = prefs.getLong(KEY_LAST_CHECK, 0L)
            if (now - lastCheck < THROTTLE_INTERVAL_MS) {
                // Throttle active, keep Idle
                return
            }
        }

        _updateState.value = UpdateState.Checking
        withContext(Dispatchers.IO) {
            try {
                val url = URL(GITHUB_RELEASES_API)
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 8000
                conn.readTimeout = 8000
                conn.requestMethod = "GET"
                conn.setRequestProperty("Accept", "application/vnd.github.v3+json")
                conn.setRequestProperty("User-Agent", "AqeelRider-Android-App")

                val responseCode = conn.responseCode
                if (responseCode == 200) {
                    // Record check time on successful query
                    prefs.edit().putLong(KEY_LAST_CHECK, now).apply()

                    val text = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(text)
                    val tagName = json.optString("tag_name", "").trim()
                    val body = json.optString("body", "")
                    val releaseName = json.optString("name", tagName)

                    // Extract version name from tag (e.g., "v1.3.3" -> "1.3.3")
                    val remoteVersionName = if (tagName.startsWith("v", ignoreCase = true)) {
                        tagName.substring(1).trim()
                    } else if (tagName.isNotEmpty()) {
                        tagName
                    } else {
                        "1.3.3"
                    }

                    // Extract versionCode from release body (e.g., "**versionCode**: 133" or "versionCode: 133")
                    var remoteCode = 0
                    val codeRegex = Regex("""(?i)versionCode\s*[:=*\s]+(\d+)""")
                    val codeMatch = codeRegex.find(body)
                    if (codeMatch != null) {
                        remoteCode = codeMatch.groupValues[1].toIntOrNull() ?: 0
                    }

                    // Fallback if the release body doesn't carry an explicit
                    // versionCode line: derive a comparable code from the
                    // semantic version itself (major.minor.patch -> padded
                    // major*10000 + minor*100 + patch) instead of naively
                    // concatenating digits, which silently breaks version
                    // comparisons the moment any segment reaches two digits
                    // (e.g. "1.3.10" candidate would wrongly look smaller
                    // than "1.4.0" as "1310" vs "140" under plain concat, but
                    // compares correctly as 10310 vs 10400 here).
                    if (remoteCode == 0) {
                        val parts = remoteVersionName.split(".").mapNotNull { it.trim().toIntOrNull() }
                        remoteCode = when {
                            parts.size >= 3 -> parts[0] * 10000 + parts[1] * 100 + parts[2]
                            parts.size == 2 -> parts[0] * 10000 + parts[1] * 100
                            parts.size == 1 -> parts[0] * 10000
                            else -> 0
                        }
                    }

                    // Dynamically extract APK download URL from assets strictly matching app-release.apk
                    var apkUrl = ""
                    val assets = json.optJSONArray("assets")
                    if (assets != null) {
                        for (i in 0 until assets.length()) {
                            val asset = assets.getJSONObject(i)
                            val name = asset.optString("name", "")
                            val downloadUrl = asset.optString("browser_download_url", "")
                            if (name.equals("app-release.apk", ignoreCase = true)) {
                                apkUrl = downloadUrl
                                break
                            }
                        }
                    }

                    val releaseNotes = if (body.isNotBlank()) body else "Latest updates and performance improvements for Aqeel Rider."

                    if (remoteCode > currentVersionCode && apkUrl.isNotEmpty()) {
                        _updateState.value = UpdateState.UpdateAvailable(
                            UpdateInfo(
                                versionCode = remoteCode,
                                versionName = remoteVersionName,
                                releaseNotes = releaseNotes,
                                downloadUrl = apkUrl,
                                isMandatory = false
                            )
                        )
                    } else {
                        _updateState.value = if (isManual) UpdateState.UpToDate else UpdateState.Idle
                    }
                } else {
                    val errMsg = when (responseCode) {
                        403 -> "GitHub API rate limit reached. Please try again in a few minutes."
                        404 -> "No releases found for this repository yet."
                        else -> "GitHub API request failed (HTTP $responseCode)"
                    }
                    _updateState.value = if (isManual) UpdateState.Error(errMsg) else UpdateState.Idle
                }
            } catch (e: Exception) {
                if (isManual) {
                    _updateState.value = UpdateState.Error("Network error: ${e.message ?: "Unable to check updates"}")
                } else {
                    _updateState.value = UpdateState.Idle
                }
            }
        }
    }

    suspend fun downloadApk(downloadUrl: String) {
        _updateState.value = UpdateState.Downloading(0)
        withContext(Dispatchers.IO) {
            try {
                var currentUrl = downloadUrl
                var conn: HttpURLConnection
                var redirectCount = 0
                val maxRedirects = 5

                // Handle redirects (e.g., GitHub 302 -> AWS S3 / Azure Blob)
                while (true) {
                    val url = URL(currentUrl)
                    conn = url.openConnection() as HttpURLConnection
                    conn.instanceFollowRedirects = false
                    conn.connectTimeout = 15000
                    conn.readTimeout = 20000
                    conn.setRequestProperty("User-Agent", "AqeelRider-Android-App")
                    conn.connect()

                    val responseCode = conn.responseCode
                    if (responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
                        responseCode == HttpURLConnection.HTTP_MOVED_TEMP ||
                        responseCode == HttpURLConnection.HTTP_SEE_OTHER ||
                        responseCode == 307 || responseCode == 308) {
                        val newLocation = conn.getHeaderField("Location")
                        conn.disconnect()
                        if (!newLocation.isNullOrEmpty() && redirectCount < maxRedirects) {
                            currentUrl = newLocation
                            redirectCount++
                            continue
                        }
                    }
                    break
                }

                val finalResponseCode = conn.responseCode
                if (finalResponseCode !in 200..299) {
                    conn.disconnect()
                    _updateState.value = UpdateState.Error("Download failed with HTTP status $finalResponseCode")
                    return@withContext
                }

                val fileLength = conn.contentLength
                val outputFile = File(context.cacheDir, "update.apk")
                if (outputFile.exists()) outputFile.delete()

                val input = conn.inputStream
                val output = FileOutputStream(outputFile)

                val data = ByteArray(8192)
                var total: Long = 0
                var count: Int

                while (input.read(data).also { count = it } != -1) {
                    total += count
                    if (fileLength > 0) {
                        val progress = ((total * 100) / fileLength).toInt().coerceIn(0, 100)
                        _updateState.value = UpdateState.Downloading(progress)
                    }
                    output.write(data, 0, count)
                }

                output.flush()
                output.close()
                input.close()
                conn.disconnect()

                if (outputFile.exists() && outputFile.length() > 0) {
                    _updateState.value = UpdateState.Downloaded(outputFile)
                } else {
                    _updateState.value = UpdateState.Error("Downloaded file is empty or corrupted")
                }
            } catch (e: Exception) {
                _updateState.value = UpdateState.Error(e.message ?: "Failed to download update")
            }
        }
    }

    fun installApk(apkFile: File) {
        if (!apkFile.exists() || apkFile.length() == 0L) {
            _updateState.value = UpdateState.Error("APK file not found or invalid.")
            return
        }
        try {
            _updateState.value = UpdateState.Installing
            val apkUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            _updateState.value = UpdateState.Error("Installation error: ${e.message}")
        }
    }

    fun reset() {
        _updateState.value = UpdateState.Idle
    }
}
