package com.arpokrat.common.wallet

import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

actual object PendingSwapStore {
  private const val PREFS_NAME = "wallet_pending_swap"
  private const val KEY_SWAP = "pending_swap"

  private val json = Json { ignoreUnknownKeys = true }

  private val securePrefs by lazy {
    val context = WalletContext.appContext
    val masterKey = MasterKey.Builder(context)
      .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
      .build()

    EncryptedSharedPreferences.create(
      context,
      PREFS_NAME,
      masterKey,
      EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
      EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
  }

  actual fun save(swap: PendingSwap) {
    securePrefs.edit().putString(KEY_SWAP, json.encodeToString(swap)).apply()
  }

  actual fun load(): PendingSwap? {
    val raw = securePrefs.getString(KEY_SWAP, null) ?: return null
    return try {
      json.decodeFromString<PendingSwap>(raw)
    } catch (e: Exception) {
      null
    }
  }

  actual fun clear() {
    securePrefs.edit().remove(KEY_SWAP).apply()
  }
}
