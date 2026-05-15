package com.arpokrat.common.views.helpers

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arpokrat.common.model.ChatModel
import com.arpokrat.common.platform.*
import com.arpokrat.common.ui.theme.DEFAULT_PADDING
import com.arpokrat.common.ui.theme.DEFAULT_PADDING_HALF
import com.arpokrat.res.MR
import dev.icerock.moko.resources.StringResource
import dev.icerock.moko.resources.compose.painterResource
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
private fun ArpokratAlertDialog(
  onDismissRequest: () -> Unit,
  title: @Composable (() -> Unit)? = null,
  buttons: @Composable () -> Unit
) {
  val isDark = !MaterialTheme.colors.isLight
  val borderColor = if (isDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.08f)
  val shape = RoundedCornerShape(corner = CornerSize(25.dp))

  AlertDialog(
    onDismissRequest = onDismissRequest,
    title = title,
    buttons = buttons,
    shape = shape,
    backgroundColor = MaterialTheme.colors.surface,
    modifier = Modifier.border(1.dp, borderColor, shape)
  )
}

class AlertManager {
  private var alertViews = MutableStateFlow(listOf<(@Composable () -> Unit)>())

  fun showAlert(alert: @Composable () -> Unit) {
    alertViews.value += alert
  }

  fun hideAlert() {
    alertViews.value = ArrayList(alertViews.value).also { it.removeLastOrNull() }
  }

  fun hideAllAlerts() {
    alertViews.value = listOf()
  }

  fun hasAlertsShown() = alertViews.value.isNotEmpty()

  fun showAlertDialogButtons(
    title: String,
    text: String? = null,
    buttons: @Composable () -> Unit,
  ) {
    showAlert {
      ArpokratAlertDialog(
        onDismissRequest = this::hideAlert,
        title = alertTitle(title),
        buttons = { AlertContent(text, null, extraPadding = true) { buttons() } }
      )
    }
  }

  fun showAlertDialogButtonsColumn(
    title: String,
    text: String? = null,
    textAlign: TextAlign = TextAlign.Center,
    dismissible: Boolean = true,
    onDismissRequest: (() -> Unit)? = null,
    hostDevice: Pair<Long?, String>? = null,
    belowTextContent: @Composable (() -> Unit) = {},
    buttons: @Composable () -> Unit,
  ) {
    showAlert {
      ArpokratAlertDialog(
        onDismissRequest = { onDismissRequest?.invoke(); if (dismissible) hideAlert() },
        title = alertTitle(title),
        buttons = { AlertContent(text, hostDevice, extraPadding = true, textAlign = textAlign, belowTextContent = belowTextContent) { buttons() } }
      )
    }
  }

  fun showAlertDialogButtonsColumn(
    title: String,
    text: AnnotatedString,
    onDismissRequest: (() -> Unit)? = null,
    hostDevice: Pair<Long?, String>? = null,
    buttons: @Composable () -> Unit,
  ) {
    showAlert {
      ArpokratAlertDialog(
        onDismissRequest = { onDismissRequest?.invoke(); hideAlert() },
        title = alertTitle(title),
        buttons = { AlertContent(text, hostDevice, extraPadding = true) { buttons() } }
      )
    }
  }

  fun showAlertDialog(
    title: String,
    text: String? = null,
    confirmText: String = generalGetString(MR.strings.ok),
    onConfirm: (() -> Unit)? = null,
    dismissText: String = generalGetString(MR.strings.cancel_verb),
    onDismiss: (() -> Unit)? = null,
    onDismissRequest: (() -> Unit)? = null,
    destructive: Boolean = false,
    hostDevice: Pair<Long?, String>? = null,
  ) {
    showAlert {
      ArpokratAlertDialog(
        onDismissRequest = { onDismissRequest?.invoke(); hideAlert() },
        title = alertTitle(title),
        buttons = {
          AlertContent(text, hostDevice, true) {
            Row(
              Modifier.fillMaxWidth().padding(horizontal = DEFAULT_PADDING),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              val focusRequester = remember { FocusRequester() }
              LaunchedEffect(Unit) {
                delay(200)
                focusRequester.requestFocus()
              }
              TextButton(onClick = { onDismiss?.invoke(); hideAlert() }) { Text(dismissText) }
              TextButton(onClick = { onConfirm?.invoke(); hideAlert() }, Modifier.focusRequester(focusRequester)) {
                Text(confirmText, color = if (destructive) MaterialTheme.colors.error else Color.Unspecified)
              }
            }
          }
        }
      )
    }
  }

