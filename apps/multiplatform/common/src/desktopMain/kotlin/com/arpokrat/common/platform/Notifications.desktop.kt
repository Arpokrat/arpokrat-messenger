package com.arpokrat.common.platform

import com.arpokrat.common.simplexWindowState

actual fun allowedToShowNotification(): Boolean = !simplexWindowState.windowFocused.value
