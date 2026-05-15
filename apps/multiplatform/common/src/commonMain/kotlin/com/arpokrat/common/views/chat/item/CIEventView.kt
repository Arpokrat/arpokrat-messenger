package com.arpokrat.common.views.chat.item

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arpokrat.common.ui.theme.SimpleXTheme

@Composable
fun CIEventView(text: AnnotatedString) {
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
      Text(
        text = text,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        style = MaterialTheme.typography.caption.copy(
          color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
          fontSize = 13.sp,
          lineHeight = 18.sp,
          textAlign = TextAlign.Center,
          fontWeight = FontWeight.Medium
        )
      )
    }
  }
}

@Preview
@Composable
fun CIEventViewPreview() {
  SimpleXTheme {
    CIEventView(buildAnnotatedString { append("Alice a rejoint le groupe") })
  }
}