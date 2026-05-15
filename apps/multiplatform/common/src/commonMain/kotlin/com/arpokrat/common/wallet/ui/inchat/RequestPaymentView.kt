package com.arpokrat.common.wallet.ui.inchat

import SectionBottomSpacer
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arpokrat.app.wallet.PriceService
import com.arpokrat.common.platform.ColumnWithScrollBar
import com.arpokrat.common.views.helpers.AppBarTitle
import com.arpokrat.common.views.helpers.AppPrimaryButton
import com.arpokrat.common.wallet.*
import com.arpokrat.common.wallet.ui.components.DynamicCryptoIcon
import com.arpokrat.res.MR
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * UI component for generating a payment request invoice within a chat.
 * Allows the user to specify the requested amount in either Fiat or Crypto,
 * select the desired token/network, and preview the live conversion rate.
 */
@Composable
fun RequestPaymentView(
  isDevMode: Boolean,
  currency: String,
  currencySymbol: String,
  onDismiss: () -> Unit,
  onSendInvoice: (amount: String, token: TokenDefinition, address: String, fiatValue: String) -> Unit
) {
  // Standard testnet network IDs
  val testnetIds = listOf(1, 11155111, 80002, 9000, 10000)
  val availableAssets = remember(isDevMode) {
    if (isDevMode) DefaultAssets.getActiveAssets().filter { it.network.id in testnetIds }
    else DefaultAssets.getActiveAssets().filterNot { it.network.id in testnetIds }
  }

  var amountInput by remember { mutableStateOf("") }
  var errorText by remember { mutableStateOf("") }
  var isFiatMode by remember { mutableStateOf(false) }

  var selectedToken by remember(isDevMode) {
    mutableStateOf(availableAssets.firstOrNull { it.symbol.contains("USDT") } ?: availableAssets.first())
  }

  val focusRequester = remember { FocusRequester() }
  val keyboardController = LocalSoftwareKeyboardController.current

  var tokenPrice by remember { mutableStateOf(0.0) }
  LaunchedEffect(selectedToken.symbol, currency) {
    withContext(Dispatchers.IO) {
      var price = PriceService.getValue(selectedToken.symbol, 1.0, currency)
      if (price <= 0.0) {
        PriceService.fetchPrices(listOf(selectedToken.symbol))
        price = PriceService.getValue(selectedToken.symbol, 1.0, currency)
      }
      tokenPrice = price
    }
  }

  val cryptoAmount = if (isFiatMode) {
    val fiatVal = amountInput.toDoubleOrNull() ?: 0.0
    if (tokenPrice > 0) (fiatVal / tokenPrice).toString() else "0"
  } else {
    amountInput.ifBlank { "0" }
  }

  val displayEquivalent = if (isFiatMode) {
    val cryptoVal = cryptoAmount.toDoubleOrNull() ?: 0.0
    "${String.format(Locale.US, "%.6f", cryptoVal)} ${selectedToken.symbol}"
  } else {
    val cryptoVal = amountInput.toDoubleOrNull() ?: 0.0
    "≈ $currencySymbol${String.format(Locale.US, "%.2f", cryptoVal * tokenPrice)}"
  }

  Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colors.background)) {

    ColumnWithScrollBar(modifier = Modifier.fillMaxSize()) {

      AppBarTitle(title = stringResource(MR.strings.wallet_request_payment_title))

      LazyRow(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        contentPadding = PaddingValues(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        val nativeAssets = availableAssets.filter { it.contractAddress == null }
        val tokenAssets = availableAssets.filter { it.contractAddress != null }

        val comingSoonAssets = listOf(
          TokenDefinition("XMR", "Monero", CryptoNetwork.BITCOIN, "COMING_SOON_XMR", 12),
          TokenDefinition("ZEC", "Zcash", CryptoNetwork.BITCOIN, "COMING_SOON_ZEC", 8)
        )

        items(nativeAssets) { token -> RequestTokenItem(token, selectedToken, onSelect = { selectedToken = token }) }
        items(comingSoonAssets) { token -> RequestTokenComingSoonItem(token) }
        items(tokenAssets) { token -> RequestTokenItem(token, selectedToken, onSelect = { selectedToken = token }) }
      }

      Spacer(Modifier.height(40.dp))

      Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
          verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
          modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
            isFiatMode = !isFiatMode
            amountInput = ""
            focusRequester.requestFocus()
            keyboardController?.show()
          }.padding(vertical = 4.dp)
        ) {
          Box(modifier = Modifier.size(28.dp))
          Spacer(Modifier.width(8.dp))
          Text(text = stringResource(MR.strings.wallet_amount_format, if (isFiatMode) currency else selectedToken.symbol), color = MaterialTheme.colors.primary, fontWeight = FontWeight.Medium, fontSize = 16.sp, textAlign = TextAlign.Center)
          Spacer(Modifier.width(8.dp))
          Box(modifier = Modifier.size(28.dp).clip(CircleShape).background(MaterialTheme.colors.primary.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.SwapVert, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colors.primary)
          }
        }

        Spacer(Modifier.height(8.dp))

        Box(
          modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { focusRequester.requestFocus(); keyboardController?.show() },
          contentAlignment = Alignment.Center
        ) {
          Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.Center) {
            if (isFiatMode) {
              Text(text = "$currencySymbol ", fontSize = 56.sp, fontWeight = FontWeight.Medium, color = if (amountInput.isEmpty()) Color.Gray.copy(alpha = 0.3f) else MaterialTheme.colors.onSurface)
            }

            BasicTextField(
              value = amountInput,
              onValueChange = { newValue ->
                val filtered = newValue.replace(",", ".").replace("\n", "")
                if (filtered.isEmpty() || filtered.matches(Regex("^\\d*\\.?\\d*$"))) {
                  amountInput = filtered
                  errorText = ""
                }
              },
              textStyle = TextStyle(fontSize = 56.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center, color = MaterialTheme.colors.onSurface),
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = androidx.compose.ui.text.input.ImeAction.Done),
              cursorBrush = SolidColor(MaterialTheme.colors.primary),
              modifier = Modifier.focusRequester(focusRequester).width(IntrinsicSize.Min).defaultMinSize(minWidth = 32.dp),
              decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.Center) {
                  if (amountInput.isEmpty()) { Text("0", fontSize = 56.sp, color = Color.Gray.copy(alpha = 0.3f), fontWeight = FontWeight.Medium) }
                  innerTextField()
                }
              }
            )

            if (!isFiatMode) {
              Spacer(Modifier.width(8.dp))
              Text(text = selectedToken.symbol, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = if (amountInput.isEmpty()) Color.Gray.copy(alpha = 0.3f) else MaterialTheme.colors.onSurface, modifier = Modifier.padding(bottom = 12.dp))
            }
          }
        }

        Spacer(Modifier.height(8.dp))
        Text(displayEquivalent, color = Color.Gray, fontSize = 18.sp, fontWeight = FontWeight.Medium)

        if (errorText.isNotEmpty()) {
          Text(errorText, color = MaterialTheme.colors.error, modifier = Modifier.padding(top = 8.dp))
        }

        Spacer(Modifier.height(140.dp))
      }
    }

    Column(
      modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(MaterialTheme.colors.background).padding(horizontal = 24.dp).padding(top = 16.dp)
    ) {
      AppPrimaryButton(
        text = stringResource(MR.strings.wallet_btn_send_invoice),
        onClick = {
          val finalCryptoAmount = cryptoAmount.toDoubleOrNull() ?: 0.0
          if (finalCryptoAmount <= 0.0) {
            // TODO Add in strings.xml
            errorText = "Amount must be greater than 0"
          } else {
            val formattedCryptoAmount = String.format(Locale.US, "%.6f", finalCryptoAmount).trimEnd('0').trimEnd('.')
            val myAddress = CryptoManager.wallet?.getAddress(selectedToken.network.id) ?: "0x_ERROR"
            val finalFiatToSend = if (isFiatMode) "FIAT_MODE:$currencySymbol${String.format(Locale.US, "%.2f", amountInput.toDoubleOrNull() ?: 0.0)}" else displayEquivalent
            onSendInvoice(formattedCryptoAmount, selectedToken, myAddress, finalFiatToSend)
          }
        }
      )
      SectionBottomSpacer()
    }
  }
}

