package com.arpokrat.common.wallet.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.*
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.PullRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp

/**
 * Standard spinner for Wallet loading states (Total Balance, Asset Balance, etc.).
 */
@Composable
fun WalletSpinner(
  modifier: Modifier = Modifier,
  alpha: Float = 1f,
  size: Int = 20,
  strokeWidth: Float = 2.5f
) {
  CircularProgressIndicator(
    strokeWidth = strokeWidth.dp,
    color = MaterialTheme.colors.primary,
    modifier = modifier
      .size(size.dp)
      .alpha(alpha)
      .padding(2.dp)
  )
}

/**
 * Standard indicator for the Wallet pull-to-refresh functionality.
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun WalletPullRefreshIndicator(
  refreshing: Boolean,
  state: PullRefreshState,
  modifier: Modifier = Modifier
) {
  PullRefreshIndicator(
    refreshing = refreshing,
    state = state,
    modifier = modifier,
    backgroundColor = MaterialTheme.colors.surface,
    contentColor = MaterialTheme.colors.primary
  )
}