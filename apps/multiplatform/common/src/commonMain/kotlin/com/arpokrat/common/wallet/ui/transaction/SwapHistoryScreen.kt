package com.arpokrat.common.wallet.ui.transaction

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arpokrat.common.platform.ColumnWithScrollBar
import com.arpokrat.common.platform.LazyColumnWithScrollBar
import com.arpokrat.common.ui.theme.SimplexGreen
import com.arpokrat.common.views.chat.topPaddingToContent
import com.arpokrat.common.ui.theme.WarningOrange
import com.arpokrat.common.views.helpers.AppBarTitle
import com.arpokrat.common.views.helpers.AppNotificationManager
import com.arpokrat.common.views.helpers.ModalManager
import com.arpokrat.common.wallet.*
import com.arpokrat.common.wallet.ui.components.WalletSpinner
import com.arpokrat.common.wallet.ui.components.formatTimestamp
import com.arpokrat.res.MR
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.Locale

@Composable
fun SwapHistoryScreen(
  onResume: (PendingSwap) -> Unit,
  isWalletLocked: () -> Boolean = { false },
  onKeepAlive: () -> Unit = {},
  closeFlow: () -> Unit = {},
  initialTab: Int = 0
) {
  var swaps by remember { mutableStateOf(SwapHistoryStore.list()) }
  var tab by remember { mutableStateOf(initialTab) }
  var nowTick by remember { mutableStateOf(System.currentTimeMillis()) }
  val clipboardManager = LocalClipboardManager.current
  val copiedMsg = stringResource(MR.strings.wallet_notif_address_copied)

  fun openDetail(swap: PendingSwap) {
    ModalManager.start.showModalCloseable { _ ->
      SwapDetailScreen(
        swap = swap,
        onCopy = { text -> clipboardManager.setText(AnnotatedString(text)); AppNotificationManager.showSuccess(copiedMsg) }
      )
    }
  }

  LaunchedEffect(Unit) { while (isActive) { onKeepAlive(); delay(500) } }
  LaunchedEffect(Unit) { while (isActive) { if (isWalletLocked()) { closeFlow(); break }; delay(400) } }
  LaunchedEffect(Unit) { while (isActive) { nowTick = System.currentTimeMillis(); delay(1000) } }

  LaunchedEffect(Unit) {
    while (isActive) {
      SwapHistoryStore.list().forEach { s ->
        if (mapSwapStatus(s.lastStatus).isAwaitingDeposit &&
          s.effectiveStatus(System.currentTimeMillis()) == SwapStatusUi.EXPIRED
        ) {
          SwapHistoryStore.upsert(s.copy(lastStatus = "expired"))
          if (PendingSwapStore.load()?.tradeId == s.tradeId) PendingSwapStore.clear()
        }
      }
      val active = SwapHistoryStore.list().filterNot { it.effectiveStatus(System.currentTimeMillis()).isTerminal }
      if (active.isEmpty()) { swaps = SwapHistoryStore.list(); delay(20_000); continue }
      for (s in active) {
        val r = SwapService.status(s.tradeId)
        if (r != null && !r.status.isNullOrBlank()) {
          val api = mapSwapStatus(r.status)
          val deadline = swapExpiryDeadline(s.createdAt, s.expiresAt, System.currentTimeMillis())
          val locallyExpired = api.isAwaitingDeposit && System.currentTimeMillis() >= deadline
          val persisted = if (locallyExpired) "expired" else r.status!!
          SwapHistoryStore.upsert(s.copy(lastStatus = persisted))
          if ((api.isTerminal || locallyExpired) && PendingSwapStore.load()?.tradeId == s.tradeId) {
            PendingSwapStore.clear()
          }
        }
        delay(800)
      }
      swaps = SwapHistoryStore.list()
      delay((active.size * 10_000L).coerceIn(10_000L, 60_000L))
    }
  }

  val activeSwaps = remember(swaps, nowTick) { swaps.filterNot { it.effectiveStatus(nowTick).isTerminal } }
  val doneSwaps = remember(swaps, nowTick) { swaps.filter { it.effectiveStatus(nowTick).isTerminal } }
  val shown = if (tab == 0) activeSwaps else doneSwaps

  LazyColumnWithScrollBar(
    modifier = Modifier.fillMaxSize().background(MaterialTheme.colors.background),
    contentPadding = PaddingValues(top = topPaddingToContent(false), bottom = 32.dp)
  ) {
    item { AppBarTitle(title = stringResource(MR.strings.wallet_swap_history_title)) }

    item {
      Row(
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colors.background).padding(horizontal = 20.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        SwapSegmentChip(stringResource(MR.strings.wallet_swap_history_tab_active, activeSwaps.size), selected = tab == 0, modifier = Modifier.weight(1f)) { tab = 0 }
        SwapSegmentChip(stringResource(MR.strings.wallet_swap_history_tab_done), selected = tab == 1, modifier = Modifier.weight(1f)) { tab = 1 }
      }
    }

    if (shown.isEmpty()) {
      item {
        Text(
          stringResource(if (tab == 0) MR.strings.wallet_swap_history_empty_active else MR.strings.wallet_swap_history_empty_done),
          color = MaterialTheme.colors.onBackground.copy(alpha = 0.5f),
          fontSize = 14.sp,
          modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 32.dp)
        )
      }
    } else {
      items(shown, key = { it.tradeId }) { swap ->
        Box(Modifier.padding(horizontal = 20.dp, vertical = 5.dp)) {
          HistoryRow(
            swap = swap, now = nowTick, showMeta = tab == 1,
            onClick = if (tab == 0) ({ onResume(swap) }) else ({ openDetail(swap) })
          )
        }
      }
    }
  }
}

