package com.arpokrat.common.views.chat.item

import SectionItemView
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.*
import dev.icerock.moko.resources.compose.painterResource
import dev.icerock.moko.resources.compose.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.arpokrat.common.model.*
import com.arpokrat.common.model.ChatModel.controller
import com.arpokrat.common.model.ChatModel.currentUser
import com.arpokrat.common.platform.*
import com.arpokrat.common.ui.theme.*
import com.arpokrat.common.views.chat.*
import com.arpokrat.common.views.chatlist.openChat
import com.arpokrat.common.views.helpers.*
import com.arpokrat.res.MR
import dev.icerock.moko.resources.ImageResource
import dev.icerock.moko.resources.StringResource
import kotlinx.datetime.Clock
import kotlin.math.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import com.arpokrat.common.model.UpdatedMessage
import com.arpokrat.common.model.MsgContent
import com.arpokrat.common.wallet.*
import com.arpokrat.common.wallet.ui.dashboard.WalletMainView
import com.arpokrat.common.wallet.ui.transaction.WalletSendView
import com.arpokrat.common.wallet.ui.inchat.HandshakeMessageBubble
import com.arpokrat.common.wallet.ui.components.DevModeBadge

// TODO refactor so that FramedItemView can show all CIContent items if they're deleted (see Swift code)

private val msgRectMaxRadius = 18.dp
private val msgBubbleMaxRadius = msgRectMaxRadius * 1.2f
val msgTailWidthDp = 9.dp
private val msgTailMinHeightDp = msgTailWidthDp * 1.254f
private val msgTailMaxHeightDp = msgTailWidthDp * 1.732f

val chatEventStyle = SpanStyle()

private val PastelMemberColors = listOf(
  Color(0xFF5E97F6),
  Color(0xFF9CCC65),
  Color(0xFFFFB74D),
  Color(0xFFBA68C8),
  Color(0xFF4DD0E1),
  Color(0xFFFF8A65),
  Color(0xFF7986CB),
  Color(0xFF90A4AE),
  Color(0xFFE57373),
  Color(0xFF4DB6AC),
  Color(0xFFFFD54F),
  Color(0xFFA1887F)
)

fun getMemberColor(memberId: String, role: GroupMemberRole): Color {
  if (role == GroupMemberRole.Owner) {
    return Color(0xFFFF5252)
  }
  val hash = memberId.hashCode()
  val index = (hash % PastelMemberColors.size).absoluteValue
  return PastelMemberColors[index]
}

fun chatEventText(ci: ChatItem): AnnotatedString =
  chatEventText(ci.content.text, ci.timestampText)

fun chatEventText(eventText: String, ts: String): AnnotatedString =
  buildAnnotatedString {
    append(eventText)
    withStyle(SpanStyle(fontSize = 10.sp, color = Color.Gray.copy(alpha = 0.6f))) {
      append("  $ts")
    }
  }

data class ChatItemReactionMenuItem (
  val name: String,
  val image: String?,
  val onClick: (() -> Unit)?
)

