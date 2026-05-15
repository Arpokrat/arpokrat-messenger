package com.arpokrat.common.views.onboarding

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arpokrat.common.model.ChatController.appPrefs
import com.arpokrat.common.model.ServerOperator
import com.arpokrat.common.platform.*
import com.arpokrat.common.ui.theme.DEFAULT_ONBOARDING_HORIZONTAL_PADDING
import com.arpokrat.common.ui.theme.themedBackground
import com.arpokrat.common.views.OnboardingProgressBar
import com.arpokrat.common.views.helpers.*
import com.arpokrat.res.MR
import dev.icerock.moko.resources.compose.painterResource
import dev.icerock.moko.resources.compose.stringResource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.heightIn

@Composable
fun ModalData.OnboardingConditionsView() {
  LaunchedEffect(Unit) { prepareChatBeforeFinishingOnboarding() }

  CompositionLocalProvider(LocalAppBarHandler provides rememberAppBarHandler()) {
    ModalView({}, showClose = false) {
      val serverOperators = remember { derivedStateOf { chatModel.conditions.value.serverOperators } }
      val selectedOperatorIds = remember {
        stateGetOrPut("selectedOperatorIds") {
          serverOperators.value
            .filter { it.tradeName.contains("Switzerland", ignoreCase = true) }
            .map { it.operatorId }
            .toSet()
        }
      }

      ColumnWithScrollBar(
        Modifier.themedBackground(bgLayerSize = LocalAppBarHandler.current?.backgroundGraphicsLayerSize, bgLayer = LocalAppBarHandler.current?.backgroundGraphicsLayer),
        maxIntrinsicSize = true
      ) {
        Box(modifier = Modifier.height(48.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
          OnboardingProgressBar(currentStep = 3)
        }

        Text(
          stringResource(MR.strings.onboarding_terms_title),
          fontSize = 28.sp,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colors.onBackground,
          modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Spacer(Modifier.height(16.dp))

        Column(
          Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .heightIn(max = 400.dp)
            .background(MaterialTheme.colors.surface, RoundedCornerShape(16.dp))
            .border(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
          horizontalAlignment = Alignment.Start
        ) {
          Text(
            stringResource(MR.strings.onboarding_terms_desc),
            fontSize = 14.sp,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f),
            modifier = Modifier.padding(bottom = 24.dp)
          )

          Text(stringResource(MR.strings.onboarding_terms_1_title), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colors.primary)
          Text(stringResource(MR.strings.onboarding_terms_1_desc), fontSize = 14.sp, color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f), modifier = Modifier.padding(top = 4.dp, bottom = 24.dp))

          Text(stringResource(MR.strings.onboarding_terms_2_title), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colors.primary)
          Text(stringResource(MR.strings.onboarding_terms_2_desc), fontSize = 14.sp, color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f), modifier = Modifier.padding(top = 4.dp, bottom = 24.dp))

          Text(stringResource(MR.strings.onboarding_terms_3_title), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colors.primary)
          Text(stringResource(MR.strings.onboarding_terms_3_desc), fontSize = 14.sp, color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f), modifier = Modifier.padding(top = 4.dp, bottom = 24.dp))

          Text(stringResource(MR.strings.onboarding_terms_4_title), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colors.primary)
          Text(stringResource(MR.strings.onboarding_terms_4_desc), fontSize = 14.sp, color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f), modifier = Modifier.padding(top = 4.dp, bottom = 24.dp))

          Text(stringResource(MR.strings.onboarding_terms_5_title), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colors.primary)
          Text(stringResource(MR.strings.onboarding_terms_5_desc), fontSize = 14.sp, color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f), modifier = Modifier.padding(top = 4.dp, bottom = 24.dp))

          Text(stringResource(MR.strings.onboarding_terms_6_title), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colors.primary)
          Text(stringResource(MR.strings.onboarding_terms_6_desc), fontSize = 14.sp, color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f), modifier = Modifier.padding(top = 4.dp, bottom = 24.dp))

          Text(stringResource(MR.strings.onboarding_terms_7_title), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colors.primary)
          Text(stringResource(MR.strings.onboarding_terms_7_desc), fontSize = 14.sp, color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f), modifier = Modifier.padding(top = 4.dp, bottom = 24.dp))

          Text(stringResource(MR.strings.onboarding_terms_8_title), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colors.primary)
          Text(stringResource(MR.strings.onboarding_terms_8_desc), fontSize = 14.sp, color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f), modifier = Modifier.padding(top = 4.dp, bottom = 24.dp))

          Text(stringResource(MR.strings.onboarding_terms_9_title), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colors.primary)
          Text(stringResource(MR.strings.onboarding_terms_9_desc), fontSize = 14.sp, color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f), modifier = Modifier.padding(top = 4.dp, bottom = 24.dp))

          Text(stringResource(MR.strings.onboarding_terms_10_title), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colors.primary)
          Text(stringResource(MR.strings.onboarding_terms_10_desc), fontSize = 14.sp, color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f), modifier = Modifier.padding(top = 4.dp))
        }

        Spacer(Modifier.weight(1f))

        Column(Modifier.widthIn(max = if (appPlatform.isAndroid) 450.dp else 1000.dp).align(Alignment.CenterHorizontally), horizontalAlignment = Alignment.CenterHorizontally) {
          Spacer(Modifier.height(32.dp))
          OnboardingActionButton(
            modifier = Modifier.padding(horizontal = DEFAULT_ONBOARDING_HORIZONTAL_PADDING).fillMaxWidth().height(56.dp),
            labelId = MR.strings.onboarding_conditions_accept,
            onboarding = null,
            enabled = true,
            onclick = {
              ModalManager.fullscreen.showModalCloseable(showClose = false) { close -> ChooseServerOperators(serverOperators, selectedOperatorIds, close) }
            }
          )
          Spacer(Modifier.height(48.dp))
        }
      }
    }
  }
}

