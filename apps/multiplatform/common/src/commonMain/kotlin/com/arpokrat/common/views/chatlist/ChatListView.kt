package com.arpokrat.common.views.chatlist

import SectionItemView
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.*
import dev.icerock.moko.resources.compose.painterResource
import dev.icerock.moko.resources.compose.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.arpokrat.common.AppLock
import com.arpokrat.common.model.*
import com.arpokrat.common.model.ChatController.appPrefs
import com.arpokrat.common.model.ChatController.stopRemoteHostAndReloadHosts
import com.arpokrat.common.views.helpers.*
import com.arpokrat.common.platform.*
import com.arpokrat.common.views.call.Call
import com.arpokrat.common.views.newchat.*
import com.arpokrat.common.views.onboarding.*
import com.arpokrat.common.views.usersettings.*
import com.arpokrat.res.MR
import dev.icerock.moko.resources.ImageResource
import dev.icerock.moko.resources.StringResource
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import androidx.compose.foundation.Image
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.layout.ContentScale
import com.arpokrat.common.wallet.ui.dashboard.WalletMainView

enum class PresetTagKind { GROUP_REPORTS, FAVORITES, CONTACTS, GROUPS, BUSINESS, NOTES }

enum class ScrollDirection { Up, Down, Idle }

sealed class ActiveFilter {
  data class PresetTag(val tag: PresetTagKind) : ActiveFilter()
  data class UserTag(val tag: ChatTag) : ActiveFilter()
  data object Unread: ActiveFilter()
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
fun ChatListView(chatModel: ChatModel, userPickerState: MutableStateFlow<AnimatedViewState>, setPerformLA: (Boolean) -> Unit, stopped: Boolean) {
  val oneHandUI = remember { mutableStateOf(false) }

  LaunchedEffect(Unit) {
    val showWhatsNew = shouldShowWhatsNew(chatModel)
    val showUpdatedConditions = chatModel.conditions.value.conditionsAction?.shouldShowNotice ?: false
    if (showWhatsNew || showUpdatedConditions) {
      delay(1000L)
      ModalManager.center.showCustomModal { close -> WhatsNewView(close = close, updatedConditions = showUpdatedConditions) }
    }
  }

  if (appPlatform.isDesktop) {
    KeyChangeEffect(chatModel.chatId.value) {
      if (chatModel.chatId.value != null && !ModalManager.end.isLastModalOpen(ModalViewId.SECONDARY_CHAT)) {
        ModalManager.end.closeModalsExceptFirst()
      }
      AudioPlayer.stop()
      VideoPlayerHolder.stopAll()
    }
  }

  val searchText = rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }
  val listState = rememberLazyListState()

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colors.background)
  ) {
    Column(modifier = Modifier.fillMaxSize()) {
      ArpokratTopBar(chatModel = chatModel, setPerformLA = setPerformLA)

      Box(Modifier.weight(1f)) {
        ChatListContent(
          searchText = searchText,
          listState = listState,
          chatModel = chatModel
        )
      }
    }

    if (!stopped && !chatModel.desktopNoUserNoRemote && chatModel.chatRunning.value == true) {
      NewChatFAB(
        onClick = { showNewChatSheet(oneHandUI) },
        modifier = Modifier
          .align(Alignment.BottomEnd)
          .padding(16.dp)
          .navigationBarsPadding()
      )
    }
  }

  if (appPlatform.isAndroid) {
    val wasAllowedToSetupNotifications = rememberSaveable { mutableStateOf(false) }
    val canEnableNotifications = remember { derivedStateOf { chatModel.chatRunning.value == true } }
    if (wasAllowedToSetupNotifications.value || canEnableNotifications.value) {
      SetNotificationsModeAdditions()
      LaunchedEffect(Unit) { wasAllowedToSetupNotifications.value = true }
    }
    tryOrShowError("UserPicker", error = {}) {
      UserPicker(
        chatModel = chatModel,
        userPickerState = userPickerState,
        setPerformLA = AppLock::setPerformLA
      )
    }
  }
}

