package com.arpokrat.common.wallet

data class TokenDefinition(
  val symbol: String,
  val name: String,
  val network: CryptoNetwork,
  val contractAddress: String?,
  val decimals: Int
)

object DefaultAssets {
  private val list = listOf(
    // MAINNETS
    TokenDefinition("BTC", "Bitcoin", CryptoNetwork.BITCOIN, null, 8),
    TokenDefinition("ETH", "Ethereum", CryptoNetwork.ETHEREUM, null, 18),
    TokenDefinition("POL", "Polygon", CryptoNetwork.POLYGON, null, 18),
    TokenDefinition("SOL", "Solana", CryptoNetwork.SOLANA, null, 9),
    TokenDefinition("TRX", "Tron", CryptoNetwork.TRON, null, 6),

    // Stablecoins Mainnet
    TokenDefinition("USDC", "USD Coin", CryptoNetwork.POLYGON, "0x3c499c542cEF5E3811e1192ce70d8cC03d5c3359", 6),
    TokenDefinition("USDT", "Tether", CryptoNetwork.TRON, "TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t", 6),
    // TokenDefinition("USDT", "Tether", CryptoNetwork.ETHEREUM, "0xdAC17F958D2ee523a2206206994597C13D831ec7", 6),
    // TokenDefinition("EURC", "Euro Coin", CryptoNetwork.POLYGON, "0x1aBaEA1f7C830bD89Acc67eC4af516284b1bC33c", 6),

    // TESTNETS
    TokenDefinition("BTC", "Bitcoin Testnet", CryptoNetwork.BITCOIN_TESTNET, null, 8),
    TokenDefinition("ETH", "Ethereum Sepolia", CryptoNetwork.ETHEREUM_SEPOLIA, null, 18),
    TokenDefinition("POL", "Polygon Amoy", CryptoNetwork.POLYGON_AMOY, null, 18),
    TokenDefinition("SOL", "Solana Devnet", CryptoNetwork.SOLANA_DEVNET, null, 9),
    TokenDefinition("TRX", "Tron Nile", CryptoNetwork.TRON_NILE, null, 6),

    // Stablecoins Testnet
    TokenDefinition("USDC", "USD Coin (Amoy)", CryptoNetwork.POLYGON_AMOY, "0x41E94Eb019C0762f9Bfcf9Fb1E58725BfB0e7582", 6),
    TokenDefinition("USDT", "Tether (Nile)", CryptoNetwork.TRON_NILE, "TXYZopYRdj2D9XRtbG411XZZ3kM5VkAeBf", 6),
    // TokenDefinition("USDT", "Tether (Sepolia)", CryptoNetwork.ETHEREUM_SEPOLIA, "0x7169D38820df238525a9402a646d96E83643F3E6", 6),
    // TokenDefinition("EURC", "Euro Coin (Amoy)", CryptoNetwork.POLYGON_AMOY, "", 6)
  )

  private val testnetIds = setOf(1, 11155111, 80002, 9000, 10000)

  fun getActiveAssets(): List<TokenDefinition> {
    val isDevMode = WalletManager().isDeveloperModeEnabled()
    return list.filter { token ->
      val isTestnet = testnetIds.contains(token.network.id)
      if (isDevMode) isTestnet else !isTestnet
    }
  }
}