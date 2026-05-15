package com.arpokrat.common.views.onboarding

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arpokrat.common.model.ChatModel
import com.arpokrat.common.model.NotificationsMode
import com.arpokrat.common.platform.*
import com.arpokrat.common.ui.theme.*
import com.arpokrat.common.views.helpers.*
import com.arpokrat.common.views.usersettings.changeNotificationsMode
import com.arpokrat.common.views.OnboardingProgressBar
import com.arpokrat.res.MR
import dev.icerock.moko.resources.compose.painterResource
import dev.icerock.moko.resources.compose.stringResource
import com.arpokrat.common.wallet.ui.dashboard.WalletMainView

@Composable
fun SetNotificationsMode(m: ChatModel) {
  LaunchedEffect(Unit) { prepareChatBeforeFinishingOnboarding() }
  var showWalletPrompt by remember { mutableStateOf(false) }

  if (showWalletPrompt) {
    WalletPromptView(
      onSkip = { ModalManager.fullscreen.closeModals() },
      onGoToWallet = {
        ModalManager.fullscreen.closeModals()
        ModalManager.fullscreen.showCustomModal { close -> WalletMainView(closeWallet = close) }
      }
    )
  } else {
    CompositionLocalProvider(LocalAppBarHandler provides rememberAppBarHandler()) {
      ModalView({}, showClose = false) {
        ColumnWithScrollBar(Modifier.themedBackground(bgLayerSize = LocalAppBarHandler.current?.backgroundGraphicsLayerSize, bgLayer = LocalAppBarHandler.current?.backgroundGraphicsLayer)) {

          Box(modifier = Modifier.height(48.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
            OnboardingProgressBar(currentStep = 5)
          }

          Text(stringResource(MR.strings.onboarding_notifications_title), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colors.onBackground, modifier = Modifier.align(Alignment.CenterHorizontally))
          Spacer(Modifier.height(16.dp))
          Text(stringResource(MR.strings.onboarding_notifications_subtitle), color = MaterialTheme.colors.secondary, modifier = Modifier.align(Alignment.CenterHorizontally))

          val currentMode = rememberSaveable { mutableStateOf(NotificationsMode.default) }

          Spacer(Modifier.height(32.dp))

          Column(Modifier.padding(horizontal = DEFAULT_ONBOARDING_HORIZONTAL_PADDING)) {
            Row(
              modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colors.onSurface.copy(alpha = 0.05f)).padding(16.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(painterResource(MR.images.ic_info), null, tint = MaterialTheme.colors.primary, modifier = Modifier.size(20.dp))
              Spacer(Modifier.width(12.dp))
              Text(stringResource(MR.strings.onboarding_notifications_disclaimer), fontSize = 13.sp, color = MaterialTheme.colors.secondary, lineHeight = 18.sp)
            }
            Spacer(Modifier.height(24.dp))

            SelectableCard(currentMode, NotificationsMode.SERVICE, stringResource(MR.strings.ntf_mode_instant), AnnotatedString(generalGetString(MR.strings.ntf_mode_instant_desc))) { currentMode.value = it }
            SelectableCard(currentMode, NotificationsMode.PERIODIC, stringResource(MR.strings.ntf_mode_battery), AnnotatedString(generalGetString(MR.strings.ntf_mode_battery_desc))) { currentMode.value = it }
            SelectableCard(currentMode, NotificationsMode.OFF, stringResource(MR.strings.ntf_mode_silent), AnnotatedString(generalGetString(MR.strings.ntf_mode_silent_desc))) { currentMode.value = it }
          }
          Spacer(Modifier.weight(1f))

          Column(Modifier.widthIn(max = if (appPlatform.isAndroid) 450.dp else 1000.dp).align(Alignment.CenterHorizontally), horizontalAlignment = Alignment.CenterHorizontally) {
            OnboardingActionButton(
              modifier = Modifier.padding(horizontal = DEFAULT_ONBOARDING_HORIZONTAL_PADDING).fillMaxWidth().height(56.dp),
              labelId = MR.strings.ok,
              onboarding = OnboardingStage.OnboardingComplete,
              onclick = {
                changeNotificationsMode(currentMode.value, m)
                showWalletPrompt = true
              }
            )
            Spacer(Modifier.height(32.dp))
          }
        }
      }
    }
  }
  SetNotificationsModeAdditions()
}

@Composable
expect fun SetNotificationsModeAdditions()

@Composable
fun <T> SelectableCard(currentValue: State<T>, newValue: T, title: String, description: AnnotatedString, onSelected: (T) -> Unit) {
  val isChecked = currentValue.value == newValue
  TextButton(
    onClick = { onSelected(newValue) },
    border = BorderStroke(1.dp, color = if (isChecked) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface.copy(alpha = 0.1f)),
    shape = RoundedCornerShape(18.dp),
    modifier = Modifier.background(Color.Transparent, RoundedCornerShape(18.dp))
  ) {
    Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
      Column(Modifier.weight(1f)) {
        Text(title, style = MaterialTheme.typography.h3, fontWeight = FontWeight.Bold, color = if (isChecked) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface)
        Text(description.text, fontSize = 13.sp, color = MaterialTheme.colors.secondary, lineHeight = 18.sp, modifier = Modifier.padding(top = 4.dp))
      }
      Spacer(Modifier.width(16.dp))
      if (isChecked) {
        Box(contentAlignment = Alignment.Center) {
          Icon(painterResource(MR.images.ic_circle_filled), null, Modifier.size(26.dp), tint = MaterialTheme.colors.primary)
          Icon(painterResource(MR.images.ic_check_filled), null, Modifier.size(20.dp), tint = MaterialTheme.colors.background)
        }
      } else {
        Icon(painterResource(MR.images.ic_circle), null, Modifier.size(26.dp), tint = MaterialTheme.colors.secondary.copy(alpha = 0.3f))
      }
    }
  }
  Spacer(Modifier.height(12.dp))
}

@Composable
fun WalletPromptView(onSkip: () -> Unit, onGoToWallet: () -> Unit) {
  ModalView({}, showClose = false) {
    Column(
      modifier = Modifier.fillMaxSize().padding(32.dp),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Image(
        painter = painterResource(if (isInDarkTheme()) MR.images.logo_wallet_light else MR.images.logo),
        contentDescription = stringResource(MR.strings.app_logo_descr),
        contentScale = ContentScale.Fit,
        modifier = Modifier.fillMaxWidth().height(120.dp)
      )

      Spacer(modifier = Modifier.height(48.dp))
      Text(stringResource(MR.strings.onboarding_wallet_title), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colors.onBackground, textAlign = TextAlign.Center)
      Text(
        stringResource(MR.strings.onboarding_wallet_desc),
        fontSize = 16.sp,
        color = MaterialTheme.colors.secondary,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = 16.dp, bottom = 48.dp)
      )

      Button(
        onClick = onGoToWallet,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.primary)
      ) {
        Text(stringResource(MR.strings.onboarding_wallet_btn_activate), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
      }

      Spacer(modifier = Modifier.height(16.dp))

      TextButton(onClick = onSkip) {
        Text(stringResource(MR.strings.onboarding_wallet_btn_skip), color = MaterialTheme.colors.secondary, fontSize = 16.sp)
      }
    }
  }
}

fun prepareChatBeforeFinishingOnboarding() {
  if (chatModel.users.any { u -> !u.user.hidden }) return
  withBGApi {
    val user = chatModel.controller.apiGetActiveUser(null) ?: return@withBGApi
    chatModel.currentUser.value = user
    chatModel.controller.startChat(user)
  }
}