@Composable
internal fun SwapSegmentChip(text: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = if (selected) MaterialTheme.colors.primary.copy(alpha = 0.12f) else MaterialTheme.colors.onBackground.copy(alpha = 0.04f),
    border = if (selected) BorderStroke(1.dp, MaterialTheme.colors.primary) else null,
    modifier = modifier.clickable(onClick = onClick)
  ) {
    Text(
      text,
      color = if (selected) MaterialTheme.colors.primary else MaterialTheme.colors.onBackground.copy(alpha = 0.7f),
      fontSize = 14.sp, fontWeight = FontWeight.Medium,
      textAlign = androidx.compose.ui.text.style.TextAlign.Center,
      modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)
    )
  }
}

@Composable
private fun HistoryRow(swap: PendingSwap, now: Long, showMeta: Boolean, onClick: (() -> Unit)?) {
  val status = swap.effectiveStatus(now)
  val (label, color) = historyStatus(status)
  Card(
    backgroundColor = MaterialTheme.colors.onBackground.copy(alpha = 0.03f),
    shape = RoundedCornerShape(16.dp), elevation = 0.dp,
    modifier = Modifier.fillMaxWidth().let { if (onClick != null) it.clickable(onClick = onClick) else it }
  ) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          "${fmtAmt(swap.amountIn)} ${swap.fromSymbol.uppercase()}  →  ${swap.toSymbol.uppercase()}",
          color = MaterialTheme.colors.onBackground, fontWeight = FontWeight.Bold, fontSize = 15.sp
        )
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
          if (!status.isTerminal) { WalletSpinner(size = 12, strokeWidth = 2f); Spacer(Modifier.width(6.dp)) }
          Text(label, color = color, fontSize = 12.sp, fontWeight = FontWeight.Medium)
          if (status.isAwaitingDeposit) {
            val remain = ((swapExpiryDeadline(swap.createdAt, swap.expiresAt, now) - now) / 1000L).toInt().coerceAtLeast(0)
            if (remain > 0) {
              Text("  ·  ${fmtCountdown(remain)}", color = MaterialTheme.colors.onBackground.copy(alpha = 0.5f), fontSize = 12.sp)
            }
          }
        }
        if (showMeta) {
          Spacer(Modifier.height(6.dp))
          if (swap.createdAt > 0L) {
            Text(formatTimestamp(swap.createdAt), color = MaterialTheme.colors.onBackground.copy(alpha = 0.5f), fontSize = 12.sp)
          }
          Text(
            stringResource(MR.strings.wallet_swap_history_ref, swap.tradeId),
            color = MaterialTheme.colors.onBackground.copy(alpha = 0.4f), fontSize = 11.sp
          )
        }
      }
      if (onClick != null) Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colors.primary)
    }
  }
}

