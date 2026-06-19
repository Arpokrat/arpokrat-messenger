package com.arpokrat.common.wallet

expect object SwapHistoryStore {
  fun list(): List<PendingSwap>
  fun upsert(swap: PendingSwap)
  fun remove(tradeId: String)
}
