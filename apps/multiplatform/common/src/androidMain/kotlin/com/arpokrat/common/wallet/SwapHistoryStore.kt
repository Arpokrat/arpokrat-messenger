package com.arpokrat.common.wallet

import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

actual object SwapHistoryStore {
  private const val PREFS_NAME = "wallet_swap_history"
  private const val KEY_HISTORY = "swap_history"
  private const val MAX_ENTRIES = 50

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

  actual fun list(): List<PendingSwap> {
    val raw = securePrefs.getString(KEY_HISTORY, null) ?: return emptyList()
    return try {
      json.decodeFromString<List<PendingSwap>>(raw).sortedByDescending { it.createdAt }
    } catch (e: Exception) {
      emptyList()
    }
  }

  actual fun upsert(swap: PendingSwap) {
    val current = list()
    val existing = current.firstOrNull { it.tradeId == swap.tradeId }
    val merged = swap.copy(createdAt = swap.createdAt.takeIf { it > 0L } ?: existing?.createdAt ?: 0L)
    val updated = (listOf(merged) + current.filter { it.tradeId != swap.tradeId })
      .sortedByDescending { it.createdAt }
      .take(MAX_ENTRIES)
    save(updated)
  }

  actual fun remove(tradeId: String) {
    save(list().filter { it.tradeId != tradeId })
  }

  private fun save(swaps: List<PendingSwap>) {
    securePrefs.edit().putString(KEY_HISTORY, json.encodeToString(swaps)).apply()
  }
}