@Composable
fun ChatItemView(
  chatsCtx: ChatModel.ChatsContext,
  rhId: Long?,
  chat: Chat,
  cItem: ChatItem,
  composeState: MutableState<ComposeState>,
  imageProvider: (() -> ImageGalleryProvider)? = null,
  useLinkPreviews: Boolean,
  linkMode: SimplexLinkMode,
  revealed: State<Boolean>,
  highlighted: State<Boolean>,
  hoveredItemId: MutableState<Long?>,
  range: State<IntRange?>,
  selectedChatItems: MutableState<Set<Long>?>,
  searchIsNotBlank: State<Boolean>,
  fillMaxWidth: Boolean = true,
  selectChatItem: () -> Unit,
  deleteMessage: (Long, CIDeleteMode) -> Unit,
  deleteMessages: (List<Long>) -> Unit,
  archiveReports: (List<Long>, Boolean) -> Unit,
  receiveFile: (Long) -> Unit,
  cancelFile: (Long) -> Unit,
  joinGroup: (Long, () -> Unit) -> Unit,
  acceptCall: (Contact) -> Unit,
  scrollToItem: (Long) -> Unit,
  scrollToItemId: MutableState<Long?>,
  scrollToQuotedItemFromItem: (Long) -> Unit,
  acceptFeature: (Contact, ChatFeature, Int?) -> Unit,
  openDirectChat: (Long) -> Unit,
  forwardItem: (ChatInfo, ChatItem) -> Unit,
  updateContactStats: (Contact) -> Unit,
  updateMemberStats: (GroupInfo, GroupMember) -> Unit,
  syncContactConnection: (Contact) -> Unit,
  syncMemberConnection: (GroupInfo, GroupMember) -> Unit,
  findModelChat: (String) -> Chat?,
  findModelMember: (String) -> GroupMember?,
  setReaction: (ChatInfo, ChatItem, Boolean, MsgReaction) -> Unit,
  showItemDetails: (ChatInfo, ChatItem) -> Unit,
  reveal: (Boolean) -> Unit,
  showMemberInfo: (GroupInfo, GroupMember) -> Unit,
  showChatInfo: () -> Unit,
  developerTools: Boolean,
  showViaProxy: Boolean,
  showTimestamp: Boolean,
  itemSeparation: ItemSeparation,
  preview: Boolean = false,
  onOpenMenu: () -> Unit = {}
) {
  val cInfo = chat.chatInfo
  val uriHandler = LocalUriHandler.current
  val sent = cItem.chatDir.sent

  val hideFeatureEvent = when (val cc = cItem.content) {
    is CIContent.RcvChatFeature -> !cc.enabled.shouldShowEvent(cc.feature, FeatureEventDirection.RCV)
    is CIContent.SndChatFeature -> !cc.enabled.shouldShowEvent(cc.feature, FeatureEventDirection.SND)
    else -> false
  }

  val isSystemEvent = !hideFeatureEvent && when (cItem.content) {
    is CIContent.RcvDirectEventContent,
    is CIContent.RcvGroupEventContent,
    is CIContent.SndGroupEventContent,
    is CIContent.RcvConnEventContent,
    is CIContent.SndConnEventContent,
    is CIContent.SndDirectE2EEInfo,
    is CIContent.RcvDirectE2EEInfo,
    is CIContent.SndGroupE2EEInfo,
    is CIContent.RcvGroupE2EEInfo,
    is CIContent.RcvChatFeature,
    is CIContent.SndChatFeature,
    is CIContent.SndChatPreference,
    is CIContent.RcvChatPreference,
    is CIContent.RcvChatFeatureRejected,
    is CIContent.RcvGroupFeature,
    is CIContent.SndGroupFeature,
    is CIContent.RcvGroupFeatureRejected -> true
    else -> false
  }


  val alignment = if (isSystemEvent) Alignment.Center else if (sent) Alignment.CenterEnd else Alignment.CenterStart

  val showMenuDummy = remember { mutableStateOf(false) }

  LaunchedEffect(showMenuDummy.value) {
    if (showMenuDummy.value) {
      showMenuDummy.value = false
      onOpenMenu()
    }
  }

  val fullDeleteAllowed = remember(cInfo) { cInfo.featureEnabled(ChatFeature.FullDelete) }

  val onLinkLongClick = { _: String -> onOpenMenu() }

  val live = remember { derivedStateOf { composeState.value.liveMessage != null } }.value

  Box(
    modifier = (if (fillMaxWidth) Modifier.fillMaxWidth() else Modifier),
    contentAlignment = alignment,
  ) {
    val info = cItem.meta.itemStatus.statusInto
    val onClick = if (info != null) {
      {
        AlertManager.shared.showAlertMsg(
          title = info.first,
          text = info.second,
        )
      }
    } else { {} }

    @Composable
    fun ChatItemReactions() {
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.chatItemOffset(cItem, itemSeparation.largeGap, inverted = true, revealed = true)) {
        cItem.reactions.forEach { r ->
          val showReactionMenu = remember { mutableStateOf(false) }
          val reactionMenuItems = remember { mutableStateOf(emptyList<ChatItemReactionMenuItem>()) }
          val interactionSource = remember { MutableInteractionSource() }
          val enterInteraction = remember { HoverInteraction.Enter() }
          KeyChangeEffect(highlighted.value) {
            if (highlighted.value) {
              interactionSource.emit(enterInteraction)
            } else {
              interactionSource.emit(HoverInteraction.Exit(enterInteraction))
            }
          }

          var modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp).clip(RoundedCornerShape(8.dp))
          if (cInfo.featureEnabled(ChatFeature.Reactions)) {
            fun showReactionsMenu() {
              when (cInfo) {
                is ChatInfo.Group -> {
                  withBGApi {
                    try {
                      val members = controller.apiGetReactionMembers(rhId, cInfo.groupInfo.groupId, cItem.id, r.reaction)
                      if (members != null) {
                        showReactionMenu.value = true
                        reactionMenuItems.value = members.map {
                          val enabled = cInfo.groupInfo.membership.groupMemberId != it.groupMember.groupMemberId
                          val click = if (enabled) ({ showMemberInfo(cInfo.groupInfo, it.groupMember) }) else null
                          ChatItemReactionMenuItem(it.groupMember.displayName, it.groupMember.image, click)
                        }
                      }
                    } catch (e: Exception) {
                      Log.d(TAG, "chatItemView ChatItemReactions onLongClick: unexpected exception: ${e.stackTraceToString()}")
                    }
                  }
                }
                is ChatInfo.Direct -> {
                  showReactionMenu.value = true
                  val reactions = mutableListOf<ChatItemReactionMenuItem>()

                  if (!r.userReacted || r.totalReacted > 1) {
                    val contact = cInfo.contact
                    reactions.add(ChatItemReactionMenuItem(contact.displayName, contact.image, showChatInfo))
                  }

                  if (r.userReacted) {
                    reactions.add(ChatItemReactionMenuItem(generalGetString(MR.strings.sender_you_pronoun), currentUser.value?.image, null))
                  }
                  reactionMenuItems.value = reactions
                }
                else -> {}
              }
            }
            modifier = modifier
              .combinedClickable(
                onClick = {
                  if (cItem.allowAddReaction || r.userReacted) {
                    setReaction(cInfo, cItem, !r.userReacted, r.reaction)
                  }
                },
                onLongClick = {
                  showReactionsMenu()
                },
                interactionSource = interactionSource,
                indication = LocalIndication.current
              )
              .onRightClick { showReactionsMenu() }
          }
          Row(modifier.padding(2.dp), verticalAlignment = Alignment.CenterVertically) {
            ReactionIcon(r.reaction.text, fontSize = 12.sp)
            DefaultDropdownMenu(showMenu = showReactionMenu) {
              reactionMenuItems.value.forEach { m ->
                ItemAction(
                  text = m.name,
                  composable = { ProfileImage(44.dp, m.image) },
                  onClick = {
                    val click = m.onClick
                    if (click != null) {
                      click()
                      showReactionMenu.value = false
                    }
                  },
                  lineLimit = 1,
                  color = if (m.onClick == null) MaterialTheme.colors.secondary else MenuTextColor
                )
              }
            }
            if (r.totalReacted > 1) {
              Spacer(Modifier.width(4.dp))
              Text(
                "${r.totalReacted}",
                fontSize = 11.5.sp,
                fontWeight = if (r.userReacted) FontWeight.Bold else FontWeight.Normal,
                color = if (r.userReacted) MaterialTheme.colors.primary else MaterialTheme.colors.secondary,
              )
            }
          }
        }
      }
    }

    @Composable
    fun GoToItemInnerButton(alignStart: Boolean, icon: ImageResource, iconSize: Dp = 22.dp, parentActivated: State<Boolean>, onClick: () -> Unit) {
      val buttonInteractionSource = remember { MutableInteractionSource() }
      val buttonHovered = buttonInteractionSource.collectIsHoveredAsState()
      val buttonPressed = buttonInteractionSource.collectIsPressedAsState()
      val buttonActivated = remember { derivedStateOf { buttonHovered.value || buttonPressed.value } }

      val fullyVisible = parentActivated.value || buttonActivated.value || hoveredItemId.value == cItem.id
      val mixAlpha = 0.6f
      val mixedBackgroundColor = if (fullyVisible) {
        if (MaterialTheme.colors.isLight) {
          MaterialTheme.colors.secondary.mixWith(Color.White, mixAlpha)
        } else {
          MaterialTheme.colors.secondary.mixWith(Color.Black, mixAlpha)
        }
      } else {
        Color.Unspecified
      }
      val iconTint = if (fullyVisible) {
        Color.White
      } else {
        if (MaterialTheme.colors.isLight) {
          MaterialTheme.colors.secondary.mixWith(Color.White, mixAlpha)
        } else {
          MaterialTheme.colors.secondary.mixWith(Color.Black, mixAlpha)
        }
      }

      IconButton(
        onClick,
        Modifier
          .padding(start = if (alignStart) 0.dp else DEFAULT_PADDING_HALF + 3.dp, end = if (alignStart) DEFAULT_PADDING_HALF + 3.dp else 0.dp)
          .then(if (fullyVisible) Modifier.background(mixedBackgroundColor, CircleShape) else Modifier)
          .size(22.dp),
        interactionSource = buttonInteractionSource
      ) {
        Icon(painterResource(icon), null, Modifier.size(iconSize), tint = iconTint)
      }
    }

    @Composable
    fun GoToItemButton(alignStart: Boolean, parentActivated: State<Boolean>) {
      val chatTypeApiIdMsgId = cItem.meta.itemForwarded?.chatTypeApiIdMsgId
      if (searchIsNotBlank.value) {
        GoToItemInnerButton(alignStart, MR.images.ic_search, 17.dp, parentActivated) {
          withBGApi {
            openChat(secondaryChatsCtx = null, rhId, cInfo.chatType, cInfo.apiId, cItem.id)
            closeReportsIfNeeded()
          }
        }
      } else if (chatTypeApiIdMsgId != null) {
        GoToItemInnerButton(alignStart, MR.images.ic_arrow_forward, 22.dp, parentActivated) {
          val (chatType, apiId, msgId) = chatTypeApiIdMsgId
          withBGApi {
            openChat(secondaryChatsCtx = null, rhId, chatType, apiId, msgId)
            closeReportsIfNeeded()
          }
        }
      }
    }

    Column(horizontalAlignment = if (cItem.chatDir.sent) Alignment.End else Alignment.Start) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        val bubbleInteractionSource = remember { MutableInteractionSource() }
        val bubbleHovered = bubbleInteractionSource.collectIsHoveredAsState()
        if (cItem.chatDir.sent) {
          GoToItemButton(true, bubbleHovered)
        }
        Column(Modifier.weight(1f, fill = false)) {
          val enterInteraction = remember { HoverInteraction.Enter() }
          LaunchedEffect(highlighted.value, hoveredItemId.value) {
            if (highlighted.value || hoveredItemId.value == cItem.id) {
              bubbleInteractionSource.emit(enterInteraction)
            } else {
              bubbleInteractionSource.emit(HoverInteraction.Exit(enterInteraction))
            }
          }

          val bubbleModifier = Modifier
            .clipChatItem(cItem, itemSeparation.largeGap, revealed.value)
            .hoverable(bubbleInteractionSource)
            .let { base ->
              if (isSystemEvent) {
                base
              } else {
                base
                  .combinedClickable(
                    onLongClick = if (isSystemEvent) null else onOpenMenu,
                    onClick = {
                      if (!isSystemEvent) {
                        if (appPlatform.isAndroid && (searchIsNotBlank.value || cItem.meta.itemForwarded?.chatTypeApiIdMsgId != null)) {
                          hoveredItemId.value = if (hoveredItemId.value == cItem.id) null else cItem.id
                        }
                        onClick()
                      }
                    },
                    interactionSource = bubbleInteractionSource,
                    indication = if (isSystemEvent) null else LocalIndication.current
                  )
                  .onRightClick {
                    if (!isSystemEvent) {
                      onOpenMenu()
                    }
                  }
              }
            }

          Column(modifier = bubbleModifier) {
            @Composable
            fun framedItemView() {
              FramedItemView(
                chatsCtx,
                chat,
                cItem,
                uriHandler,
                imageProvider,
                linkMode = linkMode,
                showViaProxy = showViaProxy,

                showMenu = showMenuDummy,

                showTimestamp = showTimestamp,
                tailVisible = itemSeparation.largeGap,

                receiveFile = receiveFile,
                onLinkLongClick = onLinkLongClick,
                scrollToItem = scrollToItem,
                scrollToItemId = scrollToItemId,
                scrollToQuotedItemFromItem = scrollToQuotedItemFromItem
              )
            }

            @Composable
            fun ContentItem() {
              val mc = cItem.content.msgContent

              val scope = rememberCoroutineScope()
              val isMe = cItem.chatDir.sent
              val walletManager = remember { WalletManager() }

              fun editMyMessage(newText: String) {
                scope.launch(Dispatchers.IO) {
                  try {
                    val result = ChatModel.controller.apiUpdateChatItem(
                      rh = chat.remoteHostId,
                      type = chat.chatInfo.chatType,
                      id = chat.chatInfo.apiId,
                      scope = null,
                      itemId = cItem.meta.itemId,
                      updatedMessage = UpdatedMessage(MsgContent.MCText(newText), emptyMap())
                    )
                    if (result != null) {
                      launch(Dispatchers.Main) {
                        chatsCtx.upsertChatItem(chat.remoteHostId, chat.chatInfo, result.chatItem)
                      }
                    }
                  } catch (e: Exception) { e.printStackTrace() }
                }
              }

              fun replyAndClean(newText: String) {
                scope.launch(Dispatchers.IO) {
                  try {
                    val content = MsgContent.MCText(newText)
                    val composedMsg = ComposedMessage(null, cItem.id, content, emptyMap())

                    ChatModel.controller.apiSendMessages(
                      rh = chat.remoteHostId,
                      type = chat.chatInfo.chatType,
                      id = chat.chatInfo.apiId,
                      scope = null,
                      live = false,
                      ttl = null,
                      composedMessages = listOf(composedMsg)
                    )

                    launch(Dispatchers.Main) {
                      deleteMessage(cItem.id, CIDeleteMode.cidmInternal)
                    }
                  } catch (e: Exception) { e.printStackTrace() }
                }
              }

              fun updateStatus(newText: String) {
                if (isMe) editMyMessage(newText) else replyAndClean(newText)
              }

              if (mc is MsgContent.MCText &&
                (CryptoProtocol.isInvoice(mc.text) || CryptoProtocol.isPaid(mc.text) || CryptoProtocol.isCancelled(mc.text) || CryptoProtocol.isDeclined(mc.text))) {

                HandshakeMessageBubble(
                  message = cItem,
                  textContent = mc.text,
                  onCancel = { updateStatus(CryptoProtocol.formatCancelled()) },
                  onDecline = { updateStatus(CryptoProtocol.formatDeclined()) },
                  onPay = {
                    if (CryptoManager.isReady()) {
                      val invoice = CryptoProtocol.parseInvoice(mc.text)

                      if (invoice != null) {
                        val isDevModeEnabled = walletManager.isDeveloperModeEnabled()
                        val testnetIds = listOf(1, 11155111, 80002, 9000, 10000)
                        val isTestnetInvoice = invoice.coinType in testnetIds

                        if (isTestnetInvoice && !isDevModeEnabled) {
                          AlertManager.shared.showAlertMsg(
                            generalGetString(MR.strings.dev_mode_required_title),
                            generalGetString(MR.strings.dev_mode_required_desc)
                          )
                          return@HandshakeMessageBubble
                        }
                        if (!isTestnetInvoice && isDevModeEnabled) {
                          AlertManager.shared.showAlertMsg(
                            generalGetString(MR.strings.mainnet_invoice_title),
                            generalGetString(MR.strings.mainnet_invoice_desc)
                          )
                          return@HandshakeMessageBubble
                        }

                        val assetDef = DefaultAssets.getActiveAssets().find { it.symbol == invoice.symbol && it.network.id == invoice.coinType }

                        val cachedAssets = WalletAssetCache.loadAssets()
                        val cachedAsset = cachedAssets.find { it.coinType == invoice.coinType && it.symbol == invoice.symbol }
                        val asset = cachedAsset ?: CryptoAsset(
                          symbol = invoice.symbol,
                          name = assetDef?.name ?: invoice.symbol,
                          balance = "0.0",
                          decimals = assetDef?.decimals ?: 18,
                          coinType = invoice.coinType,
                          contractAddress = assetDef?.contractAddress
                        )

                        val nativeNetworkId = CryptoNetwork.fromId(invoice.coinType).id
                        val nativeAsset = cachedAssets.find { it.coinType == nativeNetworkId && it.contractAddress.isNullOrBlank() }
                        val realNativeBalance = nativeAsset?.balance?.replace(",", ".")?.toDoubleOrNull() ?: 0.0

                        val rawCurrency = walletManager.getCurrency()
                        val sym = if (rawCurrency.equals("USD", ignoreCase = true)) "$" else "€"

                        ModalManager.start.showModalCloseable(
                          endButtons = {
                            if (isDevModeEnabled) {
                              Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 16.dp)) {
                                DevModeBadge()
                              }
                            }
                          }
                        ) { closeSheet ->
                          WalletSendView(
                            asset = asset,
                            nativeBalance = realNativeBalance,
                            currency = rawCurrency,
                            currencySymbol = sym,
                            initialAddress = invoice.address,
                            initialAmount = invoice.amount,
                            isInvoiceMode = true,
                            verifyPin = { pin -> walletManager.checkPin(pin) },
                            onBack = { closeSheet() },
                            onScanClick = {},
                            onNext = { dest, amount ->
                              scope.launch {
                                try {
                                  val txHash: String? = when (asset.coinType) {
                                    CryptoNetwork.POLYGON.id, 80002, CryptoNetwork.ETHEREUM.id, 11155111 -> {
                                      NetworkFactory.getEvmService(asset.coinType)
                                        .sendTransaction(walletManager, dest, amount, asset.contractAddress, asset.decimals)
                                    }
                                    CryptoNetwork.SOLANA.id, CryptoNetwork.SOLANA_DEVNET.id -> {
                                      NetworkFactory.getSolanaService(asset.coinType)
                                        .sendTransaction(walletManager, dest, amount)
                                    }
                                    CryptoNetwork.BITCOIN.id, CryptoNetwork.BITCOIN_TESTNET.id -> {
                                      NetworkFactory.getBitcoinService(asset.coinType)
                                        .sendTransaction(walletManager, dest, amount, asset.coinType)
                                    }
                                    CryptoNetwork.TRON.id, CryptoNetwork.TRON_NILE.id -> {
                                      NetworkFactory.getTronService(asset.coinType)
                                        .sendTransaction(walletManager, asset.coinType, dest, amount, asset.contractAddress, asset.decimals)
                                    }
                                    else -> null
                                  }

                                  if (txHash != null && !txHash.startsWith("Error")) {
                                    updateStatus(CryptoProtocol.formatPaid(invoice.amount, invoice.symbol, txHash, invoice.fiatValue))
                                    closeSheet()
                                  } else {
                                    AlertManager.shared.showAlertMsg(
                                      generalGetString(MR.strings.transaction_failed_title),
                                      generalGetString(MR.strings.transaction_failed_desc)
                                    )
                                  }
                                } catch (e: Exception) {
                                  AlertManager.shared.showAlertMsg(
                                    generalGetString(MR.strings.error),
                                    generalGetString(MR.strings.unexpected_error_desc)
                                  )
                                }
                              }
                            }
                          )
                        }
                      }
                    } else {
                      ModalManager.fullscreen.showCustomModal { close -> WalletMainView(closeWallet = close) }
                    }
                  }
                )
                return
              }

              if (cItem.meta.itemDeleted != null && (!revealed.value || cItem.isDeletedContent)) {
                MarkedDeletedItemView(chatsCtx, cItem, cInfo, cInfo.timedMessagesTTL, revealed, showViaProxy = showViaProxy, showTimestamp = showTimestamp)
              } else {
                if (cItem.quotedItem == null && cItem.meta.itemForwarded == null && cItem.meta.itemDeleted == null && !cItem.meta.isLive) {
                  if (mc is MsgContent.MCText && isShortEmoji(cItem.content.text)) {
                    EmojiItemView(cItem, cInfo.timedMessagesTTL, showViaProxy = showViaProxy, showTimestamp = showTimestamp)
                  } else if (mc is MsgContent.MCVoice && cItem.content.text.isEmpty()) {
                    CIVoiceView(
                      mc.duration, cItem.file, cItem.meta.itemEdited, cItem.chatDir.sent, hasText = false, cItem, cInfo.timedMessagesTTL,
                      showViaProxy = showViaProxy, showTimestamp = showTimestamp,
                      longClick = { onOpenMenu() },
                      receiveFile = receiveFile
                    )
                  } else {
                    framedItemView()
                  }
                } else {
                  framedItemView()
                }
              }
            }

            @Composable fun LegacyDeletedItem() {
              DeletedItemView(cItem, cInfo.timedMessagesTTL, showViaProxy = showViaProxy, showTimestamp = showTimestamp)
            }

            @Composable fun CallItem(status: CICallStatus, duration: Int) {
              CICallItemView(cInfo, cItem, status, duration, showTimestamp = showTimestamp, acceptCall, cInfo.timedMessagesTTL)
            }

            fun mergedGroupEventText(chatItem: ChatItem, reversedChatItems: List<ChatItem>): String? {
              val (count, ns) = chatModel.getConnectedMemberNames(chatItem, reversedChatItems)
              val members = when {
                ns.size == 1 -> String.format(generalGetString(MR.strings.rcv_group_event_1_member_connected), ns[0])
                ns.size == 2 -> String.format(generalGetString(MR.strings.rcv_group_event_2_members_connected), ns[0], ns[1])
                ns.size == 3 -> String.format(generalGetString(MR.strings.rcv_group_event_3_members_connected), ns[0], ns[1], ns[2])
                ns.size > 3 -> String.format(generalGetString(MR.strings.rcv_group_event_n_members_connected), ns[0], ns[1], ns.size - 2)
                else -> ""
              }
              return if (count <= 1) {
                null
              } else if (ns.isEmpty()) {
                generalGetString(MR.strings.rcv_group_events_count).format(count)
              } else if (count > ns.size) {
                members + " " + generalGetString(MR.strings.rcv_group_and_other_events).format(count - ns.size)
              } else {
                members
              }
            }

            fun eventItemViewText(reversedChatItems: List<ChatItem>): AnnotatedString {
              val memberDisplayName = cItem.memberDisplayName
              val t = mergedGroupEventText(cItem, reversedChatItems)
              return if (!revealed.value && t != null) {
                chatEventText(t, cItem.timestampText)
              } else if (memberDisplayName != null) {
                buildAnnotatedString {
                  withStyle(chatEventStyle) { append(memberDisplayName) }
                  append(" ")
                }.plus(chatEventText(cItem))
              } else {
                chatEventText(cItem)
              }
            }

            @Composable fun EventItemView() {
              val reversedChatItems = chatsCtx.chatItems.value.asReversed()
              CIEventView(eventItemViewText(reversedChatItems))
            }

            @Composable fun PendingReviewEventItemView() {
              val text = buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(cItem.content.text) }
                append("  ${cItem.timestampText}")
              }
              CIEventView(text)
            }

            @Composable
            fun DeletedItem() {
              MarkedDeletedItemView(chatsCtx, cItem, cInfo, cInfo.timedMessagesTTL, revealed, showViaProxy = showViaProxy, showTimestamp = showTimestamp)
            }

            @Composable
            fun e2eeInfoText(sId: StringResource) {
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 12.dp, horizontal = 16.dp),
                contentAlignment = Alignment.Center
              ) {
                Surface(
                  shape = RoundedCornerShape(12.dp),
                  color = MaterialTheme.colors.surface.copy(alpha = 0.6f),
                  border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.05f)),
                  elevation = 0.dp
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                  ) {
                    Icon(
                      painter = painterResource(MR.images.ic_lock),
                      contentDescription = null,
                      modifier = Modifier.size(14.dp),
                      tint = MaterialTheme.colors.primary.copy(alpha = 0.8f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                      text = stringResource(sId),
                      style = MaterialTheme.typography.caption.copy(
                        fontSize = 12.5.sp,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium
                      )
                    )
                  }
                }
              }
            }

            @Composable
            fun E2EEInfoNoPQText() {
              e2eeInfoText(MR.strings.e2ee_info_no_pq)
            }

            @Composable
            fun DirectE2EEInfoText(e2EEInfo: E2EEInfo) {
              if (e2EEInfo.pqEnabled != null) {
                if (e2EEInfo.pqEnabled) {
                  e2eeInfoText(MR.strings.e2ee_info_pq)
                } else {
                  E2EEInfoNoPQText()
                }
              } else {
                e2eeInfoText(MR.strings.e2ee_info_e2ee)
              }
            }

            when (val c = cItem.content) {

              is CIContent.SndMsgContent -> ContentItem()
              is CIContent.RcvMsgContent -> ContentItem()

              is CIContent.SndDeleted -> LegacyDeletedItem()
              is CIContent.RcvDeleted -> LegacyDeletedItem()

              is CIContent.SndCall -> CallItem(c.status, c.duration)
              is CIContent.RcvCall -> CallItem(c.status, c.duration)

              is CIContent.RcvIntegrityError ->
                if (developerTools)
                  IntegrityErrorItemView(c.msgError, cItem, showTimestamp, cInfo.timedMessagesTTL)
                else
                  Box(Modifier.size(0.dp)) {}

              is CIContent.RcvDecryptionError ->
                CIRcvDecryptionError(
                  c.msgDecryptError,
                  c.msgCount,
                  cInfo,
                  cItem,
                  updateContactStats = updateContactStats,
                  updateMemberStats = updateMemberStats,
                  syncContactConnection = syncContactConnection,
                  syncMemberConnection = syncMemberConnection,
                  findModelChat = findModelChat,
                  findModelMember = findModelMember
                )

              is CIContent.RcvGroupInvitation ->
                CIGroupInvitationView(
                  ci = cItem,
                  groupInvitation = c.groupInvitation,
                  memberRole = c.memberRole,
                  showTimestamp = showTimestamp,
                  chatIncognito = cInfo.incognito,
                  joinGroup = joinGroup,
                  cancelInvitation = { itemId ->
                    deleteMessage(itemId, CIDeleteMode.cidmInternal)
                  },
                  timedMessagesTTL = cInfo.timedMessagesTTL
                )

              is CIContent.SndGroupInvitation ->
                CIGroupInvitationView(
                  ci = cItem,
                  groupInvitation = c.groupInvitation,
                  memberRole = c.memberRole,
                  showTimestamp = showTimestamp,
                  chatIncognito = cInfo.incognito,
                  joinGroup = joinGroup,
                  cancelInvitation = { itemId ->
                    deleteMessage(itemId, CIDeleteMode.cidmBroadcast)
                  },
                  timedMessagesTTL = cInfo.timedMessagesTTL
                )

              is CIContent.RcvDirectEventContent -> EventItemView()
              is CIContent.RcvConnEventContent -> EventItemView()
              is CIContent.SndConnEventContent -> EventItemView()

              is CIContent.RcvGroupEventContent ->
                when (c.rcvGroupEvent) {
                  is RcvGroupEvent.MemberCreatedContact -> CIMemberCreatedContactView(cItem, openDirectChat)
                  is RcvGroupEvent.NewMemberPendingReview -> PendingReviewEventItemView()
                  else -> EventItemView()
                }

              is CIContent.SndGroupEventContent ->
                when (c.sndGroupEvent) {
                  is SndGroupEvent.UserPendingReview -> PendingReviewEventItemView()
                  else -> EventItemView()
                }

              is CIContent.RcvChatFeature -> {
                val feature = c.feature
                val enabled = c.enabled

                if (!enabled.shouldShowEvent(feature, FeatureEventDirection.RCV)) {
                  Box(Modifier.size(0.dp)) {}
                } else {
                  val active = enabled.isActive(feature, FeatureEventDirection.RCV)
                  val status = if (active) FeatureAllowed.YES else FeatureAllowed.NO
                  val ct = (cInfo as? ChatInfo.Direct)?.contact

                  Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                    CIFeaturePreferenceView(
                      chatItem = cItem,
                      contact = ct,
                      feature = feature,
                      allowed = status,
                      acceptFeature = { _, _, _ -> }
                    )
                  }
                }
              }

              is CIContent.SndChatFeature -> {
                val feature = c.feature
                val enabled = c.enabled

                if (!enabled.shouldShowEvent(feature, FeatureEventDirection.SND)) {
                  Box(Modifier.size(0.dp)) {}
                } else {
                  val active = enabled.isActive(feature, FeatureEventDirection.SND)
                  val status = if (active) FeatureAllowed.YES else FeatureAllowed.NO
                  val ct = (cInfo as? ChatInfo.Direct)?.contact

                  Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                    CIFeaturePreferenceView(
                      chatItem = cItem,
                      contact = ct,
                      feature = feature,
                      allowed = status,
                      acceptFeature = { _, _, _ -> }
                    )
                  }
                }
              }

              is CIContent.RcvChatPreference -> {
                val ct = (cInfo as? ChatInfo.Direct)?.contact
                Box(
                  contentAlignment = Alignment.Center,
                  modifier = Modifier.fillMaxWidth()
                ) {
                  CIFeaturePreferenceView(
                    chatItem = cItem,
                    contact = ct,
                    feature = c.feature,
                    allowed = c.allowed,
                    acceptFeature = acceptFeature
                  )
                }
              }

              is CIContent.SndChatPreference -> {
                val ct = (cInfo as? ChatInfo.Direct)?.contact
                Box(
                  contentAlignment = Alignment.Center,
                  modifier = Modifier.fillMaxWidth()
                ) {
                  CIFeaturePreferenceView(
                    chatItem = cItem,
                    contact = ct,
                    feature = c.feature,
                    allowed = c.allowed,
                    acceptFeature = acceptFeature
                  )
                }
              }

              is CIContent.RcvGroupFeature ->
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                  CIChatFeatureView(
                    chatsCtx, cInfo, cItem,
                    feature = c.groupFeature, allowed = FeatureAllowed.YES,
                    iconColor = Color.Gray, revealed = revealed, showMenu = showMenuDummy
                  )
                }

              is CIContent.SndGroupFeature ->
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                  CIChatFeatureView(
                    chatsCtx, cInfo, cItem,
                    feature = c.groupFeature, allowed = FeatureAllowed.YES,
                    iconColor = Color.Gray, revealed = revealed, showMenu = showMenuDummy
                  )
                }

              is CIContent.RcvChatFeatureRejected ->
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                  CIChatFeatureView(
                    chatsCtx, cInfo, cItem,
                    feature = c.feature, allowed = FeatureAllowed.NO,
                    iconColor = MaterialTheme.colors.error, revealed = revealed, showMenu = showMenuDummy
                  )
                }

              is CIContent.RcvGroupFeatureRejected ->
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                  CIChatFeatureView(
                    chatsCtx, cInfo, cItem,
                    feature = c.groupFeature, allowed = FeatureAllowed.NO,
                    iconColor = MaterialTheme.colors.error, revealed = revealed, showMenu = showMenuDummy
                  )
                }

              is CIContent.SndModerated -> DeletedItem()
              is CIContent.RcvModerated -> DeletedItem()
              is CIContent.RcvBlocked -> DeletedItem()

              is CIContent.SndDirectE2EEInfo ->
                SystemEventPill(
                  text = stringResource(if (c.e2eeInfo.pqEnabled == true) MR.strings.e2ee_info_pq else MR.strings.e2ee_info_e2ee),
                  icon = painterResource(MR.images.ic_lock)
                )

              is CIContent.RcvDirectE2EEInfo ->
                SystemEventPill(
                  text = stringResource(if (c.e2eeInfo.pqEnabled == true) MR.strings.e2ee_info_pq else MR.strings.e2ee_info_e2ee),
                  icon = painterResource(MR.images.ic_lock)
                )

              is CIContent.SndGroupE2EEInfo ->
                SystemEventPill(
                  text = stringResource(MR.strings.e2ee_info_no_pq),
                  icon = painterResource(MR.images.ic_lock)
                )

              is CIContent.RcvGroupE2EEInfo ->
                SystemEventPill(
                  text = stringResource(MR.strings.e2ee_info_no_pq),
                  icon = painterResource(MR.images.ic_lock)
                )

              is CIContent.ChatBanner -> Spacer(Modifier.size(0.dp))
              is CIContent.InvalidJSON -> CIInvalidJSONView(c.json)
            }

          }
        }
        if (!cItem.chatDir.sent) {
          GoToItemButton(false, bubbleHovered)
        }
      }
      if (cItem.content.msgContent != null && (cItem.meta.itemDeleted == null || revealed.value) && cItem.reactions.isNotEmpty()) {
        ChatItemReactions()
      }
    }
  }
}

