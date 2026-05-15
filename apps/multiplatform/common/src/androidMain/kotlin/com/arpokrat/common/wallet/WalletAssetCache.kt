package com.arpokrat.common.wallet

import kotlinx.serialization.json.*
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

actual object WalletAssetCache {
  private const val PREFS_NAME = "wallet_cache"
  private const val KEY_ASSETS = "cached_assets"

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

  actual fun saveAssets(assets: List<CryptoAsset>) {
    val jsonArray = buildJsonArray {
      assets.forEach { asset ->
        add(buildJsonObject {
          put("symbol", asset.symbol)
          put("name", asset.name)
          put("balance", asset.balance)
          put("decimals", asset.decimals)
          put("coinType", asset.coinType)
          put("contractAddress", asset.contractAddress ?: "")
        })
      }
    }
    securePrefs.edit().putString(KEY_ASSETS, jsonArray.toString()).apply()
  }

  actual fun loadAssets(): List<CryptoAsset> {
    val jsonStr = securePrefs.getString(KEY_ASSETS, null) ?: return emptyList()

    val list = mutableListOf<CryptoAsset>()
    try {
      val jsonArray = Json.parseToJsonElement(jsonStr).jsonArray
      for (element in jsonArray) {
        val obj = element.jsonObject
        list.add(CryptoAsset(
          symbol = obj["symbol"]?.jsonPrimitive?.content ?: "",
          name = obj["name"]?.jsonPrimitive?.content ?: "",
          balance = obj["balance"]?.jsonPrimitive?.content ?: "0.0",
          decimals = obj["decimals"]?.jsonPrimitive?.int ?: 18,
          coinType = obj["coinType"]?.jsonPrimitive?.int ?: 60,
          contractAddress = obj["contractAddress"]?.jsonPrimitive?.content?.ifEmpty { null }
        ))
      }
    } catch (e: Exception) {
    }
    return list
  }

  actual fun clearCache() {
    securePrefs.edit().clear().apply()
  }
}