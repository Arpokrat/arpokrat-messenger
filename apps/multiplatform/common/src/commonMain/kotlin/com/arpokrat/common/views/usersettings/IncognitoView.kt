package com.arpokrat.common.views.usersettings

import SectionBottomSpacer
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arpokrat.common.platform.ColumnWithScrollBar
import com.arpokrat.common.ui.theme.DEFAULT_PADDING
import com.arpokrat.common.views.helpers.AppBarTitle
import com.arpokrat.common.views.helpers.generalGetString
import com.arpokrat.res.MR
import dev.icerock.moko.resources.compose.stringResource

@Composable
fun IncognitoView() {
  IncognitoLayout()
}

@Composable
fun IncognitoLayout() {
  ColumnWithScrollBar {
    AppBarTitle(stringResource(MR.strings.settings_section_title_incognito))
    Column(
      Modifier
        .padding(horizontal = DEFAULT_PADDING),
      verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
      Text(generalGetString(MR.strings.incognito_info_protects))
      Text(generalGetString(MR.strings.incognito_info_allows))
      Text(generalGetString(MR.strings.incognito_info_share))
      // TODO Create a User Guide
      // ReadableTextWithLink(MR.strings.read_more_in_user_guide_with_link, "https://simplex.chat/docs/guide/chat-profiles.html#incognito-mode")
      SectionBottomSpacer()
    }
  }
}
