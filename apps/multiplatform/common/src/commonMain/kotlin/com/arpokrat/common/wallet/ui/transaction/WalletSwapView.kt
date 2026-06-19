package com.arpokrat.common.wallet.ui.transaction

import SectionBottomSpacer
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import boofcv.alg.fiducial.qrcode.QrCode
import com.arpokrat.app.wallet.PriceService
import com.arpokrat.common.platform.ColumnWithScrollBar
import com.arpokrat.common.platform.Log
import com.arpokrat.common.ui.theme.SimplexGreen
import com.arpokrat.common.ui.theme.WarningOrange
import com.arpokrat.common.ui.theme.WarningYellow
import com.arpokrat.common.views.helpers.AppBarTitle
import com.arpokrat.common.views.helpers.AppHoldToConfirmButton
import com.arpokrat.common.views.helpers.AppNotificationManager
import com.arpokrat.common.views.helpers.AppPrimaryButton
import com.arpokrat.common.views.helpers.ModalData
import com.arpokrat.common.views.helpers.ModalManager
import com.arpokrat.common.views.helpers.ModernTextField
import com.arpokrat.common.views.newchat.qrCodeBitmap
import com.arpokrat.common.wallet.*
import com.arpokrat.common.wallet.ui.components.WalletSpinner
import com.arpokrat.res.MR
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

private enum class PickerTarget { SOURCE, TARGET }

private const val SWAP_HELP_URL = "https://arpokrat.com/contact"

@Composable
fun SwapHistoryHeaderButton(onClick: () -> Unit) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier
      .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
      .padding(horizontal = 8.dp, vertical = 6.dp)
  ) {
    Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colors.primary, modifier = Modifier.size(20.dp))
    Spacer(Modifier.width(6.dp))
    Text(stringResource(MR.strings.wallet_swap_history_view), color = MaterialTheme.colors.primary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
  }
}

