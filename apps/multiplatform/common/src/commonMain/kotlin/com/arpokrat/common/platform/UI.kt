package com.arpokrat.common.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import com.arpokrat.common.views.helpers.KeyboardState

@Composable
expect fun LockToCurrentOrientationUntilDispose()

@Composable
expect fun LocalMultiplatformView(): Any?

@Composable
expect fun getKeyboardState(): State<KeyboardState>
expect fun hideKeyboard(view: Any?, clearFocus: Boolean = false)

expect fun androidIsFinishingMainActivity(): Boolean

fun registerGlobalErrorHandler() {
  Thread.setDefaultUncaughtExceptionHandler(GlobalExceptionsHandler())
}

expect class GlobalExceptionsHandler(): Thread.UncaughtExceptionHandler {
  override fun uncaughtException(thread: Thread, e: Throwable)
}
