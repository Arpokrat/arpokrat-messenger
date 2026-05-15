package com.arpokrat.common.helpers

import android.content.*
import android.net.Uri
import android.provider.Settings
import com.arpokrat.common.platform.*
import com.arpokrat.common.views.helpers.AlertManager
import com.arpokrat.common.views.helpers.generalGetString
import com.arpokrat.res.MR

fun Context.openAppSettingsInSystem() {
  Intent().apply {
    action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
    data = Uri.parse("package:${androidAppContext.packageName}")
    try {
      startActivity(this)
    } catch (e: ActivityNotFoundException) {
      Log.e(TAG, e.stackTraceToString())
    }
  }
}

fun Context.showAllowPermissionInSettingsAlert(action: () -> Unit = ::openAppSettingsInSystem) {
  AlertManager.shared.showAlertMsg(
    title =  generalGetString(MR.strings.permissions_grant_in_settings),
    text = generalGetString(MR.strings.permissions_find_in_settings_and_grant),
    confirmText = generalGetString(MR.strings.permissions_open_settings),
    onConfirm = action,
  )
}
