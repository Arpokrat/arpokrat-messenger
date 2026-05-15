package com.arpokrat.common.views.usersettings.networkAndServers

import androidx.compose.runtime.Composable
import dev.icerock.moko.resources.compose.stringResource
import com.arpokrat.common.model.ServerAddress.Companion.parseServerAddress
import com.arpokrat.common.model.UserServer
import com.arpokrat.common.platform.ColumnWithScrollBar
import com.arpokrat.common.views.helpers.*
import com.arpokrat.common.views.newchat.QRCodeScanner
import com.arpokrat.res.MR

@Composable
expect fun ScanProtocolServer(rhId: Long?, onNext: (UserServer) -> Unit)

@Composable
fun ScanProtocolServerLayout(rhId: Long?, onNext: (UserServer) -> Unit) {
  ColumnWithScrollBar {
    AppBarTitle(stringResource(MR.strings.smp_servers_scan_qr))
    QRCodeScanner { text ->
      val res = parseServerAddress(text)
      if (res != null) {
        onNext(UserServer(remoteHostId = rhId, null, text, false, null, false, false))
      } else {
        AlertManager.shared.showAlertMsg(
          title = generalGetString(MR.strings.smp_servers_invalid_address),
          text = generalGetString(MR.strings.smp_servers_check_address)
        )
      }
      res != null
    }
  }
}
