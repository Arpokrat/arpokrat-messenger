package com.arpokrat.common.views.helpers

import androidx.compose.animation.core.*
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arpokrat.common.ui.theme.SimpleXTheme
import com.arpokrat.res.MR
import dev.icerock.moko.resources.compose.painterResource

@Composable
fun SimpleButton(text: String, icon: Painter,
  color: Color = MaterialTheme.colors.primary,
  click: () -> Unit) {
  SimpleButtonFrame(click) {
    Icon(
      icon, text, tint = color,
      modifier = Modifier.padding(end = 8.dp)
    )
    Text(text, style = MaterialTheme.typography.caption, color = color)
  }
}

@Composable
fun SimpleButtonDecorated(text: String, icon: Painter,
  color: Color = MaterialTheme.colors.primary,
  textDecoration: TextDecoration = TextDecoration.Underline,
  fontWeight: FontWeight = FontWeight.Normal,
  click: () -> Unit) {
  SimpleButtonFrame(click) {
    Icon(
      icon, text, tint = color,
      modifier = Modifier.padding(end = 8.dp)
    )
    Text(text, style = MaterialTheme.typography.caption, fontWeight = fontWeight, color = color, textDecoration = textDecoration)
  }
}

@Composable
fun SimpleButton(
  text: String, icon: Painter,
  color: Color = MaterialTheme.colors.primary,
  disabled: Boolean,
  click: () -> Unit
) {
  SimpleButtonFrame(click, disabled = disabled) {
    Icon(
      icon, text, tint = if (disabled) MaterialTheme.colors.secondary else color,
      modifier = Modifier.padding(end = 8.dp)
    )
    Text(text, style = MaterialTheme.typography.caption, color = if (disabled) MaterialTheme.colors.secondary else color)
  }
}

@Composable
fun SimpleButtonIconEnded(
  text: String,
  icon: Painter,
  style: TextStyle = MaterialTheme.typography.caption,
  color: Color = MaterialTheme.colors.primary,
  disabled: Boolean = false,
  click: () -> Unit
) {
  SimpleButtonFrame(click, disabled = disabled) {
    Text(text, style = style, color = color)
    Icon(
      icon, text, tint = color,
      modifier = Modifier.padding(start = 8.dp)
    )
  }
}

@Composable
fun SimpleButtonFrame(click: () -> Unit, modifier: Modifier = Modifier, disabled: Boolean = false, content: @Composable RowScope.() -> Unit) {
  Box(Modifier.clip(RoundedCornerShape(20.dp))) {
    val modifier = if (disabled) modifier else modifier.clickable { click() }
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = modifier.padding(8.dp)
    ) { content() }
  }
}

@Preview
@Composable
fun PreviewShareButton() {
  SimpleXTheme {
    SimpleButton(text = "Share", icon = painterResource(MR.images.ic_share), click = {})
  }
}

@Composable
fun AppPrimaryButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  backgroundColor: Color = MaterialTheme.colors.primary,
  textColor: Color = MaterialTheme.colors.onPrimary
) {
  Button(
    onClick = onClick,
    enabled = enabled,
    modifier = modifier.fillMaxWidth().height(56.dp),
    shape = RoundedCornerShape(28.dp),
    colors = ButtonDefaults.buttonColors(
      backgroundColor = backgroundColor,
      disabledBackgroundColor = backgroundColor.copy(alpha = 0.5f)
    ),
    elevation = ButtonDefaults.elevation(0.dp, 0.dp)
  ) {
    Text(
      text = text,
      color = if (enabled) textColor else textColor.copy(alpha = 0.5f),
      fontSize = 18.sp,
      fontWeight = FontWeight.Bold
    )
  }
}

@Composable
fun AppSecondaryButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  textColor: Color = Color.Gray
) {
  Button(
    onClick = onClick,
    enabled = enabled,
    modifier = modifier.fillMaxWidth().height(42.dp),
    shape = RoundedCornerShape(12.dp),
    colors = ButtonDefaults.buttonColors(
      backgroundColor = MaterialTheme.colors.onSurface.copy(alpha = 0.05f),
      disabledBackgroundColor = MaterialTheme.colors.onSurface.copy(alpha = 0.02f)
    ),
    elevation = ButtonDefaults.elevation(0.dp, 0.dp)
  ) {
    Text(
      text = text,
      color = if (enabled) textColor else textColor.copy(alpha = 0.5f),
      fontSize = 15.sp,
      fontWeight = FontWeight.Medium
    )
  }
}

@Composable
fun AppTextButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  textColor: Color = Color.Gray
) {
  TextButton(
    onClick = onClick,
    enabled = enabled,
    modifier = modifier.height(48.dp)
  ) {
    Text(
      text = text,
      color = if (enabled) textColor else textColor.copy(alpha = 0.5f),
      fontSize = 16.sp,
      fontWeight = FontWeight.Medium
    )
  }
}

@Composable
fun AppHoldToConfirmButton(
  text: String,
  isFormValid: Boolean,
  onConfirmed: () -> Unit,
  modifier: Modifier = Modifier
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  var hasTriggered by remember { mutableStateOf(false) }

  val holdProgress by animateFloatAsState(
    targetValue = if (isPressed && isFormValid && !hasTriggered) 1f else 0f,
    animationSpec = tween(durationMillis = if (isPressed) 1200 else 300, easing = LinearEasing)
  )

  LaunchedEffect(holdProgress) {
    if (holdProgress >= 1f && !hasTriggered) {
      hasTriggered = true
      onConfirmed()
    }
  }

  LaunchedEffect(isPressed) {
    if (!isPressed) hasTriggered = false
  }

  Box(
    modifier = modifier
      .fillMaxWidth()
      .height(56.dp)
      .clip(RoundedCornerShape(28.dp))
      .background(if (isFormValid) MaterialTheme.colors.surface else MaterialTheme.colors.surface.copy(alpha = 0.5f))
      .border(1.dp, MaterialTheme.colors.primary, RoundedCornerShape(28.dp))
  ) {
    Box(
      modifier = Modifier
        .fillMaxHeight()
        .fillMaxWidth(holdProgress)
        .background(MaterialTheme.colors.primary)
    )

    Box(
      contentAlignment = Alignment.Center,
      modifier = Modifier
        .fillMaxSize()
        .clickable(
          interactionSource = interactionSource,
          indication = null,
          enabled = isFormValid,
          onClick = { }
        )
    ) {
      Text(
        text = text,
        color = if (holdProgress > 0.5f) MaterialTheme.colors.onPrimary else if (isFormValid) MaterialTheme.colors.primary else Color.Gray,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold
      )
    }
  }
}