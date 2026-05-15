package com.arpokrat.common.wallet.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arpokrat.common.wallet.CryptoAsset
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade

@Composable
fun DynamicCryptoIcon(
  asset: CryptoAsset,
  modifier: Modifier = Modifier,
  size: Dp = 48.dp,
  showNetworkBadge: Boolean = true
) {
  Box(modifier = modifier.size(size)) {
    val iconUrl = remember(asset) { getCryptoIconUrl(asset) }

    val fallback = @Composable {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(MaterialTheme.colors.primary.copy(alpha = 0.1f), CircleShape)
          .border(1.dp, MaterialTheme.colors.primary.copy(alpha = 0.2f), CircleShape),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = asset.symbol.take(1).uppercase(),
          color = MaterialTheme.colors.primary,
          fontWeight = FontWeight.Bold,
          fontSize = (size.value / 2.2).sp
        )
      }
    }

    SubcomposeAsyncImage(
      model = ImageRequest.Builder(LocalPlatformContext.current)
        .data(iconUrl)
        .crossfade(true)
        .build(),
      contentDescription = null,
      contentScale = ContentScale.Fit,
      modifier = Modifier.fillMaxSize().clip(CircleShape),
      loading = { fallback() },
      error = { fallback() }
    )

    val isToken = !asset.contractAddress.isNullOrEmpty()
    if (showNetworkBadge && isToken) {
      val chainName = getChainName(asset.coinType)
      val badgeUrl = "https://raw.githubusercontent.com/trustwallet/assets/master/blockchains/$chainName/info/logo.png"

      val badgeSize = size * 0.42f

      Box(
        modifier = Modifier
          .align(Alignment.BottomEnd)
          .size(badgeSize)
          .background(MaterialTheme.colors.surface, CircleShape)
          .border((badgeSize.value * 0.08f).dp, MaterialTheme.colors.surface, CircleShape)
          .padding((badgeSize.value * 0.08f).dp)
      ) {
        AsyncImage(
          model = ImageRequest.Builder(LocalPlatformContext.current)
            .data(badgeUrl)
            .crossfade(true)
            .build(),
          contentDescription = null,
          contentScale = ContentScale.Fit,
          modifier = Modifier.fillMaxSize().clip(CircleShape)
        )
      }
    }
  }
}

fun getCryptoIconUrl(asset: CryptoAsset): String {
  val baseUrl = "https://raw.githubusercontent.com/trustwallet/assets/master/blockchains"
  val symbol = asset.symbol.uppercase()

  if (symbol == "BTC") return "$baseUrl/bitcoin/info/logo.png"
  if (symbol == "ETH") return "$baseUrl/ethereum/info/logo.png"
  if (symbol == "SOL") return "$baseUrl/solana/info/logo.png"
  if (symbol == "TRX") return "$baseUrl/tron/info/logo.png"
  if (symbol == "MATIC" || symbol == "POL") return "$baseUrl/polygon/info/logo.png"
  if (symbol == "XMR") return "https://raw.githubusercontent.com/spothq/cryptocurrency-icons/master/128/color/xmr.png"
  if (symbol == "ZEC") return "$baseUrl/zcash/info/logo.png"

  if (symbol == "USDT") return "$baseUrl/ethereum/assets/0xdAC17F958D2ee523a2206206994597C13D831ec7/logo.png"
  if (symbol == "USDC") return "$baseUrl/ethereum/assets/0xA0b86991c6218b36c1d19D4a2e9Eb0cE3606eB48/logo.png"
  if (symbol == "EURC") return "$baseUrl/ethereum/assets/0x1aBaEA1f7C830bD89Acc67eC4af516284b1bC33c/logo.png"

  if (!asset.contractAddress.isNullOrEmpty()) {
    val chain = getChainName(asset.coinType)
    return "$baseUrl/$chain/assets/${asset.contractAddress}/logo.png"
  }

  val chainName = getChainName(asset.coinType)
  return "$baseUrl/$chainName/info/logo.png"
}

fun getChainName(coinType: Int): String {
  return when (coinType) {
    0, 1 -> "bitcoin"
    60, 11155111 -> "ethereum"
    195, 10000 -> "tron"
    501, 9000 -> "solana"
    137, 80002 -> "polygon"
    else -> "ethereum"
  }
}