@Composable
fun ArpokratTopBar(chatModel: ChatModel, setPerformLA: (Boolean) -> Unit) {
  val currentUser = chatModel.currentUser.value

  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier
      .fillMaxWidth()
      .statusBarsPadding()
      .height(64.dp)
      .padding(horizontal = 16.dp)
  ) {
    Box(
      modifier = Modifier
        .size(42.dp)
        .clip(CircleShape)
        .clickable {
          ModalManager.fullscreen.showModalCloseable { close ->
            SettingsView(chatModel, setPerformLA, close)
          }
        },
      contentAlignment = Alignment.Center
    ) {
      if (currentUser?.image != null) {
        ProfileImage(
          image = currentUser.image,
          size = 42.dp,
          color = MaterialTheme.colors.secondary
        )
      } else {
        Box(
          modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colors.primary),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = getInitials(currentUser?.displayName ?: "?"),
            color = Color.White,
            style = MaterialTheme.typography.body1,
            fontWeight = FontWeight.Bold
          )
        }
      }
    }

    Spacer(Modifier.weight(1f))

    val isDark = !MaterialTheme.colors.isLight
    Image(
      painter = painterResource(if (isDark) MR.images.logo_light else MR.images.logo),
      contentDescription = stringResource(MR.strings.app_logo_descr),
      modifier = Modifier
        .height(36.dp)
        .widthIn(max = 160.dp),
      contentScale = ContentScale.Fit
    )

    Spacer(Modifier.weight(1f))

    IconButton(
      onClick = {
        ModalManager.fullscreen.showCustomModal { close ->
          WalletMainView(closeWallet = close)
        }
      },
      modifier = Modifier.size(48.dp)
    ) {
      Icon(
        painter = painterResource(MR.images.ic_wallet_filled),
        contentDescription = stringResource(MR.strings.wallet_descr),
        modifier = Modifier.size(28.dp),
        tint = MaterialTheme.colors.onBackground
      )
    }
  }
}

@Composable
fun NewChatFAB(onClick: () -> Unit, modifier: Modifier = Modifier) {
  FloatingActionButton(
    onClick = onClick,
    modifier = modifier,
    backgroundColor = MaterialTheme.colors.primary,
    contentColor = MaterialTheme.colors.onBackground,
    elevation = FloatingActionButtonDefaults.elevation(6.dp)
  ) {
    Icon(
      imageVector = Icons.Default.Add,
      contentDescription = stringResource(MR.strings.add_contact_or_create_group),
      modifier = Modifier.size(28.dp)
    )
  }
}

@Composable
private fun BoxScope.ChatListContent(
  searchText: MutableState<TextFieldValue>,
  listState: LazyListState,
  chatModel: ChatModel
) {
  val allChats = remember { chatModel.chats }
  val activeFilter = remember { chatModel.activeChatTagFilter }

  val searchShowingSimplexLink = remember { mutableStateOf(false) }
  val searchChatFilteredBySimplexLink = remember { mutableStateOf<String?>(null) }

  val chats = filteredChats(searchShowingSimplexLink, searchChatFilteredBySimplexLink, searchText.value.text, allChats.value.toList(), activeFilter.value)
  val isSearching = searchText.value.text.isNotEmpty() || activeFilter.value != null

  LazyColumnWithScrollBar(
    Modifier.imePadding(),
    listState
  ) {
    item {
      Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        ChatListSearchBar(listState, searchText, searchShowingSimplexLink, searchChatFilteredBySimplexLink)
      }
    }

    item {
      TagsView(searchText)
      Spacer(Modifier.height(8.dp))
    }

    if (chats.isEmpty()) {
      item {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp, start = 20.dp, end = 20.dp),
          contentAlignment = Alignment.Center
        ) {
          if (isSearching) {
            Text(generalGetString(MR.strings.no_chats_found), color = MaterialTheme.colors.secondary)
          } else if (!chatModel.desktopNoUserNoRemote) {
            Text(generalGetString(MR.strings.no_chats), color = MaterialTheme.colors.secondary)
          }
        }
      }
    } else {
      itemsIndexed(chats, key = { _, chat -> chat.remoteHostId to chat.id }) { index, chat ->
        val nextChatSelected = remember(chat.id, chats) { derivedStateOf {
          chatModel.chatId.value != null && chats.getOrNull(index + 1)?.id == chatModel.chatId.value
        } }

        ChatListNavLinkView(chat, nextChatSelected)

        Divider(
          color = MaterialTheme.colors.onBackground.copy(alpha = 0.04f),
          thickness = 1.dp,
          modifier = Modifier.padding(start = 82.dp)
        )
      }
    }

    item { Spacer(Modifier.height(88.dp)) }
  }
}

