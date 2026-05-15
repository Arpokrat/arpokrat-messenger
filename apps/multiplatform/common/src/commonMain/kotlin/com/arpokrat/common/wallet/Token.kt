package com.arpokrat.common.wallet

import kotlinx.serialization.Serializable

@Serializable
data class Token(
  val name: String,
  val symbol: String,
  val contractAddress: String,
  val decimals: Int = 18,
  var balance: String = "0.0"
) {
  fun isNative(): Boolean = contractAddress.isEmpty()
}