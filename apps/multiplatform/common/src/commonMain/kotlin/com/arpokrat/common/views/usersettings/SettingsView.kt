package com.arpokrat.common.views.usersettings

import SectionBottomSpacer
import SectionDividerSpaced
import SectionItemView
import SectionView
import TextIconSpaced
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.arpokrat.common.model.*
import com.arpokrat.common.model.ChatController.appPrefs
import com.arpokrat.common.platform.*
import com.arpokrat.common.ui.theme.*
import com.arpokrat.common.views.chatlist.ServersSummaryView
import com.arpokrat.common.views.database.DatabaseView
import com.arpokrat.common.views.helpers.*
import com.arpokrat.common.views.migration.MigrateFromDeviceView
import com.arpokrat.common.views.usersettings.networkAndServers.NetworkAndServersView
import com.arpokrat.res.MR
import dev.icerock.moko.resources.compose.painterResource
import dev.icerock.moko.resources.compose.stringResource

val SettingsIconBackground @Composable get() = MaterialTheme.colors.primary.copy(alpha = 0.1f)
val SimplexGreenPulse = Color(0xFF00E676)

// TODO Replace
val simplexTeamUri = "simplex:/contact#/?v=1&smp=smp%3A%2F%2FPQUV2eL0t7OStZOoAsPEV2QYWt4-xilbakvGUGOItUo%3D%40smp6.simplex.im%2FK1rslx-m5bpXVIdMZg9NLUZ_8JBm8xTt%23MCowBQYDK2VuAyEALDeVe-sG8mRY22LsXlPgiwTNs9dbiLrNuA7f3ZMAJ2w%3D"

