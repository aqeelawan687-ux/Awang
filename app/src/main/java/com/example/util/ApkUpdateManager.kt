package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import com.example.BuildConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

/**
 * Information regarding the latest release on GitHub.
 */
data class UpdateInfo(
    val hasUpdate: Boolean,
    val currentVersionCode: Int,
    val currentVersionName: String,
    val latestVersionCode: Int,
    val latestVersionName: String,
    val downloadUrl: String,
    val releaseNotes: String = "",
    val releaseTagName: String = "",
    val errorMessage: String? = null
)

/**
 * Sealed interface representing UI & Background states of the update lifecycle.
 */
sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data class UpdateAvailable(val info: UpdateInfo) : UpdateState
    data class Downloading(
        val info: UpdateInfo,
        val progressPercent: Int,
        val bytesDownloaded: Long,
        val totalBytes: Long
    ) : UpdateState
    data class Downloaded(val info: UpdateInfo, val apkFile: File) : UpdateState
    data class Installing(val info: UpdateInfo, val apkFile: File) : UpdateState
    data class UpToDate(val currentVersionCode: Int, val currentVersionName: String) : UpdateState
    data class Error(val message: String, val canRetry: Boolean = true, val info: UpdateInfo? = null) : UpdateState
}

/**
 * Production-ready Auto-Update Manager for Aqeel Rider via GitHub Releases.
 */
object ApkUpdateManager {

    private const val TAG = "ApkUpdateManager"

    // GitHub repository configuration
    const val GITHUB_REPO_OWNER = "aqeelawan687-ux"
    const val GITHUB_REPO_NAME = "Aqeel"
    const val GITHUB_RELEASE_API_URL = "https://api.github.com/repos/$GITHUB_REPO_OWNER/$GITHUB_REPO_NAME/releases/latest"
    const val PERMANENT_APK_DOWNLOAD_URL = "https://github.com/$GITHUB_REPO_OWNER/$GITHUB_REPO_NAME/releases/latest/download/app-release.apk"

    // Preferences key for 6-hour check throttling
    private const val PREFS_NAME = "aqeel_rider_update_prefs"
    private const val KEY_LAST_CHECK_TIME = "last_update_check_time_ms"
    private const val CHECK_COOLDOWN_MS = 6 * 60 * 60 * 1000L // 6 hours

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    // Session-level flag to avoid repeated auto-dialog popups when user clicks "Later"
    var isDismissedForSession: Boolean = false
        private set

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    /**
     * Resets session dismissal (e.g. when user manually opens update check).
     */
    fun resetSessionDismissal() {
        isDismissedForSession = false
    }

    /**
     * Dismisses the update dialog for the current app session.
     */
    fun dismissForSession() {
        isDismissedForSession = true
    }

    /**
     * Sets the state back to Idle.
     */
    fun resetState() {
        _updateState.value = UpdateState.Idle
    }

