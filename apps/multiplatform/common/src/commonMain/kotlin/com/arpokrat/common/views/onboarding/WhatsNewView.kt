package com.arpokrat.common.views.onboarding

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arpokrat.common.model.ChatController.appPrefs
import com.arpokrat.common.model.ChatController.setConditionsNotified
import com.arpokrat.common.model.ChatModel
import com.arpokrat.common.platform.*
import com.arpokrat.common.ui.theme.DEFAULT_PADDING
import com.arpokrat.common.views.helpers.*
import com.arpokrat.common.views.usersettings.networkAndServers.UsageConditionsView
import com.arpokrat.res.MR
import dev.icerock.moko.resources.ImageResource
import dev.icerock.moko.resources.StringResource
import dev.icerock.moko.resources.compose.painterResource
import dev.icerock.moko.resources.compose.stringResource

@Composable
fun ModalData.WhatsNewView(updatedConditions: Boolean = false, viaSettings: Boolean = false, close: () -> Unit) {
  val currentVersion = remember { mutableStateOf(versionDescriptions.lastIndex) }
  val rhId = chatModel.remoteHostId()

  if (updatedConditions) {
    LaunchedEffect(Unit) {
      val conditionsId = chatModel.conditions.value.currentConditions.conditionsId
      try {
        setConditionsNotified(rh = rhId, conditionsId = conditionsId)
      } catch (e: Exception) {
        Log.d(TAG, "WhatsNewView setConditionsNotified error: ${e.message}")
      }
    }
  }

  @Composable
  fun featureDescription(icon: ImageResource?, titleId: StringResource, descrId: StringResource?, link: String?, subfeatures: List<Pair<ImageResource, StringResource>>) {
    @Composable
    fun linkButton(link: String) {
      val uriHandler = LocalUriHandler.current
      Icon(
        painterResource(MR.images.ic_open_in_new), stringResource(titleId), tint = MaterialTheme.colors.primary,
        modifier = Modifier
          .clickable { if (link.startsWith("simplex:")) uriHandler.openVerifiedSimplexUri(link) else uriHandler.openUriCatching(link) }
      )
    }

    Column(modifier = Modifier.padding(bottom = 12.dp)) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(bottom = 4.dp)
      ) {
        if (icon != null)  Icon(painterResource(icon), stringResource(titleId), tint = MaterialTheme.colors.secondary)
        Text(
          generalGetString(titleId),
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
          style = MaterialTheme.typography.h4,
          fontWeight = FontWeight.Medium,
          modifier = Modifier.padding(bottom = 6.dp)
        )
        if (link != null) {
          linkButton(link)
        }
      }
      if (descrId != null) Text(generalGetString(descrId), fontSize = 15.sp)
      for ((si, sd) in subfeatures) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.padding(bottom = 6.dp)
        ) {
          Icon(painterResource(si), stringResource(sd), tint = MaterialTheme.colors.secondary)
          Text(generalGetString(sd), fontSize = 15.sp)
        }
      }
    }
  }

  @Composable
  fun pagination() {
    Row(
      Modifier
        .padding(bottom = DEFAULT_PADDING)
    ) {
      if (currentVersion.value > 0) {
        val prev = currentVersion.value - 1
        Box(Modifier.clip(RoundedCornerShape(20.dp))) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
              .clickable { currentVersion.value = prev }
              .padding(8.dp)
          ) {
            Icon(painterResource(MR.images.ic_arrow_back_ios_new), "previous", tint = MaterialTheme.colors.primary)
            Text(versionDescriptions[prev].version, color = MaterialTheme.colors.primary)
          }
        }
      }
      Spacer(Modifier.fillMaxWidth().weight(1f))
      if (currentVersion.value < versionDescriptions.lastIndex) {
        val next = currentVersion.value + 1
        Box(Modifier.clip(RoundedCornerShape(20.dp))) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
              .clickable { currentVersion.value = next }
              .padding(8.dp)
          ) {
            Text(versionDescriptions[next].version, color = MaterialTheme.colors.primary)
            Icon(painterResource(MR.images.ic_arrow_forward_ios), "next", tint = MaterialTheme.colors.primary)
          }
        }
      }
    }
  }

  val v = versionDescriptions[currentVersion.value]

  ModalView(close = close) {
    ColumnWithScrollBar(
      Modifier
        .padding(horizontal = DEFAULT_PADDING),
      verticalArrangement = Arrangement.spacedBy(DEFAULT_PADDING.times(0.75f))
    ) {
      AppBarTitle(String.format(generalGetString(MR.strings.new_in_version), v.version), withPadding = false, bottomPadding = DEFAULT_PADDING)

      val modalManager = if (viaSettings) ModalManager.start else ModalManager.center

      v.features.forEach { feature ->
        when (feature) {
          is VersionFeature.FeatureDescription -> {
            if (feature.show) {
              featureDescription(feature.icon, feature.titleId, feature.descrId, feature.link, feature.subfeatures)
            }
          }
          is VersionFeature.FeatureView -> {
            feature.view(modalManager)
          }
        }
      }

      if (v.post != null) {
        ReadMoreButton(v.post)
      }

      if (updatedConditions) {
        Text(
          stringResource(MR.strings.view_updated_conditions),
          color = MaterialTheme.colors.primary,
          modifier = Modifier
            .clickable(
              interactionSource = remember { MutableInteractionSource() },
              indication = null
            ) {
              modalManager.showModalCloseable { close ->
                UsageConditionsView(
                  userServers = mutableStateOf(emptyList()),
                  currUserServers = mutableStateOf(emptyList()),
                  close = close,
                  rhId = rhId
                )
              }
            }
        )
      }

      if (!viaSettings) {
        Spacer(Modifier.fillMaxHeight().weight(1f))
        Box(
          Modifier.fillMaxWidth(), contentAlignment = Alignment.Center
        ) {
          Box(Modifier.clip(RoundedCornerShape(20.dp))) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.Center,
              modifier = Modifier
                .clickable { close() }
                .padding(8.dp)
            ) {
              Text(
                generalGetString(MR.strings.ok),
                style = MaterialTheme.typography.h3,
                color = MaterialTheme.colors.primary
              )
            }
          }
        }
        Spacer(Modifier.fillMaxHeight().weight(1f))
      }

      Spacer(Modifier.fillMaxHeight().weight(1f))

      pagination()
    }
  }
}

