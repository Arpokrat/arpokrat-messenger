package com.arpokrat.common.views.onboarding

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arpokrat.common.model.SharedPreference
import com.arpokrat.common.model.User
import com.arpokrat.common.platform.ColumnWithScrollBar
import com.arpokrat.common.ui.theme.DEFAULT_PADDING
import com.arpokrat.common.ui.theme.SimplexBlue
import com.arpokrat.common.views.helpers.*
import com.arpokrat.res.MR
import dev.icerock.moko.resources.StringResource
import dev.icerock.moko.resources.compose.stringResource

@Composable
fun HowItWorks(user: User?, onboardingStage: SharedPreference<OnboardingStage>? = null) {
  ColumnWithScrollBar(Modifier.padding(horizontal = DEFAULT_PADDING)) {
    Spacer(Modifier.height(48.dp))
    Text(
      stringResource(MR.strings.how_arpokrat_works_title),
      fontSize = 28.sp,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colors.onBackground,
      textAlign = TextAlign.Center,
      modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(32.dp))

    ReadableText(MR.strings.how_arpokrat_works_1)
    ReadableText(MR.strings.how_arpokrat_works_2)
    ReadableText(MR.strings.how_arpokrat_works_3)

    Spacer(Modifier.fillMaxHeight().weight(1f))
  }
}

@Composable
fun ReadableText(stringResId: StringResource, textAlign: TextAlign = TextAlign.Start, padding: PaddingValues = PaddingValues(bottom = 12.dp), style: TextStyle = LocalTextStyle.current, args: Any? = null) {
  Text(annotatedStringResource(stringResId, args), modifier = Modifier.padding(padding), textAlign = textAlign, lineHeight = 22.sp, style = style)
}

@Composable
fun ReadableTextWithLink(stringResId: StringResource, link: String, textAlign: TextAlign = TextAlign.Start, padding: PaddingValues = PaddingValues(bottom = 12.dp), simplexLink: Boolean = false) {
  val annotated = annotatedStringResource(stringResId)
  val primary = MaterialTheme.colors.primary
  val newStyles = remember(stringResId) {
    val newStyles = ArrayList<AnnotatedString.Range<SpanStyle>>()
    annotated.spanStyles.forEach {
      if (it.item.color == SimplexBlue) {
        newStyles.add(it.copy(item = it.item.copy(primary)))
      } else {
        newStyles.add(it)
      }
    }
    newStyles
  }
  val uriHandler = LocalUriHandler.current
  Text(AnnotatedString(annotated.text, newStyles), modifier = Modifier.padding(padding).clickable { if (simplexLink) uriHandler.openVerifiedSimplexUri(link) else uriHandler.openUriCatching(link) }, textAlign = textAlign, lineHeight = 22.sp)
}

@Composable
fun ReadableText(text: String, textAlign: TextAlign = TextAlign.Start, padding: PaddingValues = PaddingValues(bottom = 12.dp)) {
  Text(text, modifier = Modifier.padding(padding), textAlign = textAlign, lineHeight = 22.sp)
}