@Composable
private fun ChatListSearchBar(listState: LazyListState, searchText: MutableState<TextFieldValue>, searchShowingSimplexLink: MutableState<Boolean>, searchChatFilteredBySimplexLink: MutableState<String?>) {
  val focusRequester = remember { FocusRequester() }
  var isFocused by remember { mutableStateOf(false) }
  val activeFilter = remember { chatModel.activeChatTagFilter }
  val isUnreadFilterActive = activeFilter.value == ActiveFilter.Unread

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .height(44.dp)
      .background(MaterialTheme.colors.onBackground.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
      .clip(RoundedCornerShape(12.dp)),
    contentAlignment = Alignment.CenterStart
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(horizontal = 12.dp)
    ) {
      Icon(
        imageVector = Icons.Default.Search,
        contentDescription = null,
        tint = MaterialTheme.colors.secondary.copy(alpha = 0.7f),
        modifier = Modifier.size(20.dp)
      )
      Spacer(Modifier.width(10.dp))

      BasicTextField(
        value = searchText.value,
        onValueChange = { searchText.value = it },
        modifier = Modifier
          .weight(1f)
          .focusRequester(focusRequester)
          .onFocusChanged { isFocused = it.isFocused },
        singleLine = true,
        textStyle = MaterialTheme.typography.body1.copy(color = MaterialTheme.colors.onBackground),
        cursorBrush = SolidColor(MaterialTheme.colors.primary),
        decorationBox = { innerTextField ->
          if (searchText.value.text.isEmpty()) {
            Text(
              stringResource(MR.strings.search_or_paste_simplex_link),
              style = MaterialTheme.typography.body1,
              color = MaterialTheme.colors.secondary.copy(alpha = 0.5f)
            )
          }
          innerTextField()
        }
      )

      if (searchText.value.text.isNotEmpty()) {
        Icon(
          painter = painterResource(MR.images.ic_close),
          contentDescription = stringResource(MR.strings.clear_search_descr),
          tint = MaterialTheme.colors.secondary,
          modifier = Modifier
            .size(18.dp)
            .clickable { searchText.value = TextFieldValue("") }
        )
      } else {
        Icon(
          imageVector = Icons.Default.FilterList,
          contentDescription = stringResource(MR.strings.filter_unread_descr),
          tint = if (isUnreadFilterActive) MaterialTheme.colors.primary else MaterialTheme.colors.secondary.copy(alpha = 0.5f),
          modifier = Modifier
            .size(20.dp)
            .clickable {
              if (isUnreadFilterActive) {
                chatModel.activeChatTagFilter.value = null
              } else {
                chatModel.activeChatTagFilter.value = ActiveFilter.Unread
              }
            }
        )
      }
    }
  }

  val view = LocalMultiplatformView()
  LaunchedEffect(Unit) {
    snapshotFlow { searchText.value.text }
      .distinctUntilChanged()
      .collect {
        val link = strHasSingleSimplexLink(it.trim())
        if (link != null) {
          hideKeyboard(view)
          if (link.format is Format.SimplexLink) {
            val linkText = link.format.simplexLinkText
            searchText.value = searchText.value.copy(linkText, selection = TextRange.Zero)
          }
          searchShowingSimplexLink.value = true
          searchChatFilteredBySimplexLink.value = null
          connect(link.text, searchChatFilteredBySimplexLink) { searchText.value = TextFieldValue() }
        } else if (!searchShowingSimplexLink.value || it.isEmpty()) {
          searchShowingSimplexLink.value = false
          searchChatFilteredBySimplexLink.value = null
        }
      }
  }
}

@Composable
fun BoxScope.StatusBarBackground() {
  if (appPlatform.isAndroid) {
    val finalColor = MaterialTheme.colors.background.copy(0.88f)
    Box(Modifier.fillMaxWidth().windowInsetsTopHeight(WindowInsets.statusBars).background(finalColor))
  }
}

@Composable
fun BoxScope.NavigationBarBackground(appBarOnBottom: Boolean = false, mixedColor: Boolean, noAlpha: Boolean = false) {
  if (appPlatform.isAndroid) {
    val barPadding = WindowInsets.navigationBars.asPaddingValues()
    val paddingBottom = barPadding.calculateBottomPadding()
    val color = if (mixedColor) MaterialTheme.colors.background.mixWith(MaterialTheme.colors.onBackground, 0.97f) else MaterialTheme.colors.background
    val finalColor = color.copy(if (noAlpha) 1f else if (appBarOnBottom) remember { appPrefs.inAppBarsAlpha.state }.value else 0.6f)
    Box(Modifier.align(Alignment.BottomStart).height(paddingBottom).fillMaxWidth().background(finalColor))
  }
}

