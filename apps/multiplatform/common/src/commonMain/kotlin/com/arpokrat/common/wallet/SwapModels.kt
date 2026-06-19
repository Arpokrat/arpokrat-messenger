package com.arpokrat.common.wallet

import kotlinx.serialization.Serializable

@Serializable
data class SwapTokenDto(
  val id: String = "",
  val symbol: String = "",
  val name: String = "",
  val icon: String? = null,
  val mainnet: Boolean = true,
  val chainData: SwapChainData? = null
)

@Serializable
data class SwapChainData(val name: String = "")

@Serializable
data class SwapChainDto(
  val id: String = "",
  val name: String = "",
  val shortName: String = "",
  val icon: String? = null
)

@Serializable
data class SwapBootstrap(
  val generated: Long = 0,
  val chains: List<SwapChainDto> = emptyList(),
  val tokens: List<SwapTokenDto> = emptyList()
)

@Serializable
data class SwapSearchResponse(
  val tokens: List<SwapTokenDto> = emptyList(),
  val error: String? = null
)

@Serializable
data class SwapMinMax(
  val min: Double = 0.0,
  val max: Double = 0.0,
  val error: String? = null
)

@Serializable
data class SwapPrice(
  val usd: Double = 0.0,
  val eur: Double = 0.0,
  val error: String? = null
)

@Serializable
data class SwapQuote(
  val quoteId: String = "",
  val amountIn: Double = 0.0,
  val amountOut: Double = 0.0,
  val provider: String = "",
  val kyc: String = "",
  val quoteToken: String = "",
  val expiresAt: Long = 0,
  val expiresIn: Int = 0
)

@Serializable
data class SwapQuotesResponse(
  val quotes: List<SwapQuote> = emptyList(),
  val error: String? = null
)

@Serializable
data class SwapExchangeRequest(
  val from: String,
  val to: String,
  val quoteId: String,
  val amount: String,
  val addressTo: String,
  val dir: String,
  val quoteToken: String
)

@Serializable
data class SwapExchangeResponse(
  val id: String? = null,
  val trade_id: String? = null,
  val depositAddress: String? = null,
  val address_provider: String? = null,
  val expiry: Long? = null,
  val expires_at: Long? = null,
  val error: String? = null
) {
  val tradeId: String? get() = id ?: trade_id
  val deposit: String? get() = depositAddress ?: address_provider
  val expiryMillis: Long? get() = (expiry ?: expires_at)?.takeIf { it > 0 }?.let { it * 1000 }
}

@Serializable
data class SwapStatusResponse(
  val status: String? = null,
  val trade_id: String? = null,
  val address_provider: String? = null,
  val expiry: String? = null,
  val error: String? = null
) {
  val expiryMillis: Long? get() = expiry?.trim()?.toDoubleOrNull()?.toLong()?.takeIf { it > 0 }?.let { it * 1000 }
}

data class SwapToken(
  val ticker: String,
  val network: String,
  val symbol: String,
  val name: String,
  val iconUrl: String?,
  val walletAsset: CryptoAsset? = null
) {
  val tokenId: String get() = "$ticker|$network"
}

fun SwapTokenDto.toSwapToken(): SwapToken {
  val parts = id.split("|")
  val tk = parts.getOrNull(0)?.takeIf { it.isNotBlank() } ?: symbol
  val net = parts.getOrNull(1)?.takeIf { it.isNotBlank() } ?: (chainData?.name ?: "")
  return SwapToken(
    ticker = tk,
    network = net,
    symbol = symbol.ifBlank { tk.uppercase() },
    name = name,
    iconUrl = icon?.let { SwapService.iconUrl(it) },
    walletAsset = null
  )
}

enum class SwapStatusUi { NEW, WAITING, CONFIRMING, SENDING, FINISHED, EXPIRED, FAILED, REFUNDED, UNKNOWN }

val SwapStatusUi.isAwaitingDeposit: Boolean get() = this == SwapStatusUi.NEW || this == SwapStatusUi.WAITING

val SwapStatusUi.isDepositDetected: Boolean
  get() = this == SwapStatusUi.CONFIRMING || this == SwapStatusUi.SENDING ||
    this == SwapStatusUi.FINISHED || this == SwapStatusUi.REFUNDED

val SwapStatusUi.isTerminal: Boolean
  get() = this == SwapStatusUi.FINISHED || this == SwapStatusUi.EXPIRED ||
    this == SwapStatusUi.FAILED || this == SwapStatusUi.REFUNDED

@Serializable
data class PendingSwap(
  val tradeId: String,
  val depositAddress: String,
  val amountIn: Double,
  val fromTokenId: String,
  val toTokenId: String,
  val fromSymbol: String,
  val toSymbol: String,
  val amountOut: Double = 0.0,
  val lastStatus: String = "new",
  val createdAt: Long = 0L,
  val expiresAt: Long = 0L
)

const val SWAP_LOCAL_EXPIRY_MS = 3_600_000L

fun swapExpiryDeadline(createdAt: Long, expiresAt: Long, nowFallback: Long): Long =
  if (expiresAt > 0L) expiresAt else (createdAt.takeIf { it > 0L } ?: nowFallback) + SWAP_LOCAL_EXPIRY_MS

fun PendingSwap.effectiveStatus(now: Long): SwapStatusUi {
  val s = mapSwapStatus(lastStatus)
  return if (s.isAwaitingDeposit && now >= swapExpiryDeadline(createdAt, expiresAt, now)) SwapStatusUi.EXPIRED else s
}

fun mapSwapStatus(raw: String?): SwapStatusUi = when (raw?.lowercase()?.trim()) {
  "new" -> SwapStatusUi.NEW
  "waiting" -> SwapStatusUi.WAITING
  "confirming", "confirmation", "confirmed" -> SwapStatusUi.CONFIRMING
  "sending", "sent", "exchanging" -> SwapStatusUi.SENDING
  "finished", "completed", "success" -> SwapStatusUi.FINISHED
  "expired" -> SwapStatusUi.EXPIRED
  "refunded", "refund" -> SwapStatusUi.REFUNDED
  "halted", "error", "failed" -> SwapStatusUi.FAILED
  null, "" -> SwapStatusUi.UNKNOWN
  else -> SwapStatusUi.UNKNOWN
}