/**
 * Renders a selectable token card in the horizontal token selector list.
 */
@Composable
fun RequestTokenItem(token: TokenDefinition, selectedToken: TokenDefinition, onSelect: () -> Unit) {
  val isSelected = selectedToken == token
  val asset = CryptoAsset(token.symbol, token.name, "0", token.decimals, token.network.id, token.contractAddress)

  Box(
    modifier = Modifier.clip(RoundedCornerShape(20.dp)).border(width = if (isSelected) 2.dp else 1.dp, color = if (isSelected) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface.copy(alpha = 0.1f), shape = RoundedCornerShape(20.dp))
      .background(if (isSelected) MaterialTheme.colors.primary.copy(alpha = 0.05f) else MaterialTheme.colors.surface).clickable { onSelect() }.padding(horizontal = 16.dp, vertical = 12.dp)
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      DynamicCryptoIcon(asset = asset, size = 40.dp)
      Spacer(Modifier.width(12.dp))
      Column {
        Text(text = token.symbol, fontWeight = FontWeight.Bold, color = if (isSelected) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface, fontSize = 16.sp)
        Text(text = token.network.displayName, fontSize = 12.sp, color = if (isSelected) MaterialTheme.colors.primary.copy(alpha = 0.8f) else Color.Gray)
      }
    }
  }
}

/**
 * Renders a faded "Coming Soon" token card in the horizontal list to indicate upcoming support.
 */
@Composable
fun RequestTokenComingSoonItem(token: TokenDefinition) {
  val asset = CryptoAsset(token.symbol, token.name, "0", token.decimals, 128, null)

  Box(
    modifier = Modifier.clip(RoundedCornerShape(20.dp)).border(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.05f), RoundedCornerShape(20.dp))
      .background(MaterialTheme.colors.surface.copy(alpha = 0.3f)).padding(horizontal = 16.dp, vertical = 12.dp).alpha(0.5f)
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      DynamicCryptoIcon(asset = asset, size = 40.dp, showNetworkBadge = false)
      Spacer(Modifier.width(12.dp))
      Column {
        Text(text = token.symbol, fontWeight = FontWeight.Bold, color = MaterialTheme.colors.onSurface, fontSize = 16.sp)
        Text(text = stringResource(MR.strings.wallet_coming_soon), fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colors.primary)
      }
    }
  }
}