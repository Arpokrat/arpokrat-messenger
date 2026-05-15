package com.arpokrat.common.wallet

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object FeeService {
  fun isNativeToken(contractAddress: String?): Boolean {
    val addr = contractAddress?.trim()?.lowercase() ?: return true
    if (addr.isEmpty() || addr == "null" || addr == "undefined") return true
    if (addr.endsWith("0000000000000000000000000000000000000000")) return true
    if (addr.endsWith("eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee")) return true
    return false
  }

  suspend fun estimateFee(asset: CryptoAsset): Double {
    return try {
      when (asset.coinType) {
        CryptoNetwork.POLYGON.id, 80002,
        CryptoNetwork.ETHEREUM.id, 11155111 -> estimateEvmFee(asset)
        CryptoNetwork.SOLANA.id, CryptoNetwork.SOLANA_DEVNET.id -> 0.000005
        CryptoNetwork.BITCOIN.id, CryptoNetwork.BITCOIN_TESTNET.id -> estimateBtcFee(asset)
        CryptoNetwork.TRON.id, CryptoNetwork.TRON_NILE.id -> {
          if (isNativeToken(asset.contractAddress)) 1.5 else 30.0
        }
        else -> 0.0
      }
    } catch (e: Exception) { 0.0 }
  }

  private suspend fun estimateEvmFee(asset: CryptoAsset): Double {
    return withContext(Dispatchers.IO) {
      val evmService = NetworkFactory.getEvmService(asset.coinType)
      val gasPriceWeiStr = evmService.getGasPrice()
      val gasPriceWei = gasPriceWeiStr.toDoubleOrNull() ?: 0.0
      val gasLimit = if (isNativeToken(asset.contractAddress)) 21000.0 else 65000.0
      (gasLimit * gasPriceWei) / 1_000_000_000_000_000_000.0
    }
  }

  private suspend fun estimateBtcFee(asset: CryptoAsset): Double {
    return withContext(Dispatchers.IO) {
      val btcService = NetworkFactory.getBitcoinService(asset.coinType)
      val satsPerVByte = btcService.getRecommendedFee()
      val estimatedSats = satsPerVByte * 140L
      estimatedSats.toDouble() / 100_000_000.0
    }
  }
}