@Composable
fun BoxScope.NavigationBarBackground(modifier: Modifier, color: Color = MaterialTheme.colors.background) {
  val keyboardState = getKeyboardState()
  if (appPlatform.isAndroid && keyboardState.value == KeyboardState.Closed) {
    val barPadding = WindowInsets.navigationBars.asPaddingValues()
    val paddingBottom = barPadding.calculateBottomPadding()
    val finalColor = color.copy(0.6f)
    Box(modifier.align(Alignment.BottomStart).height(paddingBottom).fillMaxWidth().background(finalColor))
  }
}

fun connectIfOpenedViaUri(rhId: Long?, uri: String, chatModel: ChatModel) {
  if (chatModel.currentUser.value == null) {
    chatModel.appOpenUrl.value = rhId to uri
  } else {
    withBGApi {
      chatModel.appOpenUrlConnecting.value = true
      planAndConnect(rhId, uri, close = null, cleanup = { chatModel.appOpenUrlConnecting.value = false })
    }
  }
}

@Composable
fun UserProfileButton(image: String?, allRead: Boolean, onButtonClicked: () -> Unit) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    IconButton(onClick = onButtonClicked) {
      Box {
        ProfileImage(
          image = image,
          size = 37.dp * fontSizeSqrtMultiplier,
          color = MaterialTheme.colors.secondaryVariant.mixWith(MaterialTheme.colors.onBackground, 0.97f)
        )
        if (!allRead) {
          unreadBadge()
        }
      }
    }
    if (appPlatform.isDesktop) {
      val h by remember { chatModel.currentRemoteHost }
      if (h != null) {
        Spacer(Modifier.width(12.dp))
        HostDisconnectButton {
          stopRemoteHostAndReloadHosts(h!!, true)
        }
      }
    }
  }
}

@Composable
private fun BoxScope.unreadBadge(text: String? = "") {
  Text(
    text ?: "",
    color = MaterialTheme.colors.onPrimary,
    fontSize = 6.sp,
    modifier = Modifier
      .background(MaterialTheme.colors.primary, shape = CircleShape)
      .badgeLayout()
      .padding(horizontal = 3.dp)
      .padding(vertical = 1.dp)
      .align(Alignment.TopEnd)
  )
}

private fun showNewChatSheet(oneHandUI: State<Boolean>) {
  connectProgressManager.cancelConnectProgress()
  ModalManager.start.closeModals()
  ModalManager.end.closeModals()
  chatModel.newChatSheetVisible.value = true
  ModalManager.start.showCustomModal { close ->
    val safeClose = {
      chatModel.newChatSheetVisible.value = false
      close()
    }
    ModalView(safeClose, showAppBar = !oneHandUI.value) {
      if (appPlatform.isAndroid) {
        BackHandler { safeClose() }
      }
      NewChatSheet(rh = chatModel.currentRemoteHost.value, safeClose)
      DisposableEffect(Unit) {
        onDispose { chatModel.newChatSheetVisible.value = false }
      }
    }
  }
}

@Composable
expect fun ActiveCallInteractiveArea(call: Call)

fun shouldShowWhatsNew(chatModel: ChatModel): Boolean { return false }

@Composable
fun ToggleChatListCard() {}

@Composable
fun AddressCreationCard() {}

@Composable
fun SubscriptionStatusIndicator(click: (() -> Unit)) {}

@Composable
private fun NoChatsView(searchText: MutableState<TextFieldValue>) {
  val activeFilter = remember { chatModel.activeChatTagFilter }.value
  if (searchText.value.text.isBlank()) {
    Text(generalGetString(MR.strings.no_chats), color = MaterialTheme.colors.secondary, textAlign = TextAlign.Center)
  } else {
    Text(generalGetString(MR.strings.no_chats_found), color = MaterialTheme.colors.secondary, textAlign = TextAlign.Center)
  }
}

private val TAG_MIN_HEIGHT = 35.dp

