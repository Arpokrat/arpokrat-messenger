package com.arpokrat.common.wallet.ui.settings

import SectionBottomSpacer
import SectionItemView
import SectionView
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arpokrat.common.platform.BackHandler
import com.arpokrat.common.platform.ColumnWithScrollBar
import com.arpokrat.common.ui.theme.DEFAULT_PADDING
import com.arpokrat.common.views.helpers.*
import com.arpokrat.common.wallet.CryptoNetwork
import com.arpokrat.common.wallet.WalletManager
import com.arpokrat.res.MR
import dev.icerock.moko.resources.compose.painterResource
import dev.icerock.moko.resources.compose.stringResource

@Composable
fun WalletBlockExplorerSettingsView(
  walletManager: WalletManager,
  onBack: () -> Unit
) {
  var selectedNetwork by remember { mutableStateOf<CryptoNetwork?>(null) }

  BackHandler {
    if (selectedNetwork != null) {
      selectedNetwork = null
    } else {
      onBack()
    }
  }

  val isDevMode = remember { walletManager.isDeveloperModeEnabled() }
  val testnetIds = listOf(1, 11155111, 80002, 9000, 10000)
  val mainnets = CryptoNetwork.values().filterNot { it.id in testnetIds }
  val testnets = CryptoNetwork.values().filter { it.id in testnetIds }

  Crossfade(targetState = selectedNetwork) { network ->
    if (network == null) {
      Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colors.background)) {
        ColumnWithScrollBar(modifier = Modifier.fillMaxSize()) {
          AppBarTitle(title = stringResource(MR.strings.wallet_block_explorers_title))
          Spacer(Modifier.height(24.dp))

          Text(
            stringResource(MR.strings.wallet_block_explorers_desc),
            color = MaterialTheme.colors.onBackground.copy(alpha = 0.7f),
            fontSize = 14.sp,
            lineHeight = 22.sp,
            modifier = Modifier.padding(horizontal = DEFAULT_PADDING, vertical = 8.dp)
          )
          Spacer(Modifier.height(16.dp))

          SectionView(title = stringResource(MR.strings.wallet_mainnets), titleColor = MaterialTheme.colors.primary) {
            mainnets.forEach { net ->
              SectionItemView(click = { selectedNetwork = net }) {
                Text(net.displayName, color = MaterialTheme.colors.onBackground, modifier = Modifier.weight(1f))
                Icon(painterResource(MR.images.ic_chevron_right), null, tint = MaterialTheme.colors.primary)
              }
            }
          }

          if (isDevMode) {
            Spacer(Modifier.height(16.dp))
            SectionView(title = stringResource(MR.strings.wallet_testnets), titleColor = MaterialTheme.colors.primary) {
              testnets.forEach { net ->
                SectionItemView(click = { selectedNetwork = net }) {
                  Text(net.displayName, color = MaterialTheme.colors.onBackground, modifier = Modifier.weight(1f))
                  Icon(painterResource(MR.images.ic_chevron_right), null, tint = MaterialTheme.colors.primary)
                }
              }
            }
          }

          SectionBottomSpacer()
        }
      }
    } else {
      var inputTxUrl by remember { mutableStateOf(walletManager.getTxExplorerTemplate(network.id)) }
      var inputAddrUrl by remember { mutableStateOf(walletManager.getAddressExplorerTemplate(network.id)) }

      Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colors.background)) {

        ColumnWithScrollBar(modifier = Modifier.fillMaxSize()) {
          AppBarTitle(title = stringResource(MR.strings.wallet_explorer_network_title, network.displayName))

          Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
            Spacer(Modifier.height(32.dp))

            Text(stringResource(MR.strings.wallet_tx_url), color = MaterialTheme.colors.secondary, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 8.dp, start = 4.dp))
            ModernTextField(
              value = inputTxUrl,
              onValueChange = { inputTxUrl = it },
              modifier = Modifier.fillMaxWidth()
            )
            Text(stringResource(MR.strings.wallet_tx_url_desc), color = MaterialTheme.colors.onBackground.copy(alpha = 0.4f), fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp, start = 4.dp, bottom = 24.dp))

            Text(stringResource(MR.strings.wallet_addr_url), color = MaterialTheme.colors.secondary, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 8.dp, start = 4.dp))
            ModernTextField(
              value = inputAddrUrl,
              onValueChange = { inputAddrUrl = it },
              modifier = Modifier.fillMaxWidth()
            )
            Text(stringResource(MR.strings.wallet_addr_url_desc), color = MaterialTheme.colors.onBackground.copy(alpha = 0.4f), fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp, start = 4.dp))

            Spacer(Modifier.height(180.dp))
          }
        }

        Column(
          modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .background(
              Brush.verticalGradient(
                colors = listOf(Color.Transparent, MaterialTheme.colors.background),
                startY = 0f,
                endY = 80f
              )
            )
            .background(MaterialTheme.colors.background.copy(alpha = 0.8f))
            .padding(horizontal = 24.dp)
            .padding(top = 16.dp)
        ) {

          AppPrimaryButton(
            text = stringResource(MR.strings.wallet_btn_save_changes),
            onClick = {
              walletManager.setTxExplorerTemplate(network.id, inputTxUrl)
              walletManager.setAddressExplorerTemplate(network.id, inputAddrUrl)
              selectedNetwork = null
            }
          )

          Spacer(Modifier.height(12.dp))

          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AppSecondaryButton(
              text = stringResource(MR.strings.wallet_btn_cancel),
              onClick = { selectedNetwork = null },
              modifier = Modifier.weight(1f)
            )

            AppTextButton(
              text = stringResource(MR.strings.wallet_btn_clear_all),
              textColor = MaterialTheme.colors.error,
              onClick = {
                inputTxUrl = ""
                inputAddrUrl = ""
              },
              modifier = Modifier.weight(1f)
            )
          }

          SectionBottomSpacer()
        }
      }
    }
  }
}