package com.arpokrat.common.views.usersettings.networkAndServers

import androidx.compose.runtime.Composable
import com.arpokrat.common.model.UserServer

@Composable
actual fun ScanProtocolServer(rhId: Long?, onNext: (UserServer) -> Unit) {
  ScanProtocolServerLayout(rhId, onNext)
}
