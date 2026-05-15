package com.arpokrat.common.wallet.ui.transaction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arpokrat.app.wallet.PriceService
import com.arpokrat.common.ui.theme.DEFAULT_PADDING
import com.arpokrat.common.ui.theme.SimplexGreen
import com.arpokrat.common.wallet.ui.components.TransactionUiModel
import com.arpokrat.common.wallet.ui.components.formatTimestamp
import com.arpokrat.res.MR
import dev.icerock.moko.resources.compose.stringResource

/**
 * A reusable row component for displaying a single transaction item in a list.
 * It intelligently adapts its display if the transaction is a smart contract call
 * (e.g., a token transfer showing 0 native value) by displaying the network fee instead.
 */
@Composable
fun TransactionItemRow(
  tx: TransactionUiModel,
  myAddress: String,
  symbol: String,
  currency: String = "USD",
  currencySymbol: String = "$",
  onClick: () -> Unit
) {
  val isSent = tx.from.equals(myAddress, ignoreCase = true)
  val dateStr = formatTimestamp(tx.timeStamp)

  val displaySymbol = tx.tokenSymbol?.takeIf { it.isNotBlank() } ?: symbol
  val cleanValue = remember(tx.value) {
    try {
      val bd = java.math.BigDecimal(tx.value)
      if (bd.compareTo(java.math.BigDecimal.ZERO) == 0) "0"
      else bd.stripTrailingZeros().toPlainString()
    } catch (e: Exception) {
      tx.value
    }
  }

  // Detection: Smart contract call where native value is zero but a network fee was paid
  val isContractFeeOnly = isSent && cleanValue == "0" && !tx.networkFee.isNullOrBlank()

  // Dynamic Toggle: Display either the standard value/symbol or the burned fee/native symbol
  val actualDisplayValue = if (isContractFeeOnly) tx.networkFee!! else cleanValue
  val actualDisplaySymbol = if (isContractFeeOnly) symbol else displaySymbol

  // Calculate Fiat equivalent based on the displayed value (either amount or fee)
  val fiatValue = run {
    val amountDouble = actualDisplayValue.toDoubleOrNull() ?: 0.0
    val fiat = PriceService.getValue(actualDisplaySymbol, amountDouble, currency)
    if (fiat == 0.0) "" else String.format(java.util.Locale.US, "%.2f", fiat)
  }

  val txStatusText = when {
    tx.isError -> stringResource(MR.strings.wallet_tx_failed)
    isContractFeeOnly -> stringResource(MR.strings.wallet_network_fee)
    isSent -> stringResource(MR.strings.wallet_tx_sent)
    else -> stringResource(MR.strings.wallet_tx_received)
  }

  Card(
    backgroundColor = MaterialTheme.colors.surface,
    shape = RoundedCornerShape(16.dp),
    elevation = 0.dp,
    modifier = Modifier.fillMaxWidth().padding(horizontal = DEFAULT_PADDING).height(88.dp).clickable { onClick() }
  ) {
    Row(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
      Box(
        modifier = Modifier.size(48.dp).background(
          color = if (tx.isError) MaterialTheme.colors.error.copy(alpha = 0.1f) else if (isSent) MaterialTheme.colors.onSurface.copy(alpha = 0.08f) else SimplexGreen.copy(alpha = 0.15f),
          shape = CircleShape
        ),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = if (isSent) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward, contentDescription = null,
          tint = if (tx.isError) MaterialTheme.colors.error else if (isSent) MaterialTheme.colors.onSurface else SimplexGreen,
          modifier = Modifier.size(24.dp)
        )
      }

      Spacer(modifier = Modifier.width(16.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(text = txStatusText, color = MaterialTheme.colors.onSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text(text = dateStr, color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f), fontSize = 14.sp)
      }

      Column(horizontalAlignment = Alignment.End) {
        Text(
          text = "${if (isSent) "-" else "+"}$actualDisplayValue $actualDisplaySymbol",
          color = if (tx.isError) MaterialTheme.colors.onSurface.copy(alpha = 0.4f) else if (isSent) MaterialTheme.colors.onSurface else SimplexGreen,
          fontWeight = FontWeight.Bold, fontSize = 16.sp, textDecoration = if (tx.isError) TextDecoration.LineThrough else null
        )

        if (tx.isError) {
          Text(stringResource(MR.strings.wallet_tx_reverted), color = MaterialTheme.colors.error, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        } else if (fiatValue.isNotEmpty()) {
          Text("≈ $currencySymbol$fiatValue", color = androidx.compose.ui.graphics.Color.Gray, fontSize = 13.sp)
        }
      }
    }
  }
}