@Composable
fun SettingsView(chatModel: ChatModel, setPerformLA: (Boolean) -> Unit, close: () -> Unit) {
  val user = chatModel.currentUser.value
  val uriHandler = LocalUriHandler.current
  val view = LocalMultiplatformView()

  val showModal = { modalView: @Composable (ChatModel) -> Unit -> { ModalManager.start.showModal { modalView(chatModel) } } }
  val showSettingsModal = { modalView: @Composable (ChatModel) -> Unit -> { ModalManager.start.showModal(true) { modalView(chatModel) } } }
  val showCustomModal = { modalView: @Composable ModalData.(ChatModel, () -> Unit) -> Unit -> { ModalManager.start.showCustomModal { close -> modalView(chatModel, close) } } }

  LaunchedEffect(Unit) { hideKeyboard(view) }

  val currentTheme by CurrentColors.collectAsState()
  val isDark = currentTheme.base.mode == DefaultThemeMode.DARK

  ColumnWithScrollBar {

    AppBarTitle(
      title = stringResource(MR.strings.your_settings),
    )

    Spacer(Modifier.height(40.dp))

    val displayName = user?.displayName ?: "User"
    val initiales = getInitials(displayName)
    val userImage = user?.image

    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 20.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      if (userImage != null) {
        ProfileImage(
          size = 140.dp,
          image = userImage,
          color = MaterialTheme.colors.primary
        )
      } else {
        Box(
          modifier = Modifier
            .size(140.dp)
            .background(MaterialTheme.colors.primary, CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = initiales,
            fontSize = 64.sp,
            color = Color.White,
            fontWeight = FontWeight.Bold
          )
        }
      }

      Spacer(Modifier.height(24.dp))

      Text(
        text = displayName,
        style = MaterialTheme.typography.h5,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colors.onBackground
      )
    }

    Spacer(Modifier.height(40.dp))

    val themeText = if (isDark) "Switch to Light Mode" else "Switch to Dark Mode"
    val themeIcon = if (isDark) MR.images.ic_light_mode else MR.images.ic_bedtime_moon

    SectionItemView(
      click = {
        val newThemeName = if (isDark) DefaultTheme.LIGHT.themeName else DefaultTheme.BLACK.themeName
        ThemeManager.applyTheme(newThemeName)
        chatModel.controller.appPrefs.currentTheme.set(newThemeName)
      }
    ) {
      Icon(
        painter = painterResource(themeIcon),
        contentDescription = null,
        tint = MaterialTheme.colors.secondary
      )
      TextIconSpaced(false)
      Text(
        text = themeText,
        color = MaterialTheme.colors.onBackground
      )
    }

    SectionDividerSpaced()

    SectionView("ACCOUNT") {
      SettingsActionItem(
        icon = painterResource(MR.images.ic_person),
        text = stringResource(MR.strings.simplex_address),
        click = { ModalManager.start.showModalCloseable { close -> UserAddressView(chatModel = chatModel, close = close) } }
      )

      SettingsActionItem(
        icon = painterResource(MR.images.ic_edit),
        text = stringResource(MR.strings.network_session_mode_user),
        click = showCustomModal { m, close -> UserProfileView(m, close) }
      )

      val authWrapper: (() -> Unit) -> Unit = { block ->
        doWithAuth(generalGetString(MR.strings.auth_unlock), "", block)
      }

      SettingsActionItem(
        icon = painterResource(MR.images.ic_manage_accounts),
        text = stringResource(MR.strings.your_chat_profiles),
        click = showSettingsModal { m ->
          val searchState = remember { mutableStateOf("") }
          val hiddenState = remember { mutableStateOf(false) }
          UserProfilesView(m, searchState, hiddenState, authWrapper)
        }
      )

      SettingsActionItem(
        icon = painterResource(MR.images.ic_toggle_on),
        text = stringResource(MR.strings.chat_preferences),
        click = showCustomModal { m, close -> PreferencesView(m, m.currentUser.value!!, close) }
      )
    }

    SectionDividerSpaced()

    SectionView("APPLICATION") {
      SettingsActionItem(
        icon = painterResource(if (remember { chatModel.controller.appPrefs.notificationsMode.state }.value == NotificationsMode.OFF) MR.images.ic_bolt_off else MR.images.ic_bolt),
        text = stringResource(MR.strings.notifications),
        click = showSettingsModal { NotificationsSettingsView(it) }
      )
      SettingsActionItem(
        icon = painterResource(MR.images.ic_videocam),
        text = stringResource(MR.strings.settings_audio_video_calls),
        click = showSettingsModal { CallSettingsView(it, showModal) }
      )
      SettingsActionItem(
        icon = painterResource(MR.images.ic_lock),
        text = stringResource(MR.strings.privacy_and_security),
        click = showSettingsModal { PrivacySettingsView(it, showSettingsModal, setPerformLA) }
      )
      SettingsActionItem(
        icon = painterResource(MR.images.ic_light_mode),
        text = stringResource(MR.strings.appearance_settings),
        click = showSettingsModal { AppearanceView(it) }
      )
    }

    SectionDividerSpaced()

    SectionView("CONNECTION") {
      SettingsActionItem(
        icon = painterResource(MR.images.ic_wifi_tethering),
        text = stringResource(MR.strings.network_and_servers),
        click = showCustomModal { _, close -> NetworkAndServersView(close) }
      )

      ServerStatusItem(
        text = "Server Connection Status",
        onClick = {
          ModalManager.start.showModalCloseable {
            ServersSummaryView(chatModel.currentRemoteHost.value, remember { mutableStateOf(null) })
          }
        }
      )
    }

    SectionDividerSpaced()

    // TODO To setup later
    /*
    SectionView("DEVICES") {
        SettingsActionItem(
            icon = painterResource(MR.images.ic_monitor),
            text = stringResource(MR.strings.use_from_computer),
            click = {
                // ModalManager.fullscreen.showCustomModal { close -> ConnectDesktopView(close) }
            }
        )
    }
    SectionDividerSpaced()
    */

    SectionView("DATA & SUPPORT") {
      SettingsActionItem(
        icon = painterResource(MR.images.ic_database),
        text = stringResource(MR.strings.database_passphrase_and_export),
        click = showSettingsModal { DatabaseView() }
      )
      SettingsActionItem(
        icon = painterResource(MR.images.ic_ios_share),
        text = stringResource(MR.strings.migrate_from_device_to_another_device),
        click = { doWithAuth(generalGetString(MR.strings.auth_open_migration_to_another_device), generalGetString(MR.strings.auth_log_in_using_credential)) { ModalManager.fullscreen.showCustomModal { close -> MigrateFromDeviceView(close) } } }
      )
      SettingsActionItem(
        icon = painterResource(MR.images.ic_help),
        text = "Help & Support",
        click = { uriHandler.openUriCatching("https://arpokrat.com") }
      )
    }

    SectionDividerSpaced()

    Spacer(Modifier.height(10.dp))

    Column(
      modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      val isDarkLogo = !MaterialTheme.colors.isLight
      Image(
        painter = painterResource(if (isDarkLogo) MR.images.logo_light else MR.images.logo),
        contentDescription = "Arpokrat Logo",
        modifier = Modifier.width(350.dp),
        contentScale = ContentScale.Fit,
        alpha = 1.0f
      )
      Spacer(Modifier.height(2.dp))
      AppVersionText()
    }
    SectionBottomSpacer()
  }
}

