package com.arpokrat.common.views.localauth

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import com.arpokrat.common.model.*
import com.arpokrat.common.model.ChatModel.controller
import dev.icerock.moko.resources.compose.stringResource
import com.arpokrat.common.views.helpers.*
import com.arpokrat.common.views.helpers.DatabaseUtils.ksSelfDestructPassword
import com.arpokrat.common.views.helpers.DatabaseUtils.ksAppPassword
import com.arpokrat.common.views.onboarding.OnboardingStage
import com.arpokrat.common.platform.*
import com.arpokrat.common.views.database.*
import com.arpokrat.res.MR
import kotlinx.coroutines.delay

@Composable
fun LocalAuthView(m: ChatModel, authRequest: LocalAuthRequest) {
  val passcode = rememberSaveable { mutableStateOf("") }
  val allowToReact = rememberSaveable { mutableStateOf(true) }
  if (!allowToReact.value) {
    BackHandler {
      // do nothing until submit action finishes to prevent concurrent removing of storage
    }
  }
  PasscodeView(passcode, authRequest.title ?: stringResource(MR.strings.la_enter_app_passcode), authRequest.reason, stringResource(MR.strings.submit_passcode), buttonsEnabled = allowToReact,
    submit = {
      val sdPassword = ksSelfDestructPassword.get()
      if (sdPassword == passcode.value && authRequest.selfDestruct) {
        allowToReact.value = false
        deleteStorageAndRestart(m, sdPassword) { r ->
          authRequest.completed(r)
        }
      } else {
        val r: LAResult = if (passcode.value == authRequest.password) {
          if (authRequest.selfDestruct && sdPassword != null && controller.getChatCtrl() == -1L) {
            initChatControllerOnStart()
          }
          LAResult.Success
        } else {
          LAResult.Error(generalGetString(MR.strings.incorrect_passcode))
        }
        authRequest.completed(r)
      }
    },
    cancel = {
      authRequest.completed(LAResult.Error(generalGetString(MR.strings.authentication_cancelled)))
    })
}

private fun deleteStorageAndRestart(m: ChatModel, password: String, completed: (LAResult) -> Unit) {
  withLongRunningApi {
    try {
      /** Waiting until [initChatController] finishes */
      while (m.ctrlInitInProgress.value) {
        delay(50)
      }
      if (m.chatRunning.value == true) {
        stopChatAsync(m)
      }
      val ctrl = m.controller.getChatCtrl()
      if (ctrl != null && ctrl != -1L) {
        /**
         * The following sequence can bring a user here:
         * the user opened the app, entered app passcode, went to background, returned back, entered self-destruct code.
         * In this case database should be closed to prevent possible situation when OS can deny database removal command
         * */
        chatCloseStore(ctrl)
      }
      deleteChatDatabaseFilesAndState()
      com.arpokrat.common.wallet.WalletManager().deleteWallet()
      ksAppPassword.set(password)
      ksSelfDestructPassword.remove()
      ntfManager.cancelAllNotifications()
      val selfDestructPref = m.controller.appPrefs.selfDestruct
      val displayNamePref = m.controller.appPrefs.selfDestructDisplayName
      val displayName = displayNamePref.get()
      selfDestructPref.set(false)
      displayNamePref.set(null)
      reinitChatController()
      if (m.currentUser.value != null) {
        return@withLongRunningApi
      }
      var profile: Profile? = null
      if (!displayName.isNullOrEmpty()) {
        profile = Profile(displayName = displayName, fullName = "", shortDescr = null)
      }
      val createdUser = m.controller.apiCreateActiveUser(null, profile, pastTimestamp = true)
      m.currentUser.value = createdUser
      m.controller.appPrefs.onboardingStage.set(OnboardingStage.OnboardingComplete)
      if (createdUser != null) {
        m.controller.startChat(createdUser)
      }
      ModalManager.closeAllModalsEverywhere()
      AlertManager.shared.hideAllAlerts()
      AlertManager.privacySensitive.hideAllAlerts()
      completed(LAResult.Success)
    } catch (e: Exception) {
      completed(LAResult.Error(generalGetString(MR.strings.incorrect_passcode)))
    }
  }
}

suspend fun reinitChatController() {
  chatModel.chatDbChanged.value = true
  chatModel.chatDbStatus.value = null
  try {
    initChatController()
  } catch (e: Exception) {
  }
  chatModel.chatDbChanged.value = false
}