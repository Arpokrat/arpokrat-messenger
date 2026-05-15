package com.arpokrat.common.views.helpers

import com.arpokrat.common.views.usersettings.LAMode

actual fun authenticate(
  promptTitle: String,
  promptSubtitle: String,
  selfDestruct: Boolean,
  usingLAMode: LAMode,
  oneTime: Boolean,
  completed: (LAResult) -> Unit
) {
  when (usingLAMode) {
    LAMode.PASSCODE -> authenticateWithPasscode(promptTitle, promptSubtitle, selfDestruct, oneTime, completed)
    else -> {}
  }
}