@Composable
expect fun ReactionIcon(text: String, fontSize: TextUnit = TextUnit.Unspecified)

@Composable
expect fun SaveContentItemAction(cItem: ChatItem, saveFileLauncher: FileChooserLauncher, showMenu: MutableState<Boolean>)

@Composable
fun CancelFileItemAction(
  fileId: Long,
  showMenu: MutableState<Boolean>,
  cancelFile: (Long) -> Unit,
  cancelAction: CancelAction
) {
  ItemAction(
    stringResource(cancelAction.uiActionId),
    painterResource(MR.images.ic_close),
    onClick = {
      showMenu.value = false
      cancelFileAlertDialog(fileId, cancelFile = cancelFile, cancelAction = cancelAction)
    },
    color = MaterialTheme.colors.error
  )
}

@Composable
fun ItemInfoAction(
  cInfo: ChatInfo,
  cItem: ChatItem,
  showItemDetails: (ChatInfo, ChatItem) -> Unit,
  showMenu: MutableState<Boolean>
) {
  ItemAction(
    stringResource(MR.strings.info_menu),
    painterResource(MR.images.ic_info),
    onClick = {
      showItemDetails(cInfo, cItem)
      showMenu.value = false
    }
  )
}


@Composable
fun DeleteItemAction(
  chatsCtx: ChatModel.ChatsContext,
  cItem: ChatItem,
  revealed: State<Boolean>,
  showMenu: MutableState<Boolean>,
  questionText: String,
  deleteMessage: (Long, CIDeleteMode) -> Unit,
  deleteMessages: (List<Long>) -> Unit,
  buttonText: String = stringResource(MR.strings.delete_verb),
) {
  ItemAction(
    buttonText,
    painterResource(MR.images.ic_delete),
    onClick = {
      showMenu.value = false
      if (!revealed.value) {
        val reversedChatItems = chatsCtx.chatItems.value.asReversed()
        val currIndex = chatModel.getChatItemIndexOrNull(cItem, reversedChatItems)
        val ciCategory = cItem.mergeCategory
        if (currIndex != null && ciCategory != null) {
          val (prevHidden, _) = chatModel.getPrevShownChatItem(currIndex, ciCategory, reversedChatItems)
          val range = chatViewItemsRange(currIndex, prevHidden)
          if (range != null) {
            val itemIds: ArrayList<Long> = arrayListOf()
            for (i in range) {
              itemIds.add(reversedChatItems[i].id)
            }
            deleteMessagesAlertDialog(
              itemIds,
              generalGetString(MR.strings.delete_messages_cannot_be_undone_warning),
              forAll = false,
              deleteMessages = { ids, _ -> deleteMessages(ids) }
            )
          } else {
            deleteMessageAlertDialog(cItem, questionText, deleteMessage = deleteMessage)
          }
        } else {
          deleteMessageAlertDialog(cItem, questionText, deleteMessage = deleteMessage)
        }
      } else {
        deleteMessageAlertDialog(cItem, questionText, deleteMessage = deleteMessage)
      }
    },
    color = MaterialTheme.colors.error
  )
}

