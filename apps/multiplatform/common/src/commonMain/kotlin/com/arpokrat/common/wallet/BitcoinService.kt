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
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Represents an Unspent Transaction Output (UTXO) required to construct new Bitcoin transactions.
 */
data class Utxo(
  val txId: String,
  val vout: Int,
  val amount: Long
)

/**
 * Service handling Bitcoin blockchain interactions using a Mempool.space compatible REST API.
 * Manages UTXO fetching, transaction history parsing, fee estimation, and raw transaction broadcasting.
 */
class BitcoinService(private val apiUrl: String) {

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
   * Fetches the total balance (confirmed + mempool unconfirmed) for a given Bitcoin address.
   * Calculates the balance by subtracting spent outputs from funded outputs.
   */
  suspend fun getBalance(address: String): String {
    return withContext(Dispatchers.IO) {
      try {
        val responseText = sharedClient.get("$apiUrl/address/$address").bodyAsText()
        val json = Json.parseToJsonElement(responseText).jsonObject

        val chainStats = json["chain_stats"]?.jsonObject
        val funded = chainStats?.get("funded_txo_sum")?.jsonPrimitive?.long ?: 0L
        val spent = chainStats?.get("spent_txo_sum")?.jsonPrimitive?.long ?: 0L

        val mempoolStats = json["mempool_stats"]?.jsonObject
        val mempoolFunded = mempoolStats?.get("funded_txo_sum")?.jsonPrimitive?.long ?: 0L
        val mempoolSpent = mempoolStats?.get("spent_txo_sum")?.jsonPrimitive?.long ?: 0L

        val totalSats = (funded - spent) + (mempoolFunded - mempoolSpent)

        if (totalSats <= 0) return@withContext "0.0000"

        val btcValue = BigDecimal(totalSats).divide(BigDecimal(100_000_000), 8, RoundingMode.HALF_UP)
        btcValue.toPlainString().trimEnd('0').removeSuffix(".")
      } catch (e: Exception) {
        "0.0000"
      }
    }
  }

  /**
   * Retrieves and formats the transaction history for a specific address.
   * Since Bitcoin uses UTXOs, this method analyzes inputs (vin) and outputs (vout)
   * to determine the net direction (send vs. receive) and the actual transferred value.
   */
  suspend fun getHistory(address: String): List<TransactionItem> {
    return withContext(Dispatchers.IO) {
      try {
        val responseText = sharedClient.get("$apiUrl/address/$address/txs").bodyAsText()
        if (responseText.isBlank()) return@withContext emptyList()

        val jsonArray = Json.parseToJsonElement(responseText).jsonArray
        val list = mutableListOf<TransactionItem>()

        for (txElement in jsonArray) {
          val tx = txElement.jsonObject
          val txid = tx["txid"]?.jsonPrimitive?.content ?: ""

          val status = tx["status"]?.jsonObject
          val timeStamp = status?.get("block_time")?.jsonPrimitive?.long ?: (System.currentTimeMillis() / 1000)
          val blockHeight = status?.get("block_height")?.jsonPrimitive?.long

          var totalReceived = 0L
          var totalSent = 0L
          var toAddress = ""
          var fromAddress = ""

          // Parse inputs to find outgoing funds
          val vin = tx["vin"]?.jsonArray
          vin?.forEach { inputElement ->
            val prevout = inputElement.jsonObject["prevout"]?.jsonObject
            val inAddr = prevout?.get("scriptpubkey_address")?.jsonPrimitive?.content
            val inValue = prevout?.get("value")?.jsonPrimitive?.long ?: 0L
            if (inAddr == address) totalSent += inValue else if (fromAddress.isEmpty() && inAddr != null) fromAddress = inAddr
          }

          // Parse outputs to find incoming funds
          val vout = tx["vout"]?.jsonArray
          vout?.forEach { outputElement ->
            val outObj = outputElement.jsonObject
            val outAddr = outObj["scriptpubkey_address"]?.jsonPrimitive?.content
            val outValue = outObj["value"]?.jsonPrimitive?.long ?: 0L
            if (outAddr == address) totalReceived += outValue else if (toAddress.isEmpty() && outAddr != null) toAddress = outAddr
          }

          val feeSats = tx["fee"]?.jsonPrimitive?.long ?: 0L
          val feeBtc = BigDecimal(feeSats).divide(BigDecimal(100_000_000), 8, RoundingMode.HALF_UP).toPlainString()

          val isSend = totalSent > totalReceived
          val amountSats = if (isSend) totalSent - totalReceived - feeSats else totalReceived

          // Spam filter: Ignore tiny dusting attacks (less than 1000 sats received)
          if (!isSend && amountSats < 1000L) continue

          val amountBtc = BigDecimal(amountSats).divide(BigDecimal(100_000_000), 8, RoundingMode.HALF_UP).toPlainString()

          list.add(
            TransactionItem(
              hash = txid,
              from = if (isSend) address else fromAddress,
              to = if (isSend) toAddress else address,
              value = amountBtc,
              timeStamp = timeStamp,
              isError = false,
              tokenSymbol = "BTC",
              nonce = null,
              blockNumber = blockHeight,
              networkFee = feeBtc
            )
          )
        }
        list
      } catch (e: Exception) {
        emptyList()
      }
    }
  }

