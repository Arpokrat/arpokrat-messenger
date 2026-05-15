package com.arpokrat.common.platform

actual fun getWakeLock(timeout: Long): (() -> Unit) = {}
