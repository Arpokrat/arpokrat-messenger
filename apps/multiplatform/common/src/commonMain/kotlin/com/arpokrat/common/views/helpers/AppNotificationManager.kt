package com.arpokrat.common.views.helpers

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.arpokrat.common.platform.windowWidth
import com.arpokrat.common.ui.theme.SimplexGreen
import kotlinx.coroutines.delay

data class AppNotification(
  val message: String,
  val isError: Boolean = false,
  val id: Long = System.currentTimeMillis()
)

object AppNotificationManager {
  var currentNotification by mutableStateOf<AppNotification?>(null)
    private set

  fun showSuccess(message: String) {
    currentNotification = AppNotification(message = message, isError = false)
  }

  fun showError(message: String) {
    currentNotification = AppNotification(message = message, isError = true)
  }

  fun dismiss() {
    currentNotification = null
  }
}

@Composable
fun AppNotificationPopup(
  notification: AppNotification?,
  onDismiss: () -> Unit
) {
  var activeNotif by remember { mutableStateOf(notification) }

  LaunchedEffect(notification) {
    if (notification != null) activeNotif = notification
  }

  if (activeNotif != null) {
    Popup(
      alignment = Alignment.TopCenter,
      properties = PopupProperties(clippingEnabled = false)
    ) {
      Box(
        modifier = Modifier
          .width(windowWidth())
          .padding(horizontal = 16.dp, vertical = 8.dp)
          .statusBarsPadding()
      ) {
        AnimatedVisibility(
          visible = notification != null,
          enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
          exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
          modifier = Modifier.fillMaxWidth()
        ) {
          LaunchedEffect(notification?.id) {
            if (notification != null) {
              delay(3000)
              onDismiss()
            }
          }

          val displayNotif = notification ?: activeNotif!!

          Card(
            shape = RoundedCornerShape(12.dp),
            backgroundColor = if (displayNotif.isError) MaterialTheme.colors.error else SimplexGreen,
            elevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(16.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = if (displayNotif.isError) Icons.Default.ErrorOutline else Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color.White
              )
              Spacer(Modifier.width(12.dp))
              Text(
                text = displayNotif.message,
                color = Color.White,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }
      }
    }
  }
}