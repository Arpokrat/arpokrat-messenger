package com.arpokrat.common.views.helpers

import androidx.compose.runtime.Composable

@Composable
expect fun DefaultDialog(
  onDismissRequest: () -> Unit,
  content: @Composable () -> Unit
)
