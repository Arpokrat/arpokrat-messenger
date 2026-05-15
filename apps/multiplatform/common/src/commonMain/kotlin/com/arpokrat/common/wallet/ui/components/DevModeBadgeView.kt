package com.arpokrat.common.wallet.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arpokrat.res.MR
import dev.icerock.moko.resources.compose.stringResource

@Composable
fun DevModeBadge(modifier: Modifier = Modifier) {
  Text(
    text = stringResource(MR.strings.dev_mode),
    color = Color.White,
    fontSize = 11.sp,
    fontWeight = FontWeight.Bold,
    modifier = modifier
      .background(MaterialTheme.colors.error, RoundedCornerShape(6.dp))
      .padding(horizontal = 8.dp, vertical = 4.dp)
  )
}