@Composable
fun ModalData.WalletSwapView(
  walletManager: WalletManager,
  walletAssets: List<CryptoAsset>,
  currency: String,
  initialAsset: CryptoAsset? = null,
  resumePending: PendingSwap? = null,
  isWalletLocked: () -> Boolean = { false },
  onKeepAlive: () -> Unit,
  closeFlow: () -> Unit,
  onPayFromWallet: (CryptoAsset, String, String) -> Unit,
  onDestScan: (((String) -> Unit) -> Unit) = {},
  onOpenHistory: () -> Unit = {}
) {
  val clipboardManager = LocalClipboardManager.current
  val uriHandler = LocalUriHandler.current
  val scope = rememberCoroutineScope()
  val copiedMsg = stringResource(MR.strings.wallet_notif_address_copied)

  fun openTracking(tradeId: String, deposit: String, amountIn: Double, fromTok: SwapToken, toTok: SwapToken?, createdAt: Long, expiresAt: Long) {
    ModalManager.start.showModalCloseable(
      endButtons = { SwapHistoryHeaderButton(onClick = onOpenHistory) }
    ) { _ ->
      SwapTrackingScreen(
        tradeId = tradeId, depositAddress = deposit, amountIn = amountIn, fromToken = fromTok, toToken = toTok,
        createdAt = createdAt, expiresAt = expiresAt,
        walletManager = walletManager, walletAssets = walletAssets, currency = currency,
        isWalletLocked = isWalletLocked, closeFlow = closeFlow, onPayFromWallet = onPayFromWallet,
        onCopy = { text -> clipboardManager.setText(AnnotatedString(text)); AppNotificationManager.showSuccess(copiedMsg) },
        onHelp = { try { uriHandler.openUri(SWAP_HELP_URL) } catch (e: Exception) {} }
      )
    }
  }

  if (resumePending != null) {
    SwapTrackingScreen(
      tradeId = resumePending.tradeId, depositAddress = resumePending.depositAddress, amountIn = resumePending.amountIn,
      fromToken = SwapTokenMapping.reconstruct(resumePending.fromTokenId, resumePending.fromSymbol),
      toToken = SwapTokenMapping.reconstruct(resumePending.toTokenId, resumePending.toSymbol),
      createdAt = resumePending.createdAt, expiresAt = resumePending.expiresAt,
      walletManager = walletManager, walletAssets = walletAssets, currency = currency,
      isWalletLocked = isWalletLocked, closeFlow = closeFlow, onPayFromWallet = onPayFromWallet,
      initialStatus = mapSwapStatus(resumePending.lastStatus),
      onCopy = { text -> clipboardManager.setText(AnnotatedString(text)); AppNotificationManager.showSuccess(copiedMsg) },
      onHelp = { try { uriHandler.openUri(SWAP_HELP_URL) } catch (e: Exception) {} }
    )
    return
  }

  val sourceTokens = remember(walletAssets) { SwapTokenMapping.sourceTokens(walletAssets) }

  val fromTokenState = remember {
    stateGetOrPutNullable<SwapToken>("swapFrom") {
      initialAsset?.let { a -> sourceTokens.find { it.walletAsset?.coinType == a.coinType && it.walletAsset?.symbol == a.symbol } }
        ?: sourceTokens.find { it.tokenId == SwapTokenMapping.DEFAULT_SOURCE_ID }
        ?: sourceTokens.firstOrNull()
    }
  }
  val toTokenState = remember { stateGetOrPutNullable<SwapToken>("swapTo") { null } }
  val amountState = remember { stateGetOrPut("swapAmount") { "" } }
  val destState = remember { stateGetOrPut("swapDest") { "" } }
  val destExternalState = remember { stateGetOrPut("swapDestExternal") { false } }

  var fromToken by fromTokenState
  var toToken by toTokenState
  var amountIn by amountState
  var destAddress by destState
  var destIsExternal by destExternalState

  var limits by remember { mutableStateOf<SwapMinMax?>(null) }
  var quotes by remember { mutableStateOf<List<SwapQuote>>(emptyList()) }
  var selectedQuote by remember { mutableStateOf<SwapQuote?>(null) }
  var isLoadingQuotes by remember { mutableStateOf(false) }
  var errorText by remember { mutableStateOf<String?>(null) }
  var requoteTick by remember { mutableStateOf(0) }
  var quoteSecondsLeft by remember { mutableStateOf(0) }
  var fiatRate by remember { mutableStateOf(0.0) }
  var fiatSymbol by remember { mutableStateOf(if (currency == "EUR") "€" else "$") }

  val parsedAmount = amountIn.toDoubleOrNull() ?: 0.0
  val destCoinType = toToken?.let { SwapTokenMapping.walletCoinTypeFor(it.tokenId) }

  LaunchedEffect(Unit) { while (isActive) { onKeepAlive(); delay(500) } }
  LaunchedEffect(Unit) { while (isActive) { if (isWalletLocked()) { closeFlow(); break }; delay(400) } }

  LaunchedEffect(Unit) {
    if (toToken == null) {
      val xmr = withContext(Dispatchers.IO) { SwapService.searchToken("monero") }
        .map { it.toSwapToken() }
        .firstOrNull { it.tokenId.equals(SwapTokenMapping.DEFAULT_TARGET_ID, ignoreCase = true) }
      if (toToken == null && xmr != null) toToken = xmr
    }
  }

  LaunchedEffect(fromToken?.tokenId, currency) {
    val f = fromToken
    if (f == null) { fiatRate = 0.0; return@LaunchedEffect }
    val p = withContext(Dispatchers.IO) { SwapService.price(f.ticker.ifBlank { f.symbol }) }
    when {
      p == null -> fiatRate = 0.0
      currency == "EUR" && p.eur > 0.0 -> { fiatRate = p.eur; fiatSymbol = "€" }
      currency == "EUR" -> {
        val rate = PriceService.usdToEurRate()
        if (rate != null && p.usd > 0.0) { fiatRate = p.usd * rate; fiatSymbol = "€" }
        else { fiatRate = p.usd; fiatSymbol = "$" }
      }
      else -> { fiatRate = p.usd; fiatSymbol = "$" }
    }
  }

  LaunchedEffect(toToken) {
    val t = toToken
    if (t == null) { destAddress = ""; destIsExternal = false; return@LaunchedEffect }
    val ct = SwapTokenMapping.walletCoinTypeFor(t.tokenId)
    if (ct != null) {
      destAddress = withContext(Dispatchers.IO) { walletManager.getAddressForNetwork(ct) }
      destIsExternal = false
    } else {
      destAddress = ""
      destIsExternal = true
    }
  }

  LaunchedEffect(fromToken, toToken) {
    limits = null
    val f = fromToken; val t = toToken
    if (f != null && t != null) limits = SwapService.minMax(f.tokenId, t.tokenId)
  }

  val minOk = limits?.let { it.min <= 0.0 || parsedAmount >= it.min } ?: true
  val maxOk = limits?.let { it.max <= 0.0 || parsedAmount <= it.max } ?: true
  val amountInRange = parsedAmount > 0.0 && minOk && maxOk

  LaunchedEffect(fromToken, toToken, amountIn, requoteTick) {
    val prevProvider = selectedQuote?.provider
    quotes = emptyList(); selectedQuote = null; errorText = null
    val f = fromToken; val t = toToken
    if (f == null || t == null || !amountInRange) { isLoadingQuotes = false; return@LaunchedEffect }
    isLoadingQuotes = true
    delay(500)
    val resp = SwapService.quotes("in", amountIn, f.tokenId, t.tokenId)
    isLoadingQuotes = false
    if (resp.error != null && resp.quotes.isEmpty()) { errorText = resp.error; return@LaunchedEffect }
    quotes = resp.quotes
    selectedQuote = resp.quotes.firstOrNull { it.provider == prevProvider } ?: resp.quotes.firstOrNull()
  }

  LaunchedEffect(selectedQuote) {
    val q = selectedQuote ?: run { quoteSecondsLeft = 0; return@LaunchedEffect }
    var left = q.expiresIn.takeIf { it > 0 } ?: 60
    while (isActive && left > 0) { quoteSecondsLeft = left; delay(1000); left-- }
    quoteSecondsLeft = 0
    requoteTick++
  }

  val destValid: Boolean = when {
    toToken == null -> false
    destAddress.isBlank() -> false
    destCoinType != null -> WalletAddressValidator.isValid(destAddress, destCoinType)
    else -> WalletAddressValidator.isPlausibleExternal(destAddress)
  }

  fun openPicker(target: PickerTarget) {
    ModalManager.start.showModalCloseable { _ ->
      SwapTokenPickerScreen(
        title = stringResource(if (target == PickerTarget.SOURCE) MR.strings.wallet_swap_select_source else MR.strings.wallet_swap_select_target),
        walletTokens = sourceTokens,
        onKeepAlive = onKeepAlive,
        isWalletLocked = isWalletLocked,
        closeFlow = closeFlow,
        onPick = { picked ->
          if (target == PickerTarget.SOURCE) {
            if (picked.tokenId == toTokenState.value?.tokenId) toTokenState.value = null
            fromTokenState.value = picked
          } else {
            if (picked.tokenId == fromTokenState.value?.tokenId) fromTokenState.value = null
            toTokenState.value = picked
          }
          ModalManager.start.closeModal()
        }
      )
    }
  }

  fun openConfirm() {
    val f = fromToken ?: return
    val t = toToken ?: return
    val q = selectedQuote ?: return
    val amt = amountIn
    val dest = destAddress
    ModalManager.start.showModalCloseable { _ ->
      SwapConfirmScreen(
        fromToken = f, toToken = t, amountIn = amt, initialQuote = q, destAddress = dest,
        isWalletLocked = isWalletLocked, onKeepAlive = onKeepAlive, closeFlow = closeFlow,
        onCreated = { tradeId, deposit, amtIn, createdAt, expiresAt -> closeFlow(); openTracking(tradeId, deposit, amtIn, f, t, createdAt, expiresAt) }
      )
    }
  }

  fun openDestScan() {
    onDestScan { scanned ->
      destAddress = scanned.trim()
      if (destCoinType != null) destIsExternal = true
    }
  }

  SwapFormSection(
    fromToken = fromToken, toToken = toToken, amountIn = amountIn,
    onAmountChange = { v ->
      val filtered = v.replace(",", ".").replace("\n", "")
      if (filtered.isEmpty() || filtered.matches(Regex("^\\d*\\.?\\d*$"))) amountIn = filtered
    },
    onSwapTokens = { val tmp = fromToken; fromToken = toToken; toToken = tmp; amountIn = "" },
    onPickSource = { openPicker(PickerTarget.SOURCE) },
    onPickTarget = { openPicker(PickerTarget.TARGET) },
    limits = limits, amountInRange = amountInRange, minOk = minOk, maxOk = maxOk,
    fiatRate = fiatRate, fiatSymbol = fiatSymbol,
    isLoadingQuotes = isLoadingQuotes, quotes = quotes, selectedQuote = selectedQuote,
    onSelectQuote = { selectedQuote = it },
    errorText = errorText,
    quoteSecondsLeft = quoteSecondsLeft,
    destAddress = destAddress, destIsExternal = destIsExternal, destValid = destValid,
    destCoinType = destCoinType,
    onScanDest = { openDestScan() },
    onDestChange = { destAddress = it.trim() },
    onToggleExternal = { external ->
      destIsExternal = external
      if (!external && destCoinType != null) {
        scope.launch { destAddress = withContext(Dispatchers.IO) { walletManager.getAddressForNetwork(destCoinType) } }
      } else if (external) destAddress = ""
    },
    canReview = fromToken != null && toToken != null && amountInRange && selectedQuote != null && destValid,
    onReview = { openConfirm() }
  )
}

