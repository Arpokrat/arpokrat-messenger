package com.arpokrat.common.views.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arpokrat.common.model.ChatModel
import com.arpokrat.common.platform.*
import com.arpokrat.common.ui.theme.DEFAULT_PADDING
import com.arpokrat.common.ui.theme.themedBackground
import com.arpokrat.common.views.OnboardingProgressBar
import com.arpokrat.common.views.database.*
import com.arpokrat.common.views.helpers.*
import com.arpokrat.res.MR
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.delay

@Composable
fun SetupDatabasePassphrase(m: ChatModel) {
  val progressIndicator = remember { mutableStateOf(false) }
  val prefs = m.controller.appPrefs
  val initialRandomDBPassphrase = remember { mutableStateOf(prefs.initialRandomDBPassphrase.get()) }
  val currentKey = remember { mutableStateOf(if (initialRandomDBPassphrase.value) DatabaseUtils.ksDatabasePassword.get() ?: "" else "") }
  val newKey = rememberSaveable { mutableStateOf("") }
  val confirmNewKey = rememberSaveable { mutableStateOf("") }

  fun nextStep() {
    if (appPlatform.isAndroid || chatModel.currentUser.value != null) {
      m.controller.appPrefs.onboardingStage.set(OnboardingStage.Step3_ChooseServerOperators)
    } else {
      m.controller.appPrefs.onboardingStage.set(OnboardingStage.LinkAMobile)
    }
  }

  SetupDatabasePassphraseLayout(
    currentKey,
    newKey,
    confirmNewKey,
    progressIndicator,
    onConfirmEncrypt = {
      withLongRunningApi {
        if (m.chatRunning.value == true) stopChatAsync(m)
        prefs.storeDBPassphrase.set(false)

        val newKeyValue = newKey.value
        val success = encryptDatabase(
          currentKey = currentKey, newKey = newKey, confirmNewKey = confirmNewKey,
          initialRandomDBPassphrase = mutableStateOf(true), useKeychain = mutableStateOf(false),
          storedKey = mutableStateOf(true), progressIndicator = progressIndicator, migration = false
        )
        if (success) {
          startChat(newKeyValue)
          nextStep()
        } else {
          prefs.storeDBPassphrase.set(true)
        }
      }
    },
    nextStep = ::nextStep,
  )

  if (progressIndicator.value) ProgressIndicator()

  DisposableEffect(Unit) {
    onDispose {
      if (m.chatRunning.value != true) {
        withBGApi {
          val user = chatController.apiGetActiveUser(null)
          if (user != null) m.controller.startChat(user)
        }
      }
    }
  }
}

@Composable
private fun SetupDatabasePassphraseLayout(
  currentKey: MutableState<String>,
  newKey: MutableState<String>,
  confirmNewKey: MutableState<String>,
  progressIndicator: MutableState<Boolean>,
  onConfirmEncrypt: () -> Unit,
  nextStep: () -> Unit,
) {
  val keyboardState by getKeyboardState()

  CompositionLocalProvider(LocalAppBarHandler provides rememberAppBarHandler()) {
    ModalView({}, showClose = false) {
      ColumnWithScrollBar(
        Modifier.themedBackground(bgLayerSize = LocalAppBarHandler.current?.backgroundGraphicsLayerSize, bgLayer = LocalAppBarHandler.current?.backgroundGraphicsLayer).padding(horizontal = DEFAULT_PADDING),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {

        Box(modifier = Modifier.height(48.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
          OnboardingProgressBar(currentStep = 2)
        }

        Text(stringResource(MR.strings.onboarding_encrypt_title), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colors.onBackground, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Text(
          stringResource(MR.strings.onboarding_encrypt_desc),
          textAlign = TextAlign.Center,
          color = MaterialTheme.colors.secondary,
          fontSize = 15.sp,
          lineHeight = 22.sp,
          modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(Modifier.height(32.dp))

        val onClickUpdate = { if (!progressIndicator.value) onConfirmEncrypt() }
        val disabled = currentKey.value == newKey.value || newKey.value != confirmNewKey.value || newKey.value.isEmpty() || !validKey(newKey.value) || progressIndicator.value

        Column(Modifier.width(600.dp), horizontalAlignment = Alignment.CenterHorizontally) {
          val focusRequester = remember { FocusRequester() }
          LaunchedEffect(Unit) { delay(100L); focusRequester.requestFocus() }

          PassphraseField(
            newKey,
            generalGetString(MR.strings.onboarding_encrypt_master_pwd),
            modifier = Modifier.padding(horizontal = DEFAULT_PADDING).focusRequester(focusRequester),
            showStrength = true,
            isValid = ::validKey,
            keyboardActions = KeyboardActions(onNext = { defaultKeyboardAction(ImeAction.Next) }),
          )
          Spacer(Modifier.height(8.dp))
          PassphraseField(
            confirmNewKey,
            generalGetString(MR.strings.onboarding_encrypt_confirm_pwd),
            modifier = Modifier.padding(horizontal = DEFAULT_PADDING),
            isValid = { confirmNewKey.value == "" || newKey.value == confirmNewKey.value },
            keyboardActions = KeyboardActions(onDone = { defaultKeyboardAction(ImeAction.Done) }),
          )
        }
        Spacer(Modifier.weight(1f))

        Column(Modifier.widthIn(max = if (appPlatform.isAndroid) 450.dp else 1000.dp).align(Alignment.CenterHorizontally), horizontalAlignment = Alignment.CenterHorizontally) {

          Box(modifier = Modifier.fillMaxWidth().height(56.dp), contentAlignment = Alignment.Center) {
            if (!disabled) {
              OnboardingActionButton(
                Modifier.fillMaxWidth().padding(horizontal = DEFAULT_PADDING),
                labelId = MR.strings.set_database_passphrase,
                onboarding = null,
                onclick = onClickUpdate,
                enabled = true
              )
            } else if (newKey.value.isEmpty()) {
              TextButton(onClick = nextStep) {
                Text(stringResource(MR.strings.onboarding_encrypt_do_later), color = MaterialTheme.colors.secondary, fontSize = 16.sp)
              }
            }
          }

          Spacer(Modifier.height(48.dp))
        }
      }
    }
  }
}

@Composable
private fun ProgressIndicator() {
  Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    CircularProgressIndicator(Modifier.padding(horizontal = 2.dp).size(30.dp), color = MaterialTheme.colors.secondary, strokeWidth = 3.dp)
  }
}

private suspend fun startChat(key: String?) {
  val m = ChatModel
  initChatController(key)
  m.chatDbChanged.value = false
  m.chatRunning.value = true
}