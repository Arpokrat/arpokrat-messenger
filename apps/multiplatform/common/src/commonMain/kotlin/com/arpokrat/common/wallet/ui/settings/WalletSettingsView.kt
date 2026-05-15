package com.arpokrat.common.wallet.ui.settings

import SectionBottomSpacer
import SectionDividerSpaced
import SectionItemView
import SectionView
import TextIconSpaced
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.arpokrat.common.platform.ColumnWithScrollBar
import com.arpokrat.common.views.helpers.*
import com.arpokrat.common.wallet.WalletManager
import com.arpokrat.common.wallet.ui.components.*
import com.arpokrat.common.wallet.ui.onboarding.SetWalletPasscodeView
import com.arpokrat.common.wallet.ui.onboarding.VerifyWalletPasscodeView
import com.arpokrat.res.MR
import dev.icerock.moko.resources.compose.painterResource
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private val showPhraseState = mutableStateOf(false)
private val showExportState = mutableStateOf(false)
private val showExplorerState = mutableStateOf(false)

@Composable
fun WalletSettingsView(
  currency: String,
  hasPin: Boolean,
  mnemonic: String,
  autoLockDelay: Long,
  onAutoLockChange: (Long) -> Unit,
  onCurrencyChange: (String) -> Unit,
  onDevModeChange: (Boolean) -> Unit,
  checkPin: (String) -> Boolean,
  setPin: (String?) -> Unit,
  onDeleteWallet: () -> Unit,
  onCopyPhrase: (String) -> Unit,
  onKeepAlive: () -> Boolean,
  walletManager: WalletManager
) {
  var isPhraseVisible by showPhraseState
  var isExportVisible by showExportState
  var isExplorerVisible by showExplorerState

  var isDevModeEnabled by remember { mutableStateOf(walletManager.isDeveloperModeEnabled()) }

  var localCurrency by remember(currency) { mutableStateOf(currency) }

  DisposableEffect(Unit) {
    onDispose {
      showPhraseState.value = false
      showExportState.value = false
      showExplorerState.value = false
    }
  }

  val lifecycleOwner = LocalLifecycleOwner.current

  LaunchedEffect(Unit) {
    while (isActive) {
      if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
        if (onKeepAlive()) {
          isPhraseVisible = false
          isExportVisible = false
          isExplorerVisible = false
        }
      }
      delay(500)
    }
  }

  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) {
        if (onKeepAlive()) {
          isPhraseVisible = false
          isExportVisible = false
          isExplorerVisible = false
        }
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
  }

  val autoLockOptions = listOf(
    0L to stringResource(MR.strings.wallet_lock_immediately),
    60_000L to stringResource(MR.strings.wallet_lock_1min),
    300_000L to stringResource(MR.strings.wallet_lock_5min),
    3_600_000L to stringResource(MR.strings.wallet_lock_1h)
  )

  fun launchPinModal(mode: String, initialTitle: String, onSuccess: (String) -> Unit) {
    ModalManager.fullscreen.showPasscodeCustomModal(oneTime = true) { closePinModal ->
      Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background) {
        var step by remember { mutableStateOf(if (mode == "CHANGE" || mode == "VERIFY") 0 else 1) }

        if (step == 0) {
          VerifyWalletPasscodeView(
            title = initialTitle,
            verifyPin = checkPin,
            onSuccess = {
              if (mode == "CHANGE") {
                step = 1
              } else {
                onSuccess("")
                closePinModal()
              }
            },
            onCancel = { closePinModal() }
          )
        } else if (step == 1) {
          SetWalletPasscodeView(
            onSubmit = { newPin ->
              onSuccess(newPin)
              closePinModal()
            },
            onCancel = { closePinModal() }
          )
        }
      }
    }
  }

  Box(modifier = Modifier.fillMaxSize()) {

    if (isPhraseVisible) {
      WalletRecoveryPhraseView(
        mnemonic = mnemonic,
        onCopy = { phrase ->
          onCopyPhrase(phrase)
          AppNotificationManager.showSuccess(generalGetString(MR.strings.wallet_notif_phrase_copied))
        },
        onBack = { isPhraseVisible = false }
      )
    } else if (isExportVisible) {
      WalletExportKeysView(
        keys = walletManager.getPrivateKeys(),
        onCopy = { key, network ->
          onCopyPhrase(key)
          AppNotificationManager.showSuccess(generalGetString(MR.strings.wallet_notif_key_copied).replace("%s", network))
        },
        onBack = { isExportVisible = false }
      )
    } else if (isExplorerVisible) {
      WalletBlockExplorerSettingsView(
        walletManager = walletManager,
        onBack = { isExplorerVisible = false }
      )
    } else {
      ColumnWithScrollBar(modifier = Modifier.fillMaxSize()) {

        AppBarTitle(
          title = stringResource(MR.strings.wallet_step_settings),
        )

        Spacer(Modifier.height(40.dp))

        SectionView(stringResource(MR.strings.wallet_settings_security_title)) {

          SectionItemView(click = {
            launchPinModal("CHANGE", generalGetString(MR.strings.wallet_confirm_current_pin)) { newPin ->
              setPin(newPin)
              AppNotificationManager.showSuccess(generalGetString(MR.strings.wallet_notif_pin_changed))
            }
          }) {
            Icon(painterResource(MR.images.ic_lock_filled), null, tint = MaterialTheme.colors.onBackground)
            TextIconSpaced(false)
            Text(stringResource(MR.strings.wallet_settings_change_pin), color = MaterialTheme.colors.onBackground, modifier = Modifier.weight(1f))
            Icon(painterResource(MR.images.ic_chevron_right), null, tint = MaterialTheme.colors.secondary)
          }

          val autoLockState = rememberUpdatedState(autoLockDelay)

          ExposedDropDownSettingRow(
            title = stringResource(MR.strings.wallet_settings_auto_lock),
            values = autoLockOptions,
            selection = autoLockState,
            icon = painterResource(MR.images.ic_schedule),
            onSelected = { delay -> onAutoLockChange(delay) }
          )
        }

        SectionDividerSpaced()

        SectionView(stringResource(MR.strings.wallet_settings_backup_title)) {

          SectionItemView(click = {
            if (hasPin) {
              launchPinModal("VERIFY", generalGetString(MR.strings.wallet_confirm_current_pin)) { isPhraseVisible = true }
            } else {
              isPhraseVisible = true
            }
          }) {
            Icon(painterResource(MR.images.ic_visibility), null, tint = MaterialTheme.colors.secondary)
            TextIconSpaced(false)
            Text(stringResource(MR.strings.wallet_settings_show_phrase), color = MaterialTheme.colors.onBackground, modifier = Modifier.weight(1f))
            Icon(painterResource(MR.images.ic_chevron_right), null, tint = MaterialTheme.colors.secondary)
          }

          SectionItemView(click = {
            if (hasPin) {
              launchPinModal("VERIFY", generalGetString(MR.strings.wallet_confirm_current_pin)) { isExportVisible = true }
            } else {
              isExportVisible = true
            }
          }) {
            Icon(painterResource(MR.images.ic_vpn_key_filled), null, tint = MaterialTheme.colors.secondary)
            TextIconSpaced(false)
            Text(stringResource(MR.strings.wallet_settings_export_keys), color = MaterialTheme.colors.onBackground, modifier = Modifier.weight(1f))
            Icon(painterResource(MR.images.ic_chevron_right), null, tint = MaterialTheme.colors.secondary)
          }
        }

        SectionDividerSpaced()

        SectionView(stringResource(MR.strings.wallet_settings_preferences_title)) {
          SectionItemView(click = {
            val newCurrency = if (localCurrency == "USD") "EUR" else "USD"
            localCurrency = newCurrency
            onCurrencyChange(newCurrency)
          }) {
            Icon(painterResource(MR.images.ic_swap_horizontal_circle), null, tint = MaterialTheme.colors.secondary)
            TextIconSpaced(false)
            Text(stringResource(MR.strings.wallet_settings_default_currency), color = MaterialTheme.colors.onBackground, modifier = Modifier.weight(1f))
            Text(localCurrency, color = MaterialTheme.colors.primary, fontWeight = FontWeight.Bold)
          }

          SectionItemView(click = { isExplorerVisible = true }) {
            Icon(painterResource(MR.images.ic_travel_explore), null, tint = MaterialTheme.colors.secondary)
            TextIconSpaced(false)
            Text(stringResource(MR.strings.wallet_settings_block_explorer), color = MaterialTheme.colors.onBackground, modifier = Modifier.weight(1f))
            Icon(painterResource(MR.images.ic_chevron_right), null, tint = MaterialTheme.colors.secondary)
          }

          SectionItemView(click = {
            val newState = !isDevModeEnabled
            isDevModeEnabled = newState
            walletManager.setDeveloperModeEnabled(newState)
            onDevModeChange(newState)
          }) {
            Icon(painterResource(MR.images.ic_code), null, tint = if (isDevModeEnabled) MaterialTheme.colors.primary else MaterialTheme.colors.secondary)
            TextIconSpaced(false)
            Text(stringResource(MR.strings.wallet_settings_developer_mode), color = MaterialTheme.colors.onBackground, modifier = Modifier.weight(1f))

            DefaultSwitch(
              checked = isDevModeEnabled,
              onCheckedChange = { newState ->
                isDevModeEnabled = newState
                walletManager.setDeveloperModeEnabled(newState)
                onDevModeChange(newState)
              }
            )
          }
        }

        SectionDividerSpaced()

        SectionView(stringResource(MR.strings.wallet_settings_danger_title)) {

          SectionItemView(click = {
            com.arpokrat.common.wallet.WalletAssetCache.clearCache()
            AppNotificationManager.showSuccess(generalGetString(MR.strings.wallet_notif_cache_cleared))
          }) {
            Icon(painterResource(MR.images.ic_delete_sweep), null, tint = MaterialTheme.colors.error)
            TextIconSpaced(false)
            Text(stringResource(MR.strings.wallet_settings_clear_cache), color = MaterialTheme.colors.error, modifier = Modifier.weight(1f))
          }

          SectionItemView(click = {
            AlertManager.shared.showAlertDialog(
              title = generalGetString(MR.strings.wallet_settings_delete_confirm_title),
              text = generalGetString(MR.strings.wallet_settings_delete_warning),
              confirmText = generalGetString(MR.strings.wallet_settings_delete_action),
              dismissText = generalGetString(MR.strings.wallet_settings_cancel_action),
              destructive = true,
              onConfirm = {
                if (hasPin) {
                  launchPinModal("VERIFY", generalGetString(MR.strings.wallet_confirm_current_pin)) { onDeleteWallet() }
                } else {
                  onDeleteWallet()
                }
              }
            )
          }) {
            Icon(painterResource(MR.images.ic_delete), null, tint = MaterialTheme.colors.error)
            TextIconSpaced(false)
            Text(stringResource(MR.strings.wallet_settings_delete_wallet), color = MaterialTheme.colors.error, modifier = Modifier.weight(1f))
          }
        }

        SectionBottomSpacer()
      }
    }
  }
}