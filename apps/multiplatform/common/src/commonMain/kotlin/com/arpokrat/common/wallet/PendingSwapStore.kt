package com.arpokrat.common.wallet

expect object PendingSwapStore {
  fun save(swap: PendingSwap)
  fun load(): PendingSwap?
  fun clear()
}
