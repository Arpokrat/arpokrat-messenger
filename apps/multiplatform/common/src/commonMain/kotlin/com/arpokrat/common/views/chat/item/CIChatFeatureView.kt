package com.arpokrat.common.views.chat.item

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import com.arpokrat.common.model.*

@Composable
fun CIChatFeatureView(
  chatsCtx: ChatModel.ChatsContext,
  chatInfo: ChatInfo,
  chatItem: ChatItem,
  feature: Feature,
  allowed: FeatureAllowed,
  iconColor: Color,
  icon: Painter? = null,
  revealed: State<Boolean>,
  showMenu: MutableState<Boolean>,
) {
  val contact = (chatInfo as? ChatInfo.Direct)?.contact
  val text = getSmartEventText(
    chatItem = chatItem,
    contact = contact,
    feature = feature,
    allowedState = allowed
  )

  SystemEventPill(text = text, icon = icon ?: feature.iconFilled())
}