@Composable
private fun SwapFormSection(
  fromToken: SwapToken?, toToken: SwapToken?, amountIn: String,
  onAmountChange: (String) -> Unit, onSwapTokens: () -> Unit,
  onPickSource: () -> Unit, onPickTarget: () -> Unit,
  limits: SwapMinMax?, amountInRange: Boolean, minOk: Boolean, maxOk: Boolean,
  fiatRate: Double, fiatSymbol: String,
  isLoadingQuotes: Boolean, quotes: List<SwapQuote>, selectedQuote: SwapQuote?,
  onSelectQuote: (SwapQuote) -> Unit, errorText: String?,
  quoteSecondsLeft: Int,
  destAddress: String, destIsExternal: Boolean, destValid: Boolean, destCoinType: Int?,
  onScanDest: () -> Unit,
  onDestChange: (String) -> Unit, onToggleExternal: (Boolean) -> Unit,
  canReview: Boolean, onReview: () -> Unit
) {
  val parsedAmount = amountIn.toDoubleOrNull() ?: 0.0
  Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colors.background)) {
    ColumnWithScrollBar(modifier = Modifier.fillMaxSize()) {
      AppBarTitle(title = stringResource(MR.strings.wallet_swap_title))

      Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        AmountCard(
          label = stringResource(MR.strings.wallet_swap_you_send),
          token = fromToken, onPickToken = onPickSource
        ) {
          BasicTextField(
            value = amountIn, onValueChange = onAmountChange,
            textStyle = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Bold, color = if (amountInRange || amountIn.isBlank()) MaterialTheme.colors.onSurface else MaterialTheme.colors.error),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            cursorBrush = SolidColor(MaterialTheme.colors.primary),
            decorationBox = { inner -> if (amountIn.isEmpty()) Text("0", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colors.onSurface.copy(alpha = 0.25f)); inner() }
          )
          if (fiatRate > 0.0) {
            Spacer(Modifier.height(4.dp))
            Text(
              stringResource(MR.strings.wallet_swap_fiat_estimate, "$fiatSymbol${fmtFiat(parsedAmount * fiatRate)}"),
              color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f), fontSize = 12.sp
            )
          }
        }

        if (limits != null && (limits.min > 0.0 || limits.max > 0.0)) {
          Spacer(Modifier.height(6.dp))
          Text(
            text = stringResource(MR.strings.wallet_swap_min_max, fmtAmount(limits.min), fmtAmount(limits.max)),
            color = MaterialTheme.colors.onBackground.copy(alpha = 0.5f), fontSize = 12.sp,
            modifier = Modifier.padding(start = 4.dp)
          )
        }
        if (amountIn.isNotBlank() && !minOk) ErrorLine(stringResource(MR.strings.wallet_swap_amount_too_low, fmtAmount(limits?.min ?: 0.0)))
        if (amountIn.isNotBlank() && !maxOk) ErrorLine(stringResource(MR.strings.wallet_swap_amount_too_high, fmtAmount(limits?.max ?: 0.0)))

        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
          Box(
            modifier = Modifier.size(44.dp).clip(CircleShape)
              .background(MaterialTheme.colors.onBackground.copy(alpha = 0.05f))
              .clickable { onSwapTokens() },
            contentAlignment = Alignment.Center
          ) { Icon(Icons.Default.SwapVert, contentDescription = null, tint = MaterialTheme.colors.primary) }
        }

        AmountCard(
          label = stringResource(MR.strings.wallet_swap_you_receive),
          token = toToken, onPickToken = onPickTarget
        ) {
          val out = selectedQuote?.amountOut
          Text(
            text = if (out != null && out > 0.0) fmtAmount(out) else "0",
            fontSize = 30.sp, fontWeight = FontWeight.Bold,
            color = if (out != null && out > 0.0) MaterialTheme.colors.onSurface else MaterialTheme.colors.onSurface.copy(alpha = 0.25f)
          )
        }

        Spacer(Modifier.height(16.dp))

        when {
          isLoadingQuotes -> Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 12.dp)) {
            WalletSpinner(size = 18, strokeWidth = 2f); Spacer(Modifier.width(10.dp))
            Text(stringResource(MR.strings.wallet_swap_loading_quotes), color = MaterialTheme.colors.onBackground.copy(alpha = 0.6f), fontSize = 14.sp)
          }
          errorText != null && quotes.isEmpty() && amountInRange -> ErrorLine(stringResource(MR.strings.wallet_swap_no_quotes))
          quotes.isNotEmpty() -> {
            if (quoteSecondsLeft > 0) {
              Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                Icon(Icons.Default.Timer, contentDescription = null, tint = MaterialTheme.colors.primary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(MR.strings.wallet_swap_expires_in, quoteSecondsLeft), color = MaterialTheme.colors.primary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
              }
            } else if (selectedQuote != null) {
              Text(stringResource(MR.strings.wallet_swap_expired_requoting), color = MaterialTheme.colors.onBackground.copy(alpha = 0.5f), fontSize = 13.sp, modifier = Modifier.padding(bottom = 8.dp))
            }
            quotes.forEach { q -> QuoteCard(quote = q, toSymbol = toToken?.symbol ?: "", selected = q.quoteId == selectedQuote?.quoteId, onClick = { onSelectQuote(q) }) }
          }
        }

        Spacer(Modifier.height(20.dp))

        if (toToken != null) {
          ModernTextField(
            value = destAddress, onValueChange = onDestChange, modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(if (destCoinType != null && !destIsExternal) stringResource(MR.strings.wallet_swap_dest_wallet, toToken.symbol) else stringResource(MR.strings.wallet_swap_destination)) },
            trailingIcon = {
              IconButton(onClick = onScanDest) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = stringResource(MR.strings.wallet_scan_qr_desc), tint = MaterialTheme.colors.primary)
              }
            },
            isError = destAddress.isNotBlank() && !destValid
          )
          if (destAddress.isNotBlank() && !destValid) ErrorLine(stringResource(MR.strings.wallet_swap_invalid_address))
          if (destCoinType != null) {
            Spacer(Modifier.height(4.dp))
            Text(
              text = if (destIsExternal) stringResource(MR.strings.wallet_swap_use_my_wallet) else stringResource(MR.strings.wallet_swap_send_elsewhere),
              color = MaterialTheme.colors.primary, fontSize = 13.sp, fontWeight = FontWeight.Medium,
              modifier = Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onToggleExternal(!destIsExternal) }.padding(vertical = 4.dp)
            )
          }
        }

        Spacer(Modifier.height(160.dp))
      }
    }

    Column(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(MaterialTheme.colors.background).padding(horizontal = 20.dp).padding(top = 12.dp)) {
      AppPrimaryButton(
        text = when {
          fromToken == null || toToken == null -> stringResource(MR.strings.wallet_swap_btn_select_token)
          amountIn.isBlank() || !amountInRange -> stringResource(MR.strings.wallet_swap_btn_enter_amount)
          selectedQuote == null -> stringResource(MR.strings.wallet_swap_btn_no_offer)
          !destValid -> stringResource(MR.strings.wallet_swap_btn_enter_address)
          else -> stringResource(MR.strings.wallet_swap_btn_review)
        },
        onClick = onReview, enabled = canReview
      )
      SectionBottomSpacer()
    }
  }
}

