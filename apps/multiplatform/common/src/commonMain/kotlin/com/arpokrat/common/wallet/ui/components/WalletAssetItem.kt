package com.arpokrat.common.wallet.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arpokrat.common.wallet.CryptoAsset

@Composable
fun WalletAssetItem(
  asset: CryptoAsset,
  fiatValue: String,
  onClick: () -> Unit
) {
  Card(
    backgroundColor = MaterialTheme.colors.surface,
    shape = RoundedCornerShape(16.dp),
    elevation = 0.dp,
    modifier = Modifier
      .fillMaxWidth()
      .height(88.dp)
      .clickable { onClick() }
  ) {
    Row(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 16.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {

      DynamicCryptoIcon(
        asset = asset,
        size = 56.dp
      )

      Spacer(modifier = Modifier.width(16.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = asset.name,
          color = MaterialTheme.colors.onSurface,
          fontWeight = FontWeight.Bold,
          fontSize = 16.sp
        )
        Text(
          text = asset.symbol,
          color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
          fontSize = 14.sp
        )
      }

      Column(horizontalAlignment = Alignment.End) {
        Text(
          text = asset.balance,
          color = MaterialTheme.colors.onSurface,
          fontWeight = FontWeight.Bold,
          fontSize = 16.sp
        )
        Text(
          text = fiatValue,
          color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
          fontSize = 14.sp
        )
      }
    }
  }
}