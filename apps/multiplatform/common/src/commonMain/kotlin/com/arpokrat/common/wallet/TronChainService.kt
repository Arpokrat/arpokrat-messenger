package com.arpokrat.common.wallet

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import java.math.BigInteger

/**
 * Service handling interactions with the Tron blockchain via JSON-RPC.
 * Manages fetching balances (native TRX and TRC20 tokens), transaction history,
 * custom Base58 address decoding, and raw transaction broadcasting.
 */
class TronChainService(
  private val rpcUrl: String,
  private val explorerUrl: String
) {
  private val client = HttpClient {
    install(ContentNegotiation) {
      json(Json {
        ignoreUnknownKeys = true
        isLenient = true
      })
    }
  }

  private val TRX_DECIMALS = 6

  private fun formatTokenAmount(amountStr: String, decimals: Int): String {
    val cleanStr = amountStr.filter { it.isDigit() }
    if (cleanStr.isEmpty() || cleanStr == "0") return "0.0"

    return if (cleanStr.length <= decimals) {
      val padding = "0".repeat(decimals - cleanStr.length)
      "0.$padding$cleanStr".trimEnd('0').removeSuffix(".")
    } else {
      val integerPart = cleanStr.substring(0, cleanStr.length - decimals)
      val fractionalPart = cleanStr.substring(cleanStr.length - decimals).trimEnd('0')
      if (fractionalPart.isEmpty()) integerPart else "$integerPart.$fractionalPart"
    }
  }

  suspend fun getTrxBalance(address: String): String {
    return withContext(Dispatchers.IO) {
      try {
        val url = "$explorerUrl/api/account?address=$address"
        val responseText = client.get(url).bodyAsText()
        if (responseText.isBlank()) return@withContext "0.0000"

        val json = Json.parseToJsonElement(responseText).jsonObject
        var balanceSunStr = json["balance"]?.jsonPrimitive?.content ?: "-1"

        if (balanceSunStr == "-1") {
          val balancesArr = json["balances"]?.jsonArray
          if (balancesArr != null) {
            for (tokenElement in balancesArr) {
              val token = tokenElement.jsonObject
              if (token["tokenName"]?.jsonPrimitive?.content == "trx") {
                balanceSunStr = token["balance"]?.jsonPrimitive?.content ?: "0"
                break
              }
            }
          }
        }

        if (balanceSunStr == "-1" || balanceSunStr == "0") return@withContext "0.0000"

        val formatted = formatTokenAmount(balanceSunStr, TRX_DECIMALS)
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

  suspend fun getTrc20Balance(contractAddress: String, ownerAddress: String): String {
    return withContext(Dispatchers.IO) {
      try {
        val url = "$explorerUrl/api/account?address=$ownerAddress"
        val responseText = client.get(url).bodyAsText()
        if (responseText.isBlank()) return@withContext "0.00"

        val json = Json.parseToJsonElement(responseText).jsonObject
        val trc20Balances = json["trc20token_balances"]?.jsonArray

        if (trc20Balances != null) {
          for (tokenElement in trc20Balances) {
            val token = tokenElement.jsonObject

            if (token["tokenId"]?.jsonPrimitive?.content == contractAddress) {
              val balanceRaw = token["balance"]?.jsonPrimitive?.content ?: "0"
              val decimals = token["tokenDecimal"]?.jsonPrimitive?.int ?: 6

              if (balanceRaw.isEmpty() || balanceRaw == "0") return@withContext "0.00"

              val formattedAmount = formatTokenAmount(balanceRaw, decimals)

              val parts = formattedAmount.split(".")
              return@withContext if (parts.size == 2 && parts[1].length > 2) {
                "${parts[0]}.${parts[1].take(2)}"
              } else {
                formattedAmount
              }
            }
          }
        }
        return@withContext "0.00"
      } catch (e: Exception) {
        "0.00"
      }
    }
  }

  suspend fun getHistory(address: String, contractAddress: String? = null): List<TransactionItem> {
    return withContext(Dispatchers.IO) {
      try {
        val list = mutableListOf<TransactionItem>()

        val isNative = FeeService.isNativeToken(contractAddress)

        if (isNative) {
          val url = "$explorerUrl/api/transaction?sort=-timestamp&count=true&limit=20&start=0&address=$address"
          val responseText = client.get(url).bodyAsText()
          if (responseText.isBlank()) return@withContext emptyList()

          val json = Json.parseToJsonElement(responseText).jsonObject
          val dataArr = json["data"]?.jsonArray ?: return@withContext emptyList()

          for (txElement in dataArr) {
            val tx = txElement.jsonObject
            val contractType = tx["contractType"]?.jsonPrimitive?.int ?: 0
            if (contractType == 2) continue

            val contractData = tx["contractData"]?.jsonObject

            var amountStr = "0"
            if (contractData != null && contractData.containsKey("amount")) {
              val rawAmount = contractData["amount"]?.jsonPrimitive?.content ?: "0"
              amountStr = formatTokenAmount(rawAmount, TRX_DECIMALS)
            } else {
              val rawAmount = tx["amount"]?.jsonPrimitive?.content ?: "0"
              amountStr = formatTokenAmount(rawAmount, TRX_DECIMALS)
            }

            val toAddr = tx["toAddress"]?.jsonPrimitive?.content ?: ""
            val isReceived = toAddr.equals(address, ignoreCase = true)
            val amountDouble = amountStr.toDoubleOrNull() ?: 0.0

            if (isReceived && amountDouble > 0.0 && amountDouble < 0.001) continue

            val timeStamp = tx["timestamp"]?.jsonPrimitive?.longOrNull ?: 0L
            val blockNum = tx["block"]?.jsonPrimitive?.longOrNull
            val costObj = tx["cost"]?.jsonObject
            val feeSun = costObj?.get("fee")?.jsonPrimitive?.longOrNull ?: tx["fee"]?.jsonPrimitive?.longOrNull
            val feeTrxStr = feeSun?.let { formatTokenAmount(it.toString(), TRX_DECIMALS) }

            list.add(TransactionItem(
              hash = tx["hash"]?.jsonPrimitive?.content ?: "",
              from = tx["ownerAddress"]?.jsonPrimitive?.content ?: "",
              to = toAddr,
              value = amountStr,
              timeStamp = timeStamp / 1000,
              isError = tx["result"]?.jsonPrimitive?.content != "SUCCESS",
              tokenSymbol = "TRX",
              nonce = null,
              blockNumber = blockNum,
              networkFee = feeTrxStr
            ))
          }

        } else {
          val url = "$explorerUrl/api/token_trc20/transfers?limit=20&start=0&sort=-timestamp&count=true&relatedAddress=$address&contractAddress=${contractAddress?.trim()}"
          val responseText = client.get(url).bodyAsText()
          if (responseText.isBlank()) return@withContext emptyList()

          val json = Json.parseToJsonElement(responseText).jsonObject
          val tokenTransfers = json["token_transfers"]?.jsonArray ?: return@withContext emptyList()

          for (txElement in tokenTransfers) {
            val tx = txElement.jsonObject

            val timeStamp = tx["block_ts"]?.jsonPrimitive?.longOrNull ?: 0L
            val rawAmount = tx["quant"]?.jsonPrimitive?.content ?: "0"

            val blockNum = tx["block"]?.jsonPrimitive?.longOrNull
            val costObj = tx["cost"]?.jsonObject
            val feeSun = costObj?.get("fee")?.jsonPrimitive?.longOrNull ?: tx["fee"]?.jsonPrimitive?.longOrNull
            val feeTrxStr = feeSun?.let { formatTokenAmount(it.toString(), TRX_DECIMALS) }

            val tokenInfo = tx["tokenInfo"]?.jsonObject
            val decimals = tokenInfo?.get("tokenDecimal")?.jsonPrimitive?.int ?: 6

            val symbol = try { tokenInfo?.get("tokenAbbr")?.jsonPrimitive?.content ?: "USDT" } catch (e: Exception) { "USDT" }

            val amountVal = formatTokenAmount(rawAmount, decimals)

            list.add(TransactionItem(
              hash = tx["transaction_id"]?.jsonPrimitive?.content ?: "",
              from = tx["from_address"]?.jsonPrimitive?.content ?: "",
              to = tx["to_address"]?.jsonPrimitive?.content ?: "",
              value = amountVal,
              timeStamp = timeStamp / 1000,
              isError = false,
              tokenSymbol = symbol,
              nonce = null,
              blockNumber = blockNum,
              networkFee = feeTrxStr
            ))
          }
        }
        return@withContext list
      } catch (e: Exception) {
        emptyList()
      }
    }
  }

  private suspend fun postRequest(endpoint: String, jsonBody: JsonObject): String? {
    return try {
      val response = client.post("$rpcUrl/$endpoint") {
        contentType(ContentType.Application.Json)
        setBody(jsonBody.toString())
      }
      response.bodyAsText()
    } catch (e: Exception) {
      null
    }
  }

  private fun toTokenBaseString(amount: String, decimals: Int): String {
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
   * Standalone mathematical decoder.
   * Converts Tron Base58 addresses to pure Hexadecimal without relying on external libraries,
   * bypassing known conversion bugs in underlying dependencies.
   */
  private fun decodeTronBase58ToHex(base58: String): String {
    val alphabet = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"
    var num = java.math.BigInteger.ZERO
    val base = java.math.BigInteger.valueOf(58)

    val cleanAddress = base58.trim()
    for (char in cleanAddress) {
      val index = alphabet.indexOf(char)
      if (index == -1) throw Exception("Invalid Base58 character: $char")
      num = num.multiply(base).add(java.math.BigInteger.valueOf(index.toLong()))
    }

    val hex = num.toString(16)
    // A Tron address with Checksum consists of 50 hexadecimal characters
    val paddedHex = hex.padStart(50, '0')

    // Extract only the first 42 characters (The '41' prefix + the 20-byte address)
    return paddedHex.substring(0, 42)
  }

  suspend fun sendTransaction(
    walletManager: com.arpokrat.common.wallet.WalletManager,
    networkId: Int,
    toAddress: String,
    amountCrypto: String,
    contractAddress: String?,
    decimals: Int = 6
  ): String? {
    return withContext(Dispatchers.IO) {
      try {
        val myAddress = walletManager.getAddressForNetwork(networkId)
        val transactionJson: JsonObject
        val txId: String
        val isNative = FeeService.isNativeToken(contractAddress)

        if (isNative) {
          val amountSunStr = toTokenBaseString(amountCrypto, 6)
          val reqBody = buildJsonObject {
            put("to_address", toAddress.trim())
            put("owner_address", myAddress)
            put("amount", amountSunStr.toLongOrNull() ?: 0L)
            put("visible", true)
          }

          val resp = postRequest("wallet/createtransaction", reqBody) ?: throw Exception("RPC unreachable")
          transactionJson = Json.parseToJsonElement(resp).jsonObject

          if (transactionJson.containsKey("Error")) {
            throw Exception(transactionJson["Error"]?.jsonPrimitive?.content)
          }
          txId = transactionJson["txID"]?.jsonPrimitive?.content ?: throw Exception("Failed to create TRX tx")

        } else {
          val amountTokenStr = toTokenBaseString(amountCrypto, decimals)

          // Use the internal mathematical converter to ensure stability
          val rawHexAddress = decodeTronBase58ToHex(toAddress)

          // 1. ABI Encoding (Remove the '41' prefix and pad to 64 characters)
          val cleanReceiverHex = if (rawHexAddress.startsWith("41")) rawHexAddress.substring(2) else rawHexAddress
          val paddedAddress = cleanReceiverHex.padStart(64, '0')

          val hexAmount = BigInteger(amountTokenStr).toString(16)
          val paddedAmount = hexAmount.padStart(64, '0')
          val abiParameters = paddedAddress + paddedAmount

          // 2. Base58 RPC Request (visible: true ensures the contract is resolved correctly by the node)
          val reqBody = buildJsonObject {
            put("contract_address", contractAddress!!.trim())
            put("owner_address", myAddress)
            put("function_selector", "transfer(address,uint256)")
            put("parameter", abiParameters)                   // Validated Hex parameter
            put("fee_limit", 150_000_000L)                    // 150 TRX max fee limit
            put("call_value", 0)
            put("visible", true)
          }

          val resp = postRequest("wallet/triggersmartcontract", reqBody) ?: throw Exception("RPC unreachable")
          val fullJson = Json.parseToJsonElement(resp).jsonObject

          if (fullJson.containsKey("Error")) {
            throw Exception("Tron API Error: ${fullJson["Error"]?.jsonPrimitive?.content}")
          }

          if (fullJson.containsKey("result")) {
            val resObj = fullJson["result"]?.jsonObject
            if (resObj?.containsKey("code") == true && resObj["code"]?.jsonPrimitive?.content != "SUCCESS") {
              val code = resObj["code"]?.jsonPrimitive?.content ?: "ERROR"
              val msgHex = resObj["message"]?.jsonPrimitive?.content ?: ""
              val decoded = try { String(msgHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()) } catch(e:Exception) { msgHex }
              throw Exception("$code: $decoded")
            }
          }

          transactionJson = fullJson["transaction"]?.jsonObject ?: throw Exception("Transaction object missing")
          txId = transactionJson["txID"]?.jsonPrimitive?.content ?: throw Exception("No txID in response")
        }

        // Sign and Broadcast
        val signatureHex = walletManager.signTronRawHash(txId)
        val signedTxJson = buildJsonObject {
          transactionJson.entries.forEach { put(it.key, it.value) }
          put("signature", buildJsonArray { add(signatureHex) })
        }

        val broadcastResp = postRequest("wallet/broadcasttransaction", signedTxJson)
        val broadcastJson = Json.parseToJsonElement(broadcastResp ?: "{}").jsonObject

        if (broadcastJson["result"]?.jsonPrimitive?.boolean == true) {
          return@withContext txId
        } else {
          val code = broadcastJson["code"]?.jsonPrimitive?.content ?: "UNKNOWN"
          val msgHex = broadcastJson["message"]?.jsonPrimitive?.content ?: ""
          val decoded = try { String(msgHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()) } catch(e:Exception) { msgHex }
          return@withContext "Broadcast Error: $code - $decoded"
        }
      } catch (e: Exception) {
        return@withContext "Error: ${e.message}"
      }
    }
  }
}