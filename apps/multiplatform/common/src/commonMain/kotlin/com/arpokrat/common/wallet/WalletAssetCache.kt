package com.arpokrat.common.wallet

expect object WalletAssetCache {
  fun saveAssets(assets: List<CryptoAsset>)
  fun loadAssets(): List<CryptoAsset>
  fun clearCache()
}