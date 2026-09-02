package com.example.util

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

data class LicenseData(
    val code: String,
    val status: String, // "ACTIVE", "BLOCKED", "UNACTIVATED"
    val deviceId: String,
    val activatedDate: Long,
    val maxDevices: Int = 1,
    val customerName: String = "",
    val notes: String = ""
)

sealed class LicenseState {
    object Loading : LicenseState()
    object Unactivated : LicenseState()
    data class Active(val license: LicenseData) : LicenseState()
    data class Blocked(val reason: String = "This license has been blocked by administrator.") : LicenseState()
}

class LicenseBlockedException(message: String) : Exception(message)

class LicenseManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("aqeel_rider_license_prefs", Context.MODE_PRIVATE)

    private val _licenseState = MutableStateFlow<LicenseState>(LicenseState.Loading)
    val licenseState: StateFlow<LicenseState> = _licenseState.asStateFlow()

    companion object {
        private const val KEY_LICENSE_CODE = "license_code"
        private const val KEY_STATUS = "license_status"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_ACTIVATED_DATE = "activated_date"
        private const val KEY_CUSTOMER_NAME = "customer_name"
        private const val KEY_SERVER_URL = "server_url"
        const val DEFAULT_SERVER_URL = "https://rider-admin-api.local"
    }

    init {
        init()
    }

    fun getDeviceId(): String {
        var id = prefs.getString(KEY_DEVICE_ID, null)
        if (id == null) {
            id = "DEV-" + UUID.randomUUID().toString().take(8).uppercase()
            prefs.edit().putString(KEY_DEVICE_ID, id).apply()
        }
        return id
    }

    fun getServerUrl(): String {
        return prefs.getString(KEY_SERVER_URL, DEFAULT_SERVER_URL) ?: DEFAULT_SERVER_URL
    }

    fun setServerUrl(url: String) {
        prefs.edit().putString(KEY_SERVER_URL, url.trimEnd('/')).apply()
    }

    fun init() {
        val code = prefs.getString(KEY_LICENSE_CODE, null)
        val status = prefs.getString(KEY_STATUS, "UNACTIVATED")
        val deviceId = getDeviceId()
        val activatedDate = prefs.getLong(KEY_ACTIVATED_DATE, 0L)
        val customerName = prefs.getString(KEY_CUSTOMER_NAME, "") ?: ""

        if (code.isNullOrEmpty() || status == "UNACTIVATED") {
            _licenseState.value = LicenseState.Unactivated
        } else if (status == "BLOCKED") {
            _licenseState.value = LicenseState.Blocked()
        } else {
            val data = LicenseData(
                code = code,
                status = "ACTIVE",
                deviceId = deviceId,
                activatedDate = activatedDate,
                customerName = customerName
            )
            _licenseState.value = LicenseState.Active(data)
        }
    }

    suspend fun activateLicense(
        licenseKey: String,
        customerName: String = "",
        serverUrl: String = getServerUrl()
    ): Result<LicenseData> = withContext(Dispatchers.IO) {
        val formattedKey = licenseKey.trim().uppercase()
        val deviceId = getDeviceId()

        if (!formattedKey.matches(Regex("^AR-[A-Z0-9]{4}-[A-Z0-9]{4}$"))) {
            return@withContext Result.failure(Exception("Invalid License Format. Expected format: AR-XXXX-XXXX"))
        }

        try {
            val endpoint = "$serverUrl/api/license/activate"
            val url = URL(endpoint)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.connectTimeout = 6000
            conn.readTimeout = 6000
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")

            val jsonBody = JSONObject().apply {
                put("licenseKey", formattedKey)
                put("deviceId", deviceId)
                put("customerName", customerName)
                put("deviceModel", android.os.Build.MODEL ?: "Android Device")
            }

            conn.outputStream.use { it.write(jsonBody.toString().toByteArray()) }

            if (conn.responseCode == 200) {
                val resp = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(resp)
                val status = json.optString("status", "ACTIVE")
                if (status == "BLOCKED") {
                    saveLicense(formattedKey, "BLOCKED", customerName)
                    _licenseState.value = LicenseState.Blocked()
                    return@withContext Result.failure(LicenseBlockedException("This license is blocked."))
                }
                val licenseData = LicenseData(
                    code = formattedKey,
                    status = "ACTIVE",
                    deviceId = deviceId,
                    activatedDate = System.currentTimeMillis(),
                    customerName = customerName
                )
                saveLicense(formattedKey, "ACTIVE", customerName)
                _licenseState.value = LicenseState.Active(licenseData)
                return@withContext Result.success(licenseData)
            } else if (conn.responseCode == 403 || conn.responseCode == 401) {
                val errorText = try {
                    conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "License error"
                } catch (_: Exception) { "Invalid or blocked license" }
                val errJson = try { JSONObject(errorText) } catch (_: Exception) { JSONObject() }
                val msg = errJson.optString("message", "License is invalid or device limit reached.")
                return@withContext Result.failure(Exception(msg))
            }
        } catch (_: Exception) {
            // Local fallback activation if server is unreachable or offline test
            if (formattedKey.startsWith("AR-")) {
                val licenseData = LicenseData(
                    code = formattedKey,
                    status = "ACTIVE",
                    deviceId = deviceId,
                    activatedDate = System.currentTimeMillis(),
                    customerName = customerName
                )
                saveLicense(formattedKey, "ACTIVE", customerName)
                _licenseState.value = LicenseState.Active(licenseData)
                return@withContext Result.success(licenseData)
            }
        }

        return@withContext Result.failure(Exception("Failed to activate license"))
    }

    suspend fun verifyLicense(serverUrl: String = getServerUrl()): LicenseState = withContext(Dispatchers.IO) {
        val code = prefs.getString(KEY_LICENSE_CODE, null)
        val deviceId = getDeviceId()
        if (code == null) {
            _licenseState.value = LicenseState.Unactivated
            return@withContext LicenseState.Unactivated
        }

        try {
            val endpoint = "$serverUrl/api/license/verify?code=$code&deviceId=$deviceId"
            val url = URL(endpoint)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 4000
            conn.readTimeout = 4000
            conn.requestMethod = "GET"

            if (conn.responseCode == 200) {
                val resp = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(resp)
                val status = json.optString("status", "ACTIVE")
                if (status == "BLOCKED") {
                    saveLicense(code, "BLOCKED", prefs.getString(KEY_CUSTOMER_NAME, "") ?: "")
                    _licenseState.value = LicenseState.Blocked()
                    return@withContext _licenseState.value
                } else {
                    saveLicense(code, "ACTIVE", prefs.getString(KEY_CUSTOMER_NAME, "") ?: "")
                    val data = LicenseData(
                        code = code,
                        status = "ACTIVE",
                        deviceId = deviceId,
                        activatedDate = prefs.getLong(KEY_ACTIVATED_DATE, System.currentTimeMillis()),
                        customerName = prefs.getString(KEY_CUSTOMER_NAME, "") ?: ""
                    )
                    _licenseState.value = LicenseState.Active(data)
                    return@withContext _licenseState.value
                }
            } else if (conn.responseCode == 403) {
                saveLicense(code, "BLOCKED", prefs.getString(KEY_CUSTOMER_NAME, "") ?: "")
                _licenseState.value = LicenseState.Blocked()
                return@withContext _licenseState.value
            }
        } catch (_: Exception) {
            // Offline Grace: maintain active state
        }

        return@withContext _licenseState.value
    }

    private fun saveLicense(code: String, status: String, customerName: String) {
        prefs.edit()
            .putString(KEY_LICENSE_CODE, code)
            .putString(KEY_STATUS, status)
            .putString(KEY_CUSTOMER_NAME, customerName)
            .putLong(KEY_ACTIVATED_DATE, if (status == "ACTIVE") System.currentTimeMillis() else 0L)
            .apply()
    }

    fun unblockLocally() {
        val code = prefs.getString(KEY_LICENSE_CODE, "AR-DEMO-0001") ?: "AR-DEMO-0001"
        val name = prefs.getString(KEY_CUSTOMER_NAME, "") ?: ""
        saveLicense(code, "ACTIVE", name)
        init()
    }

    fun blockLocally() {
        val code = prefs.getString(KEY_LICENSE_CODE, "") ?: ""
        val name = prefs.getString(KEY_CUSTOMER_NAME, "") ?: ""
        saveLicense(code, "BLOCKED", name)
        _licenseState.value = LicenseState.Blocked()
    }

    fun resetLicense() {
        prefs.edit()
            .remove(KEY_LICENSE_CODE)
            .putString(KEY_STATUS, "UNACTIVATED")
            .remove(KEY_ACTIVATED_DATE)
            .apply()
        _licenseState.value = LicenseState.Unactivated
    }
}