@Composable
fun ModerateItemAction(
  cItem: ChatItem,
  questionText: String,
  showMenu: MutableState<Boolean>,
  deleteMessage: (Long, CIDeleteMode) -> Unit
) {
  ItemAction(
    stringResource(MR.strings.moderate_verb),
    painterResource(MR.images.ic_flag),
    onClick = {
      showMenu.value = false
      moderateMessageAlertDialog(cItem, questionText, deleteMessage = deleteMessage)
    },
    color = MaterialTheme.colors.error
  )
}

@Composable
fun SelectItemAction(
  showMenu: MutableState<Boolean>,
  selectItem: () -> Unit,
) {
  ItemAction(
    stringResource(MR.strings.select_verb),
    painterResource(MR.images.ic_check_circle),
    onClick = {
      showMenu.value = false
      selectItem()
    }
  )
}

@Composable
private fun RevealItemAction(revealed: State<Boolean>, showMenu: MutableState<Boolean>, reveal: (Boolean) -> Unit) {
  ItemAction(
    stringResource(MR.strings.reveal_verb),
    painterResource(MR.images.ic_visibility),
    onClick = {
      reveal(true)
      showMenu.value = false
    }
  )
}

@Composable
private fun HideItemAction(revealed: State<Boolean>, showMenu: MutableState<Boolean>, reveal: (Boolean) -> Unit) {
  ItemAction(
    stringResource(MR.strings.hide_verb),
    painterResource(MR.images.ic_visibility_off),
    onClick = {
      reveal(false)
      showMenu.value = false
    }
  )
}

