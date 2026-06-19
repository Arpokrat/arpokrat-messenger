package com.arpokrat.common.wallet

import com.arpokrat.common.platform.Log
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.encodeURLParameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

const val DEBUG_SWAP_POLLING = true

object SwapService {
  const val BASE = "https://arpokrat.com/api/proxy-swap.php"

  private val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
  }

  private val client = HttpClient {
    install(ContentNegotiation) { json(json) }
    install(HttpTimeout) {
      requestTimeoutMillis = 15000L
      connectTimeoutMillis = 15000L
      socketTimeoutMillis = 15000L
    }
    expectSuccess = false
  }

  suspend fun bootstrap(): SwapBootstrap? = withContext(Dispatchers.IO) {
    try {
      client.get(BASE) { parameter("action", "bootstrap") }.body<SwapBootstrap>()
    } catch (e: Exception) {
      null
    }
  }

  suspend fun searchToken(term: String): List<SwapTokenDto> = withContext(Dispatchers.IO) {
    if (term.isBlank()) return@withContext emptyList()
    try {
      client.get(BASE) {
        parameter("action", "searchToken")
        parameter("term", term)
      }.body<SwapSearchResponse>().tokens
    } catch (e: Exception) {
      emptyList()
    }
  }

  suspend fun minMax(fromId: String, toId: String): SwapMinMax? = withContext(Dispatchers.IO) {
    try {
      client.get(BASE) {
        parameter("action", "minMax")
        parameter("tokenIdFrom", fromId)
        parameter("tokenIdTo", toId)
      }.body<SwapMinMax>()
    } catch (e: Exception) {
      null
    }
  }

  suspend fun price(ticker: String): SwapPrice? = withContext(Dispatchers.IO) {
    try {
      client.get(BASE) {
        parameter("action", "price")
        parameter("ticker", ticker)
      }.body<SwapPrice>()
    } catch (e: Exception) {
      null
    }
  }

  suspend fun quotes(dir: String, amount: String, fromId: String, toId: String): SwapQuotesResponse =
    withContext(Dispatchers.IO) {
      try {
        val resp = client.get(BASE) {
          parameter("action", "quote")
          parameter("dir", dir)
          parameter("amount", amount)
          parameter("from", fromId)
          parameter("to", toId)
        }
        val raw = resp.bodyAsText()
        val typed = json.decodeFromString(SwapQuotesResponse.serializer(), raw)
        val quotes =
          if (typed.quotes.any { it.kyc.isBlank() }) {
            val root = json.parseToJsonElement(raw)
            typed.quotes.map { q ->
              if (q.kyc.isBlank()) q.copy(kyc = root.findRatingForProvider(q.provider) ?: q.kyc) else q
            }
          } else typed.quotes
        if (DEBUG_SWAP_POLLING) Log.d(
          "SwapPolling",
          "quotes $fromId->$toId http=${resp.status.value} -> " +
            quotes.joinToString { "${it.provider}:${it.kyc.ifBlank { "N" }}" }
        )
        typed.copy(quotes = quotes)
      } catch (e: Exception) {
        if (DEBUG_SWAP_POLLING) Log.e("SwapPolling", "quotes $fromId->$toId FAILED: ${e::class.simpleName}: ${e.message}")
        SwapQuotesResponse(error = "network")
      }
    }

  private val RATING_KEYS = listOf("kyc", "kycRating", "kycrating", "kyc_rating", "rating", "score", "grade")

  private fun JsonElement.findRatingForProvider(provider: String): String? {
    when (this) {
      is JsonObject -> {
        val prov = (this["provider"] as? JsonPrimitive)?.contentOrNull
        if (prov != null && prov.equals(provider, ignoreCase = true)) {
          for (k in RATING_KEYS) {
            val v = (this[k] as? JsonPrimitive)?.contentOrNull?.trim()
            if (!v.isNullOrBlank()) return v
          }
        }
        for (v in values) v.findRatingForProvider(provider)?.let { return it }
      }
      is JsonArray -> for (v in this) v.findRatingForProvider(provider)?.let { return it }
      else -> {}
    }
    return null
  }

  suspend fun exchange(request: SwapExchangeRequest): SwapExchangeResponse = withContext(Dispatchers.IO) {
    try {
      client.post(BASE) {
        parameter("action", "exchange")
        contentType(ContentType.Application.Json)
        setBody(request)
      }.body<SwapExchangeResponse>()
    } catch (e: Exception) {
      SwapExchangeResponse(error = "network")
    }
  }

  suspend fun status(id: String): SwapStatusResponse? = withContext(Dispatchers.IO) {
    try {
      val resp = client.get(BASE) {
        parameter("action", "status")
        parameter("id", id)
      }
      val raw = resp.bodyAsText()
      if (DEBUG_SWAP_POLLING) Log.d("SwapPolling", "GET ?action=status&id=$id -> http=${resp.status.value} body=${raw.take(500)}")
      val root = json.parseToJsonElement(raw)
      val parsed = SwapStatusResponse(
        status = root.findStringDeep("status"),
        trade_id = root.findStringDeep("trade_id") ?: root.findStringDeep("tradeId"),
        address_provider = root.findStringDeep("address_provider"),
        expiry = root.findStringDeep("expiry") ?: root.findStringDeep("expires_at"),
        error = root.findStringDeep("error")
      )
      if (parsed.status.isNullOrBlank() && DEBUG_SWAP_POLLING) {
        Log.w("SwapPolling", "status id=$id: no 'status' key found in body=${raw.take(500)}")
      }
      if (DEBUG_SWAP_POLLING) Log.d("SwapPolling", "status id=$id parsed status='${parsed.status}' error='${parsed.error}'")
      parsed
    } catch (e: Exception) {
      if (DEBUG_SWAP_POLLING) Log.e("SwapPolling", "status id=$id FAILED: ${e::class.simpleName}: ${e.message}")
      null
    }
  }

  private fun JsonElement.findStringDeep(key: String): String? {
    when (this) {
      is JsonObject -> {
        this[key]?.let { el -> (el as? JsonPrimitive)?.contentOrNull?.let { return it } }
        for (v in values) v.findStringDeep(key)?.let { return it }
      }
      is JsonArray -> for (v in this) v.findStringDeep(key)?.let { return it }
      else -> {}
    }
    return null
  }

  fun iconUrl(remoteUrl: String): String =
    "$BASE?action=image&url=${remoteUrl.encodeURLParameter()}"
}
