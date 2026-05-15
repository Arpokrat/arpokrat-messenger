package com.arpokrat.common.views

import SectionTextFooter
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arpokrat.common.model.ChatController.appPrefs
import com.arpokrat.common.model.ChatModel
import com.arpokrat.common.model.ChatModel.controller
import com.arpokrat.common.model.Profile
import com.arpokrat.common.platform.*
import com.arpokrat.common.ui.theme.*
import com.arpokrat.common.views.helpers.*
import com.arpokrat.common.views.onboarding.*
import com.arpokrat.common.views.usersettings.SettingsActionItem
import com.arpokrat.res.MR
import dev.icerock.moko.resources.compose.painterResource
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

const val MAX_BIO_LENGTH_BYTES = 160

fun bioFitsLimit(bio: String): Boolean {
  return chatJsonLength(bio) <= MAX_BIO_LENGTH_BYTES
}

private fun generateCoolName(): String {
  val adjs = listOf(
    "Quantum", "Cyber", "Neon", "Silent", "Phantom", "Ghost", "Stellar", "Lunar", "Nova", "Cosmic",
    "Dark", "Astral", "Solar", "Ethereal", "Void", "Apex", "Zenith", "Cryptic", "Mystic", "Radiant",
    "Lucid", "Omni", "Primal", "Prime", "Alpha", "Omega", "Hyper", "Ultra", "Meta", "Synth",
    "Kinetic", "Static", "Dynamic", "Rogue", "Rebel", "Noble", "Grand", "Epic", "Mythic", "Divine",
    "Hidden", "Secret", "Stealth", "Sacred", "Blind", "Fierce", "Calm", "Brave", "Bold", "Swift",
    "Wise", "True", "Pure", "Free", "Wild", "Deep", "High", "Cold", "Warm", "Dark",
    "Hollow", "Vast", "Infinite", "Boundless", "Timeless", "Ancient", "Modern", "Future", "Past", "Lost",
    "Found", "Lone", "Lone", "Wandering", "Wandering", "Nomad", "Exile", "Exile", "Fallen", "Risen",
    "Awake", "Asleep", "Dreaming", "Waking", "Living", "Undead", "Immortal", "Mortal", "Eternal", "Fading",
    "Glowing", "Burning", "Freezing", "Shining", "Dim", "Bright", "Dull", "Sharp", "Blunt", "Keen"
  ).distinct()

  val materials = listOf(
    "Crimson", "Cobalt", "Obsidian", "Onyx", "Azure", "Silver", "Golden", "Emerald", "Ruby", "Sapphire",
    "Amethyst", "Quartz", "Jade", "Pearl", "Opal", "Bronze", "Iron", "Steel", "Copper", "Titanium",
    "Carbon", "Chrome", "Plasma", "Frost", "Ember", "Ash", "Shadow", "Light", "Crystal", "Glass",
    "Stone", "Amber", "Coral", "Ivory", "Marble", "Slate", "Flint", "Spark", "Aura", "Nebula",
    "Velvet", "Silk", "Cotton", "Linen", "Wool", "Leather", "Wood", "Paper", "Parchment", "Vellum",
    "Ink", "Paint", "Dye", "Blood", "Bone", "Flesh", "Skin", "Scale", "Feather", "Fur",
    "Sand", "Dust", "Dirt", "Mud", "Clay", "Chalk", "Gravel", "Pebble", "Rock", "Boulder",
    "Magma", "Lava", "Ice", "Snow", "Rain", "Mist", "Fog", "Cloud", "Smoke", "Steam",
    "Wind", "Breeze", "Gale", "Storm", "Thunder", "Lightning", "Volt", "Watt", "Amp", "Ohm"
  ).distinct()

  val entities = listOf(
    "Fox", "Wolf", "Raven", "Falcon", "Owl", "Phoenix", "Lynx", "Bear", "Viper", "Dragon",
    "Tiger", "Panther", "Shark", "Cobra", "Griffin", "Hawk", "Eagle", "Node", "Block", "Hash",
    "Byte", "Data", "Grid", "Matrix", "Nexus", "Core", "Link", "Chain", "Vault", "Key",
    "Cipher", "Enigma", "Punk", "Whale", "Vertex", "Horizon", "Echo", "Pulsar", "Quasar", "Comet",
    "Tide", "Wave", "Storm", "Glacier", "Meteor", "Orbit", "Galaxy", "Cosmos", "Star", "Planet",
    "Moon", "Sun", "Asteroid", "Blackhole", "Wormhole", "Void", "Space", "Time", "Dimension", "Realm",
    "Realm", "World", "Earth", "Ocean", "Sea", "River", "Lake", "Pond", "Pool", "Stream",
    "Mountain", "Hill", "Valley", "Canyon", "Cave", "Cavern", "Abyss", "Chasm", "Pit", "Crater",
    "Forest", "Wood", "Jungle", "Grove", "Thicket", "Orchard", "Garden", "Park", "Meadow", "Field",
    "Desert", "Dune", "Oasis", "Mirage", "Mirage", "Illusion", "Vision", "Dream", "Nightmare", "Fantasy"
  ).distinct()

  val adj = adjs.random()
  val material = materials.random()
  val entity = entities.random()

  return "$adj$material$entity"
}

