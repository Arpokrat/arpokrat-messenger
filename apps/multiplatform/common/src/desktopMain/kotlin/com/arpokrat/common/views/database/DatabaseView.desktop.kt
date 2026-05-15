package com.arpokrat.common.views.database

import androidx.compose.runtime.mutableStateOf
import com.arpokrat.common.platform.chatModel
import com.arpokrat.common.views.helpers.withBGApi
import kotlinx.coroutines.delay
import kotlinx.datetime.Instant

actual fun restartChatOrApp() {
  if (chatModel.chatRunning.value == false) {
    chatModel.chatDbChanged.value = true
    startChat(chatModel, mutableStateOf(Instant.DISTANT_PAST), chatModel.chatDbChanged, mutableStateOf(false))
  } else {
    authStopChat(chatModel) {
      withBGApi {
        // adding delay in order to prevent locked database by previous initialization
        delay(1000)
        chatModel.chatDbChanged.value = true
        startChat(chatModel, mutableStateOf(Instant.DISTANT_PAST), chatModel.chatDbChanged, mutableStateOf(false))
      }
    }
  }
}
