package com.arpokrat.common.wallet.ui.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arpokrat.common.platform.ColumnWithScrollBar
import com.arpokrat.common.ui.theme.DEFAULT_PADDING
import com.arpokrat.common.views.helpers.AppBarTitle
import com.arpokrat.common.views.helpers.AppPrimaryButton
import com.arpokrat.res.MR
import dev.icerock.moko.resources.compose.stringResource

@Composable
fun ImportWalletView(onImport: (String) -> Unit) {
  var text by remember { mutableStateOf("") }

  ColumnWithScrollBar(modifier = Modifier.fillMaxSize()) {

    AppBarTitle(title = stringResource(MR.strings.wallet_step_import))
    Spacer(Modifier.height(40.dp))

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = DEFAULT_PADDING)) {
      Text(
        text = stringResource(MR.strings.wallet_import_desc),
        color = MaterialTheme.colors.onBackground.copy(0.7f),
        modifier = Modifier.padding(bottom = 24.dp),
        lineHeight = 22.sp
      )

      OutlinedTextField(
        value = text,
        onValueChange = { text = it.lowercase() },
        modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp),
        placeholder = {
          Text(stringResource(MR.strings.wallet_import_placeholder), color = MaterialTheme.colors.onBackground.copy(alpha = 0.3f))
        },
        colors = TextFieldDefaults.outlinedTextFieldColors(
          textColor = MaterialTheme.colors.onBackground,
          cursorColor = MaterialTheme.colors.primary,
          focusedBorderColor = MaterialTheme.colors.primary,
          unfocusedBorderColor = MaterialTheme.colors.onBackground.copy(alpha = 0.2f)
        ),
        shape = RoundedCornerShape(16.dp)
      )

      Spacer(Modifier.height(48.dp))

      AppPrimaryButton(
        text = stringResource(MR.strings.wallet_btn_restore),
        enabled = text.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }.size >= 12,
        onClick = { onImport(text.trim()) }
      )

      Spacer(Modifier.height(40.dp))
    }
  }
}