@Composable
private fun ExpandItemAction(revealed: State<Boolean>, showMenu: MutableState<Boolean>, reveal: (Boolean) -> Unit) {
  ItemAction(
    stringResource(MR.strings.expand_verb),
    painterResource(MR.images.ic_expand_all),
    onClick = {
      reveal(true)
      showMenu.value = false
    },
  )
}

@Composable
private fun ShrinkItemAction(revealed: State<Boolean>, showMenu: MutableState<Boolean>, reveal: (Boolean) -> Unit) {
  ItemAction(
    stringResource(MR.strings.hide_verb),
    painterResource(MR.images.ic_collapse_all),
    onClick = {
      reveal(false)
      showMenu.value = false
    },
  )
}

@Composable
private fun ReportItemAction(
  cItem: ChatItem,
  composeState: MutableState<ComposeState>,
  showMenu: MutableState<Boolean>,
) {
  ItemAction(
    stringResource(MR.strings.report_verb),
    painterResource(MR.images.ic_flag),
    onClick = {
      AlertManager.shared.showAlertDialogButtons(
        title = generalGetString(MR.strings.report_reason_alert_title),
        buttons = {
          ReportReason.supportedReasons.forEach { reason ->
            SectionItemView({
              if (composeState.value.editing) {
                composeState.value = ComposeState(
                  contextItem = ComposeContextItem.ReportedItem(cItem, reason),
                  useLinkPreviews = false,
                  preview = ComposePreview.NoPreview,
                )
              } else {
                composeState.value = composeState.value.copy(
                  contextItem = ComposeContextItem.ReportedItem(cItem, reason),
                  useLinkPreviews = false,
                  preview = ComposePreview.NoPreview,
                )
              }
              AlertManager.shared.hideAlert()
            }) {
              Text(reason.text, Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = MaterialTheme.colors.error)
            }
          }
          SectionItemView({
            AlertManager.shared.hideAlert()
          }) {
            Text(stringResource(MR.strings.cancel_verb), Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = MaterialTheme.colors.primary)
          }
        }
      )
      showMenu.value = false
    },
    color = MaterialTheme.colors.error
  )
}