@Composable
private fun AmountCard(label: String, token: SwapToken?, onPickToken: () -> Unit, amountContent: @Composable () -> Unit) {
  Card(backgroundColor = MaterialTheme.colors.surface, shape = RoundedCornerShape(22.dp), elevation = 0.dp, modifier = Modifier.fillMaxWidth()) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
      Column(modifier = Modifier.weight(1f)) {
        Text(label, color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f), fontSize = 12.sp)
        Spacer(Modifier.height(8.dp))
        amountContent()
      }
      TokenPill(token = token, onClick = onPickToken)
    }
  }
}

@Composable
private fun TokenPill(token: SwapToken?, onClick: () -> Unit) {
  Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colors.onSurface.copy(alpha = 0.07f), modifier = Modifier.clickable(onClick = onClick)) {
    Row(modifier = Modifier.padding(start = 8.dp, end = 6.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
      if (token != null) {
        SwapTokenIcon(token = token, size = 32.dp)
        Spacer(Modifier.width(8.dp))
        Text(token.symbol.uppercase(), color = MaterialTheme.colors.onSurface, fontWeight = FontWeight.Bold, fontSize = 15.sp)
      } else {
        Spacer(Modifier.width(4.dp))
        Text(stringResource(MR.strings.wallet_swap_select_token), color = MaterialTheme.colors.onSurface, fontWeight = FontWeight.Medium, fontSize = 14.sp)
      }
      Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colors.onSurface.copy(alpha = 0.7f))
    }
  }
}