  fun showAlertDialogStacked(
    title: String,
    text: String? = null,
    confirmText: String = generalGetString(MR.strings.ok),
    onConfirm: (() -> Unit)? = null,
    dismissText: String = generalGetString(MR.strings.cancel_verb),
    onDismiss: (() -> Unit)? = null,
    onDismissRequest: (() -> Unit)? = null,
    destructive: Boolean = false
  ) {
    showAlert {
      ArpokratAlertDialog(
        onDismissRequest = { onDismissRequest?.invoke(); hideAlert() },
        title = alertTitle(title),
        buttons = {
          AlertContent(text, null) {
            Column(
              Modifier.fillMaxWidth().padding(horizontal = DEFAULT_PADDING_HALF).padding(top = DEFAULT_PADDING, bottom = 2.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              TextButton(onClick = { onDismiss?.invoke(); hideAlert() }) { Text(dismissText) }
              TextButton(onClick = { onConfirm?.invoke(); hideAlert() }) {
                Text(confirmText, color = if (destructive) MaterialTheme.colors.error else Color.Unspecified, textAlign = TextAlign.End)
              }
            }
          }
        }
      )
    }
  }

  fun showAlertMsg(
    title: String, text: String? = null,
    confirmText: String = generalGetString(MR.strings.ok),
    onConfirm: (() -> Unit)? = null,
    hostDevice: Pair<Long?, String>? = null,
    shareText: Boolean? = null
  ) {
    showAlert {
      ArpokratAlertDialog(
        onDismissRequest = this::hideAlert,
        title = alertTitle(title),
        buttons = {
          AlertContent(text, hostDevice, extraPadding = true) {
            val focusRequester = remember { FocusRequester() }
            LaunchedEffect(Unit) {
              delay(200)
              focusRequester.requestFocus()
            }
            val showShareButton = text != null && (shareText == true || (shareText == null && text.length > 500))
            Row(
              Modifier.fillMaxWidth().padding(horizontal = DEFAULT_PADDING),
              horizontalArrangement = if (showShareButton) Arrangement.SpaceBetween else Arrangement.Center
            ) {
              val clipboard = LocalClipboardManager.current
              if (showShareButton && text != null) {
                TextButton(onClick = { clipboard.shareText(text); hideAlert() }) { Text(stringResource(MR.strings.share_verb)) }
              }
              TextButton(onClick = { onConfirm?.invoke(); hideAlert() }, Modifier.focusRequester(focusRequester)) {
                Text(confirmText, color = Color.Unspecified)
              }
            }
          }
        }
      )
    }
  }

  fun showAlertMsgWithProgress(
    title: String,
    text: String? = null,
  ) {
    showAlert {
      ArpokratAlertDialog(
        onDismissRequest = this::hideAlert,
        title = alertTitle(title),
        buttons = {
          AlertContent(text, null) {
            Box(Modifier.fillMaxWidth().height(72.dp).padding(bottom = DEFAULT_PADDING * 2), contentAlignment = Alignment.Center) {
              CircularProgressIndicator(Modifier.size(36.dp).padding(4.dp), color = MaterialTheme.colors.secondary, strokeWidth = 3.dp)
            }
          }
        }
      )
    }
  }

  fun showAlertMsg(
    title: StringResource,
    text: StringResource? = null,
    confirmText: StringResource = MR.strings.ok,
    onConfirm: (() -> Unit)? = null,
    hostDevice: Pair<Long?, String>? = null,
  ) = showAlertMsg(generalGetString(title), if (text != null) generalGetString(text) else null, generalGetString(confirmText), onConfirm, hostDevice)

  fun showOpenChatAlert(
    profileName: String,
    profileFullName: String,
    profileImage: @Composable () -> Unit,
    confirmText: String = generalGetString(MR.strings.connect_plan_open_chat),
    onConfirm: () -> Unit,
    dismissText: String = generalGetString(MR.strings.cancel_verb),
    onDismiss: (() -> Unit)?,
  ) {
    showAlert {
      ArpokratAlertDialog(
        onDismissRequest = { onDismiss?.invoke(); hideAlert() },
        buttons = {
          AlertContent(text = null as String?, null) {
            Column(Modifier.padding(top = DEFAULT_PADDING_HALF).width(360.dp), verticalArrangement = Arrangement.SpaceEvenly) {
              Column(Modifier.fillMaxWidth().padding(horizontal = DEFAULT_PADDING), horizontalAlignment = Alignment.CenterHorizontally) {
                profileImage()
                Spacer(Modifier.height(DEFAULT_PADDING_HALF))
                Text(profileName, textAlign = TextAlign.Center, style = MaterialTheme.typography.h4, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, modifier = Modifier.fillMaxWidth())
                if (profileFullName.isNotEmpty() && profileFullName != profileName) {
                  Spacer(Modifier.height(DEFAULT_PADDING_HALF))
                  Text(profileFullName, textAlign = TextAlign.Center, style = MaterialTheme.typography.body2, maxLines = 2, modifier = Modifier.fillMaxWidth())
                }
              }
              Column(Modifier.fillMaxWidth().padding(horizontal = DEFAULT_PADDING_HALF).padding(top = DEFAULT_PADDING, bottom = 2.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                val focusRequester = remember { FocusRequester() }
                LaunchedEffect(Unit) { delay(200); focusRequester.requestFocus() }
                TextButton(onClick = { onConfirm.invoke(); hideAlert() }, Modifier.focusRequester(focusRequester)) { Text(confirmText) }
                TextButton(onClick = { onDismiss?.invoke(); hideAlert() }) { Text(dismissText) }
              }
            }
          }
        }
      )
    }
  }

  @Composable
  fun showInView() {
    alertViews.collectAsState().value.lastOrNull()?.invoke()
  }

  companion object {
    val shared = AlertManager()
    val privacySensitive = AlertManager()
  }
}

private fun alertTitle(title: String): (@Composable () -> Unit)? {
  return {
    Text(
      title,
      Modifier.fillMaxWidth(),
      textAlign = TextAlign.Center,
      fontSize = 20.sp
    )
  }
}

@Composable
private fun AlertContent(
  text: String?,
  hostDevice: Pair<Long?, String>?,
  extraPadding: Boolean = false,
  textAlign: TextAlign = TextAlign.Center,
  belowTextContent: @Composable (() -> Unit) = {},
  content: @Composable (() -> Unit)
) {
  BoxWithConstraints {
    Column(
      Modifier
        .padding(bottom = if (appPlatform.isDesktop) DEFAULT_PADDING else DEFAULT_PADDING_HALF)
    ) {
      if (appPlatform.isDesktop) {
        HostDeviceTitle(hostDevice, extraPadding = extraPadding)
      } else {
        Spacer(Modifier.size(DEFAULT_PADDING_HALF))
      }
      CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.high) {
        if (text != null) {
          Column(Modifier.heightIn(max = this@BoxWithConstraints.maxHeight * 0.7f)
            .padding(start = DEFAULT_PADDING, end = DEFAULT_PADDING)
            .verticalScroll(rememberScrollState())
          ) {
            SelectionContainer {
              Text(
                escapedHtmlToAnnotatedString(text, LocalDensity.current),
                Modifier.fillMaxWidth(),
                fontSize = 16.sp,
                textAlign = textAlign,
                color = MaterialTheme.colors.secondary
              )
            }
            belowTextContent()
            Spacer(Modifier.height(DEFAULT_PADDING * 1.5f))
          }
        }
      }
      content()
    }
  }
}

@Composable
private fun AlertContent(text: AnnotatedString?, hostDevice: Pair<Long?, String>?, extraPadding: Boolean = false, content: @Composable (() -> Unit)) {
  BoxWithConstraints {
    Column(
      Modifier
        .verticalScroll(rememberScrollState())
        .padding(bottom = if (appPlatform.isDesktop) DEFAULT_PADDING else DEFAULT_PADDING_HALF)
    ) {
      if (appPlatform.isDesktop) {
        HostDeviceTitle(hostDevice, extraPadding = extraPadding)
      } else {
        Spacer(Modifier.size(DEFAULT_PADDING_HALF))
      }
      CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.high) {
        if (text != null) {
          Column(
            Modifier.heightIn(max = this@BoxWithConstraints.maxHeight * 0.7f)
              .verticalScroll(rememberScrollState())
          ) {
            SelectionContainer {
              Text(
                text,
                Modifier.fillMaxWidth().padding(start = DEFAULT_PADDING, end = DEFAULT_PADDING, bottom = DEFAULT_PADDING * 1.5f),
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colors.secondary
              )
            }
          }
        }
      }
      content()
    }
  }
}