@Composable
private fun TagsView(searchText: MutableState<TextFieldValue>) {
  val userTags = remember { chatModel.userTags }
  val presetTags = remember { chatModel.presetTags }

  Row(
    modifier = Modifier
      .horizontalScroll(rememberScrollState())
      .padding(horizontal = 16.dp, vertical = 4.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    val rhId = chatModel.remoteHostId()
    Box(
      modifier = Modifier
        .size(32.dp)
        .clip(CircleShape)
        .background(MaterialTheme.colors.onBackground.copy(alpha=0.05f))
        .clickable {
          ModalManager.start.showModalCloseable { close ->
            TagListEditor(rhId = rhId, close = close)
          }
        },
      contentAlignment = Alignment.Center
    ) {
      Icon(
        painter = painterResource(MR.images.ic_add),
        contentDescription = stringResource(MR.strings.add_list_descr),
        modifier = Modifier.size(18.dp),
        tint = MaterialTheme.colors.secondary
      )
    }

    PresetTagKind.entries.filter { t -> (presetTags[t] ?: 0) > 0 }.forEach { tag ->
      ExpandedTagFilterView(tag)
    }

    userTags.value.forEach { tag ->
      val activeFilter = remember { chatModel.activeChatTagFilter }
      val current = when (val af = activeFilter.value) {
        is ActiveFilter.UserTag -> af.tag == tag
        else -> false
      }

      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(8.dp))
          .clickable {
            if (current) chatModel.activeChatTagFilter.value = null
            else chatModel.activeChatTagFilter.value = ActiveFilter.UserTag(tag)
          }
          .background(
            if (current) MaterialTheme.colors.primary.copy(alpha = 0.15f)
            else MaterialTheme.colors.onBackground.copy(alpha = 0.05f)
          )
          .padding(horizontal = 12.dp, vertical = 6.dp)
      ) {
        Text(
          text = tag.chatTagText,
          color = if (current) MaterialTheme.colors.primary else MaterialTheme.colors.onBackground.copy(alpha=0.8f),
          style = MaterialTheme.typography.body2,
          fontWeight = if (current) FontWeight.Bold else FontWeight.Normal
        )
      }
    }
  }
}

@Composable
expect fun TagsRow(content: @Composable() (() -> Unit))

