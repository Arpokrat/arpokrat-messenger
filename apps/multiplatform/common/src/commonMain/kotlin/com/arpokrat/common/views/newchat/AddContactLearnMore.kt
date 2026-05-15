package com.arpokrat.common.views.newchat

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.arpokrat.common.platform.ColumnWithScrollBar
import com.arpokrat.common.platform.chatModel
import com.arpokrat.common.ui.theme.DEFAULT_PADDING
import dev.icerock.moko.resources.compose.stringResource
import com.arpokrat.common.views.helpers.AppBarTitle
import com.arpokrat.common.views.helpers.KeyChangeEffect
import com.arpokrat.common.views.onboarding.ReadableText
import com.arpokrat.common.views.onboarding.ReadableTextWithLink
import com.arpokrat.res.MR

@Composable
fun AddContactLearnMore(close: () -> Unit) {
  ColumnWithScrollBar(Modifier.padding(horizontal = DEFAULT_PADDING)) {
    AppBarTitle(stringResource(MR.strings.one_time_link), withPadding = false)
    ReadableText(MR.strings.scan_qr_to_connect_to_contact)
    ReadableText(MR.strings.if_you_cant_meet_in_person)
    // TODO Create User Guide
  // ReadableTextWithLink(MR.strings.read_more_in_user_guide_with_link, "https://Arpokrat.com/connect-to-friends")
  }
  KeyChangeEffect(chatModel.chatId.value) {
    close()
  }
}
