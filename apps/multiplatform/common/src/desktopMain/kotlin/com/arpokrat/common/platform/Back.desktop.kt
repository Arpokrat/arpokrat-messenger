package com.arpokrat.common.platform

import androidx.compose.runtime.*
import com.arpokrat.common.simplexWindowState

@Composable
actual fun BackHandler(enabled: Boolean, onBack: () -> Unit) {
  DisposableEffect(enabled) {
    if (enabled) simplexWindowState.backstack.add(onBack)
    onDispose {
      simplexWindowState.backstack.remove(onBack)
    }
  }
}
