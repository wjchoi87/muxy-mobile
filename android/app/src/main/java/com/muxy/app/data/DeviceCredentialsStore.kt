package com.muxy.app.data

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.SecureRandom
import java.util.UUID

data class DeviceCredentials(val deviceID: UUID, val token: String)

class DeviceCredentialsStore(context: Context) {
    private val prefs = EncryptedSharedPreferences.create(
        context,
        FILE_NAME,
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    fun load(): DeviceCredentials {
        val existingId = prefs.getString(KEY_DEVICE_ID, null)
        val existingToken = prefs.getString(KEY_TOKEN, null)
        if (existingId != null && existingToken != null) {
            return DeviceCredentials(UUID.fromString(existingId), existingToken)
        }
        val created = DeviceCredentials(UUID.randomUUID(), generateToken())
        prefs.edit()
            .putString(KEY_DEVICE_ID, created.deviceID.toString())
            .putString(KEY_TOKEN, created.token)
            .apply()
        return created
    }

    private fun generateToken(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    companion object {
        private const val FILE_NAME = "muxy_credentials"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_TOKEN = "token"
    }
}