@Composable
fun OnboardingProgressBar(currentStep: Int, totalSteps: Int = 5) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.Center,
    verticalAlignment = Alignment.CenterVertically
  ) {
    for (i in 1..totalSteps) {
      val isActive = i == currentStep
      val isPassed = i < currentStep
      Box(
        modifier = Modifier
          .padding(horizontal = 4.dp)
          .height(4.dp)
          .width(if (isActive) 24.dp else 12.dp)
          .clip(RoundedCornerShape(2.dp))
          .background(
            when {
              isActive -> MaterialTheme.colors.primary
              isPassed -> MaterialTheme.colors.primary.copy(alpha = 0.5f)
              else -> MaterialTheme.colors.onSurface.copy(alpha = 0.15f)
            }
          )
      )
    }
  }
}

@Composable
fun CreateProfile(chatModel: ChatModel, close: () -> Unit) {
  val scope = rememberCoroutineScope()
  val scrollState = rememberScrollState()
  val keyboardState by getKeyboardState()
  var savedKeyboardState by remember { mutableStateOf(keyboardState) }

  Box(modifier = Modifier.fillMaxSize().padding(top = 20.dp)) {
    val displayName = rememberSaveable { mutableStateOf("") }
    val shortDescr = rememberSaveable { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    ColumnWithScrollBar {
      Column(Modifier.padding(horizontal = DEFAULT_PADDING)) {

        Spacer(Modifier.height(48.dp))
        Text(stringResource(MR.strings.create_profile), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colors.onBackground, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(32.dp))

        Row(Modifier.padding(bottom = DEFAULT_PADDING_HALF).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Text(stringResource(MR.strings.display_name), fontSize = 16.sp)
          val name = displayName.value.trim()
          val validName = mkValidName(name)
          if (name != validName) {
            IconButton({ showInvalidNameAlert(mkValidName(displayName.value), displayName) }, Modifier.size(20.dp)) {
              Icon(painterResource(MR.images.ic_info), null, tint = MaterialTheme.colors.error)
            }
          }
        }

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
          Box(Modifier.weight(1f)) {
            ProfileNameField(displayName, "", { it.trim() == mkValidName(it) }, focusRequester)
          }
          Spacer(Modifier.width(8.dp))
          IconButton(
            onClick = { displayName.value = generateCoolName() },
            modifier = Modifier.padding(top = 8.dp)
          ) {
            Icon(painterResource(MR.images.ic_dice), contentDescription = stringResource(MR.strings.random_name_desc), tint = MaterialTheme.colors.primary, modifier = Modifier.size(28.dp))
          }
        }

        Spacer(Modifier.height(DEFAULT_PADDING))

        Row(Modifier.padding(bottom = DEFAULT_PADDING_HALF).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Text(stringResource(MR.strings.short_descr), fontSize = 16.sp)
          if (!bioFitsLimit(shortDescr.value)) {
            IconButton(
              onClick = { AlertManager.shared.showAlertMsg(title = generalGetString(MR.strings.bio_too_large)) },
              Modifier.size(20.dp)) {
              Icon(painterResource(MR.images.ic_info), null, tint = MaterialTheme.colors.error)
            }
          }
        }
        ProfileNameField(shortDescr, "", isValid = { bioFitsLimit(it) })
      }

      val isValid = canCreateProfile(displayName.value) && bioFitsLimit(shortDescr.value)
      if (isValid) {
        SettingsActionItem(
          painterResource(MR.images.ic_check),
          stringResource(MR.strings.create_another_profile_button),
          disabled = false,
          textColor = MaterialTheme.colors.primary,
          iconColor = MaterialTheme.colors.primary,
          click = {
            if (chatModel.localUserCreated.value == true) {
              createProfileInProfiles(chatModel, displayName.value, shortDescr.value, close)
            } else {
              createProfileInNoProfileSetup(displayName.value, close)
            }
          },
        )
      } else {
        Spacer(Modifier.height(56.dp))
      }

      SectionTextFooter(generalGetString(MR.strings.your_profile_is_stored_on_your_device))
      SectionTextFooter(generalGetString(MR.strings.profile_is_only_shared_with_your_contacts))

      LaunchedEffect(Unit) { delay(300); focusRequester.requestFocus() }
    }
    if (savedKeyboardState != keyboardState) {
      LaunchedEffect(keyboardState) {
        scope.launch { savedKeyboardState = keyboardState; scrollState.animateScrollTo(scrollState.maxValue) }
      }
    }
  }
}

