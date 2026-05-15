package com.arpokrat.common.views.chat.item

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.ui.graphics.painter.Painter
import dev.icerock.moko.resources.compose.painterResource
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arpokrat.common.model.*
import com.arpokrat.common.ui.theme.isInDarkTheme
import com.arpokrat.res.MR
import kotlinx.datetime.Clock

@Composable
fun CIMetaView(
  chatItem: ChatItem,
  timedMessagesTTL: Int?,
  metaColor: Color = MaterialTheme.colors.secondary,
  paleMetaColor: Color = if (isInDarkTheme()) {
    metaColor.copy(
      red = metaColor.red * 0.67F,
      green = metaColor.green * 0.67F,
      blue = metaColor.red * 0.67F)
  } else {
    metaColor.copy(
      red = minOf(metaColor.red * 1.33F, 1F),
      green = minOf(metaColor.green * 1.33F, 1F),
      blue = minOf(metaColor.red * 1.33F, 1F))
  },
  showStatus: Boolean = true,
  showEdited: Boolean = true,
  showTimestamp: Boolean,
  showViaProxy: Boolean,
) {
  Row(Modifier.padding(start = 2.dp), verticalAlignment = Alignment.CenterVertically) {
    if (chatItem.isDeletedContent) {
      Text(
        chatItem.timestampText,
        color = metaColor,
        fontSize = 11.sp,
        modifier = Modifier.padding(start = 2.dp)
      )
    } else {
      CIMetaText(
        chatItem.meta,
        timedMessagesTTL,
        encrypted = chatItem.encryptedFile,
        metaColor,
        paleMetaColor,
        showStatus = showStatus,
        showEdited = showEdited,
        showViaProxy = showViaProxy,
        showTimestamp = showTimestamp
      )
    }
  }
}

@Composable
private fun CIMetaText(
  meta: CIMeta,
  chatTTL: Int?,
  encrypted: Boolean?,
  color: Color,
  paleColor: Color,
  showStatus: Boolean = true,
  showEdited: Boolean = true,
  showTimestamp: Boolean,
  showViaProxy: Boolean,
) {

  if (showEdited && meta.itemEdited) {
    StatusIconText(painterResource(MR.images.ic_edit), color)
  }
  if (meta.disappearing) {
    StatusIconText(painterResource(MR.images.ic_timer), color)
    val ttl = meta.itemTimed?.ttl
    if (ttl != chatTTL) {
      Text(shortTimeText(ttl), color = color, fontSize = 11.sp)
    }
  }
  if (showViaProxy && meta.sentViaProxy == true) {
    Spacer(Modifier.width(2.dp))
    Icon(painterResource(MR.images.ic_arrow_forward), null, Modifier.height(14.dp), tint = MaterialTheme.colors.secondary)
  }
  if (showStatus) {
    Spacer(Modifier.width(2.dp))
    val statusIcon = meta.statusIcon(MaterialTheme.colors.primary, color, paleColor)
    if (statusIcon != null) {
      val (icon, statusColor) = statusIcon
      if (meta.itemStatus is CIStatus.SndSent || meta.itemStatus is CIStatus.SndRcvd) {
        Icon(painterResource(icon), null, Modifier.height(14.dp), tint = statusColor)
      } else {
        StatusIconText(painterResource(icon), statusColor)
      }
    } else if (!meta.disappearing) {
      StatusIconText(painterResource(MR.images.ic_circle_filled), Color.Transparent)
    }
  }

  if (showTimestamp) {
    Spacer(Modifier.width(4.dp))
    Text(meta.timestampText, color = color, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
  }
}

fun reserveSpaceForMeta(
  meta: CIMeta,
  chatTTL: Int?,
  encrypted: Boolean?,
  secondaryColor: Color,
  showStatus: Boolean = true,
  showEdited: Boolean = true,
  showViaProxy: Boolean = false,
  showTimestamp: Boolean
): String {
  val iconSpace = " \u00A0\u00A0"
  val whiteSpace = "\u00A0"
  var res = if (showTimestamp) "" else iconSpace
  var space: String? = null

  fun appendSpace() {
    if (space != null) {
      res += space
      space = null
    }
  }

  if (showEdited && meta.itemEdited) {
    res += iconSpace
  }
  if (meta.itemTimed != null) {
    res += iconSpace
    val ttl = meta.itemTimed.ttl
    if (ttl != chatTTL) {
      res += shortTimeText(ttl)
    }
    space = whiteSpace
  }
  if (showViaProxy && meta.sentViaProxy == true) {
    appendSpace()
    res += iconSpace
  }
  if (showStatus) {
    appendSpace()
    if (meta.statusIcon(secondaryColor) != null) {
      res += iconSpace
    } else if (!meta.disappearing) {
      res += iconSpace
    }
    space = whiteSpace
  }

  if (showTimestamp) {
    appendSpace()
    res += meta.timestampText
  }
  return res
}

@Composable
private fun StatusIconText(icon: Painter, color: Color) {
  Icon(icon, null, Modifier.height(12.dp), tint = color)
}

@Preview
@Composable
fun PreviewCIMetaView() {
  CIMetaView(
    chatItem = ChatItem.getSampleData(
      1, CIDirection.DirectSnd(), Clock.System.now(), "hello"
    ),
    null,
    showViaProxy = false,
    showTimestamp = true
  )
}