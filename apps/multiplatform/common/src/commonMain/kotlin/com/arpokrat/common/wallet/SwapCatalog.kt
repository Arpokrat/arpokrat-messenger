package com.arpokrat.common.wallet

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.arpokrat.common.platform.Log

object SwapCatalog {
  private val chainIcons = mutableStateMapOf<String, String>()

  var featured by mutableStateOf<List<SwapToken>>(emptyList())
    private set

  var xStocks by mutableStateOf<List<SwapToken>>(emptyList())
    private set

  private var loaded = false
  private var xStocksLoaded = false

  suspend fun ensureLoaded() {
    if (loaded) return
    val b = SwapService.bootstrap() ?: return
    b.chains.forEach { c ->
      val icon = c.icon ?: return@forEach
      val url = SwapService.iconUrl(icon)
      listOf(c.name, c.id, c.shortName)
        .filter { it.isNotBlank() }
        .forEach { chainIcons[it.lowercase()] = url }
    }
    featured = b.tokens.map { it.toSwapToken() }
    loaded = true
  }

  suspend fun ensureXStocksLoaded() {
    if (xStocksLoaded) return
    val fromSearch = SwapService.searchToken("xstock").map { it.toSwapToken() }
    val candidates = (fromSearch + featured).distinctBy { it.tokenId }
    val found = candidates
      .filter { SwapTokenMapping.isXStock(it) }
      .sortedBy { it.symbol }
    if (DEBUG_SWAP_POLLING) Log.d(
      "SwapPolling",
      "xStocks: searchHits=${candidates.size} kept=${found.size}" +
        " sample=${candidates.take(12).joinToString { it.tokenId }} -> ${found.joinToString { it.tokenId }}"
    )
    xStocks = found
    xStocksLoaded = found.isNotEmpty()
  }

  fun chainIconUrl(network: String): String? = chainIcons[network.trim().lowercase()]
}