@Composable
fun CreateFirstProfile(chatModel: ChatModel, close: () -> Unit) {
  val scope = rememberCoroutineScope()
  val scrollState = rememberScrollState()
  val keyboardState by getKeyboardState()
  var savedKeyboardState by remember { mutableStateOf(keyboardState) }

  CompositionLocalProvider(LocalAppBarHandler provides rememberAppBarHandler()) {
    ModalView({
      if (chatModel.users.none { !it.user.hidden }) {
        appPrefs.onboardingStage.set(OnboardingStage.Step1_SimpleXInfo)
      } else close()
    }) {
      ColumnWithScrollBar {
        val displayName = rememberSaveable { mutableStateOf("") }
        val focusRequester = remember { FocusRequester() }

        Column(if (appPlatform.isAndroid) Modifier.fillMaxSize().padding(start = DEFAULT_ONBOARDING_HORIZONTAL_PADDING * 2, end = DEFAULT_ONBOARDING_HORIZONTAL_PADDING * 2, bottom = DEFAULT_PADDING) else Modifier.widthIn(max = 600.dp).fillMaxHeight().padding(horizontal = DEFAULT_PADDING).align(Alignment.CenterHorizontally), horizontalAlignment = Alignment.CenterHorizontally) {

          Box(modifier = Modifier.height(48.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
            OnboardingProgressBar(currentStep = 1)
          }

          Text(stringResource(MR.strings.create_your_profile), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colors.onBackground, textAlign = TextAlign.Center)
          Spacer(Modifier.height(16.dp))

          ReadableText(MR.strings.your_profile_is_stored_on_your_device, TextAlign.Center, padding = PaddingValues(horizontal = 16.dp), style = MaterialTheme.typography.body1.copy(color = MaterialTheme.colors.secondary))
          Spacer(Modifier.height(8.dp))
          ReadableText(MR.strings.profile_is_only_shared_with_your_contacts, TextAlign.Center, padding = PaddingValues(horizontal = 16.dp), style = MaterialTheme.typography.body1.copy(color = MaterialTheme.colors.secondary))
          Spacer(Modifier.height(32.dp))

          Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Box(Modifier.weight(1f)) {
              ProfileNameField(displayName, stringResource(MR.strings.display_name), { it.trim() == mkValidName(it) }, focusRequester)
            }
            Spacer(Modifier.width(8.dp))
            IconButton(
              onClick = { displayName.value = generateCoolName() },
              modifier = Modifier.padding(top = 8.dp)
            ) {
              Icon(painterResource(MR.images.ic_dice), contentDescription = stringResource(MR.strings.random_name_desc), tint = MaterialTheme.colors.primary, modifier = Modifier.size(28.dp))
            }
          }
        }
        Spacer(Modifier.fillMaxHeight().weight(1f))

        Column(Modifier.widthIn(max = if (appPlatform.isAndroid) 450.dp else 1000.dp).align(Alignment.CenterHorizontally), horizontalAlignment = Alignment.CenterHorizontally) {
          if (canCreateProfile(displayName.value)) {
            OnboardingActionButton(
              if (appPlatform.isAndroid) Modifier.padding(horizontal = DEFAULT_ONBOARDING_HORIZONTAL_PADDING).fillMaxWidth() else Modifier.widthIn(min = 300.dp),
              labelId = MR.strings.create_profile_button,
              onboarding = null,
              enabled = true,
              onclick = { createProfileOnboarding(com.arpokrat.common.platform.chatModel, displayName.value, close) }
            )
          } else {
            Spacer(Modifier.height(56.dp))
          }
          Spacer(Modifier.height(16.dp))
        }

        LaunchedEffect(Unit) { delay(300); focusRequester.requestFocus() }
      }
      LaunchedEffect(Unit) { setLastVersionDefault(chatModel) }
      if (savedKeyboardState != keyboardState) {
        LaunchedEffect(keyboardState) {
          scope.launch { savedKeyboardState = keyboardState; scrollState.animateScrollTo(scrollState.maxValue) }
        }
      }
    }
  }
}