@Composable
fun ModalData.ChooseServerOperators(
  serverOperators: State<List<ServerOperator>>,
  selectedOperatorIds: MutableState<Set<Long>>,
  close: (() -> Unit)
) {
  CompositionLocalProvider(LocalAppBarHandler provides rememberAppBarHandler()) {
    ModalView({}, showClose = false) {
      ColumnWithScrollBar(
        Modifier.themedBackground(bgLayerSize = LocalAppBarHandler.current?.backgroundGraphicsLayerSize, bgLayer = LocalAppBarHandler.current?.backgroundGraphicsLayer),
        maxIntrinsicSize = true
      ) {
        Box(modifier = Modifier.height(48.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
          OnboardingProgressBar(currentStep = 4)
        }

        Text(stringResource(MR.strings.onboarding_relays_title), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colors.onBackground, modifier = Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(16.dp))

        Box(Modifier.align(Alignment.CenterHorizontally).padding(bottom = 24.dp)) {
          OnboardingInformationButton(stringResource(MR.strings.onboarding_relays_learn_more), onClick = { ModalManager.fullscreen.showModal { RelayJurisdictionsInfoView() } })
        }

        Column(Modifier.fillMaxWidth().padding(horizontal = DEFAULT_ONBOARDING_HORIZONTAL_PADDING), horizontalAlignment = Alignment.CenterHorizontally) {
          val uiCountries = listOf(
            Triple("🇨🇭", stringResource(MR.strings.relay_switzerland), "Switzerland"),
            Triple("🇮🇸", stringResource(MR.strings.relay_iceland), "Iceland"),
            Triple("🇵🇦", stringResource(MR.strings.relay_panama), "Panama"),
            Triple("🇿🇦", stringResource(MR.strings.relay_south_africa), "South Africa"),
            Triple("🇲🇾", stringResource(MR.strings.relay_malaysia), "Malaysia")
          )

          uiCountries.forEachIndexed { index, (flag, name, keyword) ->
            val realOp = serverOperators.value.find { it.tradeName.contains(keyword, ignoreCase = true) }
            val opId = realOp?.operatorId ?: -(index + 1L)
            val isChecked = selectedOperatorIds.value.contains(opId)
            val isAvailable = keyword.equals("Switzerland", ignoreCase = true) && opId != -1L

            TextButton(
              onClick = {
                if (isAvailable) {
                  if (isChecked) {
                    if (selectedOperatorIds.value.count { it > 0L } > 1) {
                      selectedOperatorIds.value -= opId
                    }
                  } else {
                    selectedOperatorIds.value += opId
                  }
                }
              },
              border = BorderStroke(1.dp, color = if (isChecked && isAvailable) MaterialTheme.colors.primary else if (!isAvailable) MaterialTheme.colors.secondary.copy(alpha = 0.2f) else MaterialTheme.colors.onSurface.copy(alpha = 0.1f)),
              shape = RoundedCornerShape(18.dp),
              modifier = Modifier.background(Color.Transparent, RoundedCornerShape(18.dp)),
              enabled = isAvailable
            ) {
              Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(text = flag, fontSize = 42.sp, modifier = Modifier.alpha(if (isAvailable) 1f else 0.5f))
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                  Text(
                    text = name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isChecked && isAvailable) MaterialTheme.colors.primary else if (!isAvailable) MaterialTheme.colors.secondary.copy(alpha = 0.5f) else MaterialTheme.colors.onSurface
                  )
                  Text(
                    text = if (isChecked && isAvailable) stringResource(MR.strings.relay_connected) else if (!isAvailable) stringResource(MR.strings.in_developing_title) else stringResource(MR.strings.relay_tap_to_connect),
                    fontSize = 12.sp,
                    color = if (!isAvailable) MaterialTheme.colors.secondary.copy(alpha = 0.5f) else MaterialTheme.colors.secondary
                  )
                }
                CircleCheckbox(isChecked && isAvailable)
              }
            }
            Spacer(Modifier.height(12.dp))
          }
        }

        Spacer(Modifier.weight(1f))

        Column(Modifier.widthIn(max = if (appPlatform.isAndroid) 450.dp else 1000.dp).align(Alignment.CenterHorizontally), horizontalAlignment = Alignment.CenterHorizontally) {
          val enabled = selectedOperatorIds.value.any { it > 0L }
          OnboardingActionButton(
            modifier = Modifier.padding(horizontal = DEFAULT_ONBOARDING_HORIZONTAL_PADDING).fillMaxWidth().height(56.dp),
            labelId = MR.strings.ok,
            onboarding = null,
            enabled = enabled,
            onclick = {
              withBGApi {
                val validIds = selectedOperatorIds.value.filter { it > 0 }.toSet()

                val enabledOps = enabledOperators(serverOperators.value, validIds)

                if (enabledOps != null) {
                  val r2 = chatController.setServerOperators(rh = chatModel.remoteHostId(), operators = enabledOps)
                  if (r2 != null) chatModel.conditions.value = r2
                }

                if (appPlatform.isDesktop) {
                  appPrefs.onboardingStage.set(OnboardingStage.OnboardingComplete)
                } else {
                  appPrefs.onboardingStage.set(OnboardingStage.Step4_SetNotificationsMode)
                  ModalManager.fullscreen.showModalCloseable(showClose = false) { SetNotificationsMode(chatModel) }
                }
              }
            }
          )
          Spacer(Modifier.height(32.dp))
        }
      }
    }
  }
}

