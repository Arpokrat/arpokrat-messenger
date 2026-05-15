package com.arpokrat.common.wallet.ui.dashboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arpokrat.common.ui.theme.DEFAULT_PADDING
import com.arpokrat.common.wallet.CryptoAsset
import com.arpokrat.common.wallet.WalletManager
import com.arpokrat.common.wallet.ui.components.*
import com.arpokrat.res.MR
import dev.icerock.moko.resources.compose.painterResource
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.delay

/**
 * Main dashboard screen for the wallet module.
 * Displays the total fiat balance, a list of supported active assets,
 * upcoming assets, and provides access to core wallet actions (Swap, Buy/Sell, Settings).
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun WalletHomeView(
  assets: List<CryptoAsset>,
  totalValue: Double,
  currency: String,
  isRefreshing: Boolean,
  onToggleCurrency: () -> Unit,
  onAssetClick: (CryptoAsset) -> Unit,
  onSwapClick: () -> Unit,
  onBuySellClick: () -> Unit,
  onSettingsClick: () -> Unit,
  onRefresh: () -> Unit,
  getAssetFiatValue: (CryptoAsset) -> String,
  close: () -> Unit
) {
  val currencySymbol = if (currency == "EUR") "€" else "$"
  val totalValueStr = String.format(java.util.Locale.US, "%.2f", totalValue)

  var showLoader by remember { mutableStateOf(false) }
  LaunchedEffect(isRefreshing) {
    if (isRefreshing) showLoader = true
    else if (showLoader) { delay(1000); showLoader = false }
  }
  val loaderAlpha by animateFloatAsState(targetValue = if (showLoader) 1f else 0f, animationSpec = tween(300))

  var isManualPull by remember { mutableStateOf(false) }
  LaunchedEffect(isRefreshing) { if (!isRefreshing) isManualPull = false }

  val haptic = LocalHapticFeedback.current
  val pullRefreshState = rememberPullRefreshState(
    refreshing = isManualPull,
    onRefresh = {
      isManualPull = true
      haptic.performHapticFeedback(HapticFeedbackType.LongPress)
      onRefresh()
    },
    refreshThreshold = 70.dp
  )

  val walletManager = remember { WalletManager() }
  val isDevMode = remember { walletManager.isDeveloperModeEnabled() }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colors.background)
  ) {
    Column(modifier = Modifier.fillMaxSize()) {

      Box(
        modifier = Modifier
          .fillMaxWidth()
          .statusBarsPadding()
          .height(64.dp)
          .padding(horizontal = 16.dp)
      ) {
        Box(
          modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .clickable { close() }
            .align(Alignment.CenterStart),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            painter = painterResource(MR.images.ic_message_bubble),
            contentDescription = stringResource(MR.strings.back_to_chat),
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colors.primary
          )
        }

        val isDark = !MaterialTheme.colors.isLight
        Image(
          painter = painterResource(if (isDark) MR.images.logo_wallet_light else MR.images.logo_wallet),
          contentDescription = null,
          modifier = Modifier
            .height(36.dp)
            .widthIn(max = 160.dp)
            .align(Alignment.Center),
          contentScale = ContentScale.Fit
        )

        IconButton(
          onClick = onSettingsClick,
          modifier = Modifier.align(Alignment.CenterEnd).size(48.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Settings,
            contentDescription = stringResource(MR.strings.settings),
            modifier = Modifier.size(28.dp),
            tint = MaterialTheme.colors.onBackground
          )
        }
      }

      Card(
        backgroundColor = MaterialTheme.colors.onBackground.copy(alpha = 0.03f),
        shape = RoundedCornerShape(20.dp),
        elevation = 0.dp,
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = DEFAULT_PADDING, vertical = 16.dp)
      ) {
        Box(modifier = Modifier.fillMaxWidth()) {
          if (isDevMode) {
            DevModeBadge(
              modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 12.dp, end = 12.dp)
            )
          }

          Column(
            modifier = Modifier
              .fillMaxWidth()
              .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onToggleCurrency() }
              .padding(vertical = 32.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text(
              text = stringResource(MR.strings.wallet_total_balance),
              color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
              fontSize = 14.sp,
              fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
              Spacer(Modifier.width(28.dp))
              Text(
                text = "$currencySymbol $totalValueStr",
                color = MaterialTheme.colors.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 40.sp
              )
              Box(modifier = Modifier.padding(start = 8.dp).size(20.dp), contentAlignment = Alignment.Center) {
                WalletSpinner(alpha = loaderAlpha, size = 20)
              }
            }
          }
        }
      }

      Text(
        text = stringResource(MR.strings.wallet_assets_title),
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colors.onBackground,
        modifier = Modifier.padding(start = 32.dp, bottom = 12.dp, top = 8.dp)
      )

      Box(modifier = Modifier.fillMaxWidth().weight(1f).pullRefresh(pullRefreshState)) {
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(start = DEFAULT_PADDING, end = DEFAULT_PADDING, top = 0.dp, bottom = 130.dp)
        ) {
          val nativeAssets = assets.filter { it.contractAddress.isNullOrBlank() }
          val tokenAssets = assets.filter { !it.contractAddress.isNullOrBlank() }

          val comingSoonAssets = listOf(
            CryptoAsset("XMR", "Monero", "0.00", 12, 128, null),
            CryptoAsset("ZEC", "Zcash", "0.00", 8, 133, null)
          )

          items(nativeAssets) { asset ->
            val fiatPlaceholder = "$currencySymbol ${getAssetFiatValue(asset)}"
            Box(modifier = Modifier.padding(vertical = 6.dp)) { WalletAssetItem(asset = asset, fiatValue = fiatPlaceholder, onClick = { onAssetClick(asset) }) }
          }
          items(comingSoonAssets) { asset ->
            Box(modifier = Modifier.padding(vertical = 6.dp)) { ComingSoonAssetItem(asset = asset) }
          }
          items(tokenAssets) { asset ->
            val fiatPlaceholder = "$currencySymbol ${getAssetFiatValue(asset)}"
            Box(modifier = Modifier.padding(vertical = 6.dp)) { WalletAssetItem(asset = asset, fiatValue = fiatPlaceholder, onClick = { onAssetClick(asset) }) }
          }
        }
        WalletPullRefreshIndicator(
          refreshing = isManualPull,
          state = pullRefreshState,
          modifier = Modifier.align(Alignment.TopCenter)
        )
      }
    }

    Box(
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .fillMaxWidth()
        .background(
          Brush.verticalGradient(
            colors = listOf(Color.Transparent, MaterialTheme.colors.background),
            startY = 0f,
            endY = 100f
          )
        )
        .navigationBarsPadding()
        .padding(vertical = 16.dp)
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = DEFAULT_PADDING),
        horizontalArrangement = Arrangement.SpaceEvenly
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(80.dp)) {
          Card(
            shape = CircleShape, backgroundColor = MaterialTheme.colors.primary, elevation = 6.dp,
            modifier = Modifier.size(60.dp).clickable { onSwapClick() }
          ) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.SwapHoriz, null, tint = MaterialTheme.colors.onPrimary, modifier = Modifier.size(28.dp)) } }
          Spacer(modifier = Modifier.height(8.dp))
          Text(stringResource(MR.strings.wallet_action_swap), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colors.onBackground)
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(80.dp)) {
          Card(
            shape = CircleShape, backgroundColor = MaterialTheme.colors.surface, elevation = 6.dp,
            modifier = Modifier.size(60.dp).clickable { onBuySellClick() }
          ) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.AccountBalance, null, tint = MaterialTheme.colors.onSurface, modifier = Modifier.size(28.dp)) } }
          Spacer(modifier = Modifier.height(8.dp))
          Text(stringResource(MR.strings.wallet_action_buy_sell), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colors.onBackground)
        }
      }
    }
  }
}

/**
 * UI component for displaying upcoming but currently unsupported crypto assets.
 * Renders with a ghosted/faded appearance and a "Coming Soon" badge.
 */
@Composable
fun ComingSoonAssetItem(asset: CryptoAsset) {
  Card(
    backgroundColor = MaterialTheme.colors.surface.copy(alpha = 0.5f),
    shape = RoundedCornerShape(16.dp),
    elevation = 0.dp,
    modifier = Modifier.fillMaxWidth().height(88.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(modifier = Modifier.alpha(0.5f)) { DynamicCryptoIcon(asset = asset, size = 56.dp, showNetworkBadge = false) }
      Spacer(modifier = Modifier.width(16.dp))
      Column(modifier = Modifier.weight(1f).alpha(0.5f)) {
        Text(text = asset.name, color = MaterialTheme.colors.onSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text(text = asset.symbol, color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f), fontSize = 14.sp)
      }
      Surface(
        shape = RoundedCornerShape(8.dp), color = MaterialTheme.colors.primary.copy(alpha = 0.1f), border = BorderStroke(1.dp, MaterialTheme.colors.primary.copy(alpha = 0.3f))
      ) { Text(text = stringResource(MR.strings.wallet_coming_soon), color = MaterialTheme.colors.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) }
    }
  }
}