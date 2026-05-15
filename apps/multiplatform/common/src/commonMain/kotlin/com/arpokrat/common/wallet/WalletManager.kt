package com.arpokrat.common.wallet

expect class WalletManager() {
  fun hasWallet(): Boolean
  fun createNewWallet(strength: Int, saveNow: Boolean): String
  fun importWalletFromMnemonic(mnemonic: String): Boolean
  fun getMnemonic(): String?
  fun deleteWallet()

  // Signatures
  fun signTransaction(toAddress: String, amountInWei: String, nonce: Long, gasPrice: String, gasLimit: Long, chainId: Long): String
  fun signERC20Transaction(tokenContractAddress: String, toReceiverAddress: String, amountInUnits: String, nonce: Long, gasPrice: String, gasLimit: Long, chainId: Long): String
  fun signSolanaTransaction(toAddress: String, amountInLamports: Long, recentBlockhash: String): String

  // CORRECTION : Ajout de networkId: Int à la fin
  fun signBitcoinTransaction(toAddress: String, amountInSats: Long, byteFee: Long, utxos: List<Utxo>, networkId: Int): String

  fun signTronRawHash(txIdHex: String): String
  fun getTronAddressHex(address: String): String

  // PIN & Lockout
  fun setPin(pin: String?)
  fun checkPin(inputPin: String): Boolean
  fun isPinSet(): Boolean
  fun getFailedAttempts(): Int
  fun getLockoutEndTime(): Long
  fun getRemainingLockoutTime(): Long
  fun recordFailedAttempt()
  fun resetFailedAttempts()

  // Auto Lock
  fun setAutoLockDelay(delayMs: Long)
  fun getAutoLockDelay(): Long
  fun updateLastActiveTime()
  fun shouldAutoLock(): Boolean

  // Preferences (currency)
  fun setCurrency(currency: String)
  fun getCurrency(): String

  // Network
  fun getAddressForNetwork(networkId: Int): String

  // Export Private Keys
  fun getPrivateKeys(): Map<String, String>

  // Block Explorers
  fun getTxExplorerTemplate(networkId: Int): String
  fun setTxExplorerTemplate(networkId: Int, template: String?)
  fun getAddressExplorerTemplate(networkId: Int): String
  fun setAddressExplorerTemplate(networkId: Int, template: String?)

  // Dev Mode
  fun isDeveloperModeEnabled(): Boolean
  fun setDeveloperModeEnabled(enabled: Boolean)
}