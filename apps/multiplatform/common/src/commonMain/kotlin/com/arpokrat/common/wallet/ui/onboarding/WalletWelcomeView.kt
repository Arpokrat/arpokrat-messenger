package com.arpokrat.common.wallet.ui.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arpokrat.common.ui.theme.isInDarkTheme
import com.arpokrat.common.views.helpers.AppPrimaryButton
import com.arpokrat.res.MR
import dev.icerock.moko.resources.compose.painterResource
import dev.icerock.moko.resources.compose.stringResource

@Composable
fun NoWalletView(
  onCreateClick: () -> Unit,
  onImportClick: () -> Unit
) {
  Column(
    modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 24.dp),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Image(
      painter = painterResource(if (isInDarkTheme()) MR.images.logo_wallet_light else MR.images.logo),
      contentDescription = null,
      contentScale = ContentScale.Fit,
      modifier = Modifier.fillMaxWidth().height(120.dp)
    )

    Spacer(modifier = Modifier.height(32.dp))

    Text(
      text = stringResource(MR.strings.wallet_welcome_subtitle),
      fontSize = 16.sp,
      color = MaterialTheme.colors.onBackground.copy(alpha = 0.6f),
      textAlign = TextAlign.Center,
      lineHeight = 24.sp
    )

    Spacer(modifier = Modifier.height(56.dp))

    AppPrimaryButton(
      text = stringResource(MR.strings.wallet_btn_create_new),
      onClick = onCreateClick
    )

    Spacer(modifier = Modifier.height(16.dp))

    OutlinedButton(
      onClick = onImportClick,
      modifier = Modifier.fillMaxWidth().height(56.dp),
      shape = RoundedCornerShape(28.dp),
      colors = ButtonDefaults.outlinedButtonColors(backgroundColor = Color.Transparent, contentColor = MaterialTheme.colors.onBackground.copy(alpha = 0.8f)),
      border = BorderStroke(1.dp, MaterialTheme.colors.onBackground.copy(alpha = 0.2f))
    ) {
      Text(
        text = stringResource(MR.strings.wallet_btn_import_phrase),
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold
      )
    }
  }
}