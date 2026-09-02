package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
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

    suspend fun checkLatestUpdate(
        updateJsonUrl: String = "https://raw.githubusercontent.com/aqeelawan687/AqeelRiderUpdates/main/version.json",
        currentVersionCode: Int = 133
    ) {
        _updateState.value = UpdateState.Checking
        withContext(Dispatchers.IO) {
            try {
                val url = URL(updateJsonUrl)
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                conn.requestMethod = "GET"

                if (conn.responseCode == 200) {
                    val text = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(text)
                    val remoteCode = json.optInt("versionCode", 1)
                    val remoteName = json.optString("versionName", "1.0")
                    val releaseNotes = json.optString("changelog", "Bug fixes and performance improvements")
                    val apkUrl = json.optString("apkUrl", "")
                    val isMandatory = json.optBoolean("mandatory", false)

                    if (remoteCode > currentVersionCode) {
                        _updateState.value = UpdateState.UpdateAvailable(
                            UpdateInfo(
                                versionCode = remoteCode,
                                versionName = remoteName,
                                releaseNotes = releaseNotes,
                                downloadUrl = apkUrl,
                                isMandatory = isMandatory
                            )
                        )
                    } else {
                        _updateState.value = UpdateState.UpToDate
                    }
                } else {
                    _updateState.value = UpdateState.UpToDate
                }
            } catch (_: Exception) {
                // If offline or invalid URL, keep app usable
                _updateState.value = UpdateState.Idle
            }
        }
    }

    suspend fun downloadApk(downloadUrl: String) {
        _updateState.value = UpdateState.Downloading(0)
        withContext(Dispatchers.IO) {
            try {
                val url = URL(downloadUrl)
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 10000
                conn.readTimeout = 15000
                conn.connect()

                val fileLength = conn.contentLength
                val outputFile = File(context.cacheDir, "update.apk")
                if (outputFile.exists()) outputFile.delete()

                val input = conn.inputStream
                val output = FileOutputStream(outputFile)

                val data = ByteArray(4096)
                var total: Long = 0
                var count: Int

                while (input.read(data).also { count = it } != -1) {
                    total += count
                    if (fileLength > 0) {
                        val progress = ((total * 100) / fileLength).toInt()
                        _updateState.value = UpdateState.Downloading(progress)
                    }
                    output.write(data, 0, count)
                }

                output.flush()
                output.close()
                input.close()

                _updateState.value = UpdateState.Downloaded(outputFile)
            } catch (e: Exception) {
                _updateState.value = UpdateState.Error(e.message ?: "Failed to download update")
            }
        }
    }

    fun installApk(apkFile: File) {
        if (!apkFile.exists()) return
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
