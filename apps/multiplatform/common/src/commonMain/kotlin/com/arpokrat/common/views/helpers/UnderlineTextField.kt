package com.arpokrat.common.views.helpers

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun UnderlineTextField(
  value: String,
  onValueChange: (String) -> Unit,
  modifier: Modifier = Modifier,
  placeholder: String = "",
  isValid: Boolean = true,
  trailingIcon: (@Composable () -> Unit)? = null,
  focusRequester: FocusRequester? = null
) {
  var focused by remember { mutableStateOf(false) }

  val strokeColor = if (!isValid) MaterialTheme.colors.error
  else if (focused) MaterialTheme.colors.primary
  else MaterialTheme.colors.secondary.copy(alpha = 0.3f)

  val baseModifier = modifier.fillMaxWidth().heightIn(min = 50.dp).onFocusChanged { focused = it.isFocused }
  val finalModifier = if (focusRequester != null) baseModifier.focusRequester(focusRequester) else baseModifier

  Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
    BasicTextField(
      value = value,
      onValueChange = onValueChange,
      modifier = finalModifier,
      textStyle = TextStyle(fontSize = 18.sp, color = MaterialTheme.colors.onBackground),
      singleLine = true,
      cursorBrush = SolidColor(MaterialTheme.colors.primary),
      decorationBox = { innerTextField ->
        TextFieldDefaults.TextFieldDecorationBox(
          value = value,
          innerTextField = innerTextField,
          placeholder = if (placeholder.isNotEmpty()) {
            { Text(placeholder, style = MaterialTheme.typography.body1.copy(color = MaterialTheme.colors.secondary, lineHeight = 22.sp)) }
          } else null,
          contentPadding = PaddingValues(vertical = 12.dp),
          trailingIcon = trailingIcon,
          singleLine = true,
          enabled = true,
          isError = !isValid,
          visualTransformation = VisualTransformation.None,
          interactionSource = remember { MutableInteractionSource() },
          colors = TextFieldDefaults.textFieldColors(backgroundColor = Color.Transparent)
        )
      }
    )
    Divider(color = strokeColor, thickness = if (focused) 2.dp else 1.dp)
  }
}