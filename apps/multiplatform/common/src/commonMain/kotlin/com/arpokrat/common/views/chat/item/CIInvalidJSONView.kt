package com.arpokrat.common.views.chat.item

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import dev.icerock.moko.resources.compose.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.arpokrat.common.platform.ColumnWithScrollBar
import com.arpokrat.common.platform.shareText
import com.arpokrat.common.ui.theme.DEFAULT_PADDING
import com.arpokrat.common.views.helpers.*
import com.arpokrat.res.MR

@Composable
fun CIInvalidJSONView(json: String) {
  val clipboard = LocalClipboardManager.current
  Row(Modifier
    .clickable {
      ModalManager.center.closeModals()
      ModalManager.end.closeModals()
      ModalManager.center.showModal(true, endButtons = { ShareButton { clipboard.shareText(json) } }) { InvalidJSONView(json) }
    }
    .padding(horizontal = 10.dp, vertical = 6.dp)
  ) {
    Text(stringResource(MR.strings.invalid_data), color = MaterialTheme.colors.error, fontStyle = FontStyle.Italic)
  }
}

@Composable
fun InvalidJSONView(json: String) {
  ColumnWithScrollBar {
    Column(Modifier.padding(DEFAULT_PADDING).fillMaxWidth()) {
      Text(json)
    }
  }
}
