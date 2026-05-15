package com.arpokrat.common.platform

expect fun getWakeLock(timeout: Long): (() -> Unit)
