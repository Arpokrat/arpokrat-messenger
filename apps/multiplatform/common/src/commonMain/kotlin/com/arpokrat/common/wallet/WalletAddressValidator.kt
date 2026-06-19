package com.arpokrat.common.wallet

object WalletAddressValidator {

  private val BTC = Regex("^(bc1|[13])[a-zA-HJ-NP-Z0-9]{25,39}$")
  private val SOL = Regex("^[1-9A-HJ-NP-Za-km-z]{32,44}$")
  private val TRON = Regex("^T[A-Za-z1-9]{33}$")
  private val EVM = Regex("^0x[a-fA-F0-9]{40}$")
  private val BASIC = Regex("^[A-Za-z0-9:_-]+$")

  fun isValid(address: String, coinType: Int): Boolean {
    val a = address.trim()
    if (a.isBlank()) return false
    return when (coinType) {
      CryptoNetwork.BITCOIN.id, CryptoNetwork.BITCOIN_TESTNET.id -> a.matches(BTC)
      CryptoNetwork.SOLANA.id, CryptoNetwork.SOLANA_DEVNET.id -> a.matches(SOL)
      CryptoNetwork.TRON.id, CryptoNetwork.TRON_NILE.id -> a.matches(TRON)
      else -> a.matches(EVM)
    }
  }

  fun isPlausibleExternal(address: String): Boolean {
    val a = address.trim()
    return a.length in 16..128 && a.matches(BASIC)
  }
}
