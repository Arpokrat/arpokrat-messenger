package com.arpokrat.common.platform

actual fun allowedToShowNotification(): Boolean = !isAppOnForeground
