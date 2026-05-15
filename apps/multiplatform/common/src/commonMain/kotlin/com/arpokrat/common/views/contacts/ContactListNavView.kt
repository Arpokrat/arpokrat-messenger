package com.arpokrat.common.views.contacts

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import dev.icerock.moko.resources.compose.painterResource
import dev.icerock.moko.resources.compose.stringResource
import com.arpokrat.common.model.*
import com.arpokrat.common.platform.*
import com.arpokrat.common.views.chat.*
import com.arpokrat.common.views.chat.item.ItemAction
import com.arpokrat.common.views.chatlist.*
import com.arpokrat.common.views.helpers.*
import com.arpokrat.common.views.newchat.ContactType
import com.arpokrat.common.views.newchat.chatContactType
import com.arpokrat.res.MR
import kotlinx.coroutines.delay
import androidx.compose.material.MaterialTheme

fun onRequestAccepted(chat: Chat) {
    val chatInfo = chat.chatInfo
    if (chatInfo is ChatInfo.Direct) {
        ModalManager.start.closeModals()
        if (chatInfo.contact.sndReady) {
            withApi {
                openLoadedChat(chat)
            }
        }
    }
}

@Composable
fun ContactListNavLinkView(chat: Chat, nextChatSelected: State<Boolean>, showDeletedChatIcon: Boolean) {
    val showMenu = remember { mutableStateOf(false) }
    val rhId = chat.remoteHostId
    val disabled = chatModel.chatRunning.value == false || chatModel.deletedChats.value.contains(rhId to chat.chatInfo.id)
    val contactType = chatContactType(chat)

    LaunchedEffect(chat.id) {
        showMenu.value = false
        delay(500L)
    }

    val selectedChat = remember(chat.id) { derivedStateOf { chat.id == chatModel.chatId.value } }
    val view = LocalMultiplatformView()

    when (chat.chatInfo) {
        is ChatInfo.Direct -> {
            ChatListNavLinkLayout(
                chatLinkPreview = {
                    tryOrShowError("${chat.id}ContactListNavLink", error = { ErrorChatListItem() }) {
                        ContactPreviewView(chat, disabled, showDeletedChatIcon)
                    }
                },
                click = {
                    hideKeyboard(view)
                    when (contactType) {
                        ContactType.RECENT, ContactType.CONTACT_WITH_REQUEST, ContactType.CHAT_DELETED -> {
                            withApi {
                                openChat(secondaryChatsCtx = null, rhId, chat.chatInfo)
                                ModalManager.start.closeModals()
                            }
                        }
                        ContactType.CARD -> {
                            askCurrentOrIncognitoProfileConnectContactViaAddress(
                                chatModel,
                                rhId,
                                chat.chatInfo.contact,
                                close = { ModalManager.start.closeModals() },
                                openChat = true
                            )
                        }
                        else -> {}
                    }
                },
                dropdownMenuItems = {
                    tryOrShowError("${chat.id}ContactListNavLinkDropdown", error = {}) {
                        if (contactType == ContactType.CONTACT_WITH_REQUEST) {
                            if (chat.chatInfo.contact.contactRequestId != null) {
                                ContactRequestMenuItems(
                                    rhId = chat.remoteHostId,
                                    contactRequestId = chat.chatInfo.contact.contactRequestId,
                                    chatModel = chatModel,
                                    showMenu = showMenu,
                                    onSuccess = { onRequestAccepted(it) }
                                )
                            } else if (chat.chatInfo.contact.groupDirectInv != null && !chat.chatInfo.contact.groupDirectInv.memberRemoved) {
                                MemberContactRequestMenuItems(
                                    rhId = chat.remoteHostId,
                                    contact = chat.chatInfo.contact,
                                    showMenu = showMenu,
                                    onSuccess = { onRequestAccepted(it) }
                                )
                            } else {
                                DeleteContactAction(chat, chatModel, showMenu)
                            }
                        } else {
                            DeleteContactAction(chat, chatModel, showMenu)
                        }
                    }
                },
                showMenu,
                disabled,
                selectedChat,
                nextChatSelected,
            )
        }
        is ChatInfo.ContactRequest -> {
            ChatListNavLinkLayout(
                chatLinkPreview = {
                    tryOrShowError("${chat.id}ContactListNavLink", error = { ErrorChatListItem() }) {
                        ContactPreviewView(chat, disabled, showDeletedChatIcon)
                    }
                },
                click = {
                    hideKeyboard(view)
                    contactRequestAlertDialog(
                        rhId,
                        chat.chatInfo,
                        chatModel,
                        onSucess = { onRequestAccepted(it) }
                    )
                },
                dropdownMenuItems = {
                    tryOrShowError("${chat.id}ContactListNavLinkDropdown", error = {}) {
                        ContactRequestMenuItems(
                            rhId = chat.remoteHostId,
                            contactRequestId = chat.chatInfo.apiId,
                            chatModel = chatModel,
                            showMenu = showMenu,
                            onSuccess = { onRequestAccepted(it) }
                        )
                    }
                },
                showMenu,
                disabled,
                selectedChat,
                nextChatSelected)
        }
        else -> {}
    }
}

@Composable
fun DeleteContactAction(chat: Chat, chatModel: ChatModel, showMenu: MutableState<Boolean>) {
    ItemAction(
        stringResource(MR.strings.delete_contact_menu_action),
        painterResource(MR.images.ic_delete),
        onClick = {
            deleteContactDialog(chat, chatModel)
            showMenu.value = false
        },
        color = MaterialTheme.colors.error
    )
}