@Composable
private fun ArchiveReportItemAction(id: Long, allowForAll: Boolean, showMenu: MutableState<Boolean>, archiveReports: (List<Long>, Boolean) -> Unit) {
  ItemAction(
    stringResource(MR.strings.archive_report),
    painterResource(MR.images.ic_inventory_2),
    onClick = {
      showArchiveReportsAlert(listOf(id), allowForAll, archiveReports)
      showMenu.value = false
    }
  )
}

fun showArchiveReportsAlert(ids: List<Long>, allowForAll: Boolean, archiveReports: (List<Long>, Boolean) -> Unit) {
  AlertManager.shared.showAlertDialogButtonsColumn(
    title = if (ids.size == 1) {
      generalGetString(MR.strings.report_archive_alert_title)
    } else {
      generalGetString(MR.strings.report_archive_alert_title_nth).format(ids.size)
    },
    text = null,
    buttons = {
      SectionItemView({
        AlertManager.shared.hideAlert()
        archiveReports(ids, false)
      }) {
        Text(
          generalGetString(MR.strings.report_archive_for_me),
          Modifier.fillMaxWidth(),
          textAlign = TextAlign.Center,
          color = MaterialTheme.colors.error
        )
      }
      if (allowForAll) {
        SectionItemView({
          AlertManager.shared.hideAlert()
          archiveReports(ids, true)
        }) {
          Text(
            stringResource(MR.strings.report_archive_for_all_moderators),
            Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colors.error
          )
        }
      }
    }
  )
}

@Composable
fun ItemAction(text: String, icon: Painter, color: Color = Color.Unspecified, onClick: () -> Unit) {
  val finalColor = if (color == Color.Unspecified) {
    MenuTextColor
  } else color
  DropdownMenuItem(onClick, contentPadding = PaddingValues(horizontal = DEFAULT_PADDING * 1.5f)) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text(
        text,
        modifier = Modifier
          .fillMaxWidth()
          .weight(1F)
          .padding(end = 15.dp),
        color = finalColor
      )
      Icon(icon, text, tint = finalColor)
    }
  }
}

@Composable
fun ItemAction(text: String, icon: ImageBitmap, textColor: Color = Color.Unspecified, iconColor: Color = Color.Unspecified, onClick: () -> Unit) {
  val finalColor = if (textColor == Color.Unspecified) {
    MenuTextColor
  } else textColor
  DropdownMenuItem(onClick, contentPadding = PaddingValues(horizontal = DEFAULT_PADDING * 1.5f)) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text(
        text,
        modifier = Modifier
          .fillMaxWidth()
          .weight(1F)
          .padding(end = 15.dp),
        color = finalColor,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      if (iconColor == Color.Unspecified) {
        Image(icon, text, Modifier.size(22.dp))
      } else {
        Icon(icon, text, Modifier.size(22.dp), tint = iconColor)
      }
    }
  }
}

