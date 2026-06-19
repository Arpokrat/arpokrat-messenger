package com.arpokrat.common.wallet

object SwapTokenMapping {

  const val DEFAULT_SOURCE_ID = "btc|Mainnet"
  const val DEFAULT_TARGET_ID = "xmr|Mainnet"

  fun tokenIdFor(asset: CryptoAsset): String? {
    val isToken = !asset.contractAddress.isNullOrBlank()
    return when (asset.coinType) {
      CryptoNetwork.BITCOIN.id -> if (isToken) null else "btc|Mainnet"
      CryptoNetwork.ETHEREUM.id -> if (isToken) null else "eth|ERC20"
      CryptoNetwork.SOLANA.id -> if (isToken) null else "sol|Mainnet"
      CryptoNetwork.POLYGON.id ->
        if (!isToken) "pol|Mainnet"
        else if (asset.symbol.equals("USDC", ignoreCase = true)) "usdc|MATIC"
        else null
      CryptoNetwork.TRON.id ->
        if (!isToken) "trx|Mainnet"
        else if (asset.symbol.equals("USDT", ignoreCase = true)) "usdt|TRC20"
        else null
      else -> null
    }
  }

  fun walletCoinTypeFor(tokenId: String): Int? = when (tokenId.lowercase()) {
    "btc|mainnet" -> CryptoNetwork.BITCOIN.id
    "eth|erc20" -> CryptoNetwork.ETHEREUM.id
    "sol|mainnet" -> CryptoNetwork.SOLANA.id
    "pol|mainnet", "usdc|matic" -> CryptoNetwork.POLYGON.id
    "trx|mainnet", "usdt|trc20" -> CryptoNetwork.TRON.id
    else -> null
  }

  fun walletAssetFor(tokenId: String, walletAssets: List<CryptoAsset>): CryptoAsset? {
    val coinType = walletCoinTypeFor(tokenId) ?: return null
    val ticker = tokenId.substringBefore("|")
    return walletAssets.firstOrNull { it.coinType == coinType && it.symbol.equals(ticker, ignoreCase = true) }
  }

  fun reconstruct(tokenId: String, symbol: String): SwapToken {
    val parts = tokenId.split("|")
    return SwapToken(
      ticker = parts.getOrElse(0) { symbol.lowercase() },
      network = parts.getOrElse(1) { "" },
      symbol = symbol,
      name = symbol,
      iconUrl = null,
      walletAsset = null
    )
  }

  fun isNonNativeToken(token: SwapToken): Boolean {
    val net = token.network.trim()
    if (net.isEmpty() || net.equals("Mainnet", ignoreCase = true)) return false
    val native = nativeTickerForNetwork(net)
    return native == null || !token.ticker.equals(native, ignoreCase = true)
  }

  private fun nativeTickerForNetwork(network: String): String? = when (network.lowercase()) {
    "erc20", "arbitrum", "arbitrum one", "base", "optimism" -> "eth"
    "trc20" -> "trx"
    "bep2", "bep20", "bsc" -> "bnb"
    "matic", "polygon" -> "pol"
    "kcc" -> "kcs"
    "avaxx", "avaxc" -> "avax"
    "sol", "solana" -> "sol"
    else -> null
  }

  val XSTOCK_BASE_TICKERS: Set<String> = setOf(
    "aapl", "tsla", "nvda", "msft", "amzn", "googl", "goog", "meta", "coin", "mstr",
    "spy", "qqq", "nflx", "amd", "pltr", "hood", "crcl", "abt", "ko", "mcd", "pfe",
    "jnj", "wmt", "cs", "dfdv", "gme", "ba", "dis", "intc", "mrvl", "orcl", "crm",
    "v", "ma", "jpm", "brk", "avgo", "tqqq", "gld"
  )

  fun isXStock(token: SwapToken): Boolean {
    if (token.name.trim().lowercase().contains("xstock")) return true
    val net = token.network.trim().lowercase()
    if (net.contains("stock") || net.contains("xstock") || net.contains("equit")) return true
    val t = token.ticker.trim().lowercase()
    if (t.endsWith("x") && t.length > 2 && t.dropLast(1) in XSTOCK_BASE_TICKERS) return true
    if (net.contains("sol") && t in XSTOCK_BASE_TICKERS) return true
    return false
  }

  fun sourceTokens(walletAssets: List<CryptoAsset>): List<SwapToken> =
    walletAssets.mapNotNull { asset ->
      val id = tokenIdFor(asset) ?: return@mapNotNull null
      val parts = id.split("|")
      SwapToken(
        ticker = parts.getOrElse(0) { asset.symbol.lowercase() },
        network = parts.getOrElse(1) { "" },
        symbol = asset.symbol,
        name = asset.name,
        iconUrl = null,
        walletAsset = asset
      )
    }
}
