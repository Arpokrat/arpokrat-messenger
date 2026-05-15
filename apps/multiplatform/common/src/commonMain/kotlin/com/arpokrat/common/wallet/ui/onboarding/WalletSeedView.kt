package com.arpokrat.common.wallet.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arpokrat.common.platform.ColumnWithScrollBar
import com.arpokrat.common.ui.theme.DEFAULT_PADDING
import com.arpokrat.common.views.helpers.AppBarTitle
import com.arpokrat.common.views.helpers.AppPrimaryButton
import com.arpokrat.res.MR
import dev.icerock.moko.resources.compose.painterResource
import dev.icerock.moko.resources.compose.stringResource

@Composable
fun MnemonicBackupView(mnemonic: String, onNext: () -> Unit) {
  val clipboardManager = LocalClipboardManager.current

  ColumnWithScrollBar(modifier = Modifier.fillMaxSize()) {

    AppBarTitle(title = stringResource(MR.strings.wallet_step_backup))
    Spacer(Modifier.height(40.dp))

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = DEFAULT_PADDING)) {

      Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
      ) {
        Icon(
          imageVector = Icons.Default.Warning,
          contentDescription = "Warning",
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
        val words = mnemonic.split(" ")

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

        Spacer(Modifier.height(24.dp))

        Row(
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { clipboardManager.setText(AnnotatedString(mnemonic)) }
            .padding(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(painterResource(MR.images.ic_content_copy), null, tint = MaterialTheme.colors.primary, modifier = Modifier.size(20.dp))
          Spacer(Modifier.width(8.dp))
          Text(stringResource(MR.strings.wallet_settings_copy_phrase), color = MaterialTheme.colors.primary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
      }

      Spacer(Modifier.height(48.dp))

      AppPrimaryButton(
        text = stringResource(MR.strings.wallet_btn_written_down),
        onClick = onNext
      )

      Spacer(Modifier.height(40.dp))
    }
  }
}

@Composable
fun VerifySeedView(mnemonic: String, onVerified: () -> Unit) {
  val words = remember { mnemonic.split(" ") }
  val indices = remember { words.indices.shuffled().take(3).sorted() }
  val inputs = remember { mutableStateListOf("", "", "") }
  var error by remember { mutableStateOf<String?>(null) }
  val errorMsg = stringResource(MR.strings.wallet_verify_error_incorrect)

  ColumnWithScrollBar(modifier = Modifier.fillMaxSize()) {

    AppBarTitle(title = stringResource(MR.strings.wallet_step_verify))
    Spacer(Modifier.height(40.dp))

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = DEFAULT_PADDING)) {
      Text(
        text = stringResource(MR.strings.wallet_verify_desc),
        color = MaterialTheme.colors.onBackground.copy(0.7f),
        modifier = Modifier.padding(bottom = 24.dp),
        lineHeight = 22.sp
      )

      indices.forEachIndexed { i, originalIndex ->
        Text(
          text = stringResource(MR.strings.wallet_verify_word_format, originalIndex + 1),
          color = MaterialTheme.colors.primary,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.padding(bottom = 8.dp, top = 8.dp)
        )
        OutlinedTextField(
          value = inputs[i],
          onValueChange = { inputs[i] = it.trim().lowercase() },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true,
          colors = TextFieldDefaults.outlinedTextFieldColors(
            textColor = MaterialTheme.colors.onBackground,
            cursorColor = MaterialTheme.colors.primary,
            focusedBorderColor = MaterialTheme.colors.primary,
            unfocusedBorderColor = MaterialTheme.colors.onBackground.copy(alpha = 0.2f)
          ),
          shape = RoundedCornerShape(12.dp)
        )
      }

      if (error != null) {
        Spacer(Modifier.height(16.dp))
        Text(error!!, color = MaterialTheme.colors.error, fontSize = 14.sp, fontWeight = FontWeight.Medium)
      }

      Spacer(Modifier.height(48.dp))

      AppPrimaryButton(
        text = stringResource(MR.strings.wallet_btn_verify_create),
        onClick = {
          var isValid = true
          indices.forEachIndexed { i, originalIndex ->
            if (inputs[i] != words[originalIndex]) isValid = false
          }
          if (isValid) onVerified() else error = errorMsg
        }
      )

      Spacer(Modifier.height(40.dp))
    }
  }
}

@Composable
fun WordPill(number: Int, word: String, modifier: Modifier = Modifier) {
  Row(
    modifier = modifier
      .background(MaterialTheme.colors.onBackground.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
      .padding(horizontal = 12.dp, vertical = 14.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      text = "$number.",
      color = MaterialTheme.colors.onBackground.copy(alpha = 0.4f),
      fontSize = 14.sp,
      fontWeight = FontWeight.Medium,
      modifier = Modifier.width(28.dp)
    )
    Text(
      text = word,
      color = MaterialTheme.colors.onBackground,
      fontSize = 16.sp,
      fontWeight = FontWeight.Bold
    )
  }
}