package com.arpokrat.common.views.chat.item

import androidx.compose.runtime.*
import com.arpokrat.common.model.*

@Composable
fun CIFeaturePreferenceView(
  chatItem: ChatItem,
  contact: Contact?,
  feature: ChatFeature,
  allowed: FeatureAllowed,
  acceptFeature: (Contact, ChatFeature, Int?) -> Unit
) {
  if (contact != null) {
    var displayTTL: Int? = null
    val isIncoming = !chatItem.chatDir.sent

    if (feature == ChatFeature.TimedMessages) {
      if (isIncoming) {
        displayTTL = (contact.mergedPreferences.timedMessages.userPreference as? ContactUserPrefTimed.Contact)?.pref?.ttl
      } else {
        displayTTL = (contact.mergedPreferences.timedMessages.userPreference as? ContactUserPrefTimed.User)?.pref?.ttl
      }

      if (displayTTL == null && (allowed == FeatureAllowed.YES || allowed == FeatureAllowed.ALWAYS)) {
        displayTTL = contact.mergedPreferences.timedMessages.contactPreference?.ttl
      }
    }
    val text = getSmartEventText(
      chatItem = chatItem,
      contact = contact,
      feature = feature,
      allowedState = allowed,
      ttl = displayTTL
    )

    SystemEventPill(text = text, icon = feature.iconFilled())
  }
}