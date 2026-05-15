package com.arpokrat.common.wallet.ui.components

data class TransactionUiModel(
  val hash: String,
  val from: String,
  val to: String,
  val value: String,
  val tokenSymbol: String?,
  val timeStamp: Long,
  val isError: Boolean,
  val nonce: Long? = null,
  val blockNumber: Long? = null,
  val networkFee: String? = null
)