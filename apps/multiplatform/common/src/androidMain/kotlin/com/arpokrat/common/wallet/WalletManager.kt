package com.arpokrat.common.wallet

import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.protobuf.ByteString
import com.trustwallet.core.*
import java.math.BigInteger

import wallet.core.jni.proto.Ethereum as EthereumProto
import wallet.core.jni.proto.Solana as SolanaProto
import wallet.core.jni.proto.Bitcoin as BitcoinProto
import wallet.core.jni.proto.Common

private fun BigInteger.toRlpBytes(): ByteArray {
  if (this == BigInteger.ZERO) return ByteArray(0)
  val bytes = this.toByteArray()
  return if (bytes.size > 1 && bytes[0] == 0.toByte()) bytes.copyOfRange(1, bytes.size) else bytes
}

actual class WalletManager actual constructor() {

  companion object {
    private const val PREFS_FILE = "arpokrat_secure_wallet"
    private const val KEY_MNEMONIC = "mnemonic_phrase"
    private const val KEY_PIN = "user_pin"
    private const val KEY_CURRENCY = "app_currency"
    private const val KEY_FAILED_ATTEMPTS = "pin_failed_attempts"
    private const val KEY_LOCKOUT_END_TIME = "pin_lockout_end_time"
    private const val KEY_AUTO_LOCK_DELAY = "auto_lock_delay"
    private const val KEY_LAST_ACTIVE_TIME = "last_active_time"
    private const val KEY_DEV_MODE = "developer_mode_enabled"
  }

  init {
    System.loadLibrary("TrustWalletCore")
  }

  private val securePrefs by lazy {
    val context = WalletContext.appContext
    val masterKey = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
    EncryptedSharedPreferences.create(context, PREFS_FILE, masterKey, EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV, EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM)
  }

  actual fun hasWallet(): Boolean = securePrefs.contains(KEY_MNEMONIC)

  actual fun createNewWallet(strength: Int, saveNow: Boolean): String {
    val wallet = HDWallet(strength, "")
    val mnemonic = wallet.mnemonic
    if (saveNow) securePrefs.edit().putString(KEY_MNEMONIC, mnemonic).apply()
    return mnemonic
  }

  actual fun importWalletFromMnemonic(mnemonic: String): Boolean {
    return try {
      if (mnemonic.trim().split("\\s+".toRegex()).size < 12 || !Mnemonic.isValid(mnemonic)) return false
      securePrefs.edit().putString(KEY_MNEMONIC, HDWallet(mnemonic, "").mnemonic).apply()
      true
    } catch (e: Exception) { false }
  }

  private fun getWallet(): HDWallet? = securePrefs.getString(KEY_MNEMONIC, null)?.let { HDWallet(it, "") }
  actual fun getMnemonic(): String? = securePrefs.getString(KEY_MNEMONIC, null)
  actual fun deleteWallet() { securePrefs.edit().remove(KEY_MNEMONIC).apply() }

  actual fun signTransaction(toAddress: String, amountInWei: String, nonce: Long, gasPrice: String, gasLimit: Long, chainId: Long): String {
    val wallet = getWallet() ?: throw Exception("Wallet not found")
    val privateKey = wallet.getKeyForCoin(CoinType.Ethereum)
    val actualChainId = if (chainId == 60L) 1L else chainId

    // Sanitize destination address
    val safeToAddress = toAddress.trim().lowercase()

    val signingInput = EthereumProto.SigningInput.newBuilder()
      .setChainId(ByteString.copyFrom(BigInteger.valueOf(actualChainId).toRlpBytes()))
      .setNonce(ByteString.copyFrom(BigInteger.valueOf(nonce).toRlpBytes()))
      .setGasPrice(ByteString.copyFrom(BigInteger(gasPrice).toRlpBytes()))
      .setGasLimit(ByteString.copyFrom(BigInteger.valueOf(gasLimit).toRlpBytes()))
      .setToAddress(safeToAddress)
      .setTransaction(EthereumProto.Transaction.newBuilder().setTransfer(
        EthereumProto.Transaction.Transfer.newBuilder()
          .setAmount(ByteString.copyFrom(BigInteger(amountInWei).toRlpBytes()))
          .build()
      ).build())
      .setPrivateKey(ByteString.copyFrom(privateKey.data))
      .build()

    val outputBytes = AnySigner.sign(signingInput.toByteArray(), CoinType.Ethereum)
    val output = EthereumProto.SigningOutput.parseFrom(outputBytes)

    if (output.encoded.isEmpty) throw Exception("TrustWallet Error: Empty signature")

    return "0x" + toHex(output.encoded.toByteArray())
  }

  actual fun signERC20Transaction(tokenContractAddress: String, toReceiverAddress: String, amountInUnits: String, nonce: Long, gasPrice: String, gasLimit: Long, chainId: Long): String {
    val wallet = getWallet() ?: throw Exception("Wallet not found")
    val privateKey = wallet.getKeyForCoin(CoinType.Ethereum)
    val actualChainId = if (chainId == 60L) 1L else chainId

    // Strict address sanitization
    val safeContractAddress = tokenContractAddress.trim().lowercase()
    val safeReceiverAddress = toReceiverAddress.trim().lowercase()

    val methodId = "a9059cbb"
    val cleanAddress = safeReceiverAddress.removePrefix("0x")
    val hexAmount = BigInteger(amountInUnits).toString(16)
    val dataBytes = localHexToBytes(methodId + cleanAddress.padStart(64, '0') + hexAmount.padStart(64, '0'))

    val signingInput = EthereumProto.SigningInput.newBuilder()
      .setChainId(ByteString.copyFrom(BigInteger.valueOf(actualChainId).toRlpBytes()))
      .setNonce(ByteString.copyFrom(BigInteger.valueOf(nonce).toRlpBytes()))
      .setGasPrice(ByteString.copyFrom(BigInteger(gasPrice).toRlpBytes()))
      .setGasLimit(ByteString.copyFrom(BigInteger.valueOf(gasLimit).toRlpBytes()))
      .setToAddress(safeContractAddress)
      .setTransaction(EthereumProto.Transaction.newBuilder().setContractGeneric(
        EthereumProto.Transaction.ContractGeneric.newBuilder()
          .setAmount(ByteString.copyFrom(ByteArray(0)))
          .setData(ByteString.copyFrom(dataBytes))
          .build()
      ).build())
      .setPrivateKey(ByteString.copyFrom(privateKey.data))
      .build()

    val outputBytes = AnySigner.sign(signingInput.toByteArray(), CoinType.Ethereum)
    val output = EthereumProto.SigningOutput.parseFrom(outputBytes)

    if (output.encoded.isEmpty) throw Exception("TrustWallet Error: Empty signature")

    return "0x" + toHex(output.encoded.toByteArray())
  }

  actual fun signSolanaTransaction(toAddress: String, amountInLamports: Long, recentBlockhash: String): String {
    val wallet = getWallet() ?: throw Exception("Wallet not found")
    val privateKey = wallet.getKeyForCoin(CoinType.Solana)

    val signingInput = SolanaProto.SigningInput.newBuilder()
      .setRecentBlockhash(recentBlockhash)
      .setTransferTransaction(SolanaProto.Transfer.newBuilder().setRecipient(toAddress).setValue(amountInLamports).build())
      .setPrivateKey(ByteString.copyFrom(privateKey.data))
      .build()

    val outputBytes = AnySigner.sign(signingInput.toByteArray(), CoinType.Solana)
    return SolanaProto.SigningOutput.parseFrom(outputBytes).encoded
  }

  actual fun signBitcoinTransaction(toAddress: String, amountInSats: Long, byteFee: Long, utxos: List<Utxo>, networkId: Int): String {
    val wallet = getWallet() ?: throw Exception("Wallet not found")
    val coinType = CoinType.Bitcoin
    val privateKey = wallet.getKeyForCoin(coinType)
    val myAddress = wallet.getAddressForCoin(coinType)

    val scriptBytes = buildLockScript(myAddress, coinType)
    val inputProto = BitcoinProto.SigningInput.newBuilder()
      .setAmount(amountInSats)
      .setByteFee(byteFee)
      .setToAddress(toAddress)
      .setChangeAddress(myAddress)
      .setCoinType(if (networkId == 1) 1 else coinType.value.toInt())
      .setHashType(1) // SIGHASH_ALL (1) is required for Bitcoin

    for (utxo in utxos) {
      val txIdBytes = localHexToBytes(utxo.txId)
      val outPoint = BitcoinProto.OutPoint.newBuilder().setHash(ByteString.copyFrom(txIdBytes.reversedArray())).setIndex(utxo.vout).build()
      val utxoProto = BitcoinProto.UnspentTransaction.newBuilder().setOutPoint(outPoint).setAmount(utxo.amount).setScript(ByteString.copyFrom(scriptBytes)).build()
      inputProto.addUtxo(utxoProto)
    }

    inputProto.addPrivateKey(ByteString.copyFrom(privateKey.data))

    // The plan calculates the final fees and the change amount
    val plan = BitcoinProto.TransactionPlan.parseFrom(AnySigner.plan(inputProto.build().toByteArray(), coinType))
    inputProto.setPlan(plan)

    val resultBytes = AnySigner.sign(inputProto.build().toByteArray(), coinType)
    val output = BitcoinProto.SigningOutput.parseFrom(resultBytes)

    if (output.error != Common.SigningError.OK) throw Exception("BTC Sign Error: ${output.error.name}")

    return output.encoded.toByteArray().joinToString("") { it.toUByte().toString(16).padStart(2, '0') }
  }

  actual fun signTronRawHash(txIdHex: String): String {
    val wallet = getWallet() ?: throw Exception("Wallet not found")
    val signature = wallet.getKeyForCoin(CoinType.Tron).sign(localHexToBytes(txIdHex), CoinType.Tron.curve) ?: throw Exception("Tron failed")
    return toHex(signature)
  }

  actual fun getTronAddressHex(address: String): String = toHex(AnyAddress(address, CoinType.Tron).data)

  private fun buildLockScript(address: String, coinType: CoinType): ByteArray {
    val data = try { AnyAddress(address, coinType).data } catch(e: Exception) { ByteArray(0) }
    if (address.startsWith("bc1") || address.startsWith("tb1")) {
      val script = ByteArray(2 + data.size)
      script[0] = 0x00; script[1] = 0x14
      System.arraycopy(data, 0, script, 2, data.size)
      return script
    } else {
      val script = ByteArray(5 + data.size)
      script[0] = 0x76.toByte(); script[1] = 0xA9.toByte(); script[2] = 0x14.toByte()
      System.arraycopy(data, 0, script, 3, data.size)
      script[3 + data.size] = 0x88.toByte(); script[4 + data.size] = 0xAC.toByte()
      return script
    }
  }

  private fun toHex(bytes: ByteArray): String = bytes.joinToString("") { it.toUByte().toString(16).padStart(2, '0') }
  private fun localHexToBytes(hex: String): ByteArray {
    val data = ByteArray(hex.length / 2)
    for (i in hex.indices step 2) data[i / 2] = ((Character.digit(hex[i], 16) shl 4) + Character.digit(hex[i + 1], 16)).toByte()
    return data
  }

  actual fun setPin(pin: String?) { if (pin == null) securePrefs.edit().remove(KEY_PIN).apply() else securePrefs.edit().putString(KEY_PIN, pin).apply() }
  actual fun checkPin(inputPin: String): Boolean = securePrefs.getString(KEY_PIN, null) == inputPin
  actual fun isPinSet(): Boolean = securePrefs.contains(KEY_PIN)
  actual fun setCurrency(currency: String) { securePrefs.edit().putString(KEY_CURRENCY, currency).apply() }
  actual fun getCurrency(): String = securePrefs.getString(KEY_CURRENCY, "USD") ?: "USD"
  actual fun getFailedAttempts(): Int = securePrefs.getInt(KEY_FAILED_ATTEMPTS, 0)
  actual fun getLockoutEndTime(): Long = securePrefs.getLong(KEY_LOCKOUT_END_TIME, 0L)
  actual fun getRemainingLockoutTime(): Long { val end = getLockoutEndTime(); val now = System.currentTimeMillis(); return if (end > now) end - now else 0L }

  actual fun recordFailedAttempt() {
    val attempts = getFailedAttempts() + 1
    securePrefs.edit().putInt(KEY_FAILED_ATTEMPTS, attempts).apply()
    if (attempts >= 3) {
      val dur = when (attempts) { 3 -> 30000L; 4 -> 60000L; 5 -> 300000L; else -> 1800000L }
      securePrefs.edit().putLong(KEY_LOCKOUT_END_TIME, System.currentTimeMillis() + dur).apply()
    }
  }

  actual fun resetFailedAttempts() { securePrefs.edit().remove(KEY_FAILED_ATTEMPTS).remove(KEY_LOCKOUT_END_TIME).apply() }

  actual fun getAddressForNetwork(networkId: Int): String {
    val wallet = getWallet() ?: return ""
    val coin = when (networkId) {
      60, 137, 80002, 11155111 -> CoinType.Ethereum
      0, 1 -> CoinType.Bitcoin
      195, 10000 -> CoinType.Tron
      501, 9000 -> CoinType.Solana
      else -> CoinType.Ethereum
    }
    return wallet.getAddressForCoin(coin)
  }

  actual fun setAutoLockDelay(delayMs: Long) { securePrefs.edit().putLong(KEY_AUTO_LOCK_DELAY, delayMs).apply() }
  actual fun getAutoLockDelay(): Long = securePrefs.getLong(KEY_AUTO_LOCK_DELAY, 0L)
  actual fun updateLastActiveTime() { securePrefs.edit().putLong(KEY_LAST_ACTIVE_TIME, System.currentTimeMillis()).apply() }

  actual fun shouldAutoLock(): Boolean {
    if (!isPinSet()) return false
    val delay = getAutoLockDelay().let { if (it == 0L) 1000L else it }
    val lastActive = securePrefs.getLong(KEY_LAST_ACTIVE_TIME, 0L)
    return if (lastActive == 0L) true else (System.currentTimeMillis() - lastActive) >= delay
  }

  actual fun getPrivateKeys(): Map<String, String> {
    val wallet = getWallet() ?: return emptyMap()
    return mapOf(
      "Bitcoin (BTC)" to toHex(wallet.getKeyForCoin(CoinType.Bitcoin).data),
      "Ethereum (EVM)" to "0x" + toHex(wallet.getKeyForCoin(CoinType.Ethereum).data),
      "Solana (SOL)" to toHex(wallet.getKeyForCoin(CoinType.Solana).data),
      "Tron (TRX)" to toHex(wallet.getKeyForCoin(CoinType.Tron).data)
    )
  }

  actual fun getTxExplorerTemplate(networkId: Int): String = securePrefs.getString("explorer_tx_$networkId", null) ?: when (networkId) {
    0 -> "https://mempool.space/tx/{hash}"; 60 -> "https://etherscan.io/tx/{hash}"; 137 -> "https://polygonscan.com/tx/{hash}"; 195 -> "https://tronscan.org/#/transaction/{hash}"; 501 -> "https://solscan.io/tx/{hash}"; 1 -> "https://mempool.space/testnet/tx/{hash}"; 11155111 -> "https://sepolia.etherscan.io/tx/{hash}"; 80002 -> "https://amoy.polygonscan.com/tx/{hash}"; 10000 -> "https://nile.tronscan.org/#/transaction/{hash}"; 9000 -> "https://solscan.io/tx/{hash}?cluster=devnet"; else -> "https://polygonscan.com/tx/{hash}"
  }

  actual fun setTxExplorerTemplate(networkId: Int, template: String?) { if (template.isNullOrBlank()) securePrefs.edit().remove("explorer_tx_$networkId").apply() else securePrefs.edit().putString("explorer_tx_$networkId", template).apply() }

  actual fun getAddressExplorerTemplate(networkId: Int): String = securePrefs.getString("explorer_addr_$networkId", null) ?: when (networkId) {
    0 -> "https://mempool.space/address/{address}"; 60 -> "https://etherscan.io/address/{address}"; 137 -> "https://polygonscan.com/address/{address}"; 195 -> "https://tronscan.org/#/address/{address}"; 501 -> "https://solscan.io/account/{address}"; 1 -> "https://mempool.space/testnet/address/{address}"; 11155111 -> "https://sepolia.etherscan.io/address/{address}"; 80002 -> "https://amoy.polygonscan.com/address/{address}"; 10000 -> "https://nile.tronscan.org/#/address/{address}"; 9000 -> "solscan.io/account/{address}?cluster=devnet"; else -> "https://polygonscan.com/address/{address}"
  }

  actual fun setAddressExplorerTemplate(networkId: Int, template: String?) { if (template.isNullOrBlank()) securePrefs.edit().remove("explorer_addr_$networkId").apply() else securePrefs.edit().putString("explorer_addr_$networkId", template).apply() }

  actual fun isDeveloperModeEnabled(): Boolean = securePrefs.getBoolean(KEY_DEV_MODE, false)
  actual fun setDeveloperModeEnabled(enabled: Boolean) { securePrefs.edit().putBoolean(KEY_DEV_MODE, enabled).apply() }
}