@Composable
fun ServerStatusItem(
  text: String,
  onClick: () -> Unit
) {
  SectionItemView(onClick) {
    val infiniteTransition = rememberInfiniteTransition()
    val alpha by infiniteTransition.animateFloat(
      initialValue = 0.2f,
      targetValue = 1f,
      animationSpec = infiniteRepeatable(
        animation = tween(1500, easing = LinearEasing),
        repeatMode = RepeatMode.Reverse
      )
    )

    Box(
      modifier = Modifier.size(24.dp),
      contentAlignment = Alignment.Center
    ) {
      Box(
        modifier = Modifier
          .size(12.dp)
          .alpha(alpha)
          .background(SimplexGreenPulse, CircleShape)
      )
      Box(
        modifier = Modifier
          .size(24.dp)
          .alpha(alpha * 0.3f)
          .background(SimplexGreenPulse, CircleShape)
      )
    }

    TextIconSpaced(false)

    Text(
      text = text,
      color = MaterialTheme.colors.onBackground,
      modifier = Modifier.weight(1f)
    )
  }
}

fun getInitials(name: String): String {
  val trimmed = name.trim()
  if (trimmed.isEmpty()) return "?"
  val parts = trimmed.split(" ").filter { it.isNotEmpty() }
  return if (parts.size == 1) {
    parts[0].take(2).uppercase()
  } else {
    (parts[0].take(1) + parts[1].take(1)).uppercase()
  }
}

@Composable
fun SettingsLayout(
  stopped: Boolean,
  encrypted: Boolean,
  passphraseSaved: Boolean,
  notificationsMode: State<NotificationsMode>,
  userDisplayName: String?,
  setPerformLA: (Boolean) -> Unit,
  showModal: (@Composable (ChatModel) -> Unit) -> (() -> Unit),
  showSettingsModal: (@Composable (ChatModel) -> Unit) -> (() -> Unit),
  showSettingsModalWithSearch: (@Composable (ChatModel, MutableState<String>) -> Unit) -> Unit,
  showCustomModal: (@Composable ModalData.(ChatModel, () -> Unit) -> Unit) -> (() -> Unit),
  showVersion: () -> Unit,
  withAuth: (title: String, desc: String, block: () -> Unit) -> Unit,
) {}

@Composable
expect fun SettingsSectionApp(
  showSettingsModal: (@Composable (ChatModel) -> Unit) -> (() -> Unit),
  showVersion: () -> Unit,
  withAuth: (title: String, desc: String, block: () -> Unit) -> Unit
)

@Composable fun DatabaseItem(encrypted: Boolean, saved: Boolean, openDatabaseView: () -> Unit, stopped: Boolean) {
  SectionItemView(openDatabaseView) {
    Row(
      Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
        Icon(
          painterResource(MR.images.ic_database),
          contentDescription = stringResource(MR.strings.database_passphrase_and_export),
          tint = if (encrypted && (appPlatform.isAndroid || !saved)) MaterialTheme.colors.secondary else MaterialTheme.colors.error,
        )
        TextIconSpaced(false)
        Text(stringResource(MR.strings.database_passphrase_and_export))
      }
      if (stopped) {
        Icon(
          painterResource(MR.images.ic_report_filled),
          contentDescription = stringResource(MR.strings.chat_is_stopped),
          tint = MaterialTheme.colors.error,
          modifier = Modifier.padding(end = 6.dp)
        )
      }
    }
  }
}

@Composable fun ChatPreferencesItem(showCustomModal: ((@Composable ModalData.(ChatModel, () -> Unit) -> Unit) -> (() -> Unit)), stopped: Boolean) {
  SettingsActionItem(
    painterResource(MR.images.ic_toggle_on),
    stringResource(MR.strings.chat_preferences),
    click = if (stopped) null else ({
      showCustomModal { m, close ->
        PreferencesView(m, m.currentUser.value ?: return@showCustomModal, close)
      }()
    }),
    disabled = stopped
  )
}

@Composable
fun ChatLockItem(
  showSettingsModal: (@Composable (ChatModel) -> Unit) -> (() -> Unit),
  setPerformLA: (Boolean) -> Unit
) {
  val performLA = remember { appPrefs.performLA.state }
  val currentLAMode = remember { ChatModel.controller.appPrefs.laMode }
  SettingsActionItemWithContent(
    click = showSettingsModal { SimplexLockView(ChatModel, currentLAMode, setPerformLA) },
    icon = if (performLA.value) painterResource(MR.images.ic_lock_filled) else painterResource(MR.images.ic_lock),
    text = stringResource(MR.strings.chat_lock),
    iconColor = if (performLA.value) SimplexGreen else MaterialTheme.colors.secondary
  ) {
    Text(if (performLA.value) remember { currentLAMode.state }.value.text else generalGetString(MR.strings.la_mode_off), color = MaterialTheme.colors.secondary)
  }
}