    /**
     * Checks GitHub Releases API for the latest version.
     *
     * @param context Application context
     * @param isManualCheck If true, bypasses the 6-hour throttle cooldown.
     */
    suspend fun checkLatestUpdate(
        context: Context,
        isManualCheck: Boolean = false
    ): UpdateInfo = withContext(Dispatchers.IO) {
        val currentCode = BuildConfig.VERSION_CODE
        val currentName = BuildConfig.VERSION_NAME

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastCheckTime = prefs.getLong(KEY_LAST_CHECK_TIME, 0L)
        val currentTime = System.currentTimeMillis()

        if (!isManualCheck && (currentTime - lastCheckTime) < CHECK_COOLDOWN_MS) {
            Log.d(TAG, "Update check throttled (last checked ${(currentTime - lastCheckTime) / 1000}s ago).")
            val cachedInfo = UpdateInfo(
                hasUpdate = false,
                currentVersionCode = currentCode,
                currentVersionName = currentName,
                latestVersionCode = currentCode,
                latestVersionName = currentName,
                downloadUrl = PERMANENT_APK_DOWNLOAD_URL,
                releaseNotes = "App is running the latest version."
            )
            return@withContext cachedInfo
        }

        if (isManualCheck) {
            resetSessionDismissal()
        }

        _updateState.value = UpdateState.Checking
        Log.d(TAG, "Requesting GitHub Releases API: $GITHUB_RELEASE_API_URL (manualCheck=$isManualCheck)")

        try {
            val request = Request.Builder()
                .url(GITHUB_RELEASE_API_URL)
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "AqeelRiderApp/$currentName")
                .build()

            val response = httpClient.newCall(request).execute()
            val statusCode = response.code
            Log.d(TAG, "GitHub Releases API response: HTTP $statusCode for URL: $GITHUB_RELEASE_API_URL")

            // Save last check timestamp on attempt
            prefs.edit().putLong(KEY_LAST_CHECK_TIME, currentTime).apply()

            if (response.isSuccessful) {
                val jsonStr = response.body?.string()
                if (!jsonStr.isNullOrEmpty()) {
                    val info = parseReleaseJson(jsonStr, currentCode, currentName)
                    Log.d(TAG, "Successfully parsed release for tag '${info.releaseTagName}': latestCode=${info.latestVersionCode}, latestName=${info.latestVersionName}, hasUpdate=${info.hasUpdate}, assetUrl=${info.downloadUrl}")

                    if (info.hasUpdate) {
                        _updateState.value = UpdateState.UpdateAvailable(info)
                    } else {
                        _updateState.value = UpdateState.UpToDate(currentCode, currentName)
                    }
                    return@withContext info
                } else {
                    Log.e(TAG, "GitHub API response body was empty for URL: $GITHUB_RELEASE_API_URL")
                    val errorMsg = "GitHub release data was empty."
                    val info = UpdateInfo(
                        hasUpdate = false,
                        currentVersionCode = currentCode,
                        currentVersionName = currentName,
                        latestVersionCode = currentCode,
                        latestVersionName = currentName,
                        downloadUrl = PERMANENT_APK_DOWNLOAD_URL,
                        errorMessage = errorMsg
                    )
                    _updateState.value = UpdateState.Error(errorMsg, canRetry = true, info = info)
                    return@withContext info
                }
            } else if (statusCode == 404) {
                val errorMsg = "GitHub repository/release API returned HTTP 404 (Not Found). No published releases found on https://github.com/$GITHUB_REPO_OWNER/$GITHUB_REPO_NAME or repository is private."
                Log.e(TAG, "GitHub repository/release API returned HTTP 404 for URL: $GITHUB_RELEASE_API_URL. Error: $errorMsg")
                val info = UpdateInfo(
                    hasUpdate = false,
                    currentVersionCode = currentCode,
                    currentVersionName = currentName,
                    latestVersionCode = currentCode,
                    latestVersionName = currentName,
                    downloadUrl = PERMANENT_APK_DOWNLOAD_URL,
                    releaseNotes = "No release found on GitHub.",
                    errorMessage = "GitHub repository/release API returned HTTP 404 (URL: $GITHUB_RELEASE_API_URL)"
                )
                _updateState.value = UpdateState.Error(errorMsg, canRetry = true, info = info)
                return@withContext info
            } else if (statusCode == 403 || statusCode == 429) {
                val errorMsg = "GitHub API rate limit reached (HTTP $statusCode). Please try again later."
                Log.w(TAG, errorMsg)
                val info = UpdateInfo(
                    hasUpdate = false,
                    currentVersionCode = currentCode,
                    currentVersionName = currentName,
                    latestVersionCode = currentCode,
                    latestVersionName = currentName,
                    downloadUrl = PERMANENT_APK_DOWNLOAD_URL,
                    errorMessage = errorMsg
                )
                _updateState.value = UpdateState.Error(errorMsg, canRetry = true, info = info)
                return@withContext info
            } else {
                val errorMsg = "GitHub server responded with HTTP status $statusCode for URL: $GITHUB_RELEASE_API_URL"
                Log.e(TAG, errorMsg)
                val info = UpdateInfo(
                    hasUpdate = false,
                    currentVersionCode = currentCode,
                    currentVersionName = currentName,
                    latestVersionCode = currentCode,
                    latestVersionName = currentName,
                    downloadUrl = PERMANENT_APK_DOWNLOAD_URL,
                    errorMessage = errorMsg
                )
                _updateState.value = UpdateState.Error(errorMsg, canRetry = true, info = info)
                return@withContext info
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception connecting to GitHub Releases API ($GITHUB_RELEASE_API_URL): ${e.message}", e)
            val netMsg = when (e) {
                is UnknownHostException, is IOException -> "Network error: Please check your internet connection."
                else -> "Update check failed: ${e.localizedMessage ?: "Unknown error"}"
            }
            val info = UpdateInfo(
                hasUpdate = false,
                currentVersionCode = currentCode,
                currentVersionName = currentName,
                latestVersionCode = currentCode,
                latestVersionName = currentName,
                downloadUrl = PERMANENT_APK_DOWNLOAD_URL,
                errorMessage = netMsg
            )
            _updateState.value = UpdateState.Error(netMsg, canRetry = true, info = info)
            return@withContext info
        }
    }

