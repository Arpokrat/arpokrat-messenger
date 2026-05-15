package com.arpokrat.common.wallet.ui.transaction

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.arpokrat.common.platform.ColumnWithScrollBar
import com.arpokrat.common.views.helpers.AppBarTitle
import com.arpokrat.res.MR
import dev.icerock.moko.resources.compose.stringResource

@Composable
fun WalletScanView(
  scanSessionId: Int,
  onAddressScanned: (String) -> Unit,
  onClose: () -> Unit
) {
  val haptic = LocalHapticFeedback.current

  ColumnWithScrollBar(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colors.background)
  ) {

    AppBarTitle(title = stringResource(MR.strings.wallet_scan_title))

    Column(
      modifier = Modifier.fillMaxWidth(),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      key(scanSessionId) {
        val scannerState = remember { mutableStateOf(true) }

        com.arpokrat.common.views.newchat.QRCodeScanner(
          showQRCodeScanner = scannerState,
          padding = PaddingValues(0.dp),
          onBarcode = { barcode ->
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)

            // Clean protocol prefixes (e.g., "ethereum:0x..." -> "0x...")
            val cleanAddress = barcode.substringAfter(":").trim()

            onAddressScanned(cleanAddress)

            false
          }
        )
      }
    }
  }
}