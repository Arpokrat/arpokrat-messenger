package com.arpokrat.common.views.chat.item

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arpokrat.common.model.*
import com.arpokrat.res.MR
import dev.icerock.moko.resources.compose.stringResource
import com.arpokrat.common.platform.onRightClick

enum class FeatureEventDirection { RCV, SND }

@Composable
fun SystemEventPill(
  text: String,
  icon: Painter? = null,
  onClick: (() -> Unit)? = null
) {
  val contentColor = MaterialTheme.colors.onSurface.copy(alpha = 0.65f)
  val backgroundColor = MaterialTheme.colors.onSurface.copy(alpha = 0.06f)

  Box(
    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
    contentAlignment = Alignment.Center
  ) {
    Surface(
      shape = RoundedCornerShape(12.dp),
      color = backgroundColor,
      elevation = 0.dp,
      modifier = Modifier
        .combinedClickable(enabled = onClick != null, onClick = { onClick?.invoke() }, onLongClick = {})
        .onRightClick {}
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
      ) {
        if (icon != null) {
          Icon(painter = icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = contentColor)
          Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
          text = text,
          style = MaterialTheme.typography.caption.copy(fontSize = 12.5.sp, fontWeight = FontWeight.Medium),
          color = contentColor,
          textAlign = TextAlign.Center
        )
      }
    }
  }
}

@Composable
fun getSmartEventText(
  chatItem: ChatItem,
  contact: Contact?,
  feature: Feature,
  allowedState: FeatureAllowed,
  ttl: Int? = null
): String {
  val actorName = if (chatItem.chatDir.sent) stringResource(MR.strings.sys_event_you) else contact?.displayName ?: stringResource(MR.strings.contact_fallback)
  val isEnabled = allowedState == FeatureAllowed.YES || allowedState == FeatureAllowed.ALWAYS

  if ((feature as? ChatFeature) == ChatFeature.TimedMessages) {
    if (isEnabled && ttl != null && ttl > 0) {
      return stringResource(MR.strings.sys_event_set_timer, actorName, timeText(ttl))
    }
    return stringResource(MR.strings.sys_event_disabled_timer, actorName)
  }

  if (isEnabled) {
    return stringResource(MR.strings.sys_event_enabled_feature, actorName, feature.text)
  } else {
    return stringResource(MR.strings.sys_event_disabled_feature, actorName, feature.text)
  }
}

private data class LocalPrefs(val me: Boolean, val contact: Boolean)

private fun FeatureEnabled.localize(dir: FeatureEventDirection): LocalPrefs =
  when (dir) {
    FeatureEventDirection.SND -> LocalPrefs(me = forUser,    contact = forContact)
    FeatureEventDirection.RCV -> LocalPrefs(me = forContact, contact = forUser)
  }

private fun ChatFeature.isConsensus(): Boolean =
  this == ChatFeature.Reactions || this == ChatFeature.Voice || this == ChatFeature.Calls

fun FeatureEnabled.isActive(feature: ChatFeature, dir: FeatureEventDirection): Boolean {
  val p = localize(dir)
  return when (feature) {
    ChatFeature.Reactions, ChatFeature.Voice, ChatFeature.Calls ->
      p.me && p.contact

    ChatFeature.TimedMessages ->
      p.me

    else ->
      p.me || p.contact
  }
}

fun FeatureEnabled.shouldShowEvent(feature: ChatFeature, dir: FeatureEventDirection): Boolean {
  if (feature == ChatFeature.FullDelete) return false
  if (feature == ChatFeature.TimedMessages) return true

  val p = localize(dir)

  if (feature == ChatFeature.Reactions || feature == ChatFeature.Voice || feature == ChatFeature.Calls) {
    return when (dir) {
      FeatureEventDirection.RCV -> p.me
      FeatureEventDirection.SND -> p.contact
    }
  }

  return true
}