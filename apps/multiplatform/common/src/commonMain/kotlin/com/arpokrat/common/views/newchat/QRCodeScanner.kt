package com.arpokrat.common.views.newchat

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.*
import com.arpokrat.common.ui.theme.DEFAULT_PADDING
import com.arpokrat.common.ui.theme.DEFAULT_PADDING_HALF

@Composable
expect fun QRCodeScanner(
  showQRCodeScanner: MutableState<Boolean> = remember { mutableStateOf(true) },
  padding: PaddingValues = PaddingValues(horizontal = DEFAULT_PADDING * 2f, vertical = DEFAULT_PADDING_HALF),
  onBarcode: suspend (String) -> Boolean
)
