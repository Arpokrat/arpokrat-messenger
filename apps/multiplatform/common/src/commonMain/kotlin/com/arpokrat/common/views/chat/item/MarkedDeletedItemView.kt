package com.arpokrat.common.views.chat.item

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arpokrat.common.model.*
import com.arpokrat.common.model.ChatModel.getChatItemIndexOrNull
import com.arpokrat.common.ui.theme.*
import com.arpokrat.common.views.helpers.generalGetString
import com.arpokrat.res.MR
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.datetime.Clock

@Composable
fun MarkedDeletedItemView(chatsCtx: ChatModel.ChatsContext, ci: ChatItem, chatInfo: ChatInfo, timedMessagesTTL: Int?, revealed: State<Boolean>, showViaProxy: Boolean, showTimestamp: Boolean) {
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = MaterialTheme.colors.onSurface.copy(alpha = 0.08f),
    contentColor = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
    elevation = 0.dp
  ) {
    Row(
      Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(Modifier.weight(1f, false)) {
        MergedMarkedDeletedText(chatsCtx, ci, chatInfo, revealed)
      }

      val metaColor = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
      CIMetaView(
        chatItem = ci,
        timedMessagesTTL = timedMessagesTTL,
        metaColor = metaColor,
        showViaProxy = showViaProxy,
        showTimestamp = showTimestamp
      )
    }
  }
}

@Composable
private fun MergedMarkedDeletedText(chatsCtx: ChatModel.ChatsContext, chatItem: ChatItem, chatInfo: ChatInfo, revealed: State<Boolean>) {
  val reversedChatItems = chatsCtx.chatItems.value.asReversed()
  var i = getChatItemIndexOrNull(chatItem, reversedChatItems)
  val ciCategory = chatItem.mergeCategory
  val text =  if (!revealed.value && ciCategory != null && i != null) {
    var moderated = 0
    var blocked = 0
    var blockedByAdmin = 0
    var deleted = 0
    val moderatedBy: MutableSet<String> = mutableSetOf()
    while (i < reversedChatItems.size) {
      val ci = reversedChatItems.getOrNull(i)
      if (ci?.mergeCategory != ciCategory) break
      when (val itemDeleted = ci.meta.itemDeleted ?: break) {
        is CIDeleted.Moderated -> {
          moderated += 1
          moderatedBy.add(itemDeleted.byGroupMember.displayName)
        }
        is CIDeleted.Blocked -> blocked += 1
        is CIDeleted.BlockedByAdmin -> blockedByAdmin +=1
        is CIDeleted.Deleted -> deleted += 1
      }
      i++
    }
    val total = moderated + blocked + blockedByAdmin + deleted
    if (total <= 1)
      markedDeletedText(chatItem, chatInfo)
    else if (total == moderated)
      stringResource(MR.strings.moderated_items_description).format(total, moderatedBy.joinToString(", "))
    else if (total == blockedByAdmin)
      stringResource(MR.strings.blocked_by_admin_items_description).format(total)
    else if (total == blocked + blockedByAdmin)
      stringResource(MR.strings.blocked_items_description).format(total)
    else
      stringResource(MR.strings.marked_deleted_items_description).format(total)
  } else {
    markedDeletedText(chatItem, chatInfo)
  }

  Text(
    buildAnnotatedString {
      withStyle(SpanStyle(fontSize = 12.sp, fontStyle = FontStyle.Italic)) { append(text) }
    },
    style = MaterialTheme.typography.body1.copy(lineHeight = 18.sp),
    modifier = Modifier.padding(end = 8.dp),
    maxLines = 1,
    overflow = TextOverflow.Ellipsis,
  )
}

fun markedDeletedText(cItem: ChatItem, chatInfo: ChatInfo): String =
  if (cItem.meta.itemDeleted != null && cItem.isReport) {
    if (cItem.meta.itemDeleted is CIDeleted.Moderated && cItem.meta.itemDeleted.byGroupMember.groupMemberId != (chatInfo as? ChatInfo.Group)?.groupInfo?.membership?.groupMemberId) {
      generalGetString(MR.strings.report_item_archived_by).format(cItem.meta.itemDeleted.byGroupMember.displayName)
    } else {
      generalGetString(MR.strings.report_item_archived)
    }
  }
  else when (cItem.meta.itemDeleted) {
    is CIDeleted.Moderated ->
      String.format(generalGetString(MR.strings.moderated_item_description), cItem.meta.itemDeleted.byGroupMember.displayName)
    is CIDeleted.Blocked ->
      generalGetString(MR.strings.blocked_item_description)
    is CIDeleted.BlockedByAdmin ->
      generalGetString(MR.strings.blocked_by_admin_item_description)
    is CIDeleted.Deleted, null ->
      generalGetString(MR.strings.marked_deleted_description)
  }

@Preview
@Composable
fun PreviewMarkedDeletedItemView() {
  SimpleXTheme {
    MarkedDeletedItemView(
      ChatModel.ChatsContext(null),
      ChatItem.getSampleData(itemDeleted = CIDeleted.Deleted(Clock.System.now())),
      ChatInfo.Direct.sampleData,
      null,
      remember { mutableStateOf(false) },
      showViaProxy = false,
      showTimestamp = true
    )
  }
}