fun hostDevice(rhId: Long?): Pair<Long?, String>? = if (rhId == null && chatModel.remoteHosts.isNotEmpty()) {
  null to ChatModel.controller.appPrefs.deviceNameForRemoteAccess.get()!!
} else if (rhId == null) {
  null
} else {
  rhId to (chatModel.remoteHosts.firstOrNull { it.remoteHostId == rhId }?.hostDeviceName?.ifEmpty { rhId.toString() } ?: rhId.toString())
}

@Composable
private fun HostDeviceTitle(hostDevice: Pair<Long?, String>?, extraPadding: Boolean = false) {
  if (hostDevice != null) {
    Row(Modifier.fillMaxWidth().padding(top = 5.dp, bottom = if (extraPadding) DEFAULT_PADDING * 2 else DEFAULT_PADDING_HALF), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
      Icon(painterResource(if (hostDevice.first == null) MR.images.ic_desktop else MR.images.ic_smartphone_300), null, Modifier.size(15.dp), tint = MaterialTheme.colors.secondary)
      Spacer(Modifier.width(10.dp))
      Text(hostDevice.second, color = MaterialTheme.colors.secondary)
    }
  } else {
    Spacer(Modifier.height(if (extraPadding) DEFAULT_PADDING * 2 else 0.dp))
  }
}