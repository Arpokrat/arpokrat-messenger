package com.arpokrat.common.wallet

data class TransactionItem(
  val hash: String,
  val from: String,
  val to: String,
  val value: String,
  val timeStamp: Long,
  val isError: Boolean,
  val tokenSymbol: String? = null,
  val nonce: Long? = null,
  val blockNumber: Long? = null,
  val networkFee: String? = null
)