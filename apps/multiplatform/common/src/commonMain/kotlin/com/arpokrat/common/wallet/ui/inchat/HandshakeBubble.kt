package com.arpokrat.common.wallet.ui.inchat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arpokrat.common.model.ChatItem
import com.arpokrat.common.ui.theme.SimplexGreen
import com.arpokrat.common.views.helpers.*
import com.arpokrat.common.wallet.CryptoProtocol
import com.arpokrat.common.wallet.ui.components.DevModeBadge
import dev.icerock.moko.resources.compose.stringResource
import com.arpokrat.res.MR

private data class BubbleStyle(
  val statusColor: Color,
  val bgColor: Color,
  val icon: ImageVector,
  val titleText: String
)

@Composable
fun HandshakeMessageBubble(
  message: ChatItem,
  textContent: String,
  onPay: () -> Unit,
  onCancel: () -> Unit,
  onDecline: () -> Unit
) {
  val isMe = message.chatDir.sent

  val isInvoice = CryptoProtocol.isInvoice(textContent)
  val isPaid = CryptoProtocol.isPaid(textContent)
  val isCancelled = CryptoProtocol.isCancelled(textContent)
  val isDeclined = CryptoProtocol.isDeclined(textContent)

  val style = when {
    isPaid -> BubbleStyle(
      statusColor = SimplexGreen,
      bgColor = SimplexGreen.copy(alpha = 0.1f),
      icon = Icons.Default.CheckCircle,
      titleText = stringResource(MR.strings.wallet_bubble_paid)
    )
    isCancelled -> BubbleStyle(
      statusColor = Color.Gray,
      bgColor = Color.Gray.copy(alpha = 0.1f),
      icon = Icons.Default.Cancel,
      titleText = stringResource(MR.strings.wallet_bubble_cancelled)
    )
    isDeclined -> BubbleStyle(
      statusColor = MaterialTheme.colors.error,
      bgColor = MaterialTheme.colors.error.copy(alpha = 0.1f),
      icon = Icons.Default.Block,
      titleText = stringResource(MR.strings.wallet_bubble_declined)
    )
    else -> BubbleStyle(
      statusColor = MaterialTheme.colors.primary,
      bgColor = MaterialTheme.colors.primary.copy(alpha = 0.1f),
      icon = Icons.Default.AccountBalanceWallet,
      titleText = if (isMe) stringResource(MR.strings.wallet_bubble_req_sent) else stringResource(MR.strings.wallet_bubble_req_received)
    )
  }

  Card(
    shape = RoundedCornerShape(20.dp),
    backgroundColor = MaterialTheme.colors.surface,
    elevation = 2.dp,
    modifier = Modifier
      .widthIn(min = 240.dp, max = 280.dp)
      .padding(4.dp)
  ) {
    Column(modifier = Modifier.padding(16.dp)) {

      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
      ) {
        Box(
          modifier = Modifier.size(32.dp).clip(CircleShape).background(style.bgColor),
          contentAlignment = Alignment.Center
        ) {
          Icon(imageVector = style.icon, contentDescription = null, tint = style.statusColor, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(
          text = style.titleText,
          fontSize = 14.sp,
          fontWeight = FontWeight.Medium,
          color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
        )
      }

      val testnetIds = listOf(1, 11155111, 80002, 9000, 10000)
      val coinType = if (isInvoice) CryptoProtocol.parseInvoice(textContent)?.coinType else null

      if (coinType != null && coinType in testnetIds) {
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
          DevModeBadge()
        }
        Spacer(Modifier.height(8.dp))
      } else {
        Spacer(Modifier.height(16.dp))
      }

      if (isInvoice) {
        val data = CryptoProtocol.parseInvoice(textContent)
        if (data != null) {
          val isFiatMode = data.fiatValue.startsWith("FIAT_MODE:")
          val primaryText = if (isFiatMode) data.fiatValue.removePrefix("FIAT_MODE:") else "${data.amount} ${data.symbol}"
          val secondaryText = if (isFiatMode) "≈ ${data.amount} ${data.symbol}" else data.fiatValue

          Text(text = primaryText, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colors.onSurface)
          if (secondaryText.isNotEmpty()) {
            Text(text = secondaryText, fontSize = 15.sp, color = Color.Gray, modifier = Modifier.padding(top = 2.dp))
          }
          Text(text = stringResource(MR.strings.wallet_bubble_pending), fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
        }
      } else if (isPaid) {
        val data = CryptoProtocol.parsePaid(textContent)
        if (data != null) {
          val isFiatMode = data.fiatValue.startsWith("FIAT_MODE:")
          val primaryText = if (isFiatMode) data.fiatValue.removePrefix("FIAT_MODE:") else "${data.amount} ${data.symbol}"
          val secondaryText = if (isFiatMode) "≈ ${data.amount} ${data.symbol}" else data.fiatValue

          Text(text = primaryText, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = style.statusColor)
          if (secondaryText.isNotEmpty()) {
            Text(text = secondaryText, fontSize = 15.sp, color = Color.Gray, modifier = Modifier.padding(top = 2.dp))
          }
          Text(text = "Tx: ${data.txHash.take(6)}...${data.txHash.takeLast(4)}", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
        }
      } else {
        Text(text = if (isCancelled) stringResource(MR.strings.wallet_bubble_status_cancelled) else stringResource(MR.strings.wallet_bubble_status_declined), fontSize = 16.sp, fontStyle = FontStyle.Italic, color = Color.Gray)
      }

      if (isInvoice) {
        Spacer(Modifier.height(20.dp))

        if (isMe) {
          AppSecondaryButton(
            text = stringResource(MR.strings.wallet_bubble_btn_cancel),
            onClick = onCancel
          )
        } else {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            AppTextButton(
              text = stringResource(MR.strings.wallet_bubble_btn_decline),
              onClick = onDecline,
              modifier = Modifier.weight(0.4f)
            )
            Spacer(Modifier.width(8.dp))
            AppPrimaryButton(
              text = stringResource(MR.strings.wallet_bubble_btn_pay),
              onClick = onPay,
              modifier = Modifier.weight(0.6f).height(42.dp)
            )
          }
        }
      }
    }
  }
}