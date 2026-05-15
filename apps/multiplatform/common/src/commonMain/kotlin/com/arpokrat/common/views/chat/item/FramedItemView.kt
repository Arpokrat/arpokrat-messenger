package com.arpokrat.common.views.chat.item

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.arpokrat.common.model.*
import com.arpokrat.common.platform.*
import com.arpokrat.common.ui.theme.*
import com.arpokrat.common.views.helpers.*
import com.arpokrat.common.wallet.CryptoProtocol
import com.arpokrat.res.MR
import dev.icerock.moko.resources.compose.painterResource
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.ceil

@Composable
fun FramedItemView(
  chatsCtx: ChatModel.ChatsContext,
  chat: Chat,
  ci: ChatItem,
  uriHandler: UriHandler? = null,
  imageProvider: (() -> ImageGalleryProvider)? = null,
  linkMode: SimplexLinkMode,
  showViaProxy: Boolean,
  showMenu: MutableState<Boolean>,
  showTimestamp: Boolean,
  tailVisible: Boolean = false,
  receiveFile: (Long) -> Unit,
  onLinkLongClick: (link: String) -> Unit = {},
  scrollToItem: (Long) -> Unit = {},
  scrollToItemId: MutableState<Long?>,
  scrollToQuotedItemFromItem: (Long) -> Unit = {},
) {
  val chatInfo = chat.chatInfo
  val sent = ci.chatDir.sent
  val chatTTL = chatInfo.timedMessagesTTL

  fun membership(): GroupMember? {
    return if (chatInfo is ChatInfo.Group) chatInfo.groupInfo.membership else null
  }

  @Composable
  fun ciQuotedMsgTextView(qi: CIQuote, lines: Int, showTimestamp: Boolean) {
    val (displayText, displayFormatted) = if (CryptoProtocol.isInvoice(qi.text)) {
      val invoice = CryptoProtocol.parseInvoice(qi.text)
      if (invoice != null) {
        val text = String.format(generalGetString(MR.strings.crypto_invoice_preview), invoice.amount, invoice.symbol)
        text to FormattedText.plain(text)
      } else {
        qi.text to qi.formattedText
      }
    } else if (CryptoProtocol.isPaid(qi.text)) {
      val text = generalGetString(MR.strings.crypto_invoice_paid_preview)
      text to FormattedText.plain(text)
    } else if (CryptoProtocol.isCancelled(qi.text)) {
      val text = generalGetString(MR.strings.crypto_invoice_cancelled_preview)
      text to FormattedText.plain(text)
    } else if (CryptoProtocol.isDeclined(qi.text)) {
      val text = generalGetString(MR.strings.crypto_invoice_declined_preview)
      text to FormattedText.plain(text)
    } else {
      qi.text to qi.formattedText
    }

    MarkdownText(
      text = displayText,
      formattedText = displayFormatted,
      toggleSecrets = true,
      maxLines = lines,
      overflow = TextOverflow.Ellipsis,
      style = TextStyle(fontSize = 14.sp, color = MaterialTheme.colors.onSurface.copy(alpha = 0.9f)),
      linkMode = linkMode,
      uriHandler = if (appPlatform.isDesktop) uriHandler else null,
      showTimestamp = showTimestamp,
    )
  }

  @Composable
  fun ciQuotedMsgView(qi: CIQuote) {
    Box(
      Modifier
        .widthIn(max = 50000.dp)
        .padding(vertical = 4.dp, horizontal = 8.dp),
      contentAlignment = Alignment.TopStart
    ) {
      val sender = qi.sender(membership())
      if (sender != null) {
        Column(
          horizontalAlignment = Alignment.Start
        ) {
          Text(
            sender,
            style = TextStyle(
              fontSize = 13.sp,
              fontWeight = FontWeight.Medium,
              color = if (qi.chatDir is CIDirection.GroupSnd) MaterialTheme.colors.primary else MaterialTheme.colors.secondary
            ),
            maxLines = 1
          )
          ciQuotedMsgTextView(qi, lines = 2,  showTimestamp = showTimestamp)
        }
      } else {
        ciQuotedMsgTextView(qi, lines = 3,  showTimestamp = showTimestamp)
      }
    }
  }

  @Composable
  fun FramedItemHeader(caption: String, italic: Boolean, icon: Painter? = null, pad: Boolean = false, iconColor: Color? = null) {
    val sentColor = MaterialTheme.appColors.sentQuote
    val receivedColor = MaterialTheme.appColors.receivedQuote

    Row(
      Modifier
        .padding(horizontal = 4.dp, vertical = 2.dp)
        .clip(RoundedCornerShape(8.dp))
        .background(if (sent) sentColor else receivedColor)
        .fillMaxWidth()
        .padding(start = 8.dp, top = 6.dp, end = 12.dp, bottom = if (pad || (ci.quotedItem == null && ci.meta.itemForwarded == null)) 6.dp else 6.dp),
      horizontalArrangement = Arrangement.spacedBy(4.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      if (icon != null) {
        Icon(
          icon,
          caption,
          Modifier.size(16.dp),
          tint = iconColor ?: if (isInDarkTheme()) FileDark else FileLight
        )
      }
      Text(
        buildAnnotatedString {
          withStyle(SpanStyle(fontSize = 12.sp, fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal, color = MaterialTheme.colors.secondary)) {
            append(caption)
          }
        },
        style = MaterialTheme.typography.body1.copy(lineHeight = 22.sp),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
  }

  @Composable
  fun ciQuoteView(qi: CIQuote) {
    val sentColor = MaterialTheme.appColors.sentQuote
    val receivedColor = MaterialTheme.appColors.receivedQuote

    Row(
      Modifier
        .padding(start = 6.dp, end = 6.dp, top = 4.dp, bottom = 4.dp)
        .clip(RoundedCornerShape(6.dp))
        .background(if (sent) sentColor else receivedColor)
        .fillMaxWidth()
        .height(IntrinsicSize.Min)
    ) {
      Box(
        Modifier
          .fillMaxHeight()
          .width(4.dp)
          .background(MaterialTheme.colors.primary)
      )

      when (qi.content) {
        is MsgContent.MCImage -> {
          Box(Modifier.fillMaxWidth().weight(1f)) {
            ciQuotedMsgView(qi)
          }
          val imageBitmap = base64ToBitmap(qi.content.image)
          Image(
            imageBitmap,
            contentDescription = stringResource(MR.strings.image_descr),
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(56.dp).padding(4.dp).clip(RoundedCornerShape(4.dp))
          )
        }
        is MsgContent.MCVideo -> {
          Box(Modifier.fillMaxWidth().weight(1f)) {
            ciQuotedMsgView(qi)
          }
          val imageBitmap = base64ToBitmap(qi.content.image)
          Image(
            imageBitmap,
            contentDescription = stringResource(MR.strings.video_descr),
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(56.dp).padding(4.dp).clip(RoundedCornerShape(4.dp))
          )
        }
        is MsgContent.MCFile, is MsgContent.MCVoice -> {
          Box(Modifier.fillMaxWidth().weight(1f)) {
            ciQuotedMsgView(qi)
          }
          Icon(
            if (qi.content is MsgContent.MCFile) painterResource(MR.images.ic_draft_filled) else painterResource(MR.images.ic_mic_filled),
            if (qi.content is MsgContent.MCFile) stringResource(MR.strings.icon_descr_file) else stringResource(MR.strings.voice_message),
            Modifier
              .padding(top = 6.dp, end = 8.dp)
              .size(20.dp),
            tint = if (isInDarkTheme()) FileDark else FileLight
          )
        }
        else -> ciQuotedMsgView(qi)
      }
    }
  }

  @Composable
  fun ciFileView(ci: ChatItem, text: String) {
    CIFileView(ci.file, ci.meta.itemEdited, showMenu, false, receiveFile)
    if (text != "" || ci.meta.isLive) {
      CIMarkdownText(chatsCtx, ci, chat, chatTTL, linkMode = linkMode, uriHandler, showViaProxy = showViaProxy,  showTimestamp = showTimestamp)
    }
  }

  val transparentBackground = (ci.content.msgContent is MsgContent.MCImage || ci.content.msgContent is MsgContent.MCVideo) &&
      !ci.meta.isLive && ci.content.text.isEmpty() && ci.quotedItem == null && ci.meta.itemForwarded == null

  val sentColor = MaterialTheme.appColors.sentMessage
  val receivedColor = MaterialTheme.appColors.receivedMessage

  Box(Modifier
    .clipChatItem(ci, tailVisible, revealed = true)
    .background(
      when {
        transparentBackground -> Color.Transparent
        sent -> sentColor
        else -> receivedColor
      }
    )) {

    var metaColor = if (isInDarkTheme()) MaterialTheme.colors.onSurface.copy(alpha = 0.6f) else MaterialTheme.colors.secondary

    Box(contentAlignment = Alignment.BottomEnd) {
      val chatItemTail = remember { appPreferences.chatItemTail.state }
      val style = shapeStyle(ci, chatItemTail.value, tailVisible, true)
      val tailRendered = style is ShapeStyle.Bubble && style.tailVisible
      Column(
        Modifier
          .width(IntrinsicSize.Max)
          .padding(start = if (!sent && tailRendered) msgTailWidthDp else 0.dp, end = if (sent && tailRendered) msgTailWidthDp else 0.dp)
      ) {
        PriorityLayout(Modifier, CHAT_IMAGE_LAYOUT_ID) {
          @Composable
          fun Header() {
            if (ci.isReport) {
              if (ci.meta.itemDeleted == null) {
                FramedItemHeader(
                  stringResource(if (ci.chatDir.sent) MR.strings.report_item_visibility_submitter else MR.strings.report_item_visibility_moderators),
                  true,
                  painterResource(MR.images.ic_flag),
                  iconColor = MaterialTheme.colors.error
                )
              } else {
                val text = if (ci.meta.itemDeleted is CIDeleted.Moderated && ci.meta.itemDeleted.byGroupMember.groupMemberId != (chatInfo as ChatInfo.Group?)?.groupInfo?.membership?.groupMemberId) {
                  stringResource(MR.strings.report_item_archived_by).format(ci.meta.itemDeleted.byGroupMember.displayName)
                } else {
                  stringResource(MR.strings.report_item_archived)
                }
                FramedItemHeader(text, true, painterResource(MR.images.ic_flag))
              }
            } else if (ci.meta.itemDeleted != null) {
              when (ci.meta.itemDeleted) {
                is CIDeleted.Moderated -> {
                  FramedItemHeader(String.format(stringResource(MR.strings.moderated_item_description), ci.meta.itemDeleted.byGroupMember.chatViewName), true, painterResource(MR.images.ic_flag))
                }
                is CIDeleted.Blocked -> {
                  FramedItemHeader(stringResource(MR.strings.blocked_item_description), true, painterResource(MR.images.ic_back_hand))
                }
                is CIDeleted.BlockedByAdmin -> {
                  FramedItemHeader(stringResource(MR.strings.blocked_by_admin_item_description), true, painterResource(MR.images.ic_back_hand))
                }
                is CIDeleted.Deleted -> {
                  FramedItemHeader(stringResource(MR.strings.marked_deleted_description), true, painterResource(MR.images.ic_delete))
                }
              }
            } else if (ci.meta.isLive) {
              FramedItemHeader(stringResource(MR.strings.live), false)
            }
          }
          if (ci.quotedItem != null) {
            Column(
              Modifier
                .combinedClickable(
                  onLongClick = { showMenu.value = true },
                  onClick = {
                    if (ci.quotedItem.itemId != null) {
                      if (ci.isReport && chatsCtx.secondaryContextFilter != null) {
                        scrollToItemId.value = ci.quotedItem.itemId
                      } else {
                        scrollToItem(ci.quotedItem.itemId)
                      }
                    } else {
                      scrollToQuotedItemFromItem(ci.id)
                    }
                  }
                )
                .onRightClick { showMenu.value = true }
            ) {
              Header()
              ciQuoteView(ci.quotedItem)
            }
          } else {
            Header()
            if (ci.meta.itemForwarded != null) {
              FramedItemHeader(ci.meta.itemForwarded.text(chatInfo.chatType), true, painterResource(MR.images.ic_forward), pad = true)
            }
          }
          if (ci.file == null && ci.formattedText == null && !ci.meta.isLive && isShortEmoji(ci.content.text)) {
            Box(Modifier.padding(vertical = 6.dp, horizontal = 12.dp)) {
              Column(
                Modifier
                  .padding(bottom = 2.dp)
                  .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
              ) {
                EmojiText(ci.content.text)
                Text("")
              }
            }
          } else {
            when (val mc = ci.content.msgContent) {
              is MsgContent.MCImage -> {
                CIImageView(image = mc.image, file = ci.file, imageProvider ?: return@PriorityLayout, showMenu, false, receiveFile)
                if (mc.text == "" && !ci.meta.isLive) {
                  metaColor = Color.White
                } else {
                  CIMarkdownText(chatsCtx, ci, chat, chatTTL, linkMode, uriHandler, showViaProxy = showViaProxy, showTimestamp = showTimestamp)
                }
              }
              is MsgContent.MCVideo -> {
                CIVideoView(image = mc.image, mc.duration, file = ci.file, imageProvider ?: return@PriorityLayout, showMenu, smallView = false, receiveFile = receiveFile)
                if (mc.text == "" && !ci.meta.isLive) {
                  metaColor = Color.White
                } else {
                  CIMarkdownText(chatsCtx, ci, chat, chatTTL, linkMode, uriHandler, showViaProxy = showViaProxy, showTimestamp = showTimestamp)
                }
              }
              is MsgContent.MCVoice -> {
                CIVoiceView(mc.duration, ci.file, ci.meta.itemEdited, ci.chatDir.sent, hasText = true, ci, timedMessagesTTL = chatTTL, showViaProxy = showViaProxy, showTimestamp = showTimestamp, longClick = { onLinkLongClick("") }, receiveFile = receiveFile)
                if (mc.text != "") {
                  CIMarkdownText(chatsCtx, ci, chat, chatTTL, linkMode, uriHandler, showViaProxy = showViaProxy, showTimestamp = showTimestamp)
                }
              }
              is MsgContent.MCFile -> ciFileView(ci, mc.text)
              is MsgContent.MCUnknown ->
                if (ci.file == null) {
                  CIMarkdownText(chatsCtx, ci, chat, chatTTL, linkMode, uriHandler, onLinkLongClick, showViaProxy = showViaProxy, showTimestamp = showTimestamp)
                } else {
                  ciFileView(ci, mc.text)
                }
              is MsgContent.MCLink -> {
                ChatItemLinkView(mc.preview, showMenu, onLongClick = { showMenu.value = true })
                Box(Modifier.widthIn(max = DEFAULT_MAX_IMAGE_WIDTH)) {
                  CIMarkdownText(chatsCtx, ci, chat, chatTTL, linkMode, uriHandler, onLinkLongClick, showViaProxy = showViaProxy, showTimestamp = showTimestamp)
                }
              }
              is MsgContent.MCReport -> {
                val prefix = buildAnnotatedString {
                  withStyle(SpanStyle(color = MaterialTheme.colors.error, fontStyle = FontStyle.Italic)) {
                    append(if (mc.text.isEmpty()) mc.reason.text else "${mc.reason.text}: ")
                  }
                }
                CIMarkdownText(chatsCtx, ci, chat, chatTTL, linkMode, uriHandler, onLinkLongClick, showViaProxy = showViaProxy, showTimestamp = showTimestamp, prefix = prefix)
              }
              else -> CIMarkdownText(chatsCtx, ci, chat, chatTTL, linkMode, uriHandler, onLinkLongClick, showViaProxy = showViaProxy, showTimestamp = showTimestamp)
            }
          }
        }
      }

      Box(
        Modifier
          .padding(
            bottom = 6.dp,
            end = 12.dp + if (tailRendered && sent) msgTailWidthDp else 0.dp,
          )
      ) {
        if (transparentBackground) {
          CIMetaView(
            chatItem = ci,
            timedMessagesTTL = chatTTL,
            metaColor = Color.White,
            showViaProxy = showViaProxy,
            showTimestamp = showTimestamp
          )
        } else {
          CIMetaView(
            chatItem = ci,
            timedMessagesTTL = chatTTL,
            metaColor = metaColor,
            showViaProxy = showViaProxy,
            showTimestamp = showTimestamp
          )
        }
      }
    }
  }
}

@Composable
fun CIMarkdownText(
  chatsCtx: ChatModel.ChatsContext,
  ci: ChatItem,
  chat: Chat,
  chatTTL: Int?,
  linkMode: SimplexLinkMode,
  uriHandler: UriHandler?,
  onLinkLongClick: (link: String) -> Unit = {},
  showViaProxy: Boolean,
  showTimestamp: Boolean,
  prefix: AnnotatedString? = null
) {
  Box(Modifier.padding(vertical = 9.dp, horizontal = 14.dp)) {
    val chatInfo = chat.chatInfo
    val text = if (ci.meta.isLive) ci.content.msgContent?.text ?: ci.text else ci.text
    MarkdownText(
      text, if (text.isEmpty()) emptyList() else ci.formattedText, toggleSecrets = true,
      sendCommandMsg = if (chatInfo.useCommands && chat.chatInfo.sndReady) { { msg -> sendCommandMsg(chatsCtx, chat, msg) } } else null,
      meta = ci.meta, chatTTL = chatTTL, linkMode = linkMode,
      mentions = ci.mentions, userMemberId = when {
        chatInfo is ChatInfo.Group -> chatInfo.groupInfo.membership.memberId
        else -> null
      },
      uriHandler = uriHandler, senderBold = true, onLinkLongClick = onLinkLongClick, showViaProxy = showViaProxy, showTimestamp = showTimestamp, prefix = prefix
    )
  }
}

fun sendCommandMsg(chatsCtx: ChatModel.ChatsContext, chat: Chat, msg: String) {
  if (chat.chatInfo.sndReady) {
    withLongRunningApi(slow = 60_000) {
      val cInfo = chat.chatInfo
      val chatItems =
        chatModel.controller.apiSendMessages(
          rh = chat.remoteHostId,
          type = cInfo.chatType,
          id = cInfo.apiId,
          scope = cInfo.groupChatScope(),
          composedMessages = listOf(ComposedMessage(fileSource = null, quotedItemId = null, msgContent = MsgContent.MCText(msg), mentions = emptyMap()))
        )
      if (!chatItems.isNullOrEmpty()) {
        chatItems.forEach { aChatItem ->
          withContext(Dispatchers.Main) {
            chatsCtx.addChatItem(chat.remoteHostId, aChatItem.chatInfo, aChatItem.chatItem)
          }
        }
      }
    }
  } else {
    AlertManager.shared.showAlertMsg(MR.strings.cant_send_message_alert_title, MR.strings.cant_send_commands_alert_text)
  }
}


const val CHAT_IMAGE_LAYOUT_ID = "chatImage"
const val CHAT_BUBBLE_LAYOUT_ID = "chatBubble"
const val CHAT_COMPOSE_LAYOUT_ID = "chatCompose"
const val CONSOLE_COMPOSE_LAYOUT_ID = "consoleCompose"

private fun horizontalPaddingAroundCustomLayouts(density: Float): Int =
  36 * ceil(density).toInt()

@Composable
fun PriorityLayout(
  modifier: Modifier = Modifier,
  priorityLayoutId: String,
  content: @Composable () -> Unit
) {
  Layout(
    content = content,
    modifier = modifier
  ) { measureable, constraints ->
    val imagePlaceable = measureable.firstOrNull { it.layoutId == priorityLayoutId }?.measure(constraints)
    val placeables: List<Placeable> = measureable.map {
      if (it.layoutId == priorityLayoutId)
        imagePlaceable!!
      else
        it.measure(constraints.copy(maxWidth = imagePlaceable?.width ?: constraints.maxWidth)) }
    val width = imagePlaceable?.measuredWidth ?: placeables.maxOf { it.width }
    val height = placeables.sumOf { it.height }
    val adjustedConstraints = Constraints.fitPrioritizingHeight(constraints.minWidth, width, constraints.minHeight, height)
    layout(
      if (width > adjustedConstraints.maxWidth) adjustedConstraints.maxWidth - horizontalPaddingAroundCustomLayouts(density) else adjustedConstraints.maxWidth,
      adjustedConstraints.maxHeight
    ) {
      var y = 0
      placeables.forEach {
        it.place(0, y)
        y += it.measuredHeight
      }
    }
  }
}

@Composable
fun DependentLayout(
  modifier: Modifier = Modifier,
  mainLayoutId: String,
  content: @Composable () -> Unit
) {
  Layout(
    content = content,
    modifier = modifier
  ) { measureable, constraints ->
    val mainPlaceable = measureable.firstOrNull { it.layoutId == mainLayoutId }?.measure(constraints)
    val placeables: List<Placeable> = measureable.map {
      if (it.layoutId == mainLayoutId)
        mainPlaceable!!
      else
        it.measure(constraints.copy(minWidth = mainPlaceable?.width ?: 0, maxWidth = constraints.maxWidth)) }
    val width = mainPlaceable?.measuredWidth ?: placeables.maxOf { it.width }
    val height = placeables.sumOf { it.height }
    val adjustedConstraints = Constraints.fitPrioritizingHeight(constraints.minWidth, width, constraints.minHeight, height)
    layout(
      if (width > adjustedConstraints.maxWidth) adjustedConstraints.maxWidth - horizontalPaddingAroundCustomLayouts(density) else adjustedConstraints.maxWidth,
      adjustedConstraints.maxHeight
    ) {
      var y = 0
      placeables.forEach {
        it.place(0, y)
        y += it.measuredHeight
      }
    }
  }
}

@Composable
fun AdaptingBottomPaddingLayout(
  modifier: Modifier = Modifier,
  mainLayoutId: String,
  expectedHeight: MutableState<Dp>,
  content: @Composable () -> Unit
) {
  val expected = with(LocalDensity.current) { expectedHeight.value.roundToPx() }
  Layout(
    content = content,
    modifier = modifier
  ) { measureable, constraints ->
    require(measureable.size <= 2) { "Should be exactly one or two elements in this layout, you have ${measureable.size}" }
    val mainPlaceable = measureable.firstOrNull { it.layoutId == mainLayoutId }!!.measure(constraints)
    val placeables: List<Placeable> = measureable.map {
      if (it.layoutId == mainLayoutId)
        mainPlaceable
      else
        it.measure(constraints.copy(maxHeight = if (expected != mainPlaceable.measuredHeight) constraints.maxHeight - mainPlaceable.measuredHeight + expected else constraints.maxHeight)) }
    expectedHeight.value = mainPlaceable.measuredHeight.toDp()
    layout(constraints.maxWidth, constraints.maxHeight) {
      var y = 0
      placeables.forEach {
        if (it !== mainPlaceable) {
          it.place(0, y)
          y += it.measuredHeight
        } else {
          it.place(0, constraints.maxHeight - mainPlaceable.measuredHeight)
          y += it.measuredHeight
        }
      }
    }
  }
}

@Composable
fun CenteredRowLayout(
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit
) {
  Layout(
    content = content,
    modifier = modifier
  ) { measureable, constraints ->
    require(measureable.size == 3) { "Should be exactly three elements in this layout, you have ${measureable.size}" }
    val first = measureable[0].measure(constraints.copy(minWidth = 0, minHeight = 0))
    val third = measureable[2].measure(constraints.copy(minWidth = first.measuredWidth, minHeight = 0))
    val second = measureable[1].measure(constraints.copy(minWidth = 0, minHeight = 0, maxWidth = (constraints.maxWidth - first.measuredWidth - third.measuredWidth).coerceAtLeast(0)))
    layout(constraints.maxWidth, constraints.maxHeight) {
      first.place(0, ((constraints.maxHeight - first.measuredHeight) / 2).coerceAtLeast(0))
      second.place((constraints.maxWidth - second.measuredWidth) / 2, ((constraints.maxHeight - second.measuredHeight) / 2).coerceAtLeast(0))
      third.place(constraints.maxWidth - third.measuredWidth, ((constraints.maxHeight - third.measuredHeight) / 2).coerceAtLeast(0))
    }
  }
}

fun showQuotedItemDoesNotExistAlert() {
  AlertManager.shared.showAlertMsg(
    title = generalGetString(MR.strings.message_deleted_or_not_received_error_title),
    text = generalGetString(MR.strings.message_deleted_or_not_received_error_desc)
  )
}