@Composable
fun ReadMoreButton(url: String) {
  val uriHandler = LocalUriHandler.current
  Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = DEFAULT_PADDING.div(4))) {
    Text(
      stringResource(MR.strings.whats_new_read_more),
      color = MaterialTheme.colors.primary,
      modifier = Modifier
        .clickable(
          interactionSource = remember { MutableInteractionSource() },
          indication = null
        ) {
          uriHandler.openUriCatching(url)
        }
    )
    Icon(painterResource(MR.images.ic_open_in_new), stringResource(MR.strings.whats_new_read_more), tint = MaterialTheme.colors.primary)
  }
}

private sealed class VersionFeature {
  class FeatureDescription(
    val icon: ImageResource?,
    val titleId: StringResource,
    val descrId: StringResource?,
    var subfeatures: List<Pair<ImageResource, StringResource>> = listOf(),
    val link: String? = null,
    val show: Boolean = true
  ): VersionFeature()

  class FeatureView(
    val icon: ImageResource?,
    val titleId: StringResource,
    val view: @Composable (modalManager: ModalManager) -> Unit
  ): VersionFeature()
}

private data class VersionDescription(
  val version: String,
  val features: List<VersionFeature>,
  val post: String? = null,
)

private val versionDescriptions: List<VersionDescription> = listOf(
  VersionDescription(
    version = "v1.0.0",
    post = "https://arpokrat.com",
    features = listOf(
      VersionFeature.FeatureDescription(
        icon = MR.images.ic_visibility_off,
        titleId = MR.strings.onboarding_privacy_title,
        descrId = MR.strings.onboarding_privacy_desc
      ),
      VersionFeature.FeatureDescription(
        icon = MR.images.ic_wallet,
        titleId = MR.strings.onboarding_wallet_title,
        descrId = MR.strings.onboarding_wallet_desc
      ),
      VersionFeature.FeatureDescription(
        icon = MR.images.shield,
        titleId = MR.strings.onboarding_security_title,
        descrId = MR.strings.onboarding_security_desc
      )
    )
  )
)

private val lastVersion = versionDescriptions.last().version

fun setLastVersionDefault(m: ChatModel) {
  if (appPrefs.whatsNewVersion.get() != lastVersion) {
    appPrefs.whatsNewVersion.set(lastVersion)
  }
}

fun shouldShowWhatsNew(m: ChatModel): Boolean {
  val v = m.controller.appPrefs.whatsNewVersion.get()
  setLastVersionDefault(m)
  return v != lastVersion
}