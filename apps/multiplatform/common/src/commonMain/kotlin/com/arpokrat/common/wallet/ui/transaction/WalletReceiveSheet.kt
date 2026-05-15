package com.arpokrat.common.wallet.ui.transaction

import SectionBottomSpacer
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import boofcv.alg.fiducial.qrcode.QrCode
import com.arpokrat.common.platform.ColumnWithScrollBar
import com.arpokrat.common.platform.shareText
import com.arpokrat.common.views.helpers.AppBarTitle
import com.arpokrat.common.views.helpers.AppPrimaryButton
import com.arpokrat.common.views.newchat.qrCodeBitmap
import com.arpokrat.common.wallet.ui.components.WalletSpinner
import com.arpokrat.res.MR
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.*

@Composable
fun ReceiveSheet(
  address: String,
  networkName: String,
  onCopy: () -> Unit,
  onKeepAlive: () -> Unit
) {
  val clipboardManager = LocalClipboardManager.current
  var qrCodeBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

  LaunchedEffect(address) {
    withContext(Dispatchers.IO) {
      val generatedQr = qrCodeBitmap(content = address, size = 1024, errorLevel = QrCode.ErrorLevel.M)
      withContext(Dispatchers.Main) {
        qrCodeBitmap = generatedQr
      }
    }
  }

  LaunchedEffect(Unit) {
    while (isActive) {
      onKeepAlive()
      delay(500)
    }
  }

  Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colors.background)) {

    ColumnWithScrollBar(modifier = Modifier.fillMaxSize()) {

      AppBarTitle(title = stringResource(MR.strings.wallet_receive_title))

      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Spacer(Modifier.height(16.dp))

        Text(
          text = stringResource(MR.strings.wallet_receive_network_format, networkName),
          color = MaterialTheme.colors.secondary,
          fontSize = 16.sp,
          textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(48.dp))

        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .padding(24.dp)
        ) {
          if (qrCodeBitmap != null) {
            Image(
              bitmap = qrCodeBitmap!!,
              contentDescription = null,
              modifier = Modifier.size(220.dp)
            )
          } else {
            Box(Modifier.size(220.dp), contentAlignment = Alignment.Center) {
              WalletSpinner(size = 32, strokeWidth = 3f)
            }
          }
        }

        Spacer(Modifier.height(48.dp))

        Card(
          backgroundColor = MaterialTheme.colors.onBackground.copy(alpha = 0.05f),
          shape = RoundedCornerShape(16.dp),
          elevation = 0.dp,
          modifier = Modifier
            .fillMaxWidth()
            .clickable { onCopy() }
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text(stringResource(MR.strings.wallet_your_address), color = MaterialTheme.colors.secondary, fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
            Text(
              text = address,
              color = MaterialTheme.colors.onSurface,
              fontWeight = FontWeight.Medium,
              fontSize = 15.sp,
              textAlign = TextAlign.Center
            )
          }
        }

        Spacer(Modifier.height(140.dp))
      }
    }

    Column(
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .fillMaxWidth()
        .background(MaterialTheme.colors.background)
        .padding(horizontal = 24.dp)
        .padding(top = 16.dp)
    ) {

      AppPrimaryButton(
        text = stringResource(MR.strings.wallet_btn_share_address),
        onClick = { clipboardManager.shareText(address) }
      )

      SectionBottomSpacer()
    }
  }
}