@Composable
private fun SwapDetailScreen(swap: PendingSwap, onCopy: (String) -> Unit) {
  val status = swap.effectiveStatus(System.currentTimeMillis())
  val (label, color) = historyStatus(status)
  val fromNet = swap.fromTokenId.substringAfter("|", "").ifBlank { "—" }
  val toNet = swap.toTokenId.substringAfter("|", "").ifBlank { "—" }
  Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colors.background)) {
    ColumnWithScrollBar(modifier = Modifier.fillMaxSize()) {
      AppBarTitle(title = stringResource(MR.strings.wallet_swap_detail_title))
      Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        DetailRow(stringResource(MR.strings.wallet_swap_status_label), label, valueColor = color)
        if (swap.createdAt > 0L) DetailRow(stringResource(MR.strings.wallet_swap_detail_date), formatTimestamp(swap.createdAt))
        DetailRow(
          stringResource(MR.strings.wallet_swap_detail_sent),
          "${fmtAmt(swap.amountIn)} ${swap.fromSymbol.uppercase()}  ·  $fromNet"
        )
        DetailRow(
          stringResource(MR.strings.wallet_swap_detail_received),
          if (swap.amountOut > 0.0) "${fmtAmt(swap.amountOut)} ${swap.toSymbol.uppercase()}  ·  $toNet"
          else "${swap.toSymbol.uppercase()}  ·  $toNet"
        )
        Spacer(Modifier.height(20.dp))

        Card(
          backgroundColor = MaterialTheme.colors.onBackground.copy(alpha = 0.05f), shape = RoundedCornerShape(16.dp), elevation = 0.dp,
          modifier = Modifier.fillMaxWidth().clickable { onCopy(swap.tradeId) }
        ) {
          Column(modifier = Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(MR.strings.wallet_swap_trade_reference), color = MaterialTheme.colors.onBackground.copy(alpha = 0.5f), fontSize = 12.sp)
            Spacer(Modifier.height(6.dp))
            Text(swap.tradeId, color = MaterialTheme.colors.onSurface, fontWeight = FontWeight.Medium, fontSize = 14.sp)
          }
        }
        Spacer(Modifier.height(40.dp))
      }
    }
  }
}

@Composable
private fun DetailRow(label: String, value: String, valueColor: Color = MaterialTheme.colors.onBackground) {
  Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
    Text(label, color = MaterialTheme.colors.onBackground.copy(alpha = 0.6f), fontSize = 14.sp)
    Text(value, color = valueColor, fontWeight = FontWeight.Bold, fontSize = 14.sp, textAlign = androidx.compose.ui.text.style.TextAlign.End)
  }
  Divider(color = MaterialTheme.colors.onBackground.copy(alpha = 0.06f))
}

@Composable
private fun historyStatus(status: SwapStatusUi): Pair<String, Color> = when (status) {
  SwapStatusUi.FINISHED -> stringResource(MR.strings.wallet_swap_status_finished) to SimplexGreen
  SwapStatusUi.FAILED -> stringResource(MR.strings.wallet_swap_status_failed) to MaterialTheme.colors.error
  SwapStatusUi.EXPIRED -> stringResource(MR.strings.wallet_swap_status_expired) to MaterialTheme.colors.error
  SwapStatusUi.REFUNDED -> stringResource(MR.strings.wallet_swap_status_refunded) to WarningOrange
  SwapStatusUi.CONFIRMING -> stringResource(MR.strings.wallet_swap_status_confirming) to MaterialTheme.colors.primary
  SwapStatusUi.SENDING -> stringResource(MR.strings.wallet_swap_status_sending) to MaterialTheme.colors.primary
  SwapStatusUi.WAITING -> stringResource(MR.strings.wallet_swap_status_waiting) to MaterialTheme.colors.primary
  SwapStatusUi.NEW -> stringResource(MR.strings.wallet_swap_status_new) to MaterialTheme.colors.primary
  SwapStatusUi.UNKNOWN -> stringResource(MR.strings.wallet_swap_status_unknown) to MaterialTheme.colors.onBackground.copy(alpha = 0.6f)
}

private fun fmtAmt(v: Double): String {
  if (v == 0.0) return "0"
  val s = String.format(Locale.US, "%.8f", v)
  return if (s.contains('.')) s.trimEnd('0').trimEnd('.') else s
}

private fun fmtCountdown(totalSeconds: Int): String {
  val s = totalSeconds.coerceAtLeast(0)
  return String.format(Locale.US, "%d:%02d", s / 60, s % 60)
}