fun createProfileInNoProfileSetup(displayName: String, close: () -> Unit) {
  withBGApi {
    val user = controller.apiCreateActiveUser(null, Profile(displayName.trim(), "", null, null)) ?: return@withBGApi
    if (!chatModel.connectedToRemote()) chatModel.localUserCreated.value = true
    controller.appPrefs.onboardingStage.set(OnboardingStage.Step3_ChooseServerOperators)
    controller.startChat(user)
    controller.switchUIRemoteHost(null)
    close()
  }
}

fun createProfileInProfiles(chatModel: ChatModel, displayName: String, shortDescr: String, close: () -> Unit) {
  withBGApi {
    val rhId = chatModel.remoteHostId()
    val user = chatModel.controller.apiCreateActiveUser(
      rhId, Profile(displayName.trim(), "", shortDescr.trim().ifEmpty { null }, null)
    ) ?: return@withBGApi
    chatModel.currentUser.value = user
    if (chatModel.users.isEmpty()) {
      chatModel.controller.startChat(user)
      chatModel.controller.appPrefs.onboardingStage.set(OnboardingStage.Step4_SetNotificationsMode)
    } else {
      val users = chatModel.controller.listUsers(rhId)
      chatModel.users.clear()
      chatModel.users.addAll(users)
      chatModel.controller.getUserChatData(rhId)
      close()
    }
  }
}

fun createProfileOnboarding(chatModel: ChatModel, displayName: String, close: () -> Unit) {
  withBGApi {
    chatModel.currentUser.value = chatModel.controller.apiCreateActiveUser(
      null, Profile(displayName.trim(), "", null, null)
    ) ?: return@withBGApi
    chatModel.localUserCreated.value = true
    val onboardingStage = chatModel.controller.appPrefs.onboardingStage
    if (chatModel.users.none { u -> !u.user.hidden }) {
      onboardingStage.set(OnboardingStage.Step2_5_SetupDatabasePassphrase)
    } else {
      onboardingStage.set(OnboardingStage.OnboardingComplete)
      close()
    }
  }
}

@Composable
fun ProfileNameField(
  name: MutableState<String>,
  placeholder: String = "",
  isValid: (String) -> Boolean = { true },
  focusRequester: FocusRequester? = null
) {
  var valid by rememberSaveable { mutableStateOf(true) }

  UnderlineTextField(
    value = name.value,
    onValueChange = { name.value = it },
    placeholder = placeholder,
    isValid = valid,
    focusRequester = focusRequester,
    trailingIcon = if (!valid && placeholder != "") {
      {
        IconButton(
          onClick = { showInvalidNameAlert(mkValidName(name.value), name) },
          modifier = Modifier.size(20.dp)
        ) {
          Icon(painterResource(MR.images.ic_info), null, tint = MaterialTheme.colors.error)
        }
      }
    } else null
  )

  LaunchedEffect(Unit) {
    snapshotFlow { name.value }.distinctUntilChanged().collect { valid = isValid(it) }
  }
}

private fun canCreateProfile(displayName: String): Boolean {
  val name = displayName.trim()
  return name.isNotEmpty() && mkValidName(name) == name
}

fun showInvalidNameAlert(name: String, displayName: MutableState<String>) {
  if (name.isEmpty()) AlertManager.shared.showAlertMsg(title = generalGetString(MR.strings.invalid_name))
  else AlertManager.shared.showAlertDialog(title = generalGetString(MR.strings.invalid_name), text = generalGetString(MR.strings.correct_name_to).format(name), onConfirm = { displayName.value = name })
}

fun isValidDisplayName(name: String) : Boolean = mkValidName(name.trim()) == name
fun mkValidName(s: String): String = chatValidName(s)