package com.arpokrat.common.wallet.ui.transaction

import SectionBottomSpacer
import androidx.compose.animation.*
import androidx.compose.foundation.*
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arpokrat.app.wallet.PriceService
import com.arpokrat.common.platform.ColumnWithScrollBar
import com.arpokrat.common.views.helpers.*
import com.arpokrat.common.wallet.*
import com.arpokrat.common.wallet.ui.components.*
import com.arpokrat.common.wallet.ui.onboarding.VerifyWalletPasscodeView
import com.arpokrat.res.MR
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

private var pendingScannedAddress: String? = null

/**
 * Main UI component for sending crypto assets.
 * Handles amount input (with fiat/crypto toggle), recipient address validation across multiple chains,
 * real-time network fee estimation, balance sufficiency checks, and PIN verification before broadcasting.
 * Supports both standalone transfers and pre-filled invoice modes.
 */
@Composable
fun WalletSendView(
  asset: CryptoAsset,
  nativeBalance: Double,
  currency: String,
  currencySymbol: String,
  initialAddress: String = "",
  initialAmount: String = "",
  isInvoiceMode: Boolean = false,
  verifyPin: (String) -> Boolean,
  onBack: () -> Unit,
  onScanClick: ((String) -> Unit) -> Unit,
  onNext: (String, String) -> Unit
) {
  var address by rememberSaveable { mutableStateOf(initialAddress) }
  var inputValue by rememberSaveable { mutableStateOf(initialAmount) }
  var isFiatMode by remember { mutableStateOf(false) }
  var showPinPrompt by remember { mutableStateOf(false) }

  val msgAddressScanned = stringResource(MR.strings.wallet_notif_address_scanned)
  LaunchedEffect(Unit) {
    pendingScannedAddress?.let {
      address = it
      pendingScannedAddress = null
      AppNotificationManager.showSuccess(msgAddressScanned)
    }
  }

  val focusRequester = remember { FocusRequester() }
  val keyboardController = LocalSoftwareKeyboardController.current
  val focusManager = LocalFocusManager.current

  fun onAmountChange(newValue: String) {
    if (isInvoiceMode) return
    val filtered = newValue.replace(",", ".").replace("\n", "")
    if (filtered.isEmpty() || filtered.matches(Regex("^\\d*\\.?\\d*$"))) inputValue = filtered
  }

  var tokenPrice by remember { mutableStateOf(0.0) }
  LaunchedEffect(asset.symbol, currency) {
    withContext(Dispatchers.IO) {
      var price = PriceService.getValue(asset.symbol, 1.0, currency)
      if (price <= 0.0) { PriceService.fetchPrices(listOf(asset.symbol)); price = PriceService.getValue(asset.symbol, 1.0, currency) }
      tokenPrice = price
    }
  }

  val cryptoAmount = if (isFiatMode) {
    val fiatVal = inputValue.toDoubleOrNull() ?: 0.0
    if (tokenPrice > 0) java.math.BigDecimal(fiatVal / tokenPrice).toPlainString() else "0"
  } else inputValue.ifBlank { "0" }
  val parsedCryptoAmount = cryptoAmount.toDoubleOrNull() ?: 0.0

  val displayEquivalent = if (isFiatMode) "${String.format(Locale.US, "%.6f", parsedCryptoAmount)} ${asset.symbol}" else "≈ $currencySymbol${String.format(Locale.US, "%.2f", parsedCryptoAmount * tokenPrice)}"

  val isAddressValid = remember(address) {
    if (address.isBlank()) true
    else {
      when (asset.coinType) {
        CryptoNetwork.BITCOIN.id, CryptoNetwork.BITCOIN_TESTNET.id -> address.matches(Regex("^(bc1|[13])[a-zA-HJ-NP-Z0-9]{25,39}$"))
        CryptoNetwork.SOLANA.id, CryptoNetwork.SOLANA_DEVNET.id -> address.matches(Regex("^[1-9A-HJ-NP-Za-km-z]{32,44}$"))
        CryptoNetwork.TRON.id, CryptoNetwork.TRON_NILE.id -> address.matches(Regex("^T[A-Za-z1-9]{33}$"))
        else -> address.matches(Regex("^0x[a-fA-F0-9]{40}$"))
      }
    }
  }

  var feeCryptoAmount by remember { mutableStateOf(0.0) }
  var mockFeeCrypto by remember { mutableStateOf("0.00") }
  var mockFeeFiat by remember { mutableStateOf("0.00") }
  var isCalculatingFee by remember { mutableStateOf(true) }

  val nativeNetwork = remember(asset) { CryptoNetwork.fromId(asset.coinType) }
  val nativeSymbol = nativeNetwork.symbol

  var nativeTokenPrice by remember { mutableStateOf(0.0) }
  LaunchedEffect(nativeSymbol, currency) {
    withContext(Dispatchers.IO) {
      var price = PriceService.getValue(nativeSymbol, 1.0, currency)
      if (price <= 0.0) { PriceService.fetchPrices(listOf(nativeSymbol)); price = PriceService.getValue(nativeSymbol, 1.0, currency) }
      nativeTokenPrice = price
    }
  }

  val estimatedTime = remember(nativeNetwork) {
    when (nativeNetwork) {
      CryptoNetwork.BITCOIN, CryptoNetwork.BITCOIN_TESTNET -> "~ 10 to 30 mins"
      CryptoNetwork.ETHEREUM, CryptoNetwork.ETHEREUM_SEPOLIA -> "~ 15 secs"
      CryptoNetwork.POLYGON, CryptoNetwork.POLYGON_AMOY -> "~ 3 secs"
      CryptoNetwork.SOLANA, CryptoNetwork.SOLANA_DEVNET -> "~ 1 to 2 secs"
      CryptoNetwork.TRON, CryptoNetwork.TRON_NILE -> "~ 3 secs"
    }
  }

  LaunchedEffect(asset, nativeTokenPrice) {
    isCalculatingFee = true
    val estimatedFee = FeeService.estimateFee(asset)
    feeCryptoAmount = estimatedFee
    mockFeeCrypto = String.format(Locale.US, "%.6f", estimatedFee).trimEnd('0').trimEnd('.')
    mockFeeFiat = String.format(Locale.US, "%.2f", estimatedFee * nativeTokenPrice)
    isCalculatingFee = false
  }

  val assetBalance = asset.balance.replace(",", ".").toDoubleOrNull() ?: 0.0
  val isNativeToken = FeeService.isNativeToken(asset.contractAddress)

  val actualNativeBalance = if (isNativeToken) assetBalance else nativeBalance

  val hasEnoughAsset = if (isNativeToken) parsedCryptoAmount + feeCryptoAmount <= assetBalance else parsedCryptoAmount <= assetBalance
  val hasEnoughGas = actualNativeBalance >= feeCryptoAmount
  val isAmountValid = parsedCryptoAmount > 0.0

  val isFormValid = if (isInvoiceMode) isAmountValid && hasEnoughAsset && hasEnoughGas && !isCalculatingFee else isAmountValid && hasEnoughAsset && hasEnoughGas && address.isNotBlank() && isAddressValid && !isCalculatingFee
  val amountTextColor = if (!hasEnoughAsset && inputValue.isNotBlank()) MaterialTheme.colors.error else MaterialTheme.colors.onBackground

  Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colors.background)) {
    if (showPinPrompt) {
      VerifyWalletPasscodeView(
        title = stringResource(MR.strings.wallet_confirm_transaction),
        verifyPin = verifyPin,
        onSuccess = { showPinPrompt = false; onNext(address, cryptoAmount) },
        onCancel = { showPinPrompt = false }
      )
    } else {
      Box(modifier = Modifier.fillMaxSize()) {
        ColumnWithScrollBar(modifier = Modifier.fillMaxSize()) {
          AppBarTitle(title = if (isInvoiceMode) stringResource(MR.strings.wallet_send_title_invoice) else stringResource(MR.strings.wallet_send_title_asset, asset.symbol))

          Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(MR.strings.wallet_send_network_subtitle, asset.name), color = Color.Gray, fontSize = 14.sp)
            Spacer(Modifier.height(32.dp))

            Row(
              verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
              modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable(enabled = !isInvoiceMode, interactionSource = remember { MutableInteractionSource() }, indication = null) {
                isFiatMode = !isFiatMode; inputValue = ""; focusRequester.requestFocus(); keyboardController?.show()
              }.padding(vertical = 4.dp)
            ) {
              if (!isInvoiceMode) Box(modifier = Modifier.size(28.dp))
              Spacer(Modifier.width(8.dp))
              Text(text = stringResource(MR.strings.wallet_amount_format, if (isFiatMode) currency else asset.symbol), color = if (isInvoiceMode) Color.Gray else MaterialTheme.colors.primary, fontWeight = FontWeight.Medium, fontSize = 16.sp, textAlign = TextAlign.Center)
              Spacer(Modifier.width(8.dp))
              if (!isInvoiceMode) Box(modifier = Modifier.size(28.dp).clip(CircleShape).background(MaterialTheme.colors.primary.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) { Icon(Icons.Default.SwapVert, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colors.primary) }
            }

            Spacer(Modifier.height(4.dp))

            Box(
              modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp).clickable(enabled = !isInvoiceMode, interactionSource = remember { MutableInteractionSource() }, indication = null) { focusRequester.requestFocus(); keyboardController?.show() },
              contentAlignment = Alignment.Center
            ) {
              Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.Center) {
                if (isFiatMode) Text(text = "$currencySymbol ", fontSize = 56.sp, fontWeight = FontWeight.Medium, color = if (inputValue.isEmpty()) Color.Gray.copy(alpha = 0.3f) else amountTextColor)
                BasicTextField(
                  value = inputValue, onValueChange = ::onAmountChange, readOnly = isInvoiceMode,
                  textStyle = TextStyle(fontSize = 56.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center, color = amountTextColor),
                  keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                  cursorBrush = SolidColor(MaterialTheme.colors.primary),
                  modifier = Modifier.focusRequester(focusRequester).width(IntrinsicSize.Min).defaultMinSize(minWidth = 32.dp),
                  decorationBox = { innerTextField -> Box(contentAlignment = Alignment.Center) { if (inputValue.isEmpty()) Text("0", fontSize = 56.sp, color = Color.Gray.copy(alpha = 0.3f), fontWeight = FontWeight.Medium); innerTextField() } }
                )
                if (!isFiatMode) { Spacer(Modifier.width(8.dp)); Text(text = asset.symbol, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = if (inputValue.isEmpty()) Color.Gray.copy(alpha = 0.3f) else amountTextColor, modifier = Modifier.padding(bottom = 12.dp)) }
              }
            }

            Spacer(Modifier.height(4.dp))
            Text(displayEquivalent, color = Color.Gray, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(12.dp))

            val msgEmptyBalance = stringResource(MR.strings.wallet_error_empty_balance)
            val msgGasTooLow = stringResource(MR.strings.wallet_error_gas_too_low)
            val txtMax = stringResource(MR.strings.wallet_btn_max)

            if (!isInvoiceMode) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(enabled = !isCalculatingFee) {
                  isFiatMode = false
                  if (assetBalance <= 0.0) { AppNotificationManager.showError(msgEmptyBalance); inputValue = "0"; return@clickable }
                  val maxAmount = if (isNativeToken) { val safeFee = feeCryptoAmount * 1.05; val calculatedMax = assetBalance - safeFee; if (calculatedMax > 0) calculatedMax else { AppNotificationManager.showError(msgGasTooLow); 0.0 } } else assetBalance
                  inputValue = if (maxAmount > 0) String.format(Locale.US, "%.6f", maxAmount).trimEnd('0').trimEnd('.') else "0"
                }.padding(horizontal = 12.dp, vertical = 6.dp)
              ) {
                Text(text = if (!hasEnoughAsset && inputValue.isNotBlank()) stringResource(MR.strings.wallet_error_insufficient_asset, asset.symbol) else stringResource(MR.strings.wallet_balance_format, asset.balance, asset.symbol), color = if (!hasEnoughAsset && inputValue.isNotBlank()) MaterialTheme.colors.error else Color.Gray, fontSize = 14.sp, fontWeight = if (!hasEnoughAsset && inputValue.isNotBlank()) FontWeight.Bold else FontWeight.Normal)
                if (hasEnoughAsset || inputValue.isBlank()) { Spacer(Modifier.width(8.dp)); Text(txtMax, color = if (isCalculatingFee) Color.Gray else MaterialTheme.colors.primary, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
              }
            } else {
              Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                Text(text = if (!hasEnoughAsset) stringResource(MR.strings.wallet_error_insufficient_asset, asset.symbol) else stringResource(MR.strings.wallet_balance_format, asset.balance, asset.symbol), color = if (!hasEnoughAsset) MaterialTheme.colors.error else Color.Gray, fontSize = 14.sp, fontWeight = if (!hasEnoughAsset) FontWeight.Bold else FontWeight.Normal)
              }
            }

            Spacer(Modifier.height(32.dp))

            if (!isInvoiceMode) {
              ModernTextField(
                value = address, onValueChange = { address = it.trim() }, label = { Text(stringResource(MR.strings.wallet_recipient_address)) }, modifier = Modifier.fillMaxWidth(),
                trailingIcon = { IconButton(onClick = { focusManager.clearFocus(); keyboardController?.hide(); onScanClick { scannedResult -> pendingScannedAddress = scannedResult } }) { Icon(Icons.Default.QrCodeScanner, contentDescription = stringResource(MR.strings.wallet_scan_qr_desc), tint = MaterialTheme.colors.primary) } },
                isError = !isAddressValid && address.isNotEmpty()
              )
              if (!isAddressValid && address.isNotEmpty()) Text(text = stringResource(MR.strings.wallet_error_invalid_address, asset.name), color = MaterialTheme.colors.error, fontSize = 12.sp, modifier = Modifier.align(Alignment.Start).padding(start = 16.dp, top = 4.dp))
            }
            Spacer(Modifier.height(260.dp))
          }
        }

        Column(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(MaterialTheme.colors.background).padding(horizontal = 24.dp).padding(top = 16.dp)) {
          AnimatedVisibility(visible = !hasEnoughGas && !isCalculatingFee && inputValue.isNotBlank() && isAmountValid, enter = expandVertically(), exit = shrinkVertically()) {
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).background(MaterialTheme.colors.error.copy(alpha = 0.1f), RoundedCornerShape(12.dp)).border(1.dp, MaterialTheme.colors.error.copy(alpha = 0.3f), RoundedCornerShape(12.dp)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colors.error, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text(stringResource(MR.strings.wallet_error_need_gas, mockFeeCrypto, nativeSymbol), color = MaterialTheme.colors.error, fontSize = 13.sp, fontWeight = FontWeight.Medium, lineHeight = 18.sp)
            }
          }
          Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.LocalGasStation, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text(stringResource(MR.strings.wallet_network_fee), color = Color.Gray, fontSize = 14.sp) }
            if (isCalculatingFee) WalletSpinner(size = 16, strokeWidth = 2f)
            else Column(horizontalAlignment = Alignment.End) { Text("~ $mockFeeCrypto $nativeSymbol", color = if (!hasEnoughGas) MaterialTheme.colors.error else MaterialTheme.colors.onBackground, fontSize = 14.sp, fontWeight = FontWeight.Medium); Text("$currencySymbol$mockFeeFiat", color = Color.Gray, fontSize = 12.sp) }
          }
          Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Timer, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text(stringResource(MR.strings.wallet_estimated_time), color = Color.Gray, fontSize = 14.sp) }
            Text(estimatedTime, color = MaterialTheme.colors.onBackground, fontSize = 14.sp, fontWeight = FontWeight.Medium)
          }

          Spacer(Modifier.height(16.dp))

          AppHoldToConfirmButton(
            text = when {
              isFormValid -> stringResource(MR.strings.wallet_btn_hold_to_continue)
              !isAmountValid -> stringResource(MR.strings.wallet_btn_enter_amount)
              !hasEnoughAsset -> stringResource(MR.strings.wallet_btn_insufficient_balance)
              !hasEnoughGas -> stringResource(MR.strings.wallet_btn_not_enough_gas)
              address.isBlank() || !isAddressValid -> stringResource(MR.strings.wallet_recipient_address)
              else -> stringResource(MR.strings.wallet_recipient_address)
            },
            isFormValid = isFormValid,
            onConfirmed = { showPinPrompt = true }
          )

          SectionBottomSpacer()
        }
      }
    }
  }
}