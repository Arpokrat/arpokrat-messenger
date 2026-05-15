package com.arpokrat.common.views.helpers

import com.arpokrat.common.model.AgentErrorType
import com.arpokrat.common.platform.Log
import com.arpokrat.common.platform.TAG
import com.arpokrat.common.platform.ntfManager
import com.arpokrat.common.views.database.restartChatOrApp
import com.arpokrat.res.MR
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

class ProcessedErrors <T: AgentErrorType>(val interval: Long) {
  private var lastShownTimestamp: Long = -1
  private var lastShownOfferRestart: Boolean = false
  private var timer: Job = Job()

  fun newError(error: T, offerRestart: Boolean) {
    timer.cancel()
    timer = withLongRunningApi(slow = 130_000) {
      val delayBeforeNext = (lastShownTimestamp + interval) - System.currentTimeMillis()
      if ((lastShownOfferRestart || !offerRestart) && delayBeforeNext >= 0) {
        delay(delayBeforeNext)
      }
      lastShownTimestamp = System.currentTimeMillis()
      lastShownOfferRestart = offerRestart
      AlertManager.shared.hideAllAlerts()
      showMessage(error, offerRestart)
    }
  }

  private fun showMessage(error: T, offerRestart: Boolean) {
    when (error) {
      is AgentErrorType.CRITICAL -> {
        val title = generalGetString(MR.strings.agent_critical_error_title)
        val text = generalGetString(MR.strings.agent_critical_error_desc).format(error.criticalErr)
        try {
          ntfManager.showMessage(title, text)
        } catch (e: Throwable) {
          Log.e(TAG, e.stackTraceToString())
        }
        if (offerRestart) {
          AlertManager.shared.showAlertDialog(
            title = title,
            text = text,
            confirmText = generalGetString(MR.strings.restart_chat_button),
            onConfirm = ::restartChatOrApp
          )
        } else {
          AlertManager.shared.showAlertMsg(
            title = title,
            text = text,
          )
        }
      }
      is AgentErrorType.INTERNAL -> {
        AlertManager.shared.showAlertMsg(
          title = generalGetString(MR.strings.agent_internal_error_title),
          text = generalGetString(MR.strings.agent_internal_error_desc).format(error.internalErr),
        )
      }
      else -> {}
    }
  }
}
