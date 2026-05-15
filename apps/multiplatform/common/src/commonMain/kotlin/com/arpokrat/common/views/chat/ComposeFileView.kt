package com.arpokrat.common.views.chat

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import dev.icerock.moko.resources.compose.painterResource
import dev.icerock.moko.resources.compose.stringResource
import androidx.compose.ui.unit.dp
import com.arpokrat.common.ui.theme.*
import com.arpokrat.res.MR

@Composable
fun ComposeFileView(fileName: String, cancelFile: () -> Unit, cancelEnabled: Boolean) {
  Row(
    Modifier
      .height(60.dp)
      .fillMaxWidth()
      .padding(horizontal = 8.dp, vertical = 4.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Box(
      modifier = Modifier
        .size(42.dp)
        .clip(RoundedCornerShape(8.dp))
        .background(MaterialTheme.colors.onSurface.copy(alpha = 0.05f)),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        painterResource(MR.images.ic_draft),
        stringResource(MR.strings.icon_descr_file),
        Modifier.size(24.dp),
        tint = MaterialTheme.colors.primary
      )
    }

    Spacer(Modifier.width(12.dp))

    Text(
      text = fileName,
      style = MaterialTheme.typography.body1,
      maxLines = 1,
      modifier = Modifier.weight(1f)
    )

    if (cancelEnabled) {
      IconButton(onClick = cancelFile) {
        Icon(
          painterResource(MR.images.ic_close),
          contentDescription = stringResource(MR.strings.icon_descr_cancel_file_preview),
          tint = MaterialTheme.colors.primary,
          modifier = Modifier.size(20.dp)
        )
      }
    }
  }
}

@Preview
@Composable
fun PreviewComposeFileView() {
  SimpleXTheme {
    ComposeFileView(
      "presentation_arpokrat.pdf",
      cancelFile = {},
      cancelEnabled = true
    )
  }
}