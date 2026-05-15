package com.arpokrat.common.wallet.ui.onboarding

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import com.arpokrat.common.views.helpers.generalGetString
import com.arpokrat.common.views.localauth.PasscodeView
import com.arpokrat.res.MR

@Composable
fun SetWalletPasscodeView(
  onSubmit: (String) -> Unit,
  onCancel: () -> Unit
) {
  val passcode = rememberSaveable { mutableStateOf("") }
  var enteredPassword by rememberSaveable { mutableStateOf("") }
  var confirming by rememberSaveable { mutableStateOf(false) }

  Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background) {
    if (confirming) {
      PasscodeView(
        passcode = passcode,
        title = generalGetString(MR.strings.confirm_passcode),
        reason = null,
        submitLabel = generalGetString(MR.strings.confirm_verb),
        submitEnabled = { pwd -> pwd == enteredPassword },
        submit = {
          if (passcode.value == enteredPassword) {
            val finalPin = passcode.value
            enteredPassword = ""
            passcode.value = ""
            onSubmit(finalPin)
          }
        },
        cancel = {
          confirming = false
          passcode.value = ""
          enteredPassword = ""
        }
      )
    } else {
      PasscodeView(
        passcode = passcode,
        title = generalGetString(MR.strings.wallet_create_pin_title),
        reason = null,
        submitLabel = generalGetString(MR.strings.wallet_btn_next),
        submitEnabled = { pwd -> pwd.length >= 4 },
        submit = {
          enteredPassword = passcode.value
          passcode.value = ""
          confirming = true
        },
        cancel = onCancel
      )
    }
  }
}

@Composable
fun VerifyWalletPasscodeView(
  title: String = generalGetString(MR.strings.wallet_enter_pin),
  verifyPin: (String) -> Boolean,
  onSuccess: () -> Unit,
  onCancel: () -> Unit
) {
  val passcode = rememberSaveable { mutableStateOf("") }
  var errorReason by rememberSaveable { mutableStateOf<String?>(null) }

  Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background) {
    PasscodeView(
      passcode = passcode,
      title = title,
      reason = errorReason,
      submitLabel = generalGetString(MR.strings.wallet_btn_unlock),
      submitEnabled = { pwd -> pwd.length >= 4 },
      submit = {
        if (verifyPin(passcode.value)) {
          errorReason = null
          passcode.value = ""
          onSuccess()
        } else {
          errorReason = generalGetString(MR.strings.incorrect_passcode)
          passcode.value = ""
        }
      },
      cancel = onCancel
    )
  }
}