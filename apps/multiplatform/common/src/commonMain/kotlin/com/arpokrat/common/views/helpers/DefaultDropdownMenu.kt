package com.arpokrat.common.views.helpers

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

@Composable
fun DefaultDropdownMenu(
  showMenu: MutableState<Boolean>,
  modifier: Modifier = Modifier,
  offset: DpOffset = DpOffset(0.dp, 0.dp),
  onClosed: State<() -> Unit> = remember { mutableStateOf({}) },
  dropdownMenuItems: (@Composable () -> Unit)?
) {

  val isDark = !MaterialTheme.colors.isLight
  val borderColor = if (isDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.08f)
  val shape = RoundedCornerShape(corner = CornerSize(14.dp))

  MaterialTheme(
    shapes = MaterialTheme.shapes.copy(medium = shape)
  ) {
    DropdownMenu(
      expanded = showMenu.value,
      onDismissRequest = { showMenu.value = false },
      modifier = modifier
        .widthIn(min = 200.dp)
        .background(MaterialTheme.colors.surface)
        .border(1.dp, borderColor, shape)
        .padding(vertical = 4.dp),
      offset = offset,
    ) {
      dropdownMenuItems?.invoke()
      DisposableEffect(Unit) {
        onDispose { onClosed.value() }
      }
    }
  }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ExposedDropdownMenuBoxScope.DefaultExposedDropdownMenu(
  expanded: MutableState<Boolean>,
  modifier: Modifier = Modifier,
  dropdownMenuItems: (@Composable () -> Unit)?
) {
  val isDark = !MaterialTheme.colors.isLight
  val borderColor = if (isDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.08f)
  val shape = RoundedCornerShape(corner = CornerSize(14.dp))

  MaterialTheme(
    shapes = MaterialTheme.shapes.copy(medium = shape)
  ) {
    ExposedDropdownMenu(
      modifier = Modifier
        .widthIn(min = 200.dp)
        .background(MaterialTheme.colors.surface)
        .border(1.dp, borderColor, shape)
        .then(modifier),
      expanded = expanded.value,
      onDismissRequest = {
        expanded.value = false
      }
    ) {
      dropdownMenuItems?.invoke()
    }
  }
}