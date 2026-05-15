package com.arpokrat.common.wallet.ui.dashboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arpokrat.app.wallet.PriceService
import com.arpokrat.common.platform.BackHandler
import com.arpokrat.common.platform.ColumnWithScrollBar
import com.arpokrat.common.ui.theme.DEFAULT_PADDING
import com.arpokrat.common.views.helpers.AppBarTitle
import com.arpokrat.common.views.helpers.ModalManager
import com.arpokrat.common.wallet.*
import com.arpokrat.common.wallet.ui.components.*
import com.arpokrat.common.wallet.ui.transaction.*
import com.arpokrat.res.MR
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.*
import java.util.Locale

// In-memory cache to prevent redundant network calls for transaction history across recompositions
private val historySessionCache = mutableMapOf<String, List<TransactionUiModel>>()

/**
 * Formats the crypto amount to ensure proper UI display.
 * Avoids scientific notation and safely trims trailing zeros for fractional amounts.
 */
fun formatSmartCryptoAmount(amountStr: String): String {
  val amount = amountStr.replace(",", ".").toDoubleOrNull() ?: 0.0
  if (amount == 0.0) return "0.00"
  if (amount >= 0.01) {
    val formatted = String.format(Locale.US, "%.2f", amount)
    if (amount - amount.toLong() > 0) {
      val fullString = String.format(Locale.US, "%.6f", amount).trimEnd('0').trimEnd('.')
      if (fullString.substringAfter('.').length > 2) return fullString
    }
    return formatted
  }
  return String.format(Locale.US, "%.8f", amount).trimEnd('0').trimEnd('.')
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun WalletAssetDetailView(
  asset: CryptoAsset,
  nativeBalance: Double,
  currency: String,
  myAddress: String,
  isDevMode: Boolean,
  walletManager: WalletManager,
  onBack: () -> Unit,
  onSendConfirm: (String, String, () -> Unit) -> Unit,
  onCopy: (String) -> Unit,
  onOpenExplorer: (String) -> Unit,
  onOpenAddressExplorer: () -> Unit,
  onScanClick: ((String) -> Unit) -> Unit,
  onSwapClick: () -> Unit,
  onToggleCurrency: () -> Unit,
  onKeepAlive: () -> Unit
) {
  BackHandler(onBack = onBack)

  var showSendSheet by remember { mutableStateOf(false) }
  var selectedTx by remember { mutableStateOf<TransactionUiModel?>(null) }

  val currencySymbol = if (currency == "USD") "$" else "€"
  val fiatValue = run {
    val balance = asset.balance.replace(",", ".").trim().toDoubleOrNull() ?: 0.0
    String.format(Locale.US, "%.2f", PriceService.getValue(asset.symbol, balance, currency))
  }
  val smartBalance = formatSmartCryptoAmount(asset.balance)

  val cacheKey = "${asset.symbol}_$myAddress"

  var history by remember { mutableStateOf<List<TransactionUiModel>>(historySessionCache[cacheKey] ?: emptyList()) }
  var isLoadingHistory by remember { mutableStateOf(historySessionCache[cacheKey] == null) }
  var historyRefreshing by remember { mutableStateOf(false) }
  var historyManualTrigger by remember { mutableStateOf(0) }

  LaunchedEffect(Unit) {
    while (isActive) {
      onKeepAlive()
      delay(500)
    }
  }

  LaunchedEffect(asset, historyManualTrigger) {
    historyRefreshing = true
    if (historyManualTrigger > 0) isLoadingHistory = true

    // Asynchronously fetch transaction history depending on the active blockchain network
    val rawHistory = withContext(Dispatchers.IO) {
      try {
        when (asset.coinType) {
          CryptoNetwork.TRON.id, CryptoNetwork.TRON_NILE.id ->
            NetworkFactory.getTronService(asset.coinType).getHistory(myAddress, asset.contractAddress)

          CryptoNetwork.BITCOIN.id, CryptoNetwork.BITCOIN_TESTNET.id ->
            NetworkFactory.getBitcoinService(asset.coinType).getHistory(myAddress)

          CryptoNetwork.SOLANA.id, CryptoNetwork.SOLANA_DEVNET.id ->
            NetworkFactory.getSolanaService(asset.coinType).getHistory(myAddress)

          else ->
            NetworkFactory.getEvmService(asset.coinType).getHistory(myAddress, asset.contractAddress)
        }
      } catch (e: Exception) { null }
    }

    if (rawHistory != null) {
      val mappedHistory = rawHistory.map { tx ->
        TransactionUiModel(
          hash = tx.hash, from = tx.from, to = tx.to, value = tx.value,
          tokenSymbol = tx.tokenSymbol, timeStamp = tx.timeStamp, isError = tx.isError,
          nonce = tx.nonce, blockNumber = tx.blockNumber, networkFee = tx.networkFee
        )
      }

      history = mappedHistory
      historySessionCache[cacheKey] = mappedHistory
    }

    isLoadingHistory = false
    historyRefreshing = false
  }

  var isManualPull by remember { mutableStateOf(false) }
  LaunchedEffect(historyRefreshing) { if (!historyRefreshing) isManualPull = false }

  val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
  val pullRefreshState = rememberPullRefreshState(
    refreshing = isManualPull,
    onRefresh = {
      isManualPull = true
      haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
      historyManualTrigger++
    },
    refreshThreshold = 70.dp
  )

  var showLoader by remember { mutableStateOf(false) }
  LaunchedEffect(historyRefreshing) {
    if (historyRefreshing) showLoader = true
    else if (showLoader) { delay(1000); showLoader = false }
  }
  val loaderAlpha by animateFloatAsState(targetValue = if (showLoader) 1f else 0f, animationSpec = tween(300))

  Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colors.background)) {

    Box(modifier = Modifier.fillMaxSize().pullRefresh(pullRefreshState)) {

      ColumnWithScrollBar(modifier = Modifier.fillMaxSize()) {

        AppBarTitle(title = asset.name)

        Card(
          backgroundColor = MaterialTheme.colors.onBackground.copy(alpha = 0.03f),
          shape = RoundedCornerShape(20.dp),
          elevation = 0.dp,
          modifier = Modifier.fillMaxWidth().padding(horizontal = DEFAULT_PADDING, vertical = 16.dp)
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onToggleCurrency() }.padding(vertical = 32.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            DynamicCryptoIcon(asset = asset, size = 64.dp)
            Spacer(Modifier.height(16.dp))
            Text("$smartBalance ${asset.symbol}", color = MaterialTheme.colors.onSurface, fontWeight = FontWeight.Bold, fontSize = 32.sp)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
              Text("≈ $currencySymbol $fiatValue", color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f), fontSize = 16.sp)
              Box(modifier = Modifier.padding(start = 8.dp).size(16.dp), contentAlignment = Alignment.Center) {
                WalletSpinner(alpha = loaderAlpha, size = 16, strokeWidth = 2f)
              }
            }
          }
        }

        Text(stringResource(MR.strings.wallet_transactions), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colors.onBackground, modifier = Modifier.padding(start = DEFAULT_PADDING, bottom = 12.dp, top = 16.dp))

        if (isLoadingHistory) {
          Box(Modifier.fillMaxWidth().padding(top = 32.dp), contentAlignment = Alignment.Center) {
            WalletSpinner(size = 32, strokeWidth = 3f)
          }
        } else if (history.isEmpty()) {
          Box(Modifier.fillMaxWidth().padding(top = 32.dp), contentAlignment = Alignment.Center) {
            Text(stringResource(MR.strings.wallet_no_transactions), color = MaterialTheme.colors.onBackground.copy(alpha = 0.5f))
          }
        } else {
          history.forEach { tx ->
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
              val smartTx = tx.copy(value = formatSmartCryptoAmount(tx.value))
              val finalSymbol = tx.tokenSymbol?.takeIf { it.isNotBlank() } ?: asset.symbol
              TransactionItemRow(tx = smartTx, myAddress = myAddress, symbol = finalSymbol, onClick = { selectedTx = smartTx })
            }
          }
        }

        Spacer(modifier = Modifier.height(140.dp))
      }

      WalletPullRefreshIndicator(
        refreshing = isManualPull,
        state = pullRefreshState,
        modifier = Modifier.align(Alignment.TopCenter).padding(top = 80.dp)
      )
    }

    Box(
      modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Brush.verticalGradient(colors = listOf(Color.Transparent, MaterialTheme.colors.background), startY = 0f, endY = 100f)).navigationBarsPadding().padding(vertical = 16.dp)
    ) {
      Row(modifier = Modifier.fillMaxWidth().padding(horizontal = DEFAULT_PADDING), horizontalArrangement = Arrangement.SpaceEvenly) {
        FloatingActionCircle(
          icon = Icons.Default.ArrowUpward, label = stringResource(MR.strings.wallet_btn_send), backgroundColor = MaterialTheme.colors.surface, iconColor = MaterialTheme.colors.onSurface,
          onClick = {
            ModalManager.start.showModalCloseable(
              endButtons = {
                if (isDevMode) {
                  Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 16.dp)) {
                    DevModeBadge()
                  }
                }
              }
            ) { closeSheet ->
              WalletSendView(
                asset = asset, nativeBalance = nativeBalance, currency = currency, currencySymbol = currencySymbol, initialAddress = "",
                onBack = { closeSheet() },
                onScanClick = onScanClick,
                verifyPin = { pin -> CryptoManager.wallet?.validatePin(pin) ?: false },
                onNext = { dest, amount ->
                  closeSheet()
                  onSendConfirm(dest, amount) { historyManualTrigger++ }
                }
              )
            }
          }
        )

        FloatingActionCircle(icon = Icons.Default.SwapHoriz, label = stringResource(MR.strings.wallet_btn_swap), backgroundColor = MaterialTheme.colors.primary, iconColor = MaterialTheme.colors.onPrimary, onClick = onSwapClick)

        FloatingActionCircle(
          icon = Icons.Default.ArrowDownward, label = stringResource(MR.strings.wallet_btn_receive), backgroundColor = MaterialTheme.colors.surface, iconColor = MaterialTheme.colors.onSurface,
          onClick = {
            ModalManager.start.showModalCloseable(
              endButtons = {
                if (isDevMode) {
                  Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 16.dp)) {
                    DevModeBadge()
                  }
                }
              }
            ) { closeSheet ->
              ReceiveSheet(
                address = myAddress,
                networkName = asset.name,
                onCopy = { onCopy(myAddress); closeSheet() },
                onKeepAlive = onKeepAlive
              )
            }
          }
        )
      }
    }

    if (showSendSheet) {
      WalletSendView(
        asset = asset, nativeBalance = nativeBalance, currency = currency, currencySymbol = currencySymbol, initialAddress = "",
        onBack = { showSendSheet = false }, onScanClick = onScanClick, verifyPin = { pin -> CryptoManager.wallet?.validatePin(pin) ?: false },
        onNext = { dest, amount ->
          showSendSheet = false
          onSendConfirm(dest, amount) { historyManualTrigger++ }
        }
      )
    }

    LaunchedEffect(selectedTx) {
      selectedTx?.let { tx ->
        ModalManager.start.showModalCloseable(
          endButtons = {
            if (isDevMode) {
              Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 16.dp)) {
                DevModeBadge()
              }
            }
          }
        ) { closeSheet ->
          TransactionDetailSheet(
            tx = tx, myAddress = myAddress, symbol = asset.symbol, currency = currency, currencySymbol = currencySymbol, dateStr = formatTimestamp(tx.timeStamp), isDevMode = isDevMode,
            onDismiss = {
              selectedTx = null
              closeSheet()
            },
            onCopy = { _, text -> onCopy(text) },
            onOpenExplorer = { hash -> onOpenExplorer(hash) }
          )
        }
      }
    }
  }
}

@Composable
private fun FloatingActionCircle(icon: ImageVector, label: String, backgroundColor: Color, iconColor: Color, onClick: () -> Unit) {
  Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(80.dp)) {
    Card(shape = CircleShape, backgroundColor = backgroundColor, elevation = 6.dp, modifier = Modifier.size(60.dp).clickable { onClick() }) {
      Box(contentAlignment = Alignment.Center) { Icon(imageVector = icon, contentDescription = label, tint = iconColor, modifier = Modifier.size(28.dp)) }
    }
    Spacer(modifier = Modifier.height(8.dp))
    Text(text = label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colors.onBackground)
  }
}