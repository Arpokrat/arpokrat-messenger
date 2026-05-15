package com.arpokrat.common.platform

import androidx.compose.runtime.*

@SuppressWarnings("MissingJvmstatic")
@Composable
expect fun BackHandler(enabled: Boolean = true, onBack: () -> Unit)
