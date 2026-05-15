package com.arpokrat.common.wallet

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import java.math.BigDecimal
import kotlin.math.abs

private data class SolTxResult(
  val amount: String,
  val isSent: Boolean,
  val feeSol: String?
)

/**
 * Service handling interactions with the Solana blockchain via JSON-RPC.
 * Manages fetching balances, transaction history (using balance deltas),
 * retrieving recent blockhashes, and broadcasting base58-encoded transactions.
 */
class SolanaChainService(
  private val rpcUrl: String = "https://api.devnet.solana.com"
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
  }

  /**
   * Executes a standard JSON-RPC call to the Solana node.
   */
  private suspend fun rpcCall(method: String, params: JsonArray): JsonElement? {
    return try {
      val jsonRequest = buildJsonObject {
        put("jsonrpc", "2.0")
        put("id", 1)
        put("method", method)
        put("params", params)
      }

      val response = sharedClient.post(rpcUrl) {
        contentType(ContentType.Application.Json)
        setBody(jsonRequest.toString())
      }

      val responseBody = response.bodyAsText()
      val jsonResponse = Json.parseToJsonElement(responseBody).jsonObject

      if (jsonResponse.containsKey("error")) {
        return null
      }

      jsonResponse["result"]
    } catch (e: Exception) {
      null
    }
  }

  /**
   * Fetches the native SOL balance for the given address.
   * Converts the raw lamports (1 billionth of a SOL) to a formatted decimal string.
   */
  suspend fun getBalance(address: String): String {
    return withContext(Dispatchers.IO) {
      try {
        val params = buildJsonArray { add(address) }
        val resultElement = rpcCall("getBalance", params) ?: return@withContext "0.0000"
        val lamports = resultElement.jsonObject["value"]?.jsonPrimitive?.longOrNull ?: 0L

        if (lamports <= 0) return@withContext "0.0000"

        val valSol = lamports.toDouble() / 1_000_000_000.0
        val formatted = String.format(java.util.Locale.US, "%.6f", valSol).trimEnd('0').trimEnd('.')
        val parts = formatted.split(".")
        return@withContext if (parts.size == 2 && parts[1].length > 4) {
          "${parts[0]}.${parts[1].take(4)}"
        } else {
          formatted
        }

      } catch (e: Exception) {
        "0.0000"
      }
    }
  }

  /**
   * Retrieves the transaction history for a specific address.
   * First fetches the signatures, then iteratively fetches detailed info for each valid signature.
   */
  suspend fun getHistory(address: String): List<TransactionItem> {
    return withContext(Dispatchers.IO) {
      try {
        val config = buildJsonObject { put("limit", 10) }
        val params = buildJsonArray { add(address); add(config) }

        val resultElement = rpcCall("getSignaturesForAddress", params) ?: return@withContext emptyList()
        val txsArray = resultElement.jsonArray
        val list = mutableListOf<TransactionItem>()

        for (txElement in txsArray) {
          val tx = txElement.jsonObject
          val signature = tx["signature"]?.jsonPrimitive?.content ?: continue
          val timeStamp = tx["blockTime"]?.jsonPrimitive?.longOrNull ?: (System.currentTimeMillis() / 1000)

          val slot = tx["slot"]?.jsonPrimitive?.longOrNull

          val isError = tx["err"] != null && tx["err"] !is JsonNull

          var amountStr = "0.00"
          var isSent = false
          var feeStr: String? = null

          if (!isError) {
            val details = getTransactionDetails(signature, address)
            if (details != null) {
              val amountDouble = details.amount.toDoubleOrNull() ?: 0.0

              // Spam filter: ignore incoming dusting transactions below 0.0001 SOL
              if (!details.isSent && amountDouble < 0.0001) continue

              amountStr = details.amount
              isSent = details.isSent
              feeStr = details.feeSol
            }
          } else {
            // Ignore failed incoming transactions entirely
            if (!isSent) continue
          }

          list.add(TransactionItem(
            hash = signature,
            from = if (isSent) address else "External",
            to = if (isSent) "External" else address,
            value = amountStr,
            timeStamp = timeStamp,
            isError = isError,
            tokenSymbol = "SOL",
            nonce = null,
            blockNumber = slot,
            networkFee = feeStr
          ))
        }
        return@withContext list
      } catch (e: Exception) {
        emptyList()
      }
    }
  }

  /**
   * Parses deep transaction data to determine the net transfer amount and direction.
   * Unlike EVM, Solana calculates transfers by comparing an account's preBalances and postBalances.
   */
  private suspend fun getTransactionDetails(signature: String, myAddress: String): SolTxResult? {
    return try {
      val config = buildJsonObject {
        put("encoding", "json")
        put("maxSupportedTransactionVersion", 0)
      }

      val params = buildJsonArray { add(signature); add(config) }

      val resultElement = rpcCall("getTransaction", params) ?: return null

      if (resultElement is JsonNull) return null

      val json = resultElement.jsonObject
      val meta = json["meta"]?.jsonObject ?: return null
      val transaction = json["transaction"]?.jsonObject ?: return null
      val message = transaction["message"]?.jsonObject ?: return null
      val accountKeys = message["accountKeys"]?.jsonArray ?: return null

      val feeLamports = meta["fee"]?.jsonPrimitive?.longOrNull ?: 0L
      val feeSolStr = String.format(java.util.Locale.US, "%.6f", feeLamports.toDouble() / 1_000_000_000.0).trimEnd('0').trimEnd('.')

      // Find the index of the user's address in the transaction accounts array
      var myIndex = -1
      for (i in 0 until accountKeys.size) {
        val key = accountKeys[i].jsonPrimitive.content
        if (key == myAddress) {
          myIndex = i
          break
        }
      }

      if (myIndex != -1) {
        val preBalances = meta["preBalances"]?.jsonArray ?: return null
        val postBalances = meta["postBalances"]?.jsonArray ?: return null

        val pre = try { preBalances[myIndex].jsonPrimitive.long } catch (e: Exception) { 0L }
        val post = try { postBalances[myIndex].jsonPrimitive.long } catch (e: Exception) { 0L }

        // Negative diff means funds left the account (Sent)
        val diff = post - pre

        // Ignore micro-deltas representing only network fee payments
        if (abs(diff) < 5000) return SolTxResult("0.00", true, feeSolStr)

        val absDiff = abs(diff)
        val valSol = absDiff.toDouble() / 1_000_000_000.0

        val formatted = String.format(java.util.Locale.US, "%.6f", valSol).trimEnd('0').trimEnd('.')
        val parts = formatted.split(".")
        val finalAmount = if (parts.size == 2 && parts[1].length > 4) {
          "${parts[0]}.${parts[1].take(4)}"
        } else {
          formatted
        }

        return SolTxResult(finalAmount, diff < 0, feeSolStr)
      }
      null
    } catch (e: Exception) { null }
  }

  /**
   * Fetches the latest blockhash from the network.
   * This is a mandatory parameter for signing any new Solana transaction.
   */
  suspend fun getRecentBlockhash(): String? {
    return withContext(Dispatchers.IO) {
      try {
        val config = buildJsonObject { put("commitment", "finalized") }
        val params = buildJsonArray { add(config) }

        val resultElement = rpcCall("getLatestBlockhash", params)
        if (resultElement == null) {
          return@withContext null
        }

        val value = resultElement.jsonObject["value"]?.jsonObject
        val hash = value?.get("blockhash")?.jsonPrimitive?.content

        return@withContext hash
      } catch (e: Exception) {
        null
      }
    }
  }

  /**
   * Broadcasts a fully signed, Base58-encoded transaction to the network.
   */
  suspend fun broadcastTransaction(base58SignedTx: String): String {
    return withContext(Dispatchers.IO) {
      try {
        val config = buildJsonObject { put("encoding", "base58") }
        val params = buildJsonArray { add(base58SignedTx); add(config) }

        val resultElement = rpcCall("sendTransaction", params)

        if (resultElement != null && resultElement !is JsonNull) {
          val txHash = resultElement.jsonPrimitive.content
          return@withContext txHash
        }
        return@withContext "Error"
      } catch (e: Exception) {
        "Error: ${e.message}"
      }
    }
  }

  /**
   * Orchestrates the complete Solana transaction lifecycle:
   * 1. Fetches a recent blockhash.
   * 2. Delegates payload construction and Base58 signing to the WalletManager.
   * 3. Broadcasts the transaction.
   */
  suspend fun sendTransaction(
    walletManager: com.arpokrat.common.wallet.WalletManager,
    toAddress: String,
    amountCrypto: String
  ): String? {
    return withContext(Dispatchers.IO) {
      try {
        val cleanAmount = amountCrypto.replace(",", ".")
        val lamports = try {
          BigDecimal(cleanAmount).multiply(BigDecimal(1_000_000_000)).toLong()
        } catch (e: Exception) {
          0L
        }

        if (lamports <= 0L) throw Exception("Invalid amount")

        val recentBlockhash = getRecentBlockhash() ?: throw Exception("Failed to get recent blockhash")

        val base58SignedTx = walletManager.signSolanaTransaction(
          toAddress = toAddress,
          amountInLamports = lamports,
          recentBlockhash = recentBlockhash
        )

        val txHash = broadcastTransaction(base58SignedTx)
        return@withContext if (txHash == "Error" || txHash.startsWith("Error")) null else txHash
      } catch (e: Exception) {
        null
      }
    }
  }
}