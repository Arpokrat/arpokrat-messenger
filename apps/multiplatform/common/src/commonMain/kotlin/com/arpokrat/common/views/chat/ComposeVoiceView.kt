package com.arpokrat.common.views.chat

import androidx.compose.animation.core.Animatable
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import dev.icerock.moko.resources.compose.painterResource
import dev.icerock.moko.resources.compose.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arpokrat.common.model.CryptoFile
import com.arpokrat.common.model.durationText
import com.arpokrat.common.ui.theme.*
import com.arpokrat.common.views.helpers.*
import com.arpokrat.common.platform.AudioPlayer
import com.arpokrat.res.MR
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun ComposeVoiceView(
  filePath: String,
  recordedDurationMs: Int,
  finishedRecording: Boolean,
  cancelEnabled: Boolean,
  cancelVoice: () -> Unit
) {
  val progress = rememberSaveable { mutableStateOf(0) }
  val duration = rememberSaveable(recordedDurationMs) { mutableStateOf(recordedDurationMs) }

  Box(
    Modifier
      .fillMaxWidth()
      .background(MaterialTheme.colors.background)
  ) {
    Box(
      Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp)
    ) {
      val audioPlaying = rememberSaveable { mutableStateOf(false) }
      Row(
        Modifier
          .height(50.dp)
          .fillMaxWidth()
          .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(
          onClick = {
            if (!audioPlaying.value) {
              AudioPlayer.play(CryptoFile.plain(filePath), audioPlaying, progress, duration, resetOnEnd = false, smallView = false)
            } else {
              AudioPlayer.pause(audioPlaying, progress)
            }
          },
          enabled = finishedRecording
        ) {
          Icon(
            if (audioPlaying.value) painterResource(MR.images.ic_pause_filled) else painterResource(MR.images.ic_play_arrow_filled),
            stringResource(MR.strings.icon_descr_file),
            Modifier.size(32.dp),
            tint = MaterialTheme.colors.primary
          )
        }
        val numberInText = remember(recordedDurationMs, progress.value) {
          derivedStateOf {
            when {
              finishedRecording && progress.value == 0 && !audioPlaying.value -> duration.value / 1000
              finishedRecording -> progress.value / 1000
              else -> recordedDurationMs / 1000
            }
          }
        }
        Text(
          durationText(numberInText.value),
          fontSize = 16.sp,
          color = MaterialTheme.colors.onSurface,
          modifier = Modifier.padding(start = 8.dp)
        )
        Spacer(Modifier.weight(1f))

        if (cancelEnabled) {
          IconButton(
            onClick = {
              AudioPlayer.stop(filePath)
              cancelVoice()
            }
          ) {
            Icon(
              painterResource(MR.images.ic_close),
              contentDescription = stringResource(MR.strings.icon_descr_cancel_file_preview),
              tint = MaterialTheme.colors.primary
            )
          }
        }
      }
    }

    if (finishedRecording) {
      FinishedRecordingSlider(progress, duration, filePath)
    } else {
      RecordingInProgressSlider(recordedDurationMs)
    }
  }
}

@Composable
fun FinishedRecordingSlider(progress: MutableState<Int>, duration: MutableState<Int>, filePath: String) {
  val primary = MaterialTheme.colors.primary
  val inactiveTrackColor = MaterialTheme.colors.onSurface.copy(alpha = 0.1f)

  Slider(
    progress.value.toFloat(),
    onValueChange = { AudioPlayer.seekTo(it.toInt(), progress, filePath) },
    Modifier.fillMaxWidth().height(20.dp),
    colors = SliderDefaults.colors(
      thumbColor = primary,
      activeTrackColor = primary,
      inactiveTrackColor = inactiveTrackColor
    ),
    valueRange = 0f..duration.value.toFloat()
  )
}

@Composable
fun RecordingInProgressSlider(recordedDurationMs: Int) {
  val thumbPosition = remember { Animatable(0f) }
  val recDuration = rememberUpdatedState(recordedDurationMs)
  LaunchedEffect(Unit) {
    snapshotFlow { recDuration.value }
      .distinctUntilChanged()
      .collect {
        thumbPosition.animateTo(it.toFloat(), audioProgressBarAnimationSpec())
      }
  }
  val primary = MaterialTheme.colors.primary

  Slider(
    thumbPosition.value,
    onValueChange = {},
    Modifier.fillMaxWidth().height(20.dp),
    colors = SliderDefaults.colors(
      disabledThumbColor = Color.Transparent,
      disabledActiveTrackColor = primary,
      disabledInactiveTrackColor = MaterialTheme.colors.onSurface.copy(alpha = 0.1f)
    ),
    enabled = false,
    valueRange = 0f..MAX_VOICE_MILLIS_FOR_SENDING.toFloat()
  )
}

@Preview
@Composable
fun PreviewComposeAudioView() {
  SimpleXTheme {
    ComposeVoiceView(
      filePath = "test.m4a",
      recordedDurationMs = 15000,
      finishedRecording = true,
      cancelEnabled = true,
      cancelVoice = {}
    )
  }
}