    /**
     * Parses GitHub release JSON and constructs UpdateInfo.
     * Uses the actual `browser_download_url` of the APK asset in the release.
     * Does NOT depend on a hardcoded APK filename.
     */
    fun parseReleaseJson(jsonStr: String, currentCode: Int, currentName: String): UpdateInfo {
        val jsonObj = JSONObject(jsonStr)
        val tagName = jsonObj.optString("tag_name", "").trim()
        val releaseName = jsonObj.optString("name", "").trim()
        val bodyNotes = jsonObj.optString("body", "Bug fixes and performance updates.").trim()

        // Locate the actual APK asset from assets array using its browser_download_url
        // Only accept signed release APK assets; explicitly reject debug or unsigned APKs
        var resolvedDownloadUrl: String? = null
        var resolvedApkName: String? = null
        val assets = jsonObj.optJSONArray("assets")
        if (assets != null) {
            // First pass: look for exact "app-release.apk"
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                val name = asset.optString("name", "")
                val url = asset.optString("browser_download_url", "")
                if (name.equals("app-release.apk", ignoreCase = true) && isValidDownloadUrl(url)) {
                    resolvedDownloadUrl = url
                    resolvedApkName = name
                    break
                }
            }

            // Second pass: accept signed release APK assets ending in ".apk" (excluding debug/unsigned)
            if (resolvedDownloadUrl == null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.optString("name", "")
                    val url = asset.optString("browser_download_url", "")
                    val isExcluded = name.contains("debug", ignoreCase = true) ||
                            name.contains("unsigned", ignoreCase = true)
                    if (name.endsWith(".apk", ignoreCase = true) && !isExcluded && isValidDownloadUrl(url)) {
                        resolvedDownloadUrl = url
                        resolvedApkName = name
                        break
                    }
                }
            }
        }

        if (resolvedDownloadUrl == null) {
            Log.w(TAG, "No APK asset found in release assets for '$tagName', using permanent fallback URL.")
            resolvedDownloadUrl = PERMANENT_APK_DOWNLOAD_URL
        } else {
            Log.d(TAG, "Found release APK asset: '$resolvedApkName' -> $resolvedDownloadUrl")
        }

        val extractedVersionCode = parseVersionCode(
            tag = tagName,
            releaseName = releaseName,
            body = bodyNotes,
            fallbackCode = currentCode
        )
        val cleanVersionName = parseVersionName(tag = tagName, releaseName = releaseName)

        val hasUpdate = isNewerVersion(
            currentCode = currentCode,
            currentName = currentName,
            latestCode = extractedVersionCode,
            latestName = cleanVersionName
        )

        return UpdateInfo(
            hasUpdate = hasUpdate,
            currentVersionCode = currentCode,
            currentVersionName = currentName,
            latestVersionCode = extractedVersionCode,
            latestVersionName = cleanVersionName.ifEmpty { currentName },
            downloadUrl = resolvedDownloadUrl,
            releaseNotes = bodyNotes,
            releaseTagName = tagName
        )
    }

    /**
     * Downloads the APK securely from the update's downloadUrl to app-specific storage with live progress.
     */
    suspend fun downloadApk(
        context: Context,
        updateInfo: UpdateInfo,
        onProgress: (progress: Int) -> Unit = {}
    ): File? = withContext(Dispatchers.IO) {
        val downloadUrl = updateInfo.downloadUrl
        if (!isValidDownloadUrl(downloadUrl)) {
            Log.e(TAG, "Download rejected: Invalid download URL '$downloadUrl'")
            _updateState.value = UpdateState.Error("Invalid download URL.", canRetry = false, info = updateInfo)
            return@withContext null
        }

        _updateState.value = UpdateState.Downloading(updateInfo, 0, 0L, 0L)
        Log.d(TAG, "Starting APK download from $downloadUrl for v${updateInfo.latestVersionName}")

        try {
            val request = Request.Builder()
                .url(downloadUrl)
                .header("User-Agent", "AqeelRiderApp/${BuildConfig.VERSION_NAME}")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.e(TAG, "Download failed with HTTP status ${response.code}")
                _updateState.value = UpdateState.Error("Download failed with HTTP ${response.code}", canRetry = true, info = updateInfo)
                return@withContext null
            }

            val body = response.body
            if (body == null) {
                Log.e(TAG, "Download response body was empty")
                _updateState.value = UpdateState.Error("Download response was empty.", canRetry = true, info = updateInfo)
                return@withContext null
            }

            val contentLength = body.contentLength()

            // Save APK to app-specific external files dir or internal cache dir
            val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?: context.cacheDir
            val updatesDir = File(downloadDir, "updates")
            if (!updatesDir.exists()) {
                updatesDir.mkdirs()
            }

            val apkFile = File(updatesDir, "aqeel_rider_v${updateInfo.latestVersionName}.apk")
            if (apkFile.exists()) {
                apkFile.delete()
            }

            val inputStream = body.byteStream()
            val outputStream = FileOutputStream(apkFile)
            val buffer = ByteArray(16 * 1024)
            var bytesRead: Int
            var totalRead: Long = 0

            try {
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    if (!coroutineContext.isActive) {
                        outputStream.close()
                        inputStream.close()
                        if (apkFile.exists()) apkFile.delete()
                        _updateState.value = UpdateState.Idle
                        return@withContext null
                    }

                    outputStream.write(buffer, 0, bytesRead)
                    totalRead += bytesRead

                    val progress = if (contentLength > 0) {
                        ((totalRead * 100) / contentLength).toInt().coerceIn(0, 100)
                    } else {
                        0
                    }

                    _updateState.value = UpdateState.Downloading(updateInfo, progress, totalRead, contentLength)
                    withContext(Dispatchers.Main) {
                        onProgress(progress)
                    }
                }

                outputStream.flush()
            } finally {
                outputStream.close()
                inputStream.close()
            }

            // Verify downloaded APK file exists and has valid size (> 100 KB)
            if (!apkFile.exists() || apkFile.length() < 100 * 1024) {
                Log.e(TAG, "Downloaded file is incomplete: exists=${apkFile.exists()}, length=${apkFile.length()}")
                if (apkFile.exists()) apkFile.delete()
                _updateState.value = UpdateState.Error("Downloaded APK is corrupted or incomplete.", canRetry = true, info = updateInfo)
                return@withContext null
            }

            Log.d(TAG, "APK successfully downloaded to ${apkFile.absolutePath} (${apkFile.length()} bytes)")
            _updateState.value = UpdateState.Downloaded(updateInfo, apkFile)
            return@withContext apkFile
        } catch (e: CancellationException) {
            Log.d(TAG, "Download cancelled")
            _updateState.value = UpdateState.Idle
            return@withContext null
        } catch (e: Exception) {
            Log.e(TAG, "Exception during APK download: ${e.message}", e)
            val errorMsg = "Download error: ${e.localizedMessage ?: "Failed to download update"}"
            _updateState.value = UpdateState.Error(errorMsg, canRetry = true, info = updateInfo)
            return@withContext null
        }
    }

    /**
     * Checks if Unknown App Installs permission is granted (Android 8.0+ / API 26+)
     */
    fun canInstallUnknownApps(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    /**
     * Directs the user to the system settings screen to allow installing unknown apps.
     */
    fun openUnknownAppSourcesSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to launch unknown app sources settings: ${e.message}", e)
            }
        }
    }

    /**
     * Launches the Android Package Installer with the downloaded APK using FileProvider.
     */
    fun installApk(context: Context, apkFile: File) {
        if (!apkFile.exists()) {
            Log.e(TAG, "Cannot install: APK file does not exist at ${apkFile.absolutePath}")
            _updateState.value = UpdateState.Error("APK file not found.", canRetry = true)
            return
        }

        try {
            Log.d(TAG, "Initiating package install for ${apkFile.absolutePath}")
            val authority = "${context.packageName}.fileprovider"
            val apkUri: Uri = FileProvider.getUriForFile(context, authority, apkFile)

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }

            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch installer: ${e.message}", e)
            _updateState.value = UpdateState.Error("Failed to launch installer: ${e.localizedMessage}", canRetry = true)
        }
    }

    /**
     * Determines whether the latest version represents a newer release.
     */
    fun isNewerVersion(
        currentCode: Int,
        currentName: String = "",
        latestCode: Int,
        latestName: String = ""
    ): Boolean {
        if (latestCode > currentCode) {
            return true
        }
        if (latestCode < currentCode) {
            return false
        }
        if (currentName.isNotBlank() && latestName.isNotBlank()) {
            return compareSemVer(latestName, currentName) > 0
        }
        return false
    }

    /**
     * Compares two semantic version strings (e.g. "1.3.1" vs "1.3.0").
     */
    fun compareSemVer(v1: String, v2: String): Int {
        val clean1 = v1.trim().removePrefix("v").removePrefix("V").trim()
        val clean2 = v2.trim().removePrefix("v").removePrefix("V").trim()

        val parts1 = clean1.split(".").mapNotNull { it.toIntOrNull() }
        val parts2 = clean2.split(".").mapNotNull { it.toIntOrNull() }

        val maxLen = maxOf(parts1.size, parts2.size)
        for (i in 0 until maxLen) {
            val p1 = parts1.getOrElse(i) { 0 }
            val p2 = parts2.getOrElse(i) { 0 }
            if (p1 != p2) {
                return p1.compareTo(p2)
            }
        }
        return 0
    }

    /**
     * Extracts version code from tag name (e.g. "v1.3.1" -> 131), release body, or release name.
     */
    fun parseVersionCode(
        tag: String,
        releaseName: String = "",
        body: String = "",
        fallbackCode: Int = 0
    ): Int {
        // Priority 1: Check release body notes for explicit "versionCode: 131", "versionCode = 131"
        val bodyRegex = Regex("""version[_\s-]*code[^\d]*(\d+)""", RegexOption.IGNORE_CASE)
        val bodyMatch = bodyRegex.find(body)
        if (bodyMatch != null) {
            val code = bodyMatch.groupValues[1].toIntOrNull()
            if (code != null && code > 0) {
                return code
            }
        }

        // Priority 2: Check release title / name
        val nameMatch = bodyRegex.find(releaseName)
        if (nameMatch != null) {
            val code = nameMatch.groupValues[1].toIntOrNull()
            if (code != null && code > 0) {
                return code
            }
        }

        // Priority 3: Parse semantic version tag "vX.Y.Z" -> (X * 100) + (Y * 10) + Z
        val sourceStr = tag.ifBlank { releaseName }
        val cleanTag = sourceStr.trim().removePrefix("v").removePrefix("V").trim()
        val parts = cleanTag.split(".").mapNotNull { it.toIntOrNull() }
        if (parts.size >= 2) {
            val major = parts.getOrElse(0) { 1 }
            val minor = parts.getOrElse(1) { 0 }
            val patch = parts.getOrElse(2) { 0 }
            return (major * 100) + (minor * 10) + patch
        } else if (parts.size == 1 && parts[0] > 0) {
            return parts[0]
        }

        return fallbackCode
    }

    /**
     * Parses clean version name from release tag or release name (e.g. "v1.3.1" -> "1.3.1").
     */
    fun parseVersionName(tag: String, releaseName: String = ""): String {
        val raw = tag.ifBlank { releaseName }
        val regex = Regex("""\bv?(\d+\.\d+(?:\.\d+)?)\b""", RegexOption.IGNORE_CASE)
        val match = regex.find(raw)
        if (match != null) {
            return match.groupValues[1]
        }
        return raw.trim().removePrefix("v").removePrefix("V").trim()
    }

    /**
     * Validates that download URL belongs to GitHub and HTTPS.
     */
    fun isValidDownloadUrl(url: String): Boolean {
        return url.startsWith("https://github.com/") ||
                url.startsWith("https://objects.githubusercontent.com/") ||
                url.startsWith("https://api.github.com/") ||
                url.startsWith("https://raw.githubusercontent.com/")
    }
}
