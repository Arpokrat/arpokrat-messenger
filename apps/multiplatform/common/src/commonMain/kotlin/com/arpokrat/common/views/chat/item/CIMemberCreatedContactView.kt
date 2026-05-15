package com.arpokrat.common.views.chat.item

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arpokrat.common.model.CIDirection
import com.arpokrat.common.model.ChatItem
import com.arpokrat.common.views.helpers.generalGetString
import com.arpokrat.res.MR

@Composable
fun CIMemberCreatedContactView(
  chatItem: ChatItem,
  openDirectChat: (Long) -> Unit
) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 6.dp, horizontal = 16.dp),
    contentAlignment = Alignment.Center
  ) {
    Surface(
      shape = RoundedCornerShape(12.dp),
      color = MaterialTheme.colors.onSurface.copy(alpha = 0.08f),
      elevation = 0.dp
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
      ) {
        val memberDisplayName = chatItem.memberDisplayName
        val mainText = if (memberDisplayName != null) {
          "$memberDisplayName ${chatItem.content.text}"
        } else {
          chatItem.content.text
        }

        Text(
          text = mainText,
          style = MaterialTheme.typography.caption.copy(
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium
          )
        )

        if (chatItem.chatDir is CIDirection.GroupRcv && chatItem.chatDir.groupMember.memberContactId != null) {
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = generalGetString(MR.strings.rcv_group_event_open_chat).uppercase(),
            style = MaterialTheme.typography.caption.copy(
              color = MaterialTheme.colors.primary,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold
            ),
            modifier = Modifier
              .clickable { openDirectChat(chatItem.chatDir.groupMember.memberContactId!!) }
              .padding(4.dp)
          )
        }

        Text(
          text = chatItem.timestampText,
          style = MaterialTheme.typography.overline.copy(
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.4f),
            fontSize = 10.sp
          )
        )
      }
    }
  }
}