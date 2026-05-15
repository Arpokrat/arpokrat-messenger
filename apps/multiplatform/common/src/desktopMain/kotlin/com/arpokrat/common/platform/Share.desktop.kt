package com.arpokrat.common.platform

import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.text.AnnotatedString
import com.arpokrat.common.model.CryptoFile
import com.arpokrat.common.model.decryptCryptoFile
import com.arpokrat.common.views.helpers.*
import com.arpokrat.res.MR
import java.awt.Desktop
import java.io.File
import java.net.URI
import java.net.URLEncoder

actual fun UriHandler.sendEmail(subject: String, body: CharSequence) {
  val subjectEncoded = URLEncoder.encode(subject, "UTF-8").replace("+", "%20")
  val bodyEncoded = URLEncoder.encode(subject, "UTF-8").replace("+", "%20")
  openUri("mailto:?subject=$subjectEncoded&body=$bodyEncoded")
}

actual fun ClipboardManager.shareText(text: String) {
  setText(AnnotatedString(text))
  AppNotificationManager.showSuccess(MR.strings.copied.localized())
}

actual fun shareFile(text: String, fileSource: CryptoFile) {
  withLongRunningApi {
    FileChooserLauncher(false) { to: URI? ->
      if (to != null) {
        val absolutePath = if (fileSource.isAbsolutePath) fileSource.filePath else getAppFilePath(fileSource.filePath)
        if (fileSource.cryptoArgs != null) {
          try {
            decryptCryptoFile(absolutePath, fileSource.cryptoArgs, to.toFile().absolutePath)
            AppNotificationManager.showSuccess(generalGetString(MR.strings.file_saved))
          } catch (e: Exception) {
            Log.e(TAG, "Unable to decrypt crypto file: " + e.stackTraceToString())
            AlertManager.shared.showAlertMsg(title = generalGetString(MR.strings.error), text = e.stackTraceToString())
          }
        } else {
          copyFileToFile(File(absolutePath), to) {}
        }
      }
    }.launch(fileSource.filePath)
  }
}

actual fun openFile(fileSource: CryptoFile) {
  try {
    val filePath = filePathForShare(fileSource) ?: return
    Desktop.getDesktop().open(File(filePath))
  } catch (e: Exception) {
    Log.e(TAG, "Unable to open the file: " + e.stackTraceToString())
    AlertManager.shared.showAlertMsg(title = generalGetString(MR.strings.error), text = e.stackTraceToString())
  }
}

fun filePathForShare(fileSource: CryptoFile): String? {
  return if (fileSource.cryptoArgs != null) {
    val tmpFile = File(tmpDir, fileSource.filePath)
    tmpFile.deleteOnExit()
    try {
      decryptCryptoFile(getAppFilePath(fileSource.filePath), fileSource.cryptoArgs ?: return null, tmpFile.absolutePath)
    } catch (e: Exception) {
      Log.e(TAG, "Unable to decrypt crypto file: " + e.stackTraceToString())
      AlertManager.shared.showAlertMsg(title = generalGetString(MR.strings.error), text = e.stackTraceToString())
      return null
    }
    tmpFile.absolutePath
  } else {
    getAppFilePath(fileSource.filePath)
  }
}
