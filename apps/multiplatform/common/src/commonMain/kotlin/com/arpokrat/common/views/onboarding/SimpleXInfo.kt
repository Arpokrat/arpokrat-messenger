package com.arpokrat.common.views.onboarding

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.arpokrat.common.model.*
import com.arpokrat.common.model.ChatController.appPrefs
import com.arpokrat.common.platform.*
import com.arpokrat.common.ui.theme.*
import com.arpokrat.common.views.helpers.*
import com.arpokrat.common.views.migration.MigrateToDeviceView
import com.arpokrat.common.views.migration.MigrationToState
import com.arpokrat.res.MR
import dev.icerock.moko.resources.StringResource
import dev.icerock.moko.resources.compose.painterResource
import dev.icerock.moko.resources.compose.stringResource

@Composable
fun SimpleXInfo(chatModel: ChatModel, onboarding: Boolean = true) {
  if (onboarding) {
    CompositionLocalProvider(LocalAppBarHandler provides rememberAppBarHandler()) {
      ModalView({}, showClose = false, showAppBar = false) {
        SimpleXInfoLayout(user = chatModel.currentUser.value, onboardingStage = chatModel.controller.appPrefs.onboardingStage)
      }
    }
  } else {
    SimpleXInfoLayout(user = chatModel.currentUser.value, onboardingStage = null)
  }
}

@Composable
fun SimpleXInfoLayout(user: User?, onboardingStage: SharedPreference<OnboardingStage>?) {
  ColumnWithScrollBar(Modifier.padding(horizontal = DEFAULT_ONBOARDING_HORIZONTAL_PADDING), horizontalAlignment = Alignment.CenterHorizontally) {

    Spacer(Modifier.height(16.dp))

    Image(
      painter = painterResource(if (isInDarkTheme()) MR.images.logo_light else MR.images.logo),
      contentDescription = stringResource(MR.strings.app_logo_descr),
      contentScale = ContentScale.Fit,
      modifier = Modifier.fillMaxWidth().height(120.dp)
    )

    Spacer(Modifier.height(24.dp))

    Text(
      text = stringResource(MR.strings.welcome_to_arpokrat),
      fontSize = 28.sp,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colors.onBackground,
      textAlign = TextAlign.Center
    )
    Spacer(Modifier.height(16.dp))

    OnboardingInformationButton(
      text = stringResource(MR.strings.next_generation_of_private_messaging),
      onClick = { ModalManager.fullscreen.showModal { HowItWorks(user, onboardingStage) } },
    )

    Spacer(Modifier.weight(1f))

    Column {
      InfoRow(
        icon = Icons.Default.VisibilityOff,
        title = stringResource(MR.strings.feature_absolute_privacy_title),
        desc = stringResource(MR.strings.feature_absolute_privacy_desc)
      )
      InfoRow(
        icon = Icons.Default.AccountBalanceWallet,
        title = stringResource(MR.strings.feature_crypto_wallet_title),
        desc = stringResource(MR.strings.feature_crypto_wallet_desc)
      )
      InfoRow(
        icon = Icons.Default.Security,
        title = stringResource(MR.strings.feature_security_title),
        desc = stringResource(MR.strings.feature_security_desc)
      )
    }

    Spacer(Modifier.weight(1f))

    if (onboardingStage != null) {
      Column(Modifier.widthIn(max = if (appPlatform.isAndroid) 450.dp else 1000.dp).align(Alignment.CenterHorizontally), horizontalAlignment = Alignment.CenterHorizontally) {
        OnboardingActionButton(user, onboardingStage)
        TextButtonBelowOnboardingButton(stringResource(MR.strings.migrate_from_another_device)) {
          chatModel.migrationState.value = MigrationToState.PasteOrScanLink
          ModalManager.fullscreen.showCustomModal { close -> MigrateToDeviceView(close) }
        }
      }
    }
  }
}

@Composable
private fun InfoRow(icon: ImageVector, title: String, desc: String) {
  Row(Modifier.padding(bottom = 24.dp), verticalAlignment = Alignment.CenterVertically) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      modifier = Modifier.size(24.dp),
      tint = MaterialTheme.colors.primary
    )
    Spacer(Modifier.width(20.dp))
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
      Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colors.onBackground)
      Text(desc, fontSize = 14.sp, lineHeight = 20.sp, color = MaterialTheme.colors.secondary)
    }
  }
}

@Composable
expect fun OnboardingActionButton(user: User?, onboardingStage: SharedPreference<OnboardingStage>, onclick: (() -> Unit)? = null)

@Composable
fun OnboardingActionButton(
  modifier: Modifier = Modifier,
  labelId: StringResource,
  onboarding: OnboardingStage?,
  enabled: Boolean = true,
  icon: androidx.compose.ui.graphics.painter.Painter? = null,
  iconColor: Color = Color.White,
  onclick: (() -> Unit)?
) {
  Button(
    onClick = {
      onclick?.invoke()
      if (onboarding != null) {
        appPrefs.onboardingStage.set(onboarding)
      }
    },
    modifier = modifier.height(56.dp),
    shape = RoundedCornerShape(16.dp),
    enabled = enabled,
    elevation = ButtonDefaults.elevation(0.dp),
    colors = ButtonDefaults.buttonColors(
      backgroundColor = MaterialTheme.colors.primary,
      contentColor = Color.White,
      disabledBackgroundColor = MaterialTheme.colors.surface.copy(alpha = 0.5f),
      disabledContentColor = Color.Gray
    )
  ) {
    if (icon != null) {
      Icon(icon, stringResource(labelId), Modifier.padding(end = DEFAULT_PADDING_HALF), tint = iconColor)
    }
    Text(stringResource(labelId), fontSize = 18.sp, fontWeight = FontWeight.Bold)
  }
}

@Composable
fun TextButtonBelowOnboardingButton(text: String, onClick: (() -> Unit)?) {
  val state = getKeyboardState()
  val enabled = onClick != null
  val topPadding by animateDpAsState(if (appPlatform.isAndroid && state.value == KeyboardState.Opened) 0.dp else 7.5.dp)
  val bottomPadding by animateDpAsState(if (appPlatform.isAndroid && state.value == KeyboardState.Opened) 0.dp else 7.5.dp)
  if ((appPlatform.isAndroid && state.value == KeyboardState.Closed) || topPadding > 0.dp) {
    TextButton({ onClick?.invoke() }, Modifier.padding(top = topPadding, bottom = bottomPadding).clip(CircleShape), enabled = enabled) {
      Text(
        text,
        Modifier.padding(start = DEFAULT_PADDING_HALF, end = DEFAULT_PADDING_HALF, bottom = 5.dp),
        color = if (enabled) MaterialTheme.colors.primary else MaterialTheme.colors.secondary,
        fontWeight = FontWeight.Medium,
        textAlign = TextAlign.Center
      )
    }
  } else {
    Spacer(Modifier.height(DEFAULT_PADDING * 2))
  }
}

@Composable
fun OnboardingInformationButton(text: String, onClick: () -> Unit) {
  Box(modifier = Modifier.clip(CircleShape).clickable { onClick() }) {
    Row(Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
      Icon(painterResource(MR.images.ic_info), null, tint = MaterialTheme.colors.primary, modifier = Modifier.size(16.dp))
      Text(text, style = MaterialTheme.typography.button, color = MaterialTheme.colors.primary)
    }
  }
}