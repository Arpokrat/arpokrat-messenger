package com.arpokrat.common.wallet

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import java.math.BigInteger

/**
 * Service handling interactions with EVM-compatible blockchains (Ethereum, Polygon, etc.).
 * Manages raw JSON-RPC calls for balances and broadcasting, as well as Explorer API calls for history.
 */
class EvmChainService(
  private val rpcUrl: String,
  private val chainId: Int,
  private val explorerApiUrl: String? = "https://api.etherscan.io/v2/api",
  private val explorerApiKey: String? = null
) {

  companion object {
    private val sharedClient = HttpClient {
      install(ContentNegotiation) {
        json(Json {
          ignoreUnknownKeys = true
          isLenient = true
        })
      }
      install(HttpTimeout) {
        requestTimeoutMillis = 15000L
        connectTimeoutMillis = 15000L
        socketTimeoutMillis = 15000L
      }
      expectSuccess = false
    }

    // Prevents nonce collision when sending multiple transactions quickly
    private val localNonceCache = mutableMapOf<String, Long>()
  }

  private fun formatWeiToDecimalString(weiValueStr: String, decimals: Int): String {
    if (weiValueStr.isBlank() || weiValueStr == "0") return "0.0"
    val cleanStr = weiValueStr.filter { it.isDigit() }
    if (cleanStr.isEmpty()) return "0.0"

    return if (cleanStr.length <= decimals) {
      val padding = "0".repeat(decimals - cleanStr.length)
      "0.$padding$cleanStr".trimEnd('0').removeSuffix(".")
    } else {
      val integerPart = cleanStr.substring(0, cleanStr.length - decimals)
      val fractionalPart = cleanStr.substring(cleanStr.length - decimals).trimEnd('0')
      if (fractionalPart.isEmpty()) integerPart else "$integerPart.$fractionalPart"
    }
  }

  private fun hexToLongSafe(hexStr: String): Long {
    val clean = hexStr.removePrefix("0x").filter { it.isLetterOrDigit() }
    if (clean.isEmpty()) return 0L
    return try {
      clean.toLong(16)
    } catch (e: Exception) {
      0L
    }
  }

  /**
   * Executes a standard JSON-RPC standard call to the connected node.
   */
  private suspend fun rpcCall(method: String, params: JsonArray): String? {
    return try {
      val jsonRequest = buildJsonObject {
        put("jsonrpc", "2.0")
        put("method", method)
        put("params", params)
        put("id", 1)
      }

      val response = sharedClient.post(rpcUrl) {
        contentType(ContentType.Application.Json)
        setBody(jsonRequest.toString())
      }

      val responseBody = response.bodyAsText()
      val jsonResponse = Json.parseToJsonElement(responseBody).jsonObject

      if (jsonResponse.containsKey("error")) {
        val errMsg = jsonResponse["error"]?.jsonObject?.get("message")?.jsonPrimitive?.content ?: "Unknown network error"
        throw Exception("API: $errMsg")
      }

      jsonResponse["result"]?.jsonPrimitive?.content
    } catch (e: Exception) {
      if (method == "eth_sendRawTransaction" || method == "eth_getTransactionCount" || method == "eth_gasPrice") {
        throw e
      }
      null
    }
  }

  /**
   * Retrieves the native coin balance (ETH, MATIC/POL) of the given address.
   */
  suspend fun getBalance(address: String): String {
    return withContext(Dispatchers.IO) {
      try {
        val safeAddress = if (address.startsWith("0x")) address.lowercase() else "0x${address.lowercase()}"
        val params = buildJsonArray { add(safeAddress); add("latest") }
        val responseHex = rpcCall("eth_getBalance", params)

        if (responseHex.isNullOrBlank() || responseHex == "0x" || responseHex == "0x0") {
          return@withContext "0.0000"
        }
        val cleanHex = responseHex.removePrefix("0x").filter { it.isLetterOrDigit() }
        val decimalStringValue = BigInteger(cleanHex, 16).toString()
        val formatted = formatWeiToDecimalString(decimalStringValue, 18)

        val parts = formatted.split(".")
        if (parts.size == 2 && parts[1].length > 5) "${parts[0]}.${parts[1].take(5)}" else formatted
      } catch (e: Exception) {
        "0.0000"
      }
    }
  }

  suspend fun getNonce(address: String): Long {
    return withContext(Dispatchers.IO) {
      val safeAddress = if (address.startsWith("0x")) address.lowercase() else "0x${address.lowercase()}"
      val params = buildJsonArray { add(safeAddress); add("latest") }
      val responseHex = rpcCall("eth_getTransactionCount", params)
        ?: throw Exception("Unable to fetch Nonce")

      val networkNonce = hexToLongSafe(responseHex)
      val cacheKey = "${chainId}_$safeAddress"
      val localNonce = localNonceCache[cacheKey] ?: 0L

      maxOf(networkNonce, localNonce)
    }
  }

  suspend fun getGasPrice(): String {
    return withContext(Dispatchers.IO) {
      val responseHex = rpcCall("eth_gasPrice", buildJsonArray {}) ?: throw Exception("GasPrice null")
      val cleanHex = responseHex.removePrefix("0x").filter { it.isLetterOrDigit() }
      val baseGas = BigInteger(cleanHex, 16)

      // Add a 20% safety margin to the base gas price to ensure faster transaction inclusion
      val safeGas = baseGas.multiply(BigInteger.valueOf(120)).divide(BigInteger.valueOf(100))
      safeGas.toString()
    }
  }

  suspend fun broadcastTransaction(signedHex: String): String {
    return withContext(Dispatchers.IO) {
      val params = buildJsonArray { add(signedHex) }
      rpcCall("eth_sendRawTransaction", params) ?: throw Exception("Broadcast failed")
    }
  }

  /**
   * Retrieves the balance of a specific ERC20 token for the given address via eth_call.
   */
  suspend fun getERC20Balance(tokenContract: String, ownerAddress: String, decimals: Int): String {
    return withContext(Dispatchers.IO) {
      try {
        val methodId = "0x70a08231" // balanceOf(address)
        val cleanAddress = ownerAddress.removePrefix("0x").lowercase()
        val paddedAddress = cleanAddress.padStart(64, '0')
        val data = methodId + paddedAddress

        val txObj = buildJsonObject {
          put("to", tokenContract)
          put("data", data)
        }
        val params = buildJsonArray { add(txObj); add("latest") }
        val responseHex = rpcCall("eth_call", params) ?: return@withContext "0.0"
        val cleanHex = responseHex.removePrefix("0x").filter { it.isLetterOrDigit() }

        if (cleanHex.isEmpty() || cleanHex == "0") return@withContext "0.0"
        val decimalStringValue = BigInteger(cleanHex, 16).toString()
        formatWeiToDecimalString(decimalStringValue, decimals)
      } catch (e: Exception) {
        "0.0"
      }
    }
  }

  suspend fun getTokenMetadata(contractAddress: String): Token? {
    return withContext(Dispatchers.IO) { Token("Unknown", "ERC20", contractAddress, 18) }
  }

  /**
   * Retrieves the transaction history using a block explorer API (e.g., Etherscan, Polygonscan).
   * Automatically routes to 'txlist' for native coins and 'tokentx' for ERC20 tokens.
   */
  suspend fun getHistory(address: String, contractAddress: String? = null): List<TransactionItem> {
    val apiUrl = explorerApiUrl ?: "https://api.etherscan.io/v2/api"

    return withContext(Dispatchers.IO) {
      val isNative = FeeService.isNativeToken(contractAddress)
      val action = if (isNative) "txlist" else "tokentx"

      // FIX: Convert TrustWallet ID to the actual ChainID required by Etherscan v2 API routing
      val actualChainId = when(chainId) {
        60 -> 1
        966 -> 137
        else -> chainId
      }

      val urlBuilder = URLBuilder(apiUrl).apply {
        if (apiUrl.contains("v2")) {
          parameters.append("chainid", actualChainId.toString())
        }
        parameters.append("module", "account")
        parameters.append("action", action)
        parameters.append("address", address.trim())
        parameters.append("startblock", "0")
        parameters.append("endblock", "latest")
        parameters.append("sort", "desc")
        parameters.append("apikey", explorerApiKey?.trim() ?: "")

        if (!isNative) {
          parameters.append("contractaddress", contractAddress!!.trim())
        }
      }

      try {
        val finalUrl = urlBuilder.buildString()
        val responseText = sharedClient.get(finalUrl) {
          header(io.ktor.http.HttpHeaders.UserAgent, "Mozilla/5.0")
        }.bodyAsText()

        if (responseText.trimStart().startsWith("<")) return@withContext emptyList()

        val jsonObj = Json.parseToJsonElement(responseText).jsonObject
        val status = jsonObj["status"]?.jsonPrimitive?.content
        if (status == "0") return@withContext emptyList()

        val resultElement = jsonObj["result"] as? JsonArray ?: return@withContext emptyList()
        val list = mutableListOf<TransactionItem>()

        for (txElement in resultElement) {
          val tx = txElement.jsonObject
          val decimals = tx["tokenDecimal"]?.jsonPrimitive?.content?.toIntOrNull() ?: 18
          val rawValue = tx["value"]?.jsonPrimitive?.content ?: "0"
          val amountStr = formatWeiToDecimalString(rawValue, decimals)
          val toAddress = tx["to"]?.jsonPrimitive?.content ?: ""

          val parsedSymbol = tx["tokenSymbol"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }
            ?: if (chainId == 137 || chainId == 80002) "POL" else "ETH"

          list.add(TransactionItem(
            hash = tx["hash"]?.jsonPrimitive?.content ?: "",
            from = tx["from"]?.jsonPrimitive?.content ?: "",
            to = toAddress,
            value = amountStr,
            timeStamp = tx["timeStamp"]?.jsonPrimitive?.long ?: 0L,
            isError = tx["isError"]?.jsonPrimitive?.content == "1",
            tokenSymbol = parsedSymbol,
            nonce = tx["nonce"]?.jsonPrimitive?.content?.toLongOrNull(),
            blockNumber = tx["blockNumber"]?.jsonPrimitive?.content?.toLongOrNull(),
            networkFee = null
          ))
        }
        return@withContext list
      } catch (e: Exception) { emptyList() }
    }
  }

  private fun toWeiString(amount: String, decimals: Int = 18): String {
    return try {
      val cleanAmount = amount.replace(",", ".").filter { it.isDigit() || it == '.' || it == 'E' || it == 'e' || it == '-' || it == '+' }
      if (cleanAmount.isEmpty()) return "0"

      val bigDecimalValue = java.math.BigDecimal(cleanAmount)
      val multiplier = java.math.BigDecimal.TEN.pow(decimals)

      bigDecimalValue.multiply(multiplier).toBigInteger().toString()
    } catch (e: Exception) {
      "0"
    }
  }

  /**
   * Orchestrates the complete EVM transaction lifecycle:
   * 1. Fetches current nonce and estimates network gas price.
   * 2. Delegates payload construction and signing to the WalletManager.
   * 3. Broadcasts the signed hex to the network.
   * 4. Updates the local nonce cache to allow immediate subsequent transactions.
   */
  suspend fun sendTransaction(
    walletManager: com.arpokrat.common.wallet.WalletManager,
    toAddress: String,
    amountCrypto: String,
    contractAddress: String?,
    decimals: Int = 18
  ): String? {
    return withContext(Dispatchers.IO) {
      try {
        val myAddress = walletManager.getAddressForNetwork(chainId)
        val nonce = getNonce(myAddress)
        val gasPriceWeiStr = getGasPrice()

        val signedTxHex = if (contractAddress.isNullOrBlank()) {
          walletManager.signTransaction(
            toAddress = toAddress.trim().lowercase(),
            amountInWei = toWeiString(amountCrypto, 18),
            nonce = nonce,
            gasPrice = gasPriceWeiStr,
            gasLimit = 21000L,
            chainId = chainId.toLong()
          )
        } else {
          walletManager.signERC20Transaction(
            tokenContractAddress = contractAddress.trim().lowercase(),
            toReceiverAddress = toAddress.trim().lowercase(),
            amountInUnits = toWeiString(amountCrypto, decimals),
            nonce = nonce,
            gasPrice = gasPriceWeiStr,
            gasLimit = 120000L,
            chainId = chainId.toLong()
          )
        }

        val txHash = broadcastTransaction(signedTxHex)
        val cacheKey = "${chainId}_${myAddress.trim().lowercase()}"
        localNonceCache[cacheKey] = nonce + 1
        return@withContext txHash
      } catch (e: Exception) {
        return@withContext "Error: ${e.message}"
      }
    }
  }
}