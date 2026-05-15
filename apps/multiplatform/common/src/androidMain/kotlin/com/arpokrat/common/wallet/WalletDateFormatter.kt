package com.arpokrat.common.wallet.ui.components

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

actual fun formatTimestamp(ts: Long): String {
  val millis = if (ts < 10000000000L) ts * 1000 else ts
  val sdf = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault())
  return sdf.format(Date(millis))
}