private fun enabledOperators(operators: List<ServerOperator>, selectedOperatorIds: Set<Long>): List<ServerOperator>? {
  val ops = ArrayList(operators)
  if (ops.isNotEmpty()) {
    for (i in ops.indices) {
      val op = ops[i]
      ops[i] = op.copy(enabled = selectedOperatorIds.contains(op.operatorId))
    }
    val haveSMPStorage = ops.any { it.enabled && it.smpRoles.storage }
    val haveSMPProxy = ops.any { it.enabled && it.smpRoles.proxy }
    val haveXFTPStorage = ops.any { it.enabled && it.xftpRoles.storage }
    val haveXFTPProxy = ops.any { it.enabled && it.xftpRoles.proxy }
    val firstEnabledIndex = ops.indexOfFirst { it.enabled }
    if (haveSMPStorage && haveSMPProxy && haveXFTPStorage && haveXFTPProxy) {
      return ops
    } else if (firstEnabledIndex != -1) {
      var op = ops[firstEnabledIndex]
      if (!haveSMPStorage) op = op.copy(smpRoles = op.smpRoles.copy(storage = true))
      if (!haveSMPProxy) op = op.copy(smpRoles = op.smpRoles.copy(proxy = true))
      if (!haveXFTPStorage) op = op.copy(xftpRoles = op.xftpRoles.copy(storage = true))
      if (!haveXFTPProxy) op = op.copy(xftpRoles = op.xftpRoles.copy(proxy = true))
      ops[firstEnabledIndex] = op
      return ops
    } else {
      return null
    }
  } else {
    return null
  }
}

