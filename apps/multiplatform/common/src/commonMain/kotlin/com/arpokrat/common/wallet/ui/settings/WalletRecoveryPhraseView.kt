package com.arpokrat.common.wallet.ui.settings

import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arpokrat.common.platform.BackHandler
import com.arpokrat.common.platform.ColumnWithScrollBar
import com.arpokrat.common.ui.theme.DEFAULT_PADDING
import com.arpokrat.common.views.helpers.AppBarTitle
import com.arpokrat.common.views.helpers.AppPrimaryButton
import com.arpokrat.common.wallet.ui.onboarding.WordPill
import com.arpokrat.res.MR
import dev.icerock.moko.resources.compose.stringResource

@Composable
fun WalletRecoveryPhraseView(
  mnemonic: String,
  onCopy: (String) -> Unit,
  onBack: () -> Unit
) {
  BackHandler(onBack = onBack)

  Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colors.background)) {

    ColumnWithScrollBar(modifier = Modifier.fillMaxSize()) {

      AppBarTitle(title = stringResource(MR.strings.wallet_settings_show_phrase))
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
            stringResource(MR.strings.wallet_settings_phrase_warning),
            color = MaterialTheme.colors.error,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
          )
        }

        Column(
          modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colors.surface, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .padding(24.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          val words = mnemonic.trim().split("\\s+".toRegex())
          words.chunked(2).forEachIndexed { rowIndex, rowWords ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
              rowWords.forEachIndexed { colIndex, word ->
                val num = rowIndex * 2 + colIndex + 1
                WordPill(number = num, word = word, modifier = Modifier.weight(1f))
              }
              if (rowWords.size < 2) {
                Spacer(modifier = Modifier.weight(1f))
              }
            }
            Spacer(Modifier.height(12.dp))
          }
        }

        Spacer(Modifier.height(140.dp))
      }
    }

    Column(
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .fillMaxWidth()
        .background(MaterialTheme.colors.background)
        .padding(horizontal = 24.dp)
        .padding(top = 16.dp, bottom = 32.dp)
    ) {
      AppPrimaryButton(
        text = stringResource(MR.strings.wallet_settings_copy_phrase),
        onClick = { onCopy(mnemonic) }
      )
    }
  }
}