@Composable fun ContributeItem(uriHandler: UriHandler) {
  SectionItemView({ uriHandler.openUriCatching("https://github.com/simplex-chat/simplex-chat#contribute") }) {
    Icon(painterResource(MR.images.ic_keyboard), "GitHub", tint = MaterialTheme.colors.secondary)
    TextIconSpaced()
    Text(generalGetString(MR.strings.contribute), color = MaterialTheme.colors.primary)
  }
}

@Composable fun RateAppItem(uriHandler: UriHandler) {
  SectionItemView({
    runCatching { uriHandler.openUriCatching("market://details?id=com.arpokrat.app") }
      .onFailure { uriHandler.openUriCatching("https://play.google.com/store/apps/details?id=com.arpokrat.app") }
  }) {
    Icon(painterResource(MR.images.ic_star), "Google Play", tint = MaterialTheme.colors.secondary)
    TextIconSpaced()
    Text(generalGetString(MR.strings.rate_the_app), color = MaterialTheme.colors.primary)
  }
}

@Composable fun StarOnGithubItem(uriHandler: UriHandler) {
  SectionItemView({ uriHandler.openUriCatching("https://github.com/simplex-chat/simplex-chat") }) {
    Icon(painter = painterResource(MR.images.ic_github), contentDescription = "GitHub", tint = MaterialTheme.colors.secondary)
    TextIconSpaced()
    Text(generalGetString(MR.strings.star_on_github), color = MaterialTheme.colors.primary)
  }
}

@Composable fun ChatConsoleItem(showTerminal: () -> Unit) {
  SectionItemView(showTerminal) {
    Icon(painter = painterResource(MR.images.ic_outline_terminal), contentDescription = stringResource(MR.strings.chat_console), tint = MaterialTheme.colors.secondary)
    TextIconSpaced()
    Text(stringResource(MR.strings.chat_console))
  }
}

@Composable fun TerminalAlwaysVisibleItem(pref: SharedPreference<Boolean>, onChange: (Boolean) -> Unit) {
  SettingsActionItemWithContent(painterResource(MR.images.ic_engineering), stringResource(MR.strings.terminal_always_visible)) {
    DefaultSwitch(checked = remember { pref.state }.value, onCheckedChange = onChange)
  }
}

@Composable fun InstallTerminalAppItem(uriHandler: UriHandler) {
  SectionItemView({ uriHandler.openUriCatching("https://github.com/simplex-chat/simplex-chat") }) {
    Icon(painter = painterResource(MR.images.ic_github), contentDescription = "GitHub", tint = MaterialTheme.colors.secondary)
    TextIconSpaced()
    Text(generalGetString(MR.strings.install_simplex_chat_for_terminal), color = MaterialTheme.colors.primary)
  }
}

@Composable fun ResetHintsItem(unchangedHints: MutableState<Boolean>) {
  SectionItemView({ resetHintPreferences(); unchangedHints.value = true }, disabled = unchangedHints.value) {
    Icon(painter = painterResource(MR.images.ic_lightbulb), contentDescription = "Lightbulb", tint = MaterialTheme.colors.secondary)
    TextIconSpaced()
    Text(generalGetString(MR.strings.reset_all_hints), color = if (unchangedHints.value) MaterialTheme.colors.secondary else MaterialTheme.colors.primary)
  }
}

fun resetHintPreferences() { for ((pref, def) in appPreferences.hintPreferences) { pref.set(def) } }

fun unchangedHintPreferences(): Boolean = appPreferences.hintPreferences.all { (pref, def) -> pref.state.value == def }

@Composable fun AppVersionItem(showVersion: () -> Unit) { SectionItemView(showVersion) { AppVersionText() } }

@Composable fun AppVersionText() {
  Text(appVersionInfo.first + (if (appVersionInfo.second != null) " (" + appVersionInfo.second + ")" else ""), style = MaterialTheme.typography.caption, color = MaterialTheme.colors.secondary)
}

