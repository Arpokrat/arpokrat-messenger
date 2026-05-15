package com.arpokrat.common.views.chat.item

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.painter.Painter
import com.arpokrat.common.model.CIFile
import com.arpokrat.common.platform.*
import com.arpokrat.common.views.helpers.ModalManager

@Composable
actual fun SimpleAndAnimatedImageView(
  data: ByteArray,
  imageBitmap: ImageBitmap,
  file: CIFile?,
  imageProvider: () -> ImageGalleryProvider,
  smallView: Boolean,
  ImageView: @Composable (painter: Painter, onClick: () -> Unit) -> Unit
) {
  // LALAL make it animated too
  ImageView(imageBitmap.toAwtImage().toPainter()) {
    if (getLoadedFilePath(file) != null) {
      ModalManager.fullscreen.showCustomModal(animated = false) { close ->
        ImageFullScreenView(imageProvider, close)
      }
    }
  }
}
