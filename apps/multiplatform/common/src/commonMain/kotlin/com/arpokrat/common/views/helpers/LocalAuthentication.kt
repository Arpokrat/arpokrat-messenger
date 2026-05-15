package com.arpokrat.common.views.helpers

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.*
import androidx.compose.ui.Modifier
import com.arpokrat.common.model.ChatController
import com.arpokrat.common.model.ChatModel
import com.arpokrat.common.platform.BackHandler
import com.arpokrat.common.views.localauth.LocalAuthView
import com.arpokrat.common.views.usersettings.LAMode
import com.arpokrat.res.MR

sealed class LAResult {
  object Success: LAResult()
  class Error(val errString: CharSequence): LAResult()
  class Failed(val errString: CharSequence? = null): LAResult()
  class Unavailable(val errString: CharSequence? = null): LAResult()
}

data class LocalAuthRequest (
  val title: String?,
  val reason: String,
  val password: String,
  val selfDestruct: Boolean,
  val completed: (LAResult) -> Unit
) {
  companion object {
    val sample = LocalAuthRequest(generalGetString(MR.strings.la_enter_app_passcode), generalGetString(MR.strings.la_authenticate), "", selfDestruct = false) { }
  }
}

expect fun authenticate(
  promptTitle: String,
  promptSubtitle: String,
  selfDestruct: Boolean = false,
  usingLAMode: LAMode = ChatModel.controller.appPrefs.laMode.get(),
  oneTime: Boolean,
  completed: (LAResult) -> Unit
)

fun authenticateWithPasscode(
  promptTitle: String,
  promptSubtitle: String,
  selfDestruct: Boolean,
  oneTime: Boolean,
  completed: (LAResult) -> Unit
) {
  val password = DatabaseUtils.ksAppPassword.get() ?: return completed(LAResult.Unavailable(generalGetString(MR.strings.la_no_app_password)))
  ModalManager.fullscreen.showPasscodeCustomModal(oneTime) { close ->
    BackHandler {
      close()
      completed(LAResult.Error(generalGetString(MR.strings.authentication_cancelled)))
    }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colors.background.copy(1f), contentColor = LocalContentColor.current) {
      LocalAuthView(ChatModel, LocalAuthRequest(promptTitle, promptSubtitle, password, selfDestruct && ChatController.appPrefs.selfDestruct.get()) {
        close()
        completed(it)
      })
    }
  }
}
