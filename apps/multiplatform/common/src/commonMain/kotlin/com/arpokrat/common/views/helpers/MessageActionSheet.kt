package com.arpokrat.common.views.helpers

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arpokrat.common.model.ChatFeature
import com.arpokrat.common.model.ChatInfo
import com.arpokrat.common.model.ChatItem
import com.arpokrat.common.model.MsgReaction
import com.arpokrat.res.MR
import dev.icerock.moko.resources.compose.stringResource

@Composable
fun MessageActionSheet(
  activeMessage: ChatItem?,
  onDismiss: () -> Unit,
  content: @Composable ColumnScope.() -> Unit
) {
  AnimatedVisibility(
    visible = activeMessage != null,
    enter = fadeIn() + slideInVertically(initialOffsetY = { it / 4 }),
    exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 4 })
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(Color.Black.copy(alpha = 0.65f))
        .clickable(onClick = onDismiss)
        .padding(horizontal = 12.dp, vertical = 24.dp),
      contentAlignment = Alignment.BottomCenter
    ) {
      Column(
        modifier = Modifier.widthIn(max = 500.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {

        if (activeMessage != null) {
          MessagePreviewCard(activeMessage)
        }

        Surface(
          shape = RoundedCornerShape(24.dp),
          color = MaterialTheme.colors.surface,
          elevation = 12.dp,
          modifier = Modifier.fillMaxWidth().animateContentSize()
        ) {
          Column(
            modifier = Modifier
              .verticalScroll(rememberScrollState())
              .padding(bottom = 16.dp)
          ) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
              contentAlignment = Alignment.Center
            ) {
              Box(
                modifier = Modifier
                  .width(32.dp)
                  .height(4.dp)
                  .clip(RoundedCornerShape(2.dp))
                  .background(MaterialTheme.colors.onSurface.copy(alpha = 0.15f))
              )
            }

            content()
          }
        }
      }
    }
  }
}

@Composable
private fun MessagePreviewCard(item: ChatItem) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(18.dp))
      .background(MaterialTheme.colors.surface.copy(alpha = 0.95f))
      .padding(12.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Box(
      modifier = Modifier
        .width(4.dp)
        .height(36.dp)
        .clip(CircleShape)
        .background(MaterialTheme.colors.primary)
    )

    Spacer(modifier = Modifier.width(14.dp))

    Column {
      Text(
        text = stringResource(MR.strings.selected_message),
        style = MaterialTheme.typography.caption,
        color = MaterialTheme.colors.primary,
        fontWeight = FontWeight.Bold
      )

      val textPreview = when {
        item.content.text.startsWith(com.arpokrat.common.wallet.CryptoProtocol.PREFIX_INVOICE) -> generalGetString(MR.strings.crypto_invoice_short_preview)
        item.content.text.startsWith(com.arpokrat.common.wallet.CryptoProtocol.PREFIX_PAID) -> generalGetString(MR.strings.crypto_invoice_paid_short_preview)
        item.content.text.startsWith(com.arpokrat.common.wallet.CryptoProtocol.PREFIX_CANCELLED) -> generalGetString(MR.strings.crypto_invoice_cancelled_short_preview)
        item.content.text.startsWith(com.arpokrat.common.wallet.CryptoProtocol.PREFIX_DECLINED) -> generalGetString(MR.strings.crypto_invoice_declined_short_preview)
        item.content.text.isNotEmpty() -> item.content.text
        item.file != null -> generalGetString(MR.strings.file_media_preview)
        else -> generalGetString(MR.strings.message_preview)
      }

      Text(
        text = textPreview,
        style = MaterialTheme.typography.body2,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
        color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f)
      )
    }
  }
}

@Composable
fun ActionItem(
  text: String,
  icon: Any,
  color: Color = MaterialTheme.colors.onSurface,
  onClick: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .padding(horizontal = 24.dp, vertical = 14.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    val iconTint = if (color == MaterialTheme.colors.onSurface) color.copy(alpha = 0.7f) else color
    val modifier = Modifier.size(24.dp)

    when (icon) {
      is ImageVector -> Icon(icon, null, tint = iconTint, modifier = modifier)
      is Painter -> Icon(icon, null, tint = iconTint, modifier = modifier)
    }

    Spacer(modifier = Modifier.width(16.dp))

    Text(
      text = text,
      fontSize = 16.sp,
      fontWeight = FontWeight.Medium,
      color = color
    )
  }
}

@Composable
fun SheetReactionsRow(
  cItem: ChatItem,
  chatInfo: ChatInfo,
  onReactionSelected: (MsgReaction) -> Unit
) {
  if (!chatInfo.featureEnabled(ChatFeature.Reactions) || !cItem.allowAddReaction) return

  val rs = MsgReaction.supported.filter { r ->
    cItem.reactions.none { it.userReacted && it.reaction.text == r.text }
  }

  if (rs.isNotEmpty()) {
    Column(Modifier.fillMaxWidth()) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState())
          .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        rs.forEach { r ->
          Box(
            modifier = Modifier
              .size(48.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colors.onSurface.copy(alpha = 0.06f))
              .clickable { onReactionSelected(r) },
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = r.text,
              fontSize = 24.sp,
              modifier = Modifier.padding(bottom = 2.dp)
            )
          }
        }
      }

      Divider(
        modifier = Modifier.padding(top = 12.dp, bottom = 8.dp),
        color = MaterialTheme.colors.onSurface.copy(alpha = 0.08f)
      )
    }
  }
}