  /**
   * Fetches available UTXOs (Unspent Transaction Outputs) for the given address.
   * Required for building the transaction inputs before signing.
   */
  suspend fun getUtxos(address: String): List<Utxo> {
    return withContext(Dispatchers.IO) {
      try {
        val responseText = sharedClient.get("$apiUrl/address/$address/utxo").bodyAsText()
        val jsonArray = Json.parseToJsonElement(responseText).jsonArray
        val utxos = mutableListOf<Utxo>()

        for (item in jsonArray) {
          val obj = item.jsonObject
          val txid = obj["txid"]?.jsonPrimitive?.content ?: continue
          val vout = obj["vout"]?.jsonPrimitive?.int ?: continue
          val value = obj["value"]?.jsonPrimitive?.long ?: continue
          utxos.add(Utxo(txid, vout, value))
        }
        utxos
      } catch (e: Exception) {
        emptyList()
      }
    }
  }

  /**
   * Retrieves the current recommended network fee rate in satoshis per vByte (sats/vB).
   */
  suspend fun getRecommendedFee(): Long {
    return withContext(Dispatchers.IO) {
      try {
        val responseText = sharedClient.get("$apiUrl/v1/fees/recommended").bodyAsText()
        val json = Json.parseToJsonElement(responseText).jsonObject
        json["fastestFee"]?.jsonPrimitive?.long ?: 10L
      } catch (e: Exception) {
        15L // Fallback fee if API fails
      }
    }
  }

  /**
   * Broadcasts a fully signed raw transaction hex to the Bitcoin network.
   */
  suspend fun broadcastTransaction(signedHex: String): String? {
    return withContext(Dispatchers.IO) {
      try {
        val response = sharedClient.post("$apiUrl/tx") {
          contentType(ContentType.Text.Plain)
          setBody(signedHex)
        }
        val body = response.bodyAsText()

        // Mempool.space returns the 64-character txid on success
        if (response.status.isSuccess() && body.length == 64) {
          body
        } else {
          "Error: API: $body"
        }
      } catch (e: Exception) {
        "Error: ${e.message}"
      }
    }
  }

  /**
   * Orchestrates the complete Bitcoin transaction lifecycle:
   * 1. Fetch available UTXOs
   * 2. Estimate required network fees
   * 3. Delegate UTXO selection and cryptographic signing to the WalletManager
   * 4. Broadcast the signed transaction
   */
  suspend fun sendTransaction(
    walletManager: com.arpokrat.common.wallet.WalletManager,
    toAddress: String,
    amountCrypto: String,
    networkId: Int
  ): String? {
    return withContext(Dispatchers.IO) {
      try {
        val myAddress = walletManager.getAddressForNetwork(networkId)
        val utxos = getUtxos(myAddress)

        if (utxos.isEmpty()) return@withContext "Error: Aucun fond disponible (UTXO vide)"

        val byteFee = getRecommendedFee()
        val amountSats = BigDecimal(amountCrypto.replace(",", "."))
          .multiply(BigDecimal(100_000_000))
          .toLong()

        val signedTxHex = walletManager.signBitcoinTransaction(
          toAddress = toAddress,
          amountInSats = amountSats,
          byteFee = byteFee,
          utxos = utxos,
          networkId = networkId
        )

        return@withContext broadcastTransaction(signedTxHex)
      } catch (e: Exception) {
        "Error: ${e.message}"
      }
    }
  }
}