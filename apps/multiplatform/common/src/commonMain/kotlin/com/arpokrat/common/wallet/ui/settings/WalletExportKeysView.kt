package com.arpokrat.common.wallet.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arpokrat.common.platform.BackHandler
import com.arpokrat.common.platform.ColumnWithScrollBar
import com.arpokrat.common.ui.theme.DEFAULT_PADDING
import com.arpokrat.common.views.helpers.AppBarTitle
import com.arpokrat.res.MR
import dev.icerock.moko.resources.compose.painterResource
import dev.icerock.moko.resources.compose.stringResource

@Composable
fun WalletExportKeysView(
  keys: Map<String, String>,
  onCopy: (String, String) -> Unit,
  onBack: () -> Unit
) {
  BackHandler(onBack = onBack)

  Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colors.background)) {

    ColumnWithScrollBar(modifier = Modifier.fillMaxSize()) {

      AppBarTitle(title = stringResource(MR.strings.wallet_export_keys_title))
      Spacer(Modifier.height(40.dp))

      Column(modifier = Modifier.padding(horizontal = DEFAULT_PADDING)) {

        Row(
          modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.Center
        ) {
          Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = MaterialTheme.colors.error,
            modifier = Modifier.size(28.dp)
          )
          Spacer(Modifier.width(12.dp))
          Text(
            text = stringResource(MR.strings.wallet_export_keys_warning),
            color = MaterialTheme.colors.error,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
          )
        }

        keys.forEach { (network, key) ->
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 8.dp)
              .background(MaterialTheme.colors.surface, RoundedCornerShape(12.dp))
              .clip(RoundedCornerShape(12.dp))
              .clickable { onCopy(key, network) }
              .padding(16.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(network, color = MaterialTheme.colors.secondary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
              Icon(
                painterResource(MR.images.ic_content_copy),
                contentDescription = null,
                tint = MaterialTheme.colors.primary,
                modifier = Modifier.size(20.dp)
              )
            }
            Spacer(Modifier.height(12.dp))
            Text(
              key,
              color = MaterialTheme.colors.onBackground,
              fontSize = 14.sp,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }
        Spacer(Modifier.height(40.dp))
      }
    }
  }
}