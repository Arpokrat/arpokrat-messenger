package com.arpokrat.common.views.chat.item

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arpokrat.common.model.*
import com.arpokrat.common.ui.theme.*
import com.arpokrat.common.views.helpers.*
import com.arpokrat.res.MR
import dev.icerock.moko.resources.compose.stringResource
import dev.icerock.moko.resources.compose.painterResource
import kotlinx.coroutines.delay

@Composable
fun CIGroupInvitationView(
  ci: ChatItem,
  groupInvitation: CIGroupInvitation,
  memberRole: GroupMemberRole,
  showTimestamp: Boolean,
  chatIncognito: Boolean = false,
  joinGroup: (Long, () -> Unit) -> Unit,
  cancelInvitation: (Long) -> Unit,
  timedMessagesTTL: Int?
) {
  val sent = ci.chatDir.sent
  val status = groupInvitation.status
  val isPending = status == CIGroupInvitationStatus.Pending

  val inProgress = remember { mutableStateOf(false) }
  var progressByTimeout by rememberSaveable { mutableStateOf(false) }
  LaunchedEffect(inProgress.value) {
    progressByTimeout = if (inProgress.value) {
      delay(1000)
      inProgress.value
    } else {
      false
    }
  }

  val bubbleColor = if (sent) MaterialTheme.appColors.sentMessage else MaterialTheme.appColors.receivedMessage
  val contentColor = LocalContentColor.current
  val shape = RoundedCornerShape(18.dp)

  Box(
    modifier = Modifier
      .widthIn(min = 240.dp, max = 320.dp)
      .clip(shape)
      .background(bubbleColor)
  ) {
    CompositionLocalProvider(LocalContentColor provides contentColor) {
      Box {
        Column(
          modifier = Modifier.padding(12.dp)
        ) {
          GroupInfoHeader(groupInvitation)

          Spacer(modifier = Modifier.height(12.dp))
          Divider(color = contentColor.copy(alpha = 0.1f), thickness = 1.dp)
          Spacer(modifier = Modifier.height(12.dp))

          if (isPending) {
            if (sent) {
              GroupActionButton(
                label = stringResource(MR.strings.cancel_group_invitation),
                isPrimary = false,
                isDestructive = false,
                isLoading = false,
                showCancelIcon = true,
                onClick = {
                  cancelInvitation(ci.id)
                }
              )
            } else {
              GroupActionButton(
                label = stringResource(if (chatIncognito) MR.strings.group_invitation_tap_to_join_incognito else MR.strings.group_invitation_tap_to_join),
                isPrimary = chatIncognito,
                isLoading = inProgress.value && progressByTimeout,
                onClick = {
                  inProgress.value = true
                  joinGroup(groupInvitation.groupId) { inProgress.value = false }
                }
              )
            }
          } else {
            GroupStatusRow(status)
          }

          Spacer(modifier = Modifier.height(16.dp))
        }

        Box(
          modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(end = 8.dp, bottom = 6.dp)
        ) {
          CIMetaView(
            chatItem = ci,
            timedMessagesTTL = timedMessagesTTL,
            showStatus = false,
            showEdited = false,
            showViaProxy = false,
            showTimestamp = showTimestamp
          )
        }
      }
    }
  }
}

@Composable
private fun GroupInfoHeader(groupInvitation: CIGroupInvitation) {
  val p = groupInvitation.groupProfile

  Row(verticalAlignment = Alignment.CenterVertically) {
    ProfileImage(
      size = 52.dp,
      image = p.image,
      icon = MR.images.ic_supervised_user_circle_filled,
      color = if (isInDarkTheme()) FileDark else FileLight
    )

    Spacer(modifier = Modifier.width(12.dp))

    Column {
      Text(
        text = p.displayName,
        style = MaterialTheme.typography.subtitle1,
        fontWeight = FontWeight.Bold,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
      )

      if (p.fullName.isNotEmpty() && p.displayName != p.fullName) {
        Text(
          text = p.fullName,
          style = MaterialTheme.typography.caption,
          color = LocalContentColor.current.copy(alpha = 0.7f),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
    }
  }
}

@Composable
private fun GroupActionButton(
  label: String,
  isPrimary: Boolean = false,
  isDestructive: Boolean = false,
  isLoading: Boolean = false,
  showCancelIcon: Boolean = false,
  onClick: () -> Unit
) {
  val buttonColor = when {
    isDestructive -> MaterialTheme.colors.error.copy(alpha = 0.1f)
    isPrimary -> MaterialTheme.colors.primary
    else -> MaterialTheme.colors.onSurface.copy(alpha = 0.08f)
  }

  val textColor = when {
    isDestructive -> MaterialTheme.colors.error
    isPrimary -> MaterialTheme.colors.onPrimary
    else -> MaterialTheme.colors.primary
  }

  Column(horizontalAlignment = Alignment.Start) {
    val infoText = if (isPrimary || !isDestructive) MR.strings.you_are_invited_to_group else MR.strings.you_sent_group_invitation
    Text(
      text = stringResource(infoText),
      style = MaterialTheme.typography.body2,
      modifier = Modifier.padding(bottom = 8.dp)
    )

    Surface(
      shape = RoundedCornerShape(12.dp),
      color = buttonColor,
      modifier = Modifier
        .fillMaxWidth()
        .height(44.dp)
        .clickable(enabled = !isLoading, onClick = onClick)
    ) {
      Box(contentAlignment = Alignment.Center) {
        if (isLoading) {
          CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = textColor)
        } else {
          Row(verticalAlignment = Alignment.CenterVertically) {
            if (showCancelIcon) {
              Icon(
                painter = painterResource(MR.images.ic_close),
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(16.dp)
              )
              Spacer(Modifier.width(8.dp))
            }

            Text(
              text = label,
              style = MaterialTheme.typography.button.copy(fontSize = 14.sp),
              fontWeight = FontWeight.Bold,
              color = textColor
            )
          }
        }
      }
    }
  }
}

@Composable
private fun GroupStatusRow(status: CIGroupInvitationStatus) {
  val (text, icon, color) = when(status) {
    CIGroupInvitationStatus.Accepted -> Triple(
      stringResource(MR.strings.group_invitation_accepted),
      MR.images.ic_check,
      SimplexGreen
    )
    CIGroupInvitationStatus.Rejected -> Triple(
      stringResource(MR.strings.you_rejected_group_invitation),
      MR.images.ic_close,
      MaterialTheme.colors.error
    )
    CIGroupInvitationStatus.Expired -> Triple(
      stringResource(MR.strings.group_invitation_expired),
      MR.images.ic_error,
      LocalContentColor.current.copy(alpha = 0.5f)
    )
    else -> Triple("", null, Color.Unspecified)
  }

  if (text.isNotEmpty()) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      if (icon != null) {
        Icon(
          painter = painterResource(icon),
          contentDescription = null,
          modifier = Modifier.size(18.dp),
          tint = color
        )
        Spacer(modifier = Modifier.width(8.dp))
      }

      Text(text = text, style = MaterialTheme.typography.body2, color = color, fontWeight = FontWeight.Medium)
    }
  }
}