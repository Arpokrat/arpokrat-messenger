package com.arpokrat.common.views.chat.item

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arpokrat.common.model.ChatItem
import com.arpokrat.common.ui.theme.*

@Composable
fun DeletedItemView(ci: ChatItem, timedMessagesTTL: Int?, showViaProxy: Boolean, showTimestamp: Boolean) {
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
      Text(
        buildAnnotatedString {
          withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(ci.content.text) }
        },
        style = MaterialTheme.typography.body1.copy(lineHeight = 22.sp),
        modifier = Modifier.padding(end = 8.dp)
      )

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

@Preview
@Composable
fun PreviewDeletedItemView() {
  SimpleXTheme {
    DeletedItemView(
      ChatItem.getDeletedContentSampleData(),
      null,
      showViaProxy = false,
      showTimestamp = true
    )
  }
}