@Composable fun ProfilePreview(profileOf: NamedChat, size: Dp = 60.dp, iconColor: Color = MaterialTheme.colors.secondaryVariant, textColor: Color = MaterialTheme.colors.onBackground, stopped: Boolean = false) {
  ProfileImage(size = size, image = profileOf.image, color = iconColor)
  Spacer(Modifier.padding(horizontal = 8.dp))
  Column(Modifier.height(size), verticalArrangement = Arrangement.Center) {
    Text(profileOf.displayName, style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold, color = if (stopped) MaterialTheme.colors.secondary else textColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
    if (profileOf.fullName.isNotEmpty() && profileOf.fullName != profileOf.displayName) {
      Text(profileOf.fullName, Modifier.padding(vertical = 5.dp), color = if (stopped) MaterialTheme.colors.secondary else textColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
  }
}

@Composable
fun SettingsActionItem(icon: Painter, text: String, click: (() -> Unit)? = null, textColor: Color = Color.Unspecified, iconColor: Color = MaterialTheme.colors.secondary, disabled: Boolean = false, extraPadding: Boolean = false) {
  SectionItemView(click, disabled = disabled, extraPadding = extraPadding) {
    Icon(icon, text, tint = if (disabled) MaterialTheme.colors.secondary else iconColor)
    TextIconSpaced(extraPadding)
    Text(text, color = if (disabled) MaterialTheme.colors.secondary else textColor)
  }
}

@Composable
fun SettingsActionItemWithContent(icon: Painter?, text: String? = null, click: (() -> Unit)? = null, iconColor: Color = MaterialTheme.colors.secondary, textColor: Color = MaterialTheme.colors.onBackground, disabled: Boolean = false, extraPadding: Boolean = false, content: @Composable RowScope.() -> Unit) {
  SectionItemView(click, extraPadding = extraPadding, padding = if (extraPadding && icon != null) PaddingValues(start = DEFAULT_PADDING * 1.7f, end = DEFAULT_PADDING) else PaddingValues(horizontal = DEFAULT_PADDING), disabled = disabled) {
    if (icon != null) {
      Icon(icon, text, Modifier, tint = if (disabled) MaterialTheme.colors.secondary else iconColor)
      TextIconSpaced(extraPadding)
    }
    if (text != null) {
      val padding = with(LocalDensity.current) { 6.sp.toDp() }
      Text(text, Modifier.weight(1f).padding(vertical = padding), color = if (disabled) MaterialTheme.colors.secondary else textColor)
      Spacer(Modifier.width(DEFAULT_PADDING))
      Row(Modifier.widthIn(max = (windowWidth() - DEFAULT_PADDING * 2) / 2)) { content() }
    } else {
      Row { content() }
    }
  }
}

@Composable
fun SettingsPreferenceItem(icon: Painter?, text: String, pref: SharedPreference<Boolean>, iconColor: Color = MaterialTheme.colors.secondary, enabled: Boolean = true, onChange: ((Boolean) -> Unit)? = null) {
  SettingsActionItemWithContent(icon, text, iconColor = iconColor) {
    SharedPreferenceToggle(pref, enabled, onChange)
  }
}

@Composable
fun PreferenceToggle(text: String, disabled: Boolean = false, checked: Boolean, onChange: (Boolean) -> Unit = {}) {
  SettingsActionItemWithContent(null, text, disabled = disabled) {
    DefaultSwitch(checked = checked, onCheckedChange = onChange, enabled = !disabled)
  }
}

@Composable
fun PreferenceToggleWithIcon(text: String, icon: Painter? = null, iconColor: Color? = MaterialTheme.colors.secondary, disabled: Boolean = false, checked: Boolean, extraPadding: Boolean = false, onChange: (Boolean) -> Unit = {}) {
  SettingsActionItemWithContent(icon, text, iconColor = iconColor ?: MaterialTheme.colors.secondary, extraPadding = extraPadding) {
    DefaultSwitch(checked = checked, onCheckedChange = { onChange(it) }, enabled = !disabled)
  }
}

fun doWithAuth(title: String, desc: String, block: () -> Unit) {
  val requireAuth = chatModel.controller.appPrefs.performLA.get()
  if (!requireAuth) { block() } else {
    var autoShow = true
    ModalManager.fullscreen.showModalCloseable { close ->
      val onFinishAuth = { success: Boolean -> if (success) { close(); block() } }
      LaunchedEffect(Unit) { if (autoShow) { autoShow = false; runAuth(title, desc, onFinishAuth) } }
      Surface(color = MaterialTheme.colors.background.copy(1f), contentColor = LocalContentColor.current) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          SimpleButton(stringResource(MR.strings.auth_unlock), icon = painterResource(MR.images.ic_lock), click = { runAuth(title, desc, onFinishAuth) })
        }
      }
    }
  }
}

private fun runAuth(title: String, desc: String, onFinish: (success: Boolean) -> Unit) {
  authenticate(title, desc, oneTime = true, completed = { laResult -> onFinish(laResult == LAResult.Success || laResult is LAResult.Unavailable) })
}