@Composable
private fun ExpandedTagFilterView(tag: PresetTagKind) {
  val activeFilter = remember { chatModel.activeChatTagFilter }
  val active = when (val af = activeFilter.value) {
    is ActiveFilter.PresetTag -> af.tag == tag
    else -> false
  }
  val (icon, text) = presetTagLabel(tag, active)

  Row(
    modifier = Modifier
      .clip(RoundedCornerShape(8.dp))
      .clickable {
        if (activeFilter.value == ActiveFilter.PresetTag(tag)) {
          chatModel.activeChatTagFilter.value = null
        } else {
          chatModel.activeChatTagFilter.value = ActiveFilter.PresetTag(tag)
        }
      }
      .background(
        if (active) MaterialTheme.colors.primary.copy(alpha = 0.15f)
        else MaterialTheme.colors.onBackground.copy(alpha = 0.05f)
      )
      .padding(horizontal = 10.dp, vertical = 6.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(
      painterResource(icon),
      stringResource(text),
      Modifier.size(16.dp),
      tint = if(active) MaterialTheme.colors.primary else MaterialTheme.colors.secondary
    )
    Spacer(Modifier.width(6.dp))
    Text(
      stringResource(text),
      color = if(active) MaterialTheme.colors.primary else MaterialTheme.colors.onBackground.copy(alpha=0.8f),
      style = MaterialTheme.typography.body2,
      fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
    )
  }
}

fun filteredChats(
  searchShowingSimplexLink: State<Boolean>,
  searchChatFilteredBySimplexLink: State<String?>,
  searchText: String,
  chats: List<Chat>,
  activeFilter: ActiveFilter? = null,
): List<Chat> {
  val linkChatId = searchChatFilteredBySimplexLink.value
  return if (linkChatId != null) {
    chats.filter { it.id == linkChatId }
  } else {
    val s = if (searchShowingSimplexLink.value) "" else searchText.trim().lowercase()
    if (s.isEmpty())
      chats.filter { chat -> chat.id == chatModel.chatId.value || (!chat.chatInfo.chatDeleted && !chat.chatInfo.contactCard && filtered(chat, activeFilter)) }
    else {
      chats.filter { chat ->
        chat.id == chatModel.chatId.value ||
            when (val cInfo = chat.chatInfo) {
              is ChatInfo.Direct -> !cInfo.contact.chatDeleted && !chat.chatInfo.contactCard && cInfo.anyNameContains(s)
              is ChatInfo.Group -> cInfo.anyNameContains(s)
              is ChatInfo.Local -> cInfo.anyNameContains(s)
              is ChatInfo.ContactRequest -> cInfo.anyNameContains(s)
              is ChatInfo.ContactConnection -> cInfo.contactConnection.localAlias.lowercase().contains(s)
              is ChatInfo.InvalidJSON -> false
            }
      }
    }
  }
}

private fun filtered(chat: Chat, activeFilter: ActiveFilter?): Boolean =
  when (activeFilter) {
    is ActiveFilter.PresetTag -> presetTagMatchesChat(activeFilter.tag, chat.chatInfo, chat.chatStats)
    is ActiveFilter.UserTag -> chat.chatInfo.chatTags?.contains(activeFilter.tag.chatTagId) ?: false
    is ActiveFilter.Unread -> chat.unreadTag
    else -> true
  }

fun presetTagMatchesChat(tag: PresetTagKind, chatInfo: ChatInfo, chatStats: Chat.ChatStats): Boolean =
  when (tag) {
    PresetTagKind.GROUP_REPORTS -> chatStats.reportsCount > 0
    PresetTagKind.FAVORITES -> chatInfo.chatSettings?.favorite == true
    PresetTagKind.CONTACTS -> when (chatInfo) {
      is ChatInfo.Direct -> !chatInfo.contact.isContactCard && !chatInfo.contact.chatDeleted
      is ChatInfo.ContactRequest -> true
      is ChatInfo.ContactConnection -> true
      is ChatInfo.Group -> chatInfo.groupInfo.businessChat?.chatType == BusinessChatType.Customer
      else -> false
    }
    PresetTagKind.GROUPS -> when (chatInfo) {
      is ChatInfo.Group -> chatInfo.groupInfo.businessChat == null
      else -> false
    }
    PresetTagKind.BUSINESS -> when (chatInfo) {
      is ChatInfo.Group -> chatInfo.groupInfo.businessChat?.chatType == BusinessChatType.Business
      else -> false
    }
    PresetTagKind.NOTES -> when (chatInfo) {
      is ChatInfo.Local -> !chatInfo.noteFolder.chatDeleted
      else -> false
    }
  }

private fun presetTagLabel(tag: PresetTagKind, active: Boolean): Pair<ImageResource, StringResource> =
  when (tag) {
    PresetTagKind.GROUP_REPORTS -> (if (active) MR.images.ic_flag_filled else MR.images.ic_flag) to MR.strings.chat_list_group_reports
    PresetTagKind.FAVORITES -> (if (active) MR.images.ic_star_filled else MR.images.ic_star) to MR.strings.chat_list_favorites
    PresetTagKind.CONTACTS -> (if (active) MR.images.ic_person_filled else MR.images.ic_person) to MR.strings.chat_list_contacts
    PresetTagKind.GROUPS -> (if (active) MR.images.ic_group_filled else MR.images.ic_group) to MR.strings.chat_list_groups
    PresetTagKind.BUSINESS -> (if (active) MR.images.ic_work_filled else MR.images.ic_work) to MR.strings.chat_list_businesses
    PresetTagKind.NOTES -> (if (active) MR.images.ic_folder_closed_filled else MR.images.ic_folder_closed) to MR.strings.chat_list_notes
  }

private fun presetCanBeCollapsed(tag: PresetTagKind): Boolean = when (tag) {
  PresetTagKind.GROUP_REPORTS -> false
  else -> true
}

fun scrollToBottom(scope: CoroutineScope, listState: LazyListState) {
  scope.launch { try { listState.animateScrollToItem(0) } catch (e: Exception) {} }
}
private fun connect(link: String, searchChatFilteredBySimplexLink: MutableState<String?>, cleanup: (() -> Unit)?) {
  withBGApi {
    planAndConnect(
      chatModel.remoteHostId(),
      link,
      filterKnownContact = { searchChatFilteredBySimplexLink.value = it.id },
      filterKnownGroup = { searchChatFilteredBySimplexLink.value = it.id },
      close = null,
      cleanup = cleanup,
    )
  }
}

@Composable
private fun ChatListCard(
  close: () -> Unit,
  onCardClick: (() -> Unit)? = null,
  content: @Composable BoxScope.() -> Unit
) {}

@Composable
private fun CollapsedTagsFilterView(searchText: MutableState<TextFieldValue>) {}

@Composable
fun ItemPresetFilterAction(presetTag: PresetTagKind, active: Boolean, showMenu: MutableState<Boolean>, onCloseMenuAction: MutableState<(() -> Unit)>) {}