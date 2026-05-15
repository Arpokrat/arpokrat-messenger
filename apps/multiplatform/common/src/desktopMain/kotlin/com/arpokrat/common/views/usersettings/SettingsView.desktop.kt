package com.arpokrat.common.views.usersettings

import SectionView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.arpokrat.common.model.ChatController.appPrefs
import com.arpokrat.common.model.ChatModel
import com.arpokrat.common.platform.AppUpdatesChannel
import com.arpokrat.common.ui.theme.DEFAULT_PADDING_HALF
import com.arpokrat.common.views.helpers.*
import com.arpokrat.res.MR
import dev.icerock.moko.resources.compose.painterResource
import dev.icerock.moko.resources.compose.stringResource

@Composable
actual fun SettingsSectionApp(
  showSettingsModal: (@Composable (ChatModel) -> Unit) -> (() -> Unit),
  showVersion: () -> Unit,
  withAuth: (title: String, desc: String, block: () -> Unit) -> Unit
) {
  SectionView(stringResource(MR.strings.settings_section_title_app)) {
    SettingsActionItem(painterResource(MR.images.ic_code), stringResource(MR.strings.settings_developer_tools), showSettingsModal { DeveloperView(withAuth) })
    val selectedChannel = remember { appPrefs.appUpdateChannel.state }
    val values = AppUpdatesChannel.entries.map { it to it.text }
    ExposedDropDownSettingRow(stringResource(MR.strings.app_check_for_updates), values, selectedChannel) {
      appPrefs.appUpdateChannel.set(it)
      setupUpdateChecker()
    }
    AppVersionItem(showVersion)
  }
}
