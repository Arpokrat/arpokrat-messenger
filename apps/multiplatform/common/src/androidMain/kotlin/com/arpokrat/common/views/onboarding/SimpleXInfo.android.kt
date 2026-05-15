package com.arpokrat.common.views.onboarding

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.arpokrat.common.model.SharedPreference
import com.arpokrat.common.model.User
import com.arpokrat.res.MR

@Composable
actual fun OnboardingActionButton(user: User?, onboardingStage: SharedPreference<OnboardingStage>, onclick: (() -> Unit)?) {
  if (user == null) {
    OnboardingActionButton(Modifier.fillMaxWidth(), labelId = MR.strings.create_your_profile, onboarding = OnboardingStage.Step2_CreateProfile, onclick = onclick)
  } else {
    OnboardingActionButton(Modifier.fillMaxWidth(), labelId = MR.strings.make_private_connection, onboarding = OnboardingStage.OnboardingComplete, onclick = onclick)
  }
}