@Composable
private fun CircleCheckbox(checked: Boolean) {
  if (checked) {
    Box(contentAlignment = Alignment.Center) {
      Icon(painterResource(MR.images.ic_circle_filled), null, Modifier.size(26.dp), tint = MaterialTheme.colors.primary)
      Icon(painterResource(MR.images.ic_check_filled), null, Modifier.size(20.dp), tint = MaterialTheme.colors.background)
    }
  } else {
    Icon(painterResource(MR.images.ic_circle), null, Modifier.size(26.dp), tint = MaterialTheme.colors.secondary.copy(alpha = 0.3f))
  }
}

@Composable
fun RelayJurisdictionsInfoView() {
  ColumnWithScrollBar(Modifier.padding(horizontal = 24.dp)) {
    Spacer(Modifier.height(48.dp))
    Text(stringResource(MR.strings.jurisdictions_title), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colors.onBackground, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    Spacer(Modifier.height(16.dp))
    Text(stringResource(MR.strings.jurisdictions_desc), fontSize = 15.sp, color = MaterialTheme.colors.secondary, lineHeight = 22.sp)
    Spacer(Modifier.height(24.dp))

    JurisdictionBlock("🇨🇭", stringResource(MR.strings.jurisdiction_switzerland), stringResource(MR.strings.jurisdiction_ch_protection), stringResource(MR.strings.jurisdiction_ch_log), stringResource(MR.strings.jurisdiction_ch_infra), stringResource(MR.strings.jurisdiction_ch_coverage))
    JurisdictionBlock("🇮🇸", stringResource(MR.strings.jurisdiction_iceland), stringResource(MR.strings.jurisdiction_is_protection), stringResource(MR.strings.jurisdiction_is_log), stringResource(MR.strings.jurisdiction_is_infra), stringResource(MR.strings.jurisdiction_is_coverage))
    JurisdictionBlock("🇵🇦", stringResource(MR.strings.jurisdiction_panama), stringResource(MR.strings.jurisdiction_pa_protection), stringResource(MR.strings.jurisdiction_pa_log), stringResource(MR.strings.jurisdiction_pa_infra), stringResource(MR.strings.jurisdiction_pa_coverage))
    JurisdictionBlock("🇲🇾", stringResource(MR.strings.jurisdiction_malaysia), stringResource(MR.strings.jurisdiction_my_protection), stringResource(MR.strings.jurisdiction_my_log), stringResource(MR.strings.jurisdiction_my_infra), stringResource(MR.strings.jurisdiction_my_coverage))
    JurisdictionBlock("🇿🇦", stringResource(MR.strings.jurisdiction_south_africa), stringResource(MR.strings.jurisdiction_za_protection), stringResource(MR.strings.jurisdiction_za_log), stringResource(MR.strings.jurisdiction_za_infra), stringResource(MR.strings.jurisdiction_za_coverage))

    Spacer(Modifier.height(48.dp))
  }
}

@Composable
fun JurisdictionBlock(flag: String, country: String, protection: String, log: String, infra: String, coverage: String) {
  Column(Modifier.padding(bottom = 24.dp)) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text(flag, fontSize = 24.sp)
      Spacer(Modifier.width(8.dp))
      Text(country, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colors.primary)
    }
    Spacer(Modifier.height(8.dp))
    Text("• ${stringResource(MR.strings.label_protection)} $protection", fontSize = 14.sp, color = MaterialTheme.colors.onBackground, modifier = Modifier.padding(bottom = 4.dp))
    Text("• ${stringResource(MR.strings.label_log_retention)} $log", fontSize = 14.sp, color = MaterialTheme.colors.onBackground, modifier = Modifier.padding(bottom = 4.dp))
    Text("• ${stringResource(MR.strings.label_infrastructure)} $infra", fontSize = 14.sp, color = MaterialTheme.colors.onBackground, modifier = Modifier.padding(bottom = 4.dp))
    Text("• ${stringResource(MR.strings.label_coverage)} $coverage", fontSize = 14.sp, color = MaterialTheme.colors.onBackground)
  }
}