@Composable
fun ItemAction(
  text: String,
  composable: @Composable () -> Unit,
  color: Color = Color.Unspecified,
  onClick: () -> Unit,
  lineLimit: Int = Int.MAX_VALUE
) {
  val finalColor = if (color == Color.Unspecified) {
    MenuTextColor
  } else color
  DropdownMenuItem(onClick, contentPadding = PaddingValues(horizontal = DEFAULT_PADDING * 1.5f)) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text(
        text,
        modifier = Modifier
          .fillMaxWidth()
          .weight(1F)
          .padding(end = 15.dp),
        color = finalColor,
        maxLines = lineLimit,
        overflow = TextOverflow.Ellipsis
      )
      composable()
    }
  }
}

@Composable
fun ItemAction(text: String, icon: ImageVector, onClick: () -> Unit, color: Color = Color.Unspecified) {
  val finalColor = if (color == Color.Unspecified) {
    MenuTextColor
  } else color
  DropdownMenuItem(onClick, contentPadding = PaddingValues(horizontal = DEFAULT_PADDING * 1.5f)) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text(
        text,
        modifier = Modifier
          .fillMaxWidth()
          .weight(1F)
          .padding(end = 15.dp),
        color = finalColor
      )
      Icon(icon, text, tint = finalColor)
    }
  }
}

@Composable
fun ItemAction(text: String, color: Color = Color.Unspecified, onClick: () -> Unit) {
  val finalColor = if (color == Color.Unspecified) {
    MenuTextColor
  } else color
  DropdownMenuItem(onClick, contentPadding = PaddingValues(horizontal = DEFAULT_PADDING * 1.5f)) {
    Text(
      text,
      modifier = Modifier
        .fillMaxWidth()
        .weight(1F)
        .padding(end = 15.dp),
      color = finalColor
    )
  }
}

@Composable
fun Modifier.chatItemOffset(cItem: ChatItem, tailVisible: Boolean, inverted: Boolean = false, revealed: Boolean): Modifier {
  val chatItemTail = remember { appPreferences.chatItemTail.state }
  val style = shapeStyle(cItem, chatItemTail.value, tailVisible, revealed)

  val offset = if (style is ShapeStyle.Bubble) {
    if (style.tailVisible) {
      if (cItem.chatDir.sent) msgTailWidthDp else -msgTailWidthDp
    } else {
      0.dp
    }
  } else 0.dp

  return this.offset(x = if (inverted) (-1f * offset) else offset)
}

@Composable
fun Modifier.clipChatItem(chatItem: ChatItem? = null, tailVisible: Boolean = false, revealed: Boolean = false): Modifier {
  val chatItemRoundness = remember { appPreferences.chatItemRoundness.state }
  val chatItemTail = remember { appPreferences.chatItemTail.state }
  val style = shapeStyle(chatItem, chatItemTail.value, tailVisible, revealed)
  val cornerRoundness = chatItemRoundness.value.coerceIn(0f, 1f)

  val shape = when (style) {
    is ShapeStyle.Bubble -> chatItemShape(cornerRoundness, LocalDensity.current, style.tailVisible, chatItem?.chatDir?.sent == true)
    is ShapeStyle.RoundRect -> RoundedCornerShape(style.radius * cornerRoundness)
  }

  return this.clip(shape)
}

private fun chatItemShape(roundness: Float, density: Density, tailVisible: Boolean, sent: Boolean = false): GenericShape = GenericShape { size, _ ->
  val (msgTailWidth, msgBubbleMaxRadius) = with(density) { Pair(msgTailWidthDp.toPx(), msgBubbleMaxRadius.toPx()) }
  val width = size.width
  val height = size.height
  val rxMax = min(msgBubbleMaxRadius, width / 2)
  val ryMax = min(msgBubbleMaxRadius, height / 2)
  val rx = roundness * rxMax
  val ry = roundness * ryMax
  val tailHeight = with(density) {
    min(
      msgTailMinHeightDp.toPx() + roundness * (msgTailMaxHeightDp.toPx() - msgTailMinHeightDp.toPx()),
      height / 2
    )
  }
  moveTo(rx, 0f)
  lineTo(width - rx, 0f)
  if (roundness > 0) {
    quadraticBezierTo(width, 0f, width, ry)
  }
  if (height > 2 * ry) {
    lineTo(width, height - ry)
  }
  if (roundness > 0) {
    quadraticBezierTo(width, height, width - rx, height)
  }
  if (tailVisible) {
    lineTo(0f, height)
    if (roundness > 0) {
      val d = tailHeight - msgTailWidth * msgTailWidth / tailHeight
      val controlPoint = Offset(msgTailWidth, height - tailHeight + d * sqrt(roundness))
      quadraticBezierTo(controlPoint.x, controlPoint.y, msgTailWidth, height - tailHeight)
    } else {
      lineTo(msgTailWidth, height - tailHeight)
    }

    if (height > ry + tailHeight) {
      lineTo(msgTailWidth, ry)
    }
  } else {
    lineTo(rx, height)
    if (roundness > 0) {
      quadraticBezierTo(0f, height, 0f, height - ry)
    }
    if (height > 2 * ry) {
      lineTo(0f, ry)
    }
  }
  if (roundness > 0) {
    val bubbleInitialX = if (tailVisible) msgTailWidth else 0f
    quadraticBezierTo(bubbleInitialX, 0f, bubbleInitialX + rx, 0f)
  }

  if (sent) {
    val matrix = Matrix()
    matrix.scale(-1f, 1f)
    this.transform(matrix)
    this.translate(Offset(size.width, 0f))
  }
}

sealed class ShapeStyle {
  data class Bubble(val tailVisible: Boolean, val startPadding: Boolean) : ShapeStyle()
  data class RoundRect(val radius: Dp) : ShapeStyle()
}

val shapeStyle: (chatItem: ChatItem?, tailEnabled: Boolean, tailVisible: Boolean, revealed: Boolean) -> ShapeStyle =
  if (appPlatform.isDesktop || (platform.androidApiLevel ?: 0) > 27) ::shapeStyleWithTail
  else { _, _, _, _ -> ShapeStyle.RoundRect(msgRectMaxRadius) }

fun shapeStyleWithTail(chatItem: ChatItem? = null, tailEnabled: Boolean, tailVisible: Boolean, revealed: Boolean): ShapeStyle {
  if (chatItem == null) {
    return ShapeStyle.RoundRect(msgRectMaxRadius)
  }

  when (chatItem.content) {
    is CIContent.SndMsgContent,
    is CIContent.RcvMsgContent,
    is CIContent.RcvDecryptionError,
    is CIContent.SndDeleted,
    is CIContent.RcvDeleted,
    is CIContent.RcvIntegrityError,
    is CIContent.SndModerated,
    is CIContent.RcvModerated,
    is CIContent.RcvBlocked,
    is CIContent.InvalidJSON -> {
      if (chatItem.meta.itemDeleted != null && (!revealed || chatItem.isDeletedContent)) {
        return ShapeStyle.RoundRect(msgRectMaxRadius)
      }

      val tail = when (val content = chatItem.content.msgContent) {
        is MsgContent.MCImage,
        is MsgContent.MCVideo,
        is MsgContent.MCVoice -> {
          if (content.text.isEmpty()) {
            false
          } else {
            tailVisible
          }
        }
        is MsgContent.MCText -> {
          if (isShortEmoji(content.text)) {
            false
          } else {
            tailVisible
          }
        }
        else -> tailVisible
      }
      return if (tailEnabled) {
        ShapeStyle.Bubble(tail, !chatItem.chatDir.sent)
      } else {
        ShapeStyle.RoundRect(msgRectMaxRadius)
      }
    }

    is CIContent.RcvGroupInvitation,
    is CIContent.SndGroupInvitation -> return ShapeStyle.RoundRect(msgRectMaxRadius)
    else -> return ShapeStyle.RoundRect(8.dp)
  }
}

private fun closeReportsIfNeeded() {
  if (appPlatform.isAndroid && ModalManager.end.isLastModalOpen(ModalViewId.SECONDARY_CHAT)) {
    ModalManager.end.closeModals()
  }
}

