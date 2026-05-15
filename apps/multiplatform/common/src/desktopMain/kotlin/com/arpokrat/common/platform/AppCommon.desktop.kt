package com.arpokrat.common.platform

import com.arpokrat.common.model.*
import com.arpokrat.common.simplexWindowState
import com.arpokrat.common.views.call.RcvCallInvitation
import com.arpokrat.common.views.database.deleteOldChatArchive
import com.arpokrat.common.views.helpers.*
import java.util.*
import com.arpokrat.res.MR
import java.io.File

actual val appPlatform = AppPlatform.DESKTOP

actual val deviceName = generalGetString(MR.strings.desktop_device)

actual fun isAppVisibleAndFocused() = simplexWindowState.windowFocused.value

@Suppress("ConstantLocale")
val defaultLocale: Locale = Locale.getDefault()

fun initApp() {
  ntfManager = object : NtfManager() {
    override fun notifyCallInvitation(invitation: RcvCallInvitation): Boolean = com.arpokrat.common.model.NtfManager.notifyCallInvitation(invitation)
    override fun hasNotificationsForChat(chatId: String): Boolean = com.arpokrat.common.model.NtfManager.hasNotificationsForChat(chatId)
    override fun cancelNotificationsForChat(chatId: String) = com.arpokrat.common.model.NtfManager.cancelNotificationsForChat(chatId)
    override fun cancelNotificationsForUser(userId: Long) = com.arpokrat.common.model.NtfManager.cancelNotificationsForUser(userId)
    override fun displayNotification(user: UserLike, chatId: String, displayName: String, msgText: String, image: String?, actions: List<Pair<NotificationAction, () -> Unit>>) = com.arpokrat.common.model.NtfManager.displayNotification(user, chatId, displayName, msgText, image, actions)
    override fun androidCreateNtfChannelsMaybeShowAlert() {}
    override fun cancelCallNotification() {}
    override fun cancelAllNotifications() = com.arpokrat.common.model.NtfManager.cancelAllNotifications()
    override fun showMessage(title: String, text: String) = com.arpokrat.common.model.NtfManager.showMessage(title, text)
  }
  applyAppLocale()
  deleteOldChatArchive()
  if (DatabaseUtils.ksSelfDestructPassword.get() == null) {
    initChatControllerOnStart()
  }
  // LALAL
  //testCrypto()
}

//fun discoverVlcLibs(path: String) {
//  uk.co.caprica.vlcj.binding.LibC.INSTANCE.setenv("VLC_PLUGIN_PATH", path, 1)
//}

private fun applyAppLocale() {
  val lang = ChatController.appPrefs.appLanguage.get()
  if (lang == null || lang == Locale.getDefault().language) return
  Locale.setDefault(Locale.forLanguageTag(lang))
}
