package com.arpokrat.common.wallet

enum class CryptoNetwork(
  val id: Int,
  val symbol: String,
  val displayName: String
) {
  // Mainnets
  BITCOIN(0, "BTC", "Bitcoin"),
  ETHEREUM(60, "ETH", "Ethereum"),
  POLYGON(137, "POL", "Polygon"),
  SOLANA(501, "SOL", "Solana"),
  TRON(195, "TRX", "Tron"),

  // Testnets
  BITCOIN_TESTNET(1, "BTC", "Bitcoin Testnet"),
  ETHEREUM_SEPOLIA(11155111, "ETH", "Ethereum Sepolia"),
  POLYGON_AMOY(80002, "POL", "Polygon Amoy"),
  SOLANA_DEVNET(9000, "SOL", "Solana Devnet"),
  TRON_NILE(10000, "TRX", "Tron Nile");

  companion object {
    fun fromId(id: Int): CryptoNetwork = values().find { it.id == id } ?: POLYGON
  }

  fun getExplorerUrl(txHash: String): String {
    val cleanHash = txHash.trim()
    val template = WalletManager().getTxExplorerTemplate(this.id)
    return template.replace("{hash}", cleanHash)
  }
}

enum class NetworkFamily {
  EVM,
  UTXO,
  TRON,
  SOLANA
}

data class CryptoAsset(
  val symbol: String,
  val name: String,
  val balance: String,
  val decimals: Int,
  val coinType: Int,
  val contractAddress: String? = null
)

interface CryptoWalletInterface {
  fun getAvailableAssets(onResult: (List<CryptoAsset>) -> Unit)
  fun estimateGas(amount: String, asset: CryptoAsset, toAddress: String, onResult: (String) -> Unit)
  fun sendPayment(amount: String, asset: CryptoAsset, toAddress: String, onResult: (String?) -> Unit)
  fun getAddress(coinType: Int): String
  fun isPinSet(): Boolean
  fun validatePin(inputPin: String): Boolean
}

object CryptoManager {
  var wallet: CryptoWalletInterface? = null
  fun isReady(): Boolean = wallet != null
}

object CryptoProtocol {
  const val PREFIX_INVOICE = "ARPOKRAT::INVOICE::"
  const val PREFIX_PAID = "ARPOKRAT::PAID::"
  const val PREFIX_CANCELLED = "ARPOKRAT::CANCELLED::"
  const val PREFIX_DECLINED = "ARPOKRAT::DECLINED::"

  fun formatInvoice(amount: String, symbol: String, address: String, coinType: Int, fiatValue: String) =
    "$PREFIX_INVOICE$amount::$symbol::$address::$coinType::$fiatValue"

  fun formatPaid(amount: String, symbol: String, txHash: String, fiatValue: String) =
    "$PREFIX_PAID$amount::$symbol::$txHash::$fiatValue"

  fun formatCancelled() = "${PREFIX_CANCELLED}Cancelled"
  fun formatDeclined() = "${PREFIX_DECLINED}Declined"

  fun parseInvoice(text: String): InvoiceData? {
    if (!text.startsWith(PREFIX_INVOICE)) return null
    val parts = text.removePrefix(PREFIX_INVOICE).split("::")
    if (parts.size < 3) return null
    val coinType = if (parts.size >= 4) parts[3].toIntOrNull() ?: 137 else 137
    val fiatValue = if (parts.size >= 5) parts[4] else ""
    return InvoiceData(parts[0], parts[1], parts[2], coinType, fiatValue)
  }

  fun parsePaid(text: String): PaidData? {
    if (!text.startsWith(PREFIX_PAID)) return null
    val parts = text.removePrefix(PREFIX_PAID).split("::")
    if (parts.size < 3) return null
    val fiatValue = if (parts.size >= 4) parts[3] else ""
    return PaidData(parts[0], parts[1], parts[2], fiatValue)
  }

  data class InvoiceData(val amount: String, val symbol: String, val address: String, val coinType: Int, val fiatValue: String)
  data class PaidData(val amount: String, val symbol: String, val txHash: String, val fiatValue: String)

  fun isInvoice(text: String) = text.startsWith(PREFIX_INVOICE)
  fun isPaid(text: String) = text.startsWith(PREFIX_PAID)
  fun isCancelled(text: String) = text.startsWith(PREFIX_CANCELLED)
  fun isDeclined(text: String) = text.startsWith(PREFIX_DECLINED)
}