fun cancelFileAlertDialog(fileId: Long, cancelFile: (Long) -> Unit, cancelAction: CancelAction) {
  AlertManager.shared.showAlertDialog(
    title = generalGetString(cancelAction.alert.titleId),
    text = generalGetString(cancelAction.alert.messageId),
    confirmText = generalGetString(cancelAction.alert.confirmId),
    destructive = true,
    onConfirm = {
      cancelFile(fileId)
    }
  )
}

fun deleteMessageAlertDialog(chatItem: ChatItem, questionText: String, deleteMessage: (Long, CIDeleteMode) -> Unit) {
  AlertManager.shared.showAlertDialogButtons(
    title = generalGetString(MR.strings.delete_message__question),
    text = questionText,
    buttons = {
      Row(
        Modifier
          .fillMaxWidth()
          .padding(horizontal = 8.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.Center,
      ) {
        TextButton(onClick = {
          deleteMessage(chatItem.id, CIDeleteMode.cidmInternal)
          AlertManager.shared.hideAlert()
        }) { Text(stringResource(MR.strings.for_me_only), color = MaterialTheme.colors.error) }
        if (chatItem.meta.deletable && !chatItem.localNote && !chatItem.isReport) {
          Spacer(Modifier.padding(horizontal = 4.dp))
          TextButton(onClick = {
            deleteMessage(chatItem.id, CIDeleteMode.cidmBroadcast)
            AlertManager.shared.hideAlert()
          }) { Text(stringResource(MR.strings.for_everybody), color = MaterialTheme.colors.error) }
        }
      }
    }
  )
}

fun deleteMessagesAlertDialog(itemIds: List<Long>, questionText: String, forAll: Boolean, deleteMessages: (List<Long>, Boolean) -> Unit) {
  AlertManager.shared.showAlertDialogButtons(
    title = generalGetString(MR.strings.delete_messages__question).format(itemIds.size),
    text = questionText,
    buttons = {
      Row(
        Modifier
          .fillMaxWidth()
          .padding(horizontal = 8.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.Center,
      ) {
        TextButton(onClick = {
          deleteMessages(itemIds, false)
          AlertManager.shared.hideAlert()
        }) { Text(stringResource(MR.strings.for_me_only), color = MaterialTheme.colors.error) }

        if (forAll) {
          TextButton(onClick = {
            deleteMessages(itemIds, true)
            AlertManager.shared.hideAlert()
          }) { Text(stringResource(MR.strings.for_everybody), color = MaterialTheme.colors.error) }
        }
      }
    }
  )
}

fun moderateMessageQuestionText(fullDeleteAllowed: Boolean, count: Int): String {
  return if (fullDeleteAllowed) {
    generalGetString(if (count == 1) MR.strings.moderate_message_will_be_deleted_warning else MR.strings.moderate_messages_will_be_deleted_warning)
  } else {
    generalGetString(if (count == 1) MR.strings.moderate_message_will_be_marked_warning else MR.strings.moderate_messages_will_be_marked_warning)
  }
}

fun moderateMessageAlertDialog(chatItem: ChatItem, questionText: String, deleteMessage: (Long, CIDeleteMode) -> Unit) {
  AlertManager.shared.showAlertDialog(
    title = generalGetString(MR.strings.delete_member_message__question),
    text = questionText,
    confirmText = generalGetString(MR.strings.delete_verb),
    destructive = true,
    onConfirm = {
      deleteMessage(chatItem.id, CIDeleteMode.cidmBroadcast)
    }
  )
}

fun moderateMessagesAlertDialog(itemIds: List<Long>, questionText: String, deleteMessages: (List<Long>) -> Unit) {
  AlertManager.shared.showAlertDialog(
    title = if (itemIds.size == 1) generalGetString(MR.strings.delete_member_message__question) else generalGetString(MR.strings.delete_members_messages__question).format(itemIds.size),
    text = questionText,
    confirmText = generalGetString(MR.strings.delete_verb),
    destructive = true,
    onConfirm = { deleteMessages(itemIds) }
  )
}

expect fun copyItemToClipboard(cItem: ChatItem, clipboard: ClipboardManager)

@Preview
@Composable
fun PreviewChatItemView(
  chatItem: ChatItem = ChatItem.getSampleData(1, CIDirection.DirectSnd(), Clock.System.now(), "hello")
) {
  ChatItemView(
    chatsCtx = ChatModel.ChatsContext(secondaryContextFilter = null),
    rhId = null,
    Chat.sampleData,
    chatItem,
    useLinkPreviews = true,
    linkMode = SimplexLinkMode.DESCRIPTION,
    composeState = remember { mutableStateOf(ComposeState(useLinkPreviews = true)) },
    revealed = remember { mutableStateOf(false) },
    highlighted = remember { mutableStateOf(false) },
    hoveredItemId = remember { mutableStateOf(null) },
    range = remember { mutableStateOf(0..1) },
    selectedChatItems = remember { mutableStateOf(setOf()) },
    searchIsNotBlank = remember { mutableStateOf(false) },
    selectChatItem = {},
    deleteMessage = { _, _ -> },
    deleteMessages = { _ -> },
    archiveReports = { _, _ -> },
    receiveFile = { _ -> },
    cancelFile = {},
    joinGroup = { _, _ -> },
    acceptCall = { _ -> },
    scrollToItem = {},
    scrollToItemId = remember { mutableStateOf(null) },
    scrollToQuotedItemFromItem = {},
    acceptFeature = { _, _, _ -> },
    openDirectChat = { _ -> },
    forwardItem = { _, _ -> },
    updateContactStats = { },
    updateMemberStats = { _, _ -> },
    syncContactConnection = { },
    syncMemberConnection = { _, _ -> },
    findModelChat = { null },
    findModelMember = { null },
    setReaction = { _, _, _, _ -> },
    showItemDetails = { _, _ -> },
    reveal = {},
    showMemberInfo = { _, _ ->},
    showChatInfo = {},
    developerTools = false,
    showViaProxy = false,
    showTimestamp = true,
    preview = true,
    itemSeparation = ItemSeparation(timestamp = true, largeGap = true, null)
  )
}

@Preview
@Composable
fun PreviewChatItemViewDeletedContent() {
  SimpleXTheme {
    ChatItemView(
      chatsCtx = ChatModel.ChatsContext(secondaryContextFilter = null),
      rhId = null,
      Chat.sampleData,
      ChatItem.getDeletedContentSampleData(),
      useLinkPreviews = true,
      linkMode = SimplexLinkMode.DESCRIPTION,
      composeState = remember { mutableStateOf(ComposeState(useLinkPreviews = true)) },
      revealed = remember { mutableStateOf(false) },
      highlighted = remember { mutableStateOf(false) },
      hoveredItemId = remember { mutableStateOf(null) },
      range = remember { mutableStateOf(0..1) },
      selectedChatItems = remember { mutableStateOf(setOf()) },
      searchIsNotBlank = remember { mutableStateOf(false) },
      selectChatItem = {},
      deleteMessage = { _, _ -> },
      deleteMessages = { _ -> },
      archiveReports = { _, _ -> },
      receiveFile = { _ -> },
      cancelFile = {},
      joinGroup = { _, _ -> },
      acceptCall = { _ -> },
      scrollToItem = {},
      scrollToItemId = remember { mutableStateOf(null) },
      scrollToQuotedItemFromItem = {},
      acceptFeature = { _, _, _ -> },
      openDirectChat = { _ -> },
      forwardItem = { _, _ -> },
      updateContactStats = { },
      updateMemberStats = { _, _ -> },
      syncContactConnection = { },
      syncMemberConnection = { _, _ -> },
      findModelChat = { null },
      findModelMember = { null },
      setReaction = { _, _, _, _ -> },
      showItemDetails = { _, _ -> },
      reveal = {},
      showMemberInfo = { _, _ ->},
      showChatInfo = {},
      developerTools = false,
      showViaProxy = false,
      preview = true,
      showTimestamp = true,
      itemSeparation = ItemSeparation(timestamp = true, largeGap = true, null)
    )
  }
}