package com.arpokrat.common.views.chat

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.arpokrat.common.platform.base64ToBitmap
import com.arpokrat.common.views.helpers.UploadContent
import com.arpokrat.res.MR
import dev.icerock.moko.resources.compose.painterResource
import dev.icerock.moko.resources.compose.stringResource

@Composable
fun ComposeImageView(media: ComposePreview.MediaPreview, cancelImages: () -> Unit, cancelEnabled: Boolean) {
  Row(
    Modifier
      .padding(vertical = 8.dp)
      .fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    LazyRow(
      Modifier.weight(1f).padding(start = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      itemsIndexed(media.images) { index, item ->
        val content = media.content[index]

        Box(modifier = Modifier.clip(RoundedCornerShape(8.dp))) {
          if (content is UploadContent.Video) {
            Box(contentAlignment = Alignment.Center) {
              val imageBitmap = base64ToBitmap(item)
              Image(
                imageBitmap,
                stringResource(MR.strings.preview_video_desc),
                modifier = Modifier.widthIn(max = 100.dp).height(80.dp),
                contentScale = ContentScale.Crop
              )
              Box(
                modifier = Modifier.background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(50)).padding(4.dp)
              ) {
                Icon(
                  painterResource(MR.images.ic_videocam_filled),
                  stringResource(MR.strings.preview_video_desc),
                  Modifier.size(16.dp),
                  tint = Color.White
                )
              }
            }
          } else {
            val imageBitmap = base64ToBitmap(item)
            Image(
              imageBitmap,
              stringResource(MR.strings.preview_image_desc),
              modifier = Modifier.widthIn(max = 100.dp).height(80.dp),
              contentScale = ContentScale.Crop
            )
          }
        }
      }
    }

    if (cancelEnabled) {
      IconButton(onClick = cancelImages) {
        Icon(
          painterResource(MR.images.ic_close),
          contentDescription = stringResource(MR.strings.icon_descr_cancel_image_preview),
          tint = MaterialTheme.colors.primary,
        )
      }
    }
  }
}