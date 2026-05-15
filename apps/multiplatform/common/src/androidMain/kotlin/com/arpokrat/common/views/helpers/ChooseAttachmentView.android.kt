package com.arpokrat.common.views.helpers

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arpokrat.common.views.newchat.ActionButton
import com.arpokrat.res.MR
import dev.icerock.moko.resources.compose.painterResource
import dev.icerock.moko.resources.compose.stringResource
import com.arpokrat.common.model.ChatModel
import com.arpokrat.common.model.ChatInfo
import com.arpokrat.common.model.MsgContent
import com.arpokrat.common.model.ComposedMessage
import com.arpokrat.common.wallet.CryptoProtocol
import com.arpokrat.common.wallet.CryptoManager
import com.arpokrat.common.wallet.ui.dashboard.WalletMainView
import com.arpokrat.common.wallet.ui.inchat.RequestPaymentView
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers

@Composable
actual fun ChooseAttachmentButtons(attachmentOption: MutableState<AttachmentOption?>, hide: () -> Unit) {
  val scope = rememberCoroutineScope()

  val walletManager = remember { com.arpokrat.common.wallet.WalletManager() }
  val rawCurrency = walletManager.getCurrency()
  val sym = if (rawCurrency.equals("USD", ignoreCase = true)) "$" else "€"

  ActionButton(Modifier.fillMaxWidth(0.20f), null, stringResource(MR.strings.use_camera_button), icon = painterResource(MR.images.ic_camera_enhance)) {
    attachmentOption.value = AttachmentOption.CameraPhoto
    hide()
  }

  ActionButton(Modifier.fillMaxWidth(0.25f), null, stringResource(MR.strings.gallery_image_button), icon = painterResource(MR.images.ic_add_photo)) {
    attachmentOption.value = AttachmentOption.GalleryImage
    hide()
  }

  ActionButton(Modifier.fillMaxWidth(0.33f), null, stringResource(MR.strings.gallery_video_button), icon = painterResource(MR.images.ic_smart_display)) {
    attachmentOption.value = AttachmentOption.GalleryVideo
    hide()
  }

  ActionButton(Modifier.fillMaxWidth(0.50f), null, stringResource(MR.strings.choose_file), icon = painterResource(MR.images.ic_note_add)) {
    attachmentOption.value = AttachmentOption.File
    hide()
  }

  val currentChatId = ChatModel.chatId.value
  val chatInfoModel = if (currentChatId != null) ChatModel.getChat(currentChatId) else null
  val isDirectChat = chatInfoModel?.chatInfo is ChatInfo.Direct

  if (isDirectChat) {
    ActionButton(
      Modifier.fillMaxWidth(1f),
      null,
      stringResource(MR.strings.attachment_crypto),
      icon = painterResource(MR.images.ic_wallet)
    ) {
      hide()

      if (CryptoManager.isReady()) {
        val isDevMode = walletManager.isDeveloperModeEnabled()

        ModalManager.start.showModalCloseable(
          endButtons = {
            if (isDevMode) {
              Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 16.dp)) {
                com.arpokrat.common.wallet.ui.components.DevModeBadge()
              }
            }
          }
        ) { closeSheet ->
          RequestPaymentView(
            isDevMode = isDevMode,
            currency = rawCurrency,
            currencySymbol = sym,
            onDismiss = { closeSheet() },
            onSendInvoice = { amount, token, address, fiatValue ->
              closeSheet()

              if (chatInfoModel != null && chatInfoModel.chatInfo is ChatInfo.Direct) {
                val messageText = CryptoProtocol.formatInvoice(
                  amount = amount,
                  symbol = token.symbol,
                  address = address,
                  coinType = token.network.id,
                  fiatValue = fiatValue
                )

                scope.launch(Dispatchers.IO) {
                  try {
                    val content = MsgContent.MCText(messageText)
                    val composedMsg = ComposedMessage(null, null, content, emptyMap())
                    ChatModel.controller.apiSendMessages(
                      rh = chatInfoModel.remoteHostId,
                      type = chatInfoModel.chatInfo.chatType,
                      id = chatInfoModel.chatInfo.apiId,
                      scope = null,
                      live = false,
                      ttl = null,
                      composedMessages = listOf(composedMsg)
                    )
                  } catch (e: Exception) {
                  }
                }
              }
            }
          )
        }
      } else {
        ModalManager.fullscreen.showCustomModal { close ->
          WalletMainView(closeWallet = close)
        }
      }
    }
  }
}