@Composable
private fun QuoteCard(quote: SwapQuote, toSymbol: String, selected: Boolean, onClick: () -> Unit) {
  Card(
    backgroundColor = if (selected) MaterialTheme.colors.primary.copy(alpha = 0.06f) else MaterialTheme.colors.onBackground.copy(alpha = 0.03f),
    shape = RoundedCornerShape(16.dp), elevation = 0.dp,
    border = if (selected) BorderStroke(1.5.dp, MaterialTheme.colors.primary) else null,
    modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp).clickable(onClick = onClick)
  ) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(quote.provider, color = MaterialTheme.colors.onBackground, fontWeight = FontWeight.Bold, fontSize = 15.sp)
          Spacer(Modifier.width(8.dp))
          KycBadge(quote.kyc)
        }
        Spacer(Modifier.height(2.dp))
        Text(stringResource(MR.strings.wallet_swap_fees_included), color = MaterialTheme.colors.onBackground.copy(alpha = 0.5f), fontSize = 11.sp)
      }
      Text(
        text = "${fmtAmount(quote.amountOut)} ${toSymbol.uppercase()}",
        color = if (selected) MaterialTheme.colors.primary else MaterialTheme.colors.onBackground,
        fontWeight = FontWeight.Bold, fontSize = 16.sp, textAlign = TextAlign.End
      )
    }
  }
}

@Composable
private fun KycBadge(kyc: String) {
  val letter = kyc.trim().uppercase().firstOrNull()?.toString() ?: "N"
  val color = kycColor(kyc)
  var showInfo by remember { mutableStateOf(false) }
  Box {
    Box(
      modifier = Modifier.size(20.dp).clip(RoundedCornerShape(6.dp))
        .background(color.copy(alpha = 0.18f))
        .border(BorderStroke(1.dp, color.copy(alpha = 0.5f)), RoundedCornerShape(6.dp))
        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { showInfo = true },
      contentAlignment = Alignment.Center
    ) { Text(letter, color = color, fontWeight = FontWeight.Bold, fontSize = 11.sp) }
    DropdownMenu(expanded = showInfo, onDismissRequest = { showInfo = false }) {
      Column(modifier = Modifier.widthIn(max = 260.dp).padding(horizontal = 14.dp, vertical = 10.dp)) {
        Text(stringResource(MR.strings.wallet_swap_kyc_info_title), color = MaterialTheme.colors.onSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(Modifier.height(6.dp))
        Text(stringResource(MR.strings.wallet_swap_kyc_info_body), color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f), fontSize = 12.sp, lineHeight = 16.sp)
      }
    }
  }
}

@Composable
private fun SwapConfirmScreen(
  fromToken: SwapToken, toToken: SwapToken, amountIn: String, initialQuote: SwapQuote, destAddress: String,
  isWalletLocked: () -> Boolean, onKeepAlive: () -> Unit, closeFlow: () -> Unit,
  onCreated: (tradeId: String, deposit: String, amountIn: Double, createdAt: Long, expiresAt: Long) -> Unit
) {
  val scope = rememberCoroutineScope()
  var quote by remember { mutableStateOf(initialQuote) }
  var quoteSecondsLeft by remember { mutableStateOf(initialQuote.expiresIn.takeIf { it > 0 } ?: 60) }
  var requoteTick by remember { mutableStateOf(0) }
  var isSubmitting by remember { mutableStateOf(false) }
  var errorText by remember { mutableStateOf<String?>(null) }

  LaunchedEffect(Unit) { while (isActive) { onKeepAlive(); delay(500) } }
  LaunchedEffect(Unit) { while (isActive) { if (isWalletLocked()) { closeFlow(); break }; delay(400) } }

  LaunchedEffect(quote.quoteId, requoteTick) {
    var left = quote.expiresIn.takeIf { it > 0 } ?: 60
    while (isActive && left > 0) { quoteSecondsLeft = left; delay(1000); left-- }
    quoteSecondsLeft = 0
    val resp = SwapService.quotes("in", amountIn, fromToken.tokenId, toToken.tokenId)
    val fresh = resp.quotes.firstOrNull { it.provider == quote.provider } ?: resp.quotes.firstOrNull()
    if (fresh != null) quote = fresh else { delay(5000); requoteTick++ }
  }

  Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colors.background)) {
    ColumnWithScrollBar(modifier = Modifier.fillMaxSize()) {
      AppBarTitle(title = stringResource(MR.strings.wallet_swap_confirm_title))
      Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        SummaryRow(stringResource(MR.strings.wallet_swap_you_send), "${fmtAmount(amountIn.toDoubleOrNull() ?: 0.0)} ${fromToken.symbol.uppercase()}")
        SummaryRow(stringResource(MR.strings.wallet_swap_you_receive), "${fmtAmount(quote.amountOut)} ${toToken.symbol.uppercase()}", highlight = true)
        SummaryRow(stringResource(MR.strings.wallet_swap_summary_provider), quote.provider)
        SummaryRow(stringResource(MR.strings.wallet_swap_summary_destination), shortAddr(destAddress))
        Spacer(Modifier.height(10.dp))
        Text(stringResource(MR.strings.wallet_swap_estimate_note), color = MaterialTheme.colors.onBackground.copy(alpha = 0.5f), fontSize = 12.sp)
        Spacer(Modifier.height(16.dp))
        if (quoteSecondsLeft > 0) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Timer, contentDescription = null, tint = MaterialTheme.colors.primary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(MR.strings.wallet_swap_expires_in, quoteSecondsLeft), color = MaterialTheme.colors.primary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
          }
        }
        if (errorText != null) ErrorLine(stringResource(MR.strings.wallet_swap_error_generic))
        Spacer(Modifier.height(120.dp))
      }
    }
    Column(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(MaterialTheme.colors.background).padding(horizontal = 20.dp).padding(top = 12.dp)) {
      if (isSubmitting) {
        Row(modifier = Modifier.fillMaxWidth().height(56.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
          WalletSpinner(size = 20, strokeWidth = 2f); Spacer(Modifier.width(10.dp)); Text(stringResource(MR.strings.wallet_swap_creating), color = MaterialTheme.colors.onBackground.copy(alpha = 0.7f))
        }
      } else {
        AppHoldToConfirmButton(
          text = stringResource(MR.strings.wallet_swap_btn_confirm_hold),
          isFormValid = true,
          onConfirmed = {
            isSubmitting = true; errorText = null
            scope.launch {
              val resp = SwapService.exchange(
                SwapExchangeRequest(
                  from = fromToken.tokenId, to = toToken.tokenId, quoteId = quote.quoteId,
                  amount = amountIn, addressTo = destAddress, dir = "in", quoteToken = quote.quoteToken
                )
              )
              val id = resp.tradeId; val deposit = resp.deposit
              if (resp.error == null && id != null && deposit != null) {
                val createdAt = System.currentTimeMillis()
                val expiresAt = resp.expiryMillis ?: 0L
                val record = PendingSwap(
                  tradeId = id, depositAddress = deposit, amountIn = quote.amountIn,
                  fromTokenId = fromToken.tokenId, toTokenId = toToken.tokenId,
                  fromSymbol = fromToken.symbol, toSymbol = toToken.symbol,
                  amountOut = quote.amountOut, lastStatus = "new",
                  createdAt = createdAt, expiresAt = expiresAt
                )
                PendingSwapStore.save(record)
                SwapHistoryStore.upsert(record)
                onCreated(id, deposit, quote.amountIn, createdAt, expiresAt)
              } else {
                isSubmitting = false
                errorText = resp.error ?: "exchange"
              }
            }
          }
        )
      }
      Spacer(Modifier.height(8.dp))
      Text(
        stringResource(MR.strings.wallet_swap_btn_back), color = MaterialTheme.colors.onBackground.copy(alpha = 0.6f),
        fontSize = 14.sp, textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().clickable(enabled = !isSubmitting) { ModalManager.start.closeModal() }.padding(vertical = 8.dp)
      )
      SectionBottomSpacer()
    }
  }
}

@Composable
private fun SummaryRow(label: String, value: String, highlight: Boolean = false) {
  Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
    Text(label, color = MaterialTheme.colors.onBackground.copy(alpha = 0.6f), fontSize = 14.sp)
    Text(value, color = if (highlight) MaterialTheme.colors.primary else MaterialTheme.colors.onBackground, fontWeight = FontWeight.Bold, fontSize = 15.sp)
  }
  Divider(color = MaterialTheme.colors.onBackground.copy(alpha = 0.06f))
}

@Composable
private fun SwapTrackingScreen(
  tradeId: String, depositAddress: String, amountIn: Double, fromToken: SwapToken, toToken: SwapToken?,
  createdAt: Long, expiresAt: Long,
  walletManager: WalletManager, walletAssets: List<CryptoAsset>, currency: String,
  isWalletLocked: () -> Boolean, closeFlow: () -> Unit,
  onPayFromWallet: (CryptoAsset, String, String) -> Unit,
  initialStatus: SwapStatusUi = SwapStatusUi.NEW,
  onCopy: (String) -> Unit, onHelp: () -> Unit
) {
  var status by remember { mutableStateOf(initialStatus) }
  var consecutiveFailures by remember { mutableStateOf(0) }
  val reconnecting = consecutiveFailures >= 2
  var qr by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }

  val expiry = remember { swapExpiryDeadline(createdAt, expiresAt, System.currentTimeMillis()) }
  var expirySecondsLeft by remember { mutableStateOf(0) }

  fun persistStatus(rawStatus: String, terminal: Boolean) {
    val amountOut = SwapHistoryStore.list().firstOrNull { it.tradeId == tradeId }?.amountOut ?: 0.0
    val record = PendingSwap(
      tradeId = tradeId, depositAddress = depositAddress, amountIn = amountIn,
      fromTokenId = fromToken.tokenId, toTokenId = toToken?.tokenId ?: "",
      fromSymbol = fromToken.symbol, toSymbol = toToken?.symbol ?: "",
      amountOut = amountOut, lastStatus = rawStatus, createdAt = createdAt, expiresAt = expiresAt
    )
    SwapHistoryStore.upsert(record)
    if (terminal) PendingSwapStore.clear() else PendingSwapStore.save(record)
  }

  LaunchedEffect(status) {
    if (!status.isAwaitingDeposit) { expirySecondsLeft = 0; return@LaunchedEffect }
    while (isActive && status.isAwaitingDeposit) {
      val remain = ((expiry - System.currentTimeMillis()) / 1000L).toInt().coerceAtLeast(0)
      expirySecondsLeft = remain
      if (remain <= 0) {
        val s = SwapService.status(tradeId)
        val api = mapSwapStatus(s?.status)
        if (s != null && !s.status.isNullOrBlank() && !api.isAwaitingDeposit) {
          status = api
          persistStatus(s.status!!, api.isTerminal)
        } else {
          status = SwapStatusUi.EXPIRED
          persistStatus("expired", true)
        }
        break
      }
      delay(1000)
    }
  }

  LaunchedEffect(Unit) { while (isActive) { if (isWalletLocked()) { closeFlow(); break }; delay(400) } }

  LaunchedEffect(depositAddress) {
    withContext(Dispatchers.IO) {
      val bmp = qrCodeBitmap(content = depositAddress, size = 1024, errorLevel = QrCode.ErrorLevel.M)
      withContext(Dispatchers.Main) { qr = bmp }
    }
  }

  LaunchedEffect(tradeId) {
    if (DEBUG_SWAP_POLLING) Log.d("SwapPolling", "tracking poll START tradeId='$tradeId' initial=$initialStatus")
    while (isActive) {
      if (status == SwapStatusUi.EXPIRED) break
      val s = SwapService.status(tradeId)
      if (status == SwapStatusUi.EXPIRED) break
      if (s != null && !s.status.isNullOrBlank()) {
        consecutiveFailures = 0
        val apiStatus = mapSwapStatus(s.status)
        val locallyExpired = apiStatus.isAwaitingDeposit && System.currentTimeMillis() >= expiry
        val newStatus = if (locallyExpired) SwapStatusUi.EXPIRED else apiStatus
        if (DEBUG_SWAP_POLLING) Log.d("SwapPolling", "update tradeId='$tradeId' raw='${s.status}' -> $newStatus (locallyExpired=$locallyExpired)")
        status = newStatus
        persistStatus(if (locallyExpired) "expired" else s.status!!, newStatus.isTerminal)
        if (newStatus.isTerminal) break
      } else {
        consecutiveFailures++
        if (DEBUG_SWAP_POLLING) Log.w("SwapPolling", "no usable status for tradeId='$tradeId' (failures=$consecutiveFailures)")
      }
      delay(10000)
    }
  }

  val payAsset = remember(fromToken, walletAssets) { SwapTokenMapping.walletAssetFor(fromToken.tokenId, walletAssets) }
  val currencySymbol = if (currency == "EUR") "€" else "$"

  fun openPay() {
    val asset = payAsset ?: return
    val nativeBalance = walletAssets.firstOrNull {
      it.coinType == CryptoNetwork.fromId(asset.coinType).id && FeeService.isNativeToken(it.contractAddress)
    }?.balance?.replace(",", ".")?.toDoubleOrNull() ?: 0.0
    ModalManager.start.showModalCloseable { _ ->
      WalletSendView(
        asset = asset, nativeBalance = nativeBalance, currency = currency, currencySymbol = currencySymbol,
        initialAddress = depositAddress, initialAmount = fmtAmount(amountIn), isInvoiceMode = true,
        verifyPin = { pin -> CryptoManager.wallet?.validatePin(pin) ?: false },
        onBack = { ModalManager.start.closeModal() },
        onScanClick = {},
        onNext = { dest, amount -> ModalManager.start.closeModal(); onPayFromWallet(asset, dest, amount) }
      )
    }
  }

  Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colors.background)) {
    ColumnWithScrollBar(modifier = Modifier.fillMaxSize()) {
      AppBarTitle(title = stringResource(MR.strings.wallet_swap_tracking_title))
      Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        StatusBadge(status)
        if (reconnecting) {
          Spacer(Modifier.height(8.dp))
          Text(stringResource(MR.strings.wallet_swap_reconnecting), color = MaterialTheme.colors.onBackground.copy(alpha = 0.45f), fontSize = 12.sp)
        }
        if (status.isAwaitingDeposit && expirySecondsLeft > 0) {
          Spacer(Modifier.height(10.dp))
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Timer, contentDescription = null, tint = MaterialTheme.colors.onBackground.copy(alpha = 0.6f), modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(MR.strings.wallet_swap_swap_expires_in, fmtCountdown(expirySecondsLeft)), color = MaterialTheme.colors.onBackground.copy(alpha = 0.6f), fontSize = 13.sp)
          }
        }
        if (status.isAwaitingDeposit) {
          Spacer(Modifier.height(20.dp))
          Text(
            stringResource(MR.strings.wallet_swap_send_exactly, fmtAmount(amountIn), fromToken.symbol.uppercase()),
            color = MaterialTheme.colors.primary, fontWeight = FontWeight.Bold, fontSize = 20.sp, textAlign = TextAlign.Center
          )
          Spacer(Modifier.height(24.dp))
          Box(modifier = Modifier.clip(RoundedCornerShape(24.dp)).background(Color.White).padding(20.dp)) {
            if (qr != null) Image(bitmap = qr!!, contentDescription = null, modifier = Modifier.size(200.dp))
            else Box(Modifier.size(200.dp), contentAlignment = Alignment.Center) { WalletSpinner(size = 28, strokeWidth = 3f) }
          }
          Spacer(Modifier.height(24.dp))
          Card(
            backgroundColor = MaterialTheme.colors.onBackground.copy(alpha = 0.05f), shape = RoundedCornerShape(16.dp), elevation = 0.dp,
            modifier = Modifier.fillMaxWidth().clickable { onCopy(depositAddress) }
          ) {
            Column(modifier = Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
              Text(stringResource(MR.strings.wallet_swap_deposit_address), color = MaterialTheme.colors.onBackground.copy(alpha = 0.5f), fontSize = 12.sp)
              Spacer(Modifier.height(6.dp))
              Text(depositAddress, color = MaterialTheme.colors.onSurface, fontWeight = FontWeight.Medium, fontSize = 14.sp, textAlign = TextAlign.Center)
            }
          }

          TradeReferenceCard(tradeId = tradeId, onCopy = onCopy)

          if (payAsset != null) {
            Spacer(Modifier.height(16.dp))
            AppPrimaryButton(text = stringResource(MR.strings.wallet_swap_pay_with_wallet), onClick = { openPay() })
          }

          Spacer(Modifier.height(16.dp))
          Row(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colors.primary.copy(alpha = 0.08f), RoundedCornerShape(12.dp)).border(1.dp, MaterialTheme.colors.primary.copy(alpha = 0.25f), RoundedCornerShape(12.dp)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colors.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(MR.strings.wallet_swap_deposit_warning), color = MaterialTheme.colors.onBackground.copy(alpha = 0.8f), fontSize = 12.sp, lineHeight = 16.sp)
          }
        } else {
          Spacer(Modifier.height(24.dp))
          TradeReferenceCard(tradeId = tradeId, onCopy = onCopy)
        }

        Spacer(Modifier.height(16.dp))
        Text(
          stringResource(MR.strings.wallet_swap_need_help),
          color = MaterialTheme.colors.primary, fontSize = 14.sp, fontWeight = FontWeight.Medium,
          modifier = Modifier
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onHelp() }
            .padding(vertical = 6.dp)
        )
        Spacer(Modifier.height(40.dp))
      }
    }
  }
}

@Composable
private fun TradeReferenceCard(tradeId: String, onCopy: (String) -> Unit) {
  Card(
    backgroundColor = MaterialTheme.colors.onBackground.copy(alpha = 0.05f), shape = RoundedCornerShape(16.dp), elevation = 0.dp,
    modifier = Modifier.fillMaxWidth().clickable { onCopy(tradeId) }
  ) {
    Column(modifier = Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
      Text(stringResource(MR.strings.wallet_swap_trade_reference), color = MaterialTheme.colors.onBackground.copy(alpha = 0.5f), fontSize = 12.sp)
      Spacer(Modifier.height(6.dp))
      Text(tradeId, color = MaterialTheme.colors.onSurface, fontWeight = FontWeight.Medium, fontSize = 14.sp, textAlign = TextAlign.Center)
    }
  }
}

@Composable
private fun StatusBadge(status: SwapStatusUi) {
  val (label, color) = when (status) {
    SwapStatusUi.FINISHED -> stringResource(MR.strings.wallet_swap_status_finished) to SimplexGreen
    SwapStatusUi.FAILED -> stringResource(MR.strings.wallet_swap_status_failed) to MaterialTheme.colors.error
    SwapStatusUi.EXPIRED -> stringResource(MR.strings.wallet_swap_status_expired) to MaterialTheme.colors.error
    SwapStatusUi.REFUNDED -> stringResource(MR.strings.wallet_swap_status_refunded) to WarningOrange
    SwapStatusUi.CONFIRMING -> stringResource(MR.strings.wallet_swap_status_confirming) to MaterialTheme.colors.primary
    SwapStatusUi.SENDING -> stringResource(MR.strings.wallet_swap_status_sending) to MaterialTheme.colors.primary
    SwapStatusUi.WAITING -> stringResource(MR.strings.wallet_swap_status_waiting) to MaterialTheme.colors.primary
    SwapStatusUi.NEW -> stringResource(MR.strings.wallet_swap_status_new) to MaterialTheme.colors.primary
    SwapStatusUi.UNKNOWN -> stringResource(MR.strings.wallet_swap_status_unknown) to MaterialTheme.colors.onBackground.copy(alpha = 0.6f)
  }
  Surface(shape = RoundedCornerShape(20.dp), color = color.copy(alpha = 0.12f), border = BorderStroke(1.dp, color.copy(alpha = 0.4f))) {
    Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
      if (!status.isTerminal) {
        WalletSpinner(size = 14, strokeWidth = 2f); Spacer(Modifier.width(8.dp))
      }
      Text("${stringResource(MR.strings.wallet_swap_status_label)}: $label", color = color, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
  }
}

@Composable
private fun ErrorLine(text: String) {
  Text(text, color = MaterialTheme.colors.error, fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp, top = 4.dp))
}

@Composable
private fun kycColor(kyc: String): Color = when (kyc.trim().uppercase().firstOrNull()) {
  'A' -> SimplexGreen
  'B' -> WarningYellow
  'C' -> WarningOrange
  'D' -> MaterialTheme.colors.error
  else -> MaterialTheme.colors.onBackground.copy(alpha = 0.4f)
}

private fun fmtAmount(v: Double): String {
  if (v == 0.0) return "0"
  val s = String.format(Locale.US, "%.8f", v)
  return if (s.contains('.')) s.trimEnd('0').trimEnd('.') else s
}

private fun fmtAmount(s: String): String = fmtAmount(s.toDoubleOrNull() ?: 0.0)

private fun fmtFiat(v: Double): String = String.format(Locale.US, "%.2f", v)

private fun fmtCountdown(totalSeconds: Int): String {
  val s = totalSeconds.coerceAtLeast(0)
  return String.format(Locale.US, "%d:%02d", s / 60, s % 60)
}

private fun shortAddr(a: String): String = if (a.length <= 16) a else "${a.take(8)}…${a.takeLast(6)}"
