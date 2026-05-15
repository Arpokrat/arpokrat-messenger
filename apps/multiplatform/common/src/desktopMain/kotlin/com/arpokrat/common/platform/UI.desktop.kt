package com.arpokrat.common.platform

import androidx.compose.runtime.*
import com.arpokrat.common.views.helpers.KeyboardState

@Composable
actual fun LockToCurrentOrientationUntilDispose() {}

@Composable
actual fun LocalMultiplatformView(): Any? = null

@Composable
actual fun getKeyboardState(): State<KeyboardState> = remember { mutableStateOf(KeyboardState.Opened) }
actual fun hideKeyboard(view: Any?, clearFocus: Boolean) {}

actual fun androidIsFinishingMainActivity(): Boolean = false

actual class GlobalExceptionsHandler: Thread.UncaughtExceptionHandler {
  actual override fun uncaughtException(thread: Thread, e: Throwable) {
    Log.e(TAG, "App crashed, thread name: " + thread.name + ", exception: " + e.stackTraceToString())
  }
}
