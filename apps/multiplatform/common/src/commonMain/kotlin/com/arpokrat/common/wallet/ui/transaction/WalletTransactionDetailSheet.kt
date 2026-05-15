package com.arpokrat.common.wallet.ui.transaction

import SectionBottomSpacer
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arpokrat.app.wallet.PriceService
import com.arpokrat.common.platform.ColumnWithScrollBar
import com.arpokrat.common.ui.theme.DEFAULT_PADDING
import com.arpokrat.common.ui.theme.SimplexGreen
import com.arpokrat.common.views.helpers.AppBarTitle
import com.arpokrat.common.views.helpers.AppPrimaryButton
import com.arpokrat.common.wallet.ui.components.TransactionUiModel
import com.arpokrat.res.MR
import dev.icerock.moko.resources.compose.stringResource

/**
 * Bottom sheet displaying the full details of a specific transaction.
 * Dynamically adjusts the displayed value and symbol for smart contract calls (e.g., TRC20/ERC20 token transfers)
 * that might show as 0 native value but consumed network fees.
 */
@Composable
fun TransactionDetailSheet(
  tx: TransactionUiModel,
  myAddress: String,
  symbol: String,
  currency: String,
  currencySymbol: String,
  dateStr: String,
  isDevMode: Boolean,
  onDismiss: () -> Unit,
  onCopy: (String, String) -> Unit,
  onOpenExplorer: (String) -> Unit
) {
  val isSent = tx.from.equals(myAddress, ignoreCase = true)
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

  // Dynamic Toggle: If the transaction value is "0" but incurred network fees (like a token transfer),
  // display the burned fee instead to prevent confusing "-0" UI states.
  val isContractFeeOnly = isSent && cleanValue == "0" && !tx.networkFee.isNullOrBlank()
  val actualDisplayValue = if (isContractFeeOnly) tx.networkFee!! else cleanValue
  val actualDisplaySymbol = if (isContractFeeOnly) symbol else displaySymbol

  val fiatValue = run {
    val amountDouble = actualDisplayValue.toDoubleOrNull() ?: 0.0
    val fiat = PriceService.getValue(actualDisplaySymbol, amountDouble, currency)
    if (fiat == 0.0) "" else String.format(java.util.Locale.US, "%.2f", fiat)
  }

  val typeText = when {
    isContractFeeOnly -> stringResource(MR.strings.wallet_network_fee)
    isSent -> stringResource(MR.strings.wallet_tx_sent_transfer)
    else -> stringResource(MR.strings.wallet_tx_received_transfer)
  }

  Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colors.background)) {

    ColumnWithScrollBar(modifier = Modifier.fillMaxSize()) {
      AppBarTitle(title = stringResource(MR.strings.wallet_tx_details_title))

      Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(typeText, color = Color.Gray, fontSize = 14.sp)
        Spacer(Modifier.height(32.dp))

        Box(
          modifier = Modifier.size(64.dp).clip(CircleShape).background(if (isSent) MaterialTheme.colors.onSurface.copy(0.05f) else SimplexGreen.copy(0.1f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(imageVector = if (isSent) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(32.dp), tint = if (isSent) MaterialTheme.colors.onSurface else SimplexGreen)
        }

        Spacer(Modifier.height(16.dp))

        // Display dynamically resolved value and symbol
        Text(text = "${if(isSent) "-" else "+"}$actualDisplayValue $actualDisplaySymbol", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = if (isSent) MaterialTheme.colors.onSurface else SimplexGreen)

        if (fiatValue.isNotEmpty()) {
          Spacer(Modifier.height(4.dp))
          Text(text = "≈ $currencySymbol$fiatValue", color = Color.Gray, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.height(8.dp))

        if (tx.isError) Text(stringResource(MR.strings.wallet_tx_status_failed), color = MaterialTheme.colors.error, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        else Text(stringResource(MR.strings.wallet_tx_status_success), color = SimplexGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)

        Spacer(Modifier.height(32.dp))

        Card(backgroundColor = MaterialTheme.colors.onBackground.copy(alpha = 0.03f), shape = RoundedCornerShape(16.dp), elevation = 0.dp, modifier = Modifier.fillMaxWidth()) {
          Column(modifier = Modifier.padding(20.dp)) {
            DetailRowItem(stringResource(MR.strings.wallet_tx_label_date), dateStr)
            Divider(color = MaterialTheme.colors.onBackground.copy(0.05f), modifier = Modifier.padding(vertical = 16.dp))
            DetailRowItem(stringResource(MR.strings.wallet_tx_label_from), tx.from, copyable = true, onCopy = { onCopy("Sender", tx.from) })
            Divider(color = MaterialTheme.colors.onBackground.copy(0.05f), modifier = Modifier.padding(vertical = 16.dp))
            DetailRowItem(stringResource(MR.strings.wallet_tx_label_to), tx.to, copyable = true, onCopy = { onCopy("Receiver", tx.to) })
            Divider(color = MaterialTheme.colors.onBackground.copy(0.05f), modifier = Modifier.padding(vertical = 16.dp))
            if (tx.networkFee != null) { DetailRowItem(stringResource(MR.strings.wallet_tx_label_fee), "${tx.networkFee} (Native)", copyable = true, onCopy = { onCopy("Fee", tx.networkFee) }); Divider(color = MaterialTheme.colors.onBackground.copy(0.05f), modifier = Modifier.padding(vertical = 16.dp)) }
            DetailRowItem(stringResource(MR.strings.wallet_tx_label_hash), tx.hash, copyable = true, onCopy = { onCopy("Hash", tx.hash) })

            if (tx.blockNumber != null) { Divider(color = MaterialTheme.colors.onBackground.copy(0.05f), modifier = Modifier.padding(vertical = 16.dp)); DetailRowItem(stringResource(MR.strings.wallet_tx_label_block), tx.blockNumber.toString(), copyable = true, onCopy = { onCopy("Block", tx.blockNumber.toString()) }) }
            if (tx.nonce != null) { Divider(color = MaterialTheme.colors.onBackground.copy(0.05f), modifier = Modifier.padding(vertical = 16.dp)); DetailRowItem(stringResource(MR.strings.wallet_tx_label_nonce), tx.nonce.toString(), copyable = true, onCopy = { onCopy("Nonce", tx.nonce.toString()) }) }
          }
        }
        Spacer(Modifier.height(140.dp))
      }
    }

    Column(
      modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(MaterialTheme.colors.background).padding(horizontal = 24.dp).padding(top = 16.dp)
    ) {
      AppPrimaryButton(
        text = stringResource(MR.strings.wallet_btn_view_explorer),
        onClick = { onOpenExplorer(tx.hash) }
      )
      SectionBottomSpacer()
    }
  }
}

@Composable
fun DetailRowItem(label: String, value: String, copyable: Boolean = false, onCopy: () -> Unit = {}) {
  Column(modifier = Modifier.fillMaxWidth().clickable(enabled = copyable, onClick = onCopy)) {
    Text(label, color = Color.Gray, fontSize = 13.sp)
    Spacer(Modifier.height(6.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text(text = if (value.length > 25) value.take(12) + "..." + value.takeLast(10) else value, color = MaterialTheme.colors.onSurface, fontWeight = FontWeight.Medium, fontSize = 15.sp, modifier = Modifier.weight(1f))
      if (copyable) Icon(Icons.Default.ContentCopy, contentDescription = null, tint = MaterialTheme.colors.primary, modifier = Modifier.size(16.dp))
    }
  }
}