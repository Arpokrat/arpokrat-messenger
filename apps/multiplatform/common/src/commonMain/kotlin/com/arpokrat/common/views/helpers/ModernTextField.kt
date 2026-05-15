package com.arpokrat.common.views.helpers

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ModernTextField(
  value: String,
  onValueChange: (String) -> Unit,
  modifier: Modifier = Modifier,
  label: (@Composable () -> Unit)? = null,
  placeholder: (@Composable () -> Unit)? = null,
  leadingIcon: (@Composable () -> Unit)? = null,
  trailingIcon: (@Composable () -> Unit)? = null,
  isError: Boolean = false,
  singleLine: Boolean = true,
  keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
  keyboardActions: KeyboardActions = KeyboardActions.Default
) {
  OutlinedTextField(
    value = value,
    onValueChange = onValueChange,
    modifier = modifier,
    label = label,
    placeholder = placeholder,
    leadingIcon = leadingIcon,
    trailingIcon = trailingIcon,
    isError = isError,
    singleLine = singleLine,
    keyboardOptions = keyboardOptions,
    keyboardActions = keyboardActions,
    shape = RoundedCornerShape(16.dp),
    colors = TextFieldDefaults.outlinedTextFieldColors(
      focusedBorderColor = MaterialTheme.colors.primary,
      cursorColor = MaterialTheme.colors.primary,
      backgroundColor = MaterialTheme.colors.onBackground.copy(alpha = 0.03f),
      unfocusedBorderColor = Color.Transparent,
      errorBorderColor = MaterialTheme.colors.error,
      textColor = MaterialTheme.colors.onBackground
    )
  )
}