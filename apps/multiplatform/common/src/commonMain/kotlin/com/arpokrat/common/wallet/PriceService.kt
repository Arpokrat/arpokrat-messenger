package com.arpokrat.app.wallet

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

object PriceService {

  // TODO Later to use paid API
  private val httpClient = HttpClient {
    install(ContentNegotiation) {
      json(Json {
        ignoreUnknownKeys = true
        isLenient = true
      })
    }
  }

  // In-memory cache to avoid rate limits
  var currentPrices: Map<String, Map<String, Double>>? = null
  private var lastFetchTime = 0L
  private const val CACHE_DURATION_MS = 60_000L

  // Symbol to CoinGecko ID mapping
  private val symbolToIdMap = mapOf(
    "BTC" to "bitcoin",
    "ETH" to "ethereum",
    "POL" to "polygon-ecosystem-token",
    "MATIC" to "matic-network",
    "SOL" to "solana",
    "TRX" to "tron",
    "USDT" to "tether",
    "USDC" to "usd-coin",
    "EURC" to "euro-coin",
    "DAI" to "dai",
    "WBTC" to "wrapped-bitcoin",
    "SHIB" to "shiba-inu",
    "UNI" to "uniswap",
    "LINK" to "chainlink"
  )

  /**
   * Fetches prices for a given list of symbols.
   * @param symbols List of symbols (e.g., ["BTC", "ETH", "USDC"])
   */
  suspend fun fetchPrices(symbols: List<String>): Boolean {
    val now = System.currentTimeMillis()
    if (now - lastFetchTime < CACHE_DURATION_MS && currentPrices != null) {
      return true
    }

    return try {
      val idsToFetch = symbols
        .mapNotNull { symbolToIdMap[it.uppercase()] }
        .distinct()
        .joinToString(",")

      if (idsToFetch.isEmpty()) return false

      val response: Map<String, Map<String, Double>> = httpClient.get("https://api.coingecko.com/api/v3/simple/price") {
        parameter("ids", idsToFetch)
        parameter("vs_currencies", "usd,eur")
      }.body()

      val newMap = (currentPrices ?: emptyMap()).toMutableMap()
      newMap.putAll(response)
      currentPrices = newMap
      lastFetchTime = now

      true
    } catch (e: Exception) {
      false
    }
  }

  /**
   * Real USD→EUR conversion factor derived from the CoinGecko prices already cached
   * (each known asset is fetched in both `usd` and `eur`). Returns null until prices
   * are loaded or if no entry exposes both currencies. Used by the Swap screen, whose
   * proxy price endpoint only returns USD — this keeps the FIAT estimate in the wallet's
   * configured currency without inventing an approximate rate.
   */
  fun usdToEurRate(): Double? {
    val prices = currentPrices ?: return null
    for (byCurrency in prices.values) {
      val usd = byCurrency["usd"] ?: continue
      val eur = byCurrency["eur"] ?: continue
      if (usd > 0.0 && eur > 0.0) return eur / usd
    }
    return null
  }

  /**
   * Calculates the FIAT value of a balance.
   */
  fun getValue(tokenSymbol: String, balance: Double, currency: String): Double {
    val prices = currentPrices ?: return 0.0
    val currencyKey = currency.lowercase()

    val id = when(tokenSymbol.uppercase()) {
      "ETH", "WETH" -> "ethereum"
      else -> symbolToIdMap[tokenSymbol.uppercase()]
    }

    val coingeckoId = id ?: return 0.0
    val unitPrice = prices[coingeckoId]?.get(currencyKey) ?: 0.0

    return balance * unitPrice
  }
}