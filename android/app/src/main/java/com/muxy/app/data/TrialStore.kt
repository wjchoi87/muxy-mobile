package com.muxy.app.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class TrialStore(context: Context) {
    private val prefs = EncryptedSharedPreferences.create(
        context,
        FILE_NAME,
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    fun startedAt(): Long? {
        val v = prefs.getLong(KEY_STARTED_AT, -1L)
        return if (v <= 0L) null else v
    }

    fun startIfAbsent(now: Long = System.currentTimeMillis()): Long {
        val existing = startedAt()
        if (existing != null) return existing
        prefs.edit().putLong(KEY_STARTED_AT, now).apply()
        return now
    }

    companion object {
        private const val FILE_NAME = "muxy_trial"
        private const val KEY_STARTED_AT = "started_at"
        const val TRIAL_DURATION_MS: Long = 3L * 24 * 60 * 60 * 1000
    }
}
