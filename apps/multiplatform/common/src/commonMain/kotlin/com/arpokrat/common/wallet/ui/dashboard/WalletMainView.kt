package com.arpokrat.common.wallet.ui.dashboard

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.arpokrat.app.wallet.PriceService
import com.arpokrat.common.platform.Log
import com.arpokrat.common.views.helpers.*
import com.arpokrat.common.wallet.*
import com.arpokrat.common.wallet.ui.components.*
import com.arpokrat.common.wallet.ui.onboarding.*
import com.arpokrat.common.wallet.ui.settings.WalletSettingsView
import com.arpokrat.common.wallet.ui.transaction.WalletScanView
import com.arpokrat.res.MR
import dev.icerock.moko.resources.compose.painterResource
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.*

enum class WalletStep {
  LOCKED, WELCOME, CREATE_SHOW, CREATE_VERIFY, IMPORT_INPUT, PIN_SETUP, DASHBOARD
}

/**
 * The root controller for the Wallet UI.
 * Handles state transitions (Locked, Setup, Dashboard), automated background polling for balances,
 * and routing transaction requests to the appropriate blockchain services.
 */
@Composable
fun WalletMainView(closeWallet: () -> Unit) {
  val clipboardManager = LocalClipboardManager.current
  val uriHandler = LocalUriHandler.current
  val scope = rememberCoroutineScope()
  val walletManager = remember { WalletManager() }

  val msgNetworkError = stringResource(MR.strings.wallet_notif_network_error)
  val msgSending = stringResource(MR.strings.wallet_notif_sending)
  val msgSent = stringResource(MR.strings.wallet_notif_sent)
  val msgFailed = stringResource(MR.strings.wallet_notif_failed)
  val msgAddressCopied = stringResource(MR.strings.wallet_notif_address_copied)
  val msgExplorer = stringResource(MR.strings.wallet_notif_opening_explorer)
  val msgSwapSoon = stringResource(MR.strings.wallet_notif_swap_soon)
  val msgBuySoon = stringResource(MR.strings.wallet_notif_buy_sell_soon)
  val msgDevModeSwap = stringResource(MR.strings.wallet_notif_dev_mode_swap)
  val msgDevModeBuy = stringResource(MR.strings.wallet_notif_dev_mode_buy)

  var manualRefreshTrigger by remember { mutableStateOf(0) }
  var isPinSet by remember { mutableStateOf(walletManager.isPinSet()) }
  var autoLockDelay by remember { mutableStateOf(walletManager.getAutoLockDelay()) }

  var currentStep by remember {
    mutableStateOf(if (walletManager.hasWallet()) { if (walletManager.shouldAutoLock()) WalletStep.LOCKED else WalletStep.DASHBOARD } else WalletStep.WELCOME)
  }

  var tempMnemonic by remember { mutableStateOf("") }
  var isImportMode by remember { mutableStateOf(false) }
  var scanSessionId by remember { mutableStateOf(0) }

  val lifecycleOwner = LocalLifecycleOwner.current

  var isLockModalOpen by remember { mutableStateOf(false) }

  fun enforceFullscreenLock() {
    if (isLockModalOpen) return
    isLockModalOpen = true

    ModalManager.fullscreen.showPasscodeCustomModal(oneTime = true) { closePinModal ->
      Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background) {
        var lockRemainingTime by remember { mutableStateOf(walletManager.getRemainingLockoutTime()) }

        LaunchedEffect(lockRemainingTime) {
          if (lockRemainingTime > 0) {
            delay(1000)
            lockRemainingTime = walletManager.getRemainingLockoutTime()
          }
        }

        VerifyWalletPasscodeView(
          title = if (lockRemainingTime > 0) stringResource(MR.strings.wallet_lock_timer, lockRemainingTime / 1000) else stringResource(MR.strings.wallet_enter_pin),
          verifyPin = { input ->
            if (lockRemainingTime > 0) false
            else {
              val valid = walletManager.checkPin(input)
              if (!valid) {
                walletManager.recordFailedAttempt()
                lockRemainingTime = walletManager.getRemainingLockoutTime()
              }
              valid
            }
          },
          onSuccess = {
            walletManager.resetFailedAttempts()
            walletManager.updateLastActiveTime()
            isLockModalOpen = false
            closePinModal()
          },
          onCancel = {
            isLockModalOpen = false
            closePinModal()
            closeWallet()
          }
        )
      }
    }
  }

  fun performSecurityCheck() {
    if (walletManager.shouldAutoLock()) {
      enforceFullscreenLock()
    } else if (!isLockModalOpen && lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
      walletManager.updateLastActiveTime()
    }
  }

  LaunchedEffect(currentStep) {
    while (isActive) {
      if (currentStep == WalletStep.DASHBOARD) {
        performSecurityCheck()
      }
      delay(500)
    }
  }

  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) {
        if (currentStep == WalletStep.DASHBOARD) {
          performSecurityCheck()
        }
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
  }

  var isDevMode by remember { mutableStateOf(walletManager.isDeveloperModeEnabled()) }

  var assets by remember {
    mutableStateOf(
      run {
        val cachedAssets = WalletAssetCache.loadAssets()
        DefaultAssets.getActiveAssets().map { def ->
          val cached = cachedAssets.find { it.coinType == def.network.id && it.symbol == def.symbol }
          cached?.copy(name = def.name, contractAddress = def.contractAddress, decimals = def.decimals)
            ?: CryptoAsset(symbol = def.symbol, name = def.name, balance = "0.00", decimals = def.decimals, coinType = def.network.id, contractAddress = def.contractAddress)
        }
      }
    )
  }

  val displayAssets = remember(assets, isDevMode) {
    val testnetIds = listOf(1, 11155111, 80002, 9000, 10000)
    if (isDevMode) assets.filter { it.coinType in testnetIds } else assets.filterNot { it.coinType in testnetIds }
  }

  var currency by remember { mutableStateOf(walletManager.getCurrency()) }
  var isRefreshing by remember { mutableStateOf(false) }
  var totalValue by remember { mutableStateOf(0.0) }

  /**
   * Background polling loop. Refreshes fiat prices and blockchain balances every 30 seconds
   * or when manually triggered by the user via pull-to-refresh.
   */
  LaunchedEffect(currentStep, currency, manualRefreshTrigger) {
    if (currentStep == WalletStep.DASHBOARD) {
      while (isActive) {
        isRefreshing = true
        try {
          val symbols = displayAssets.map { it.symbol }
          PriceService.fetchPrices(symbols)

          val updatedAssets = withContext(Dispatchers.IO) {
            displayAssets.map { asset ->
              val myAddress = walletManager.getAddressForNetwork(asset.coinType)
              try {
                val newBalance = when (asset.coinType) {
                  CryptoNetwork.TRON.id, CryptoNetwork.TRON_NILE.id -> {
                    if (asset.contractAddress.isNullOrBlank()) NetworkFactory.getTronService(asset.coinType).getTrxBalance(myAddress)
                    else NetworkFactory.getTronService(asset.coinType).getTrc20Balance(asset.contractAddress!!, myAddress)
                  }
                  CryptoNetwork.BITCOIN.id, CryptoNetwork.BITCOIN_TESTNET.id -> {
                    NetworkFactory.getBitcoinService(asset.coinType).getBalance(myAddress)
                  }
                  CryptoNetwork.SOLANA.id, CryptoNetwork.SOLANA_DEVNET.id -> {
                    NetworkFactory.getSolanaService(asset.coinType).getBalance(myAddress)
                  }
                  else -> {
                    if (asset.contractAddress.isNullOrBlank()) {
                      NetworkFactory.getEvmService(asset.coinType).getBalance(myAddress)
                    } else {
                      NetworkFactory.getEvmService(asset.coinType).getERC20Balance(asset.contractAddress!!, myAddress, asset.decimals)
                    }
                  }
                }
                asset.copy(balance = newBalance ?: asset.balance)
              } catch (e: Exception) {
                asset
              }
            }
          }

          val finalAssets = assets.map { current ->
            updatedAssets.find { it.coinType == current.coinType && it.symbol == current.symbol } ?: current
          }

          assets = finalAssets
          WalletAssetCache.saveAssets(finalAssets)

          var tempTotal = 0.0
          finalAssets.forEach { asset ->
            val qty = asset.balance.replace(",", ".").trim().toDoubleOrNull() ?: 0.0
            if (qty > 0) tempTotal += PriceService.getValue(asset.symbol, qty, currency)
          }
          totalValue = tempTotal

        } catch (e: Exception) {
        } finally {
          isRefreshing = false
        }

        delay(30000)
      }
    }
  }

  ModalView(
    close = closeWallet,
    showAppBar = false
  ) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Transparent)) {
      Crossfade(targetState = currentStep) { step ->
        when (step) {
          WalletStep.DASHBOARD -> {
            WalletHomeView(
              assets = displayAssets,
              totalValue = totalValue,
              currency = currency,
              isRefreshing = isRefreshing,
              onToggleCurrency = {
                val newCurr = if (currency == "USD") "EUR" else "USD"
                walletManager.setCurrency(newCurr)
                currency = newCurr
              },
              onAssetClick = { clickedAsset ->
                val myAddress = walletManager.getAddressForNetwork(clickedAsset.coinType)

                ModalManager.start.showModalCloseable(
                  endButtons = {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 8.dp)) {
                      if (isDevMode) { DevModeBadge(); Spacer(Modifier.width(8.dp)) }
                      IconButton(onClick = {
                        AppNotificationManager.showSuccess(msgExplorer)
                        try { uriHandler.openUri(walletManager.getAddressExplorerTemplate(clickedAsset.coinType).replace("{address}", myAddress)) } catch (e: Exception) {}
                      }) { Icon(painterResource(MR.images.ic_travel_explore), stringResource(MR.strings.explorer), tint = MaterialTheme.colors.primary) }
                    }
                  }
                ) { closeAssetDetails ->

                  val liveAsset = assets.find { it.coinType == clickedAsset.coinType && it.symbol == clickedAsset.symbol } ?: clickedAsset
                  val nativeNetworkId = CryptoNetwork.fromId(liveAsset.coinType).id
                  val nativeAsset = displayAssets.find {
                    it.coinType == nativeNetworkId && FeeService.isNativeToken(it.contractAddress)
                  }
                  val nativeBalance = nativeAsset?.balance?.replace(",", ".")?.toDoubleOrNull() ?: 0.0

                  WalletAssetDetailView(
                    asset = liveAsset,
                    nativeBalance = nativeBalance,
                    currency = currency,
                    myAddress = myAddress,
                    isDevMode = isDevMode,
                    walletManager = walletManager,
                    onBack = { closeAssetDetails() },
                    onScanClick = { callback ->
                      scanSessionId++
                      ModalManager.end.showModalCloseable(
                        endButtons = {
                          if (isDevMode) { Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 16.dp)) { DevModeBadge() } }
                        }
                      ) { closeScanner ->
                        WalletScanView(
                          scanSessionId = scanSessionId,
                          onAddressScanned = { barcode -> callback(barcode); closeScanner() },
                          onClose = { closeScanner() }
                        )
                      }
                    },
                    onSendConfirm = { dest, amount, onHistoryRefreshRequested ->
                      AppNotificationManager.showSuccess(msgSending)

                      // Launching the transaction task completely independently of the UI thread
                      kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                        try {
                          val txHash: String? = when (liveAsset.coinType) {
                            CryptoNetwork.POLYGON.id, 80002, CryptoNetwork.ETHEREUM.id, 11155111 -> {
                              NetworkFactory.getEvmService(liveAsset.coinType)
                                .sendTransaction(walletManager, dest, amount, liveAsset.contractAddress, liveAsset.decimals)
                            }
                            CryptoNetwork.SOLANA.id, CryptoNetwork.SOLANA_DEVNET.id -> {
                              NetworkFactory.getSolanaService(liveAsset.coinType)
                                .sendTransaction(walletManager, dest, amount)
                            }
                            CryptoNetwork.BITCOIN.id, CryptoNetwork.BITCOIN_TESTNET.id -> {
                              NetworkFactory.getBitcoinService(liveAsset.coinType)
                                .sendTransaction(walletManager, dest, amount, liveAsset.coinType)
                            }
                            CryptoNetwork.TRON.id, CryptoNetwork.TRON_NILE.id -> {
                              NetworkFactory.getTronService(liveAsset.coinType)
                                .sendTransaction(walletManager, liveAsset.coinType, dest, amount, liveAsset.contractAddress, liveAsset.decimals)
                            }
                            else -> null
                          }

                          // Returning to the Main thread strictly for UI updates
                          kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            if (txHash != null && !txHash.startsWith("Error")) {
                              AppNotificationManager.showSuccess(msgSent)
                              onHistoryRefreshRequested()
                              manualRefreshTrigger++
                            } else {
                              val errorMsg = txHash ?: "Unknown Error (txHash is null)"
                              AppNotificationManager.showError("$msgFailed: $errorMsg")
                            }
                          }
                        } catch (e: Throwable) {
                          kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            AppNotificationManager.showError("CRASH: ${e.message}")
                          }
                        }
                      }
                    },
                    onCopy = { text -> clipboardManager.setText(AnnotatedString(text)); AppNotificationManager.showSuccess(msgAddressCopied) },
                    onOpenExplorer = { hash ->
                      AppNotificationManager.showSuccess(msgExplorer)
                      try { uriHandler.openUri(walletManager.getTxExplorerTemplate(liveAsset.coinType).replace("{hash}", hash)) } catch (e: Exception) {}
                    },
                    onOpenAddressExplorer = {
                      AppNotificationManager.showSuccess(msgExplorer)
                      try { uriHandler.openUri(walletManager.getAddressExplorerTemplate(liveAsset.coinType).replace("{address}", myAddress)) } catch (e: Exception) {}
                    },
                    onSwapClick = { if (isDevMode) AppNotificationManager.showError(msgDevModeSwap) else AppNotificationManager.showSuccess(msgSwapSoon) },
                    onToggleCurrency = {
                      val newCurr = if (currency == "USD") "EUR" else "USD"
                      walletManager.setCurrency(newCurr)
                      currency = newCurr
                    },
                    onKeepAlive = {
                      performSecurityCheck()
                    }
                  )
                }
              },
              onSwapClick = { if (isDevMode) AppNotificationManager.showError(msgDevModeSwap) else AppNotificationManager.showSuccess(msgSwapSoon) },
              onBuySellClick = { if (isDevMode) AppNotificationManager.showError(msgDevModeBuy) else AppNotificationManager.showSuccess(msgBuySoon) },
              onSettingsClick = {
                ModalManager.start.showModalCloseable(
                  endButtons = {
                    if (isDevMode) { Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 16.dp)) { DevModeBadge() } }
                  }
                ) { closeSettings ->
                  WalletSettingsView(
                    currency = currency,
                    hasPin = isPinSet,
                    mnemonic = walletManager.getMnemonic() ?: "",
                    autoLockDelay = autoLockDelay,
                    onAutoLockChange = { newDelay -> walletManager.setAutoLockDelay(newDelay); autoLockDelay = newDelay },
                    onCurrencyChange = { newCurr -> walletManager.setCurrency(newCurr); currency = newCurr },
                    onDevModeChange = { newMode -> isDevMode = newMode },
                    checkPin = { input -> walletManager.checkPin(input) },
                    setPin = { newPin -> walletManager.setPin(newPin); isPinSet = (newPin != null) },
                    onDeleteWallet = {
                      walletManager.deleteWallet()
                      walletManager.setPin(null)
                      WalletAssetCache.clearCache()
                      isPinSet = false
                      currentStep = WalletStep.WELCOME
                      closeSettings()
                    },
                    onCopyPhrase = { clipboardManager.setText(AnnotatedString(it)) },
                    onKeepAlive = {
                      performSecurityCheck()
                      isLockModalOpen
                    },
                    walletManager = walletManager
                  )
                }
              },
              onRefresh = { manualRefreshTrigger++ },
              getAssetFiatValue = { asset ->
                val qty = asset.balance.replace(",", ".").trim().toDoubleOrNull() ?: 0.0
                String.format("%.2f", PriceService.getValue(asset.symbol, qty, currency))
              },
              close = closeWallet
            )
          }
          else -> {
            WalletOnboardingFlow(
              step = step, walletManager = walletManager, tempMnemonic = tempMnemonic,
              onMnemonicChange = { tempMnemonic = it }, onStepChange = { currentStep = it },
              isImportMode = isImportMode, onSetImportMode = { isImportMode = it }, closeWallet = closeWallet
            )
          }
        }
      }
    }
  }
}

@Composable
fun WalletOnboardingFlow(
  step: WalletStep,
  walletManager: WalletManager,
  tempMnemonic: String,
  onMnemonicChange: (String) -> Unit,
  onStepChange: (WalletStep) -> Unit,
  isImportMode: Boolean,
  onSetImportMode: (Boolean) -> Unit,
  closeWallet: () -> Unit
) {
  var lockRemainingTime by remember { mutableStateOf(walletManager.getRemainingLockoutTime()) }

  LaunchedEffect(step, lockRemainingTime) {
    if (step == WalletStep.LOCKED && lockRemainingTime > 0) {
      delay(1000)
      lockRemainingTime = walletManager.getRemainingLockoutTime()
    }
  }

  Box(modifier = Modifier.fillMaxSize()) {
    if (step == WalletStep.LOCKED) {
      VerifyWalletPasscodeView(
        title = if (lockRemainingTime > 0) stringResource(MR.strings.wallet_lock_timer, lockRemainingTime / 1000) else stringResource(MR.strings.wallet_enter_pin),
        verifyPin = { input ->
          if (lockRemainingTime > 0) false
          else {
            val valid = walletManager.checkPin(input)
            if (!valid) {
              walletManager.recordFailedAttempt()
              lockRemainingTime = walletManager.getRemainingLockoutTime()
            }
            valid
          }
        },
        onSuccess = {
          walletManager.resetFailedAttempts()
          walletManager.updateLastActiveTime()
          onStepChange(WalletStep.DASHBOARD)
        },
        onCancel = { closeWallet() }
      )
      return@Box
    }

    if (step == WalletStep.PIN_SETUP) {
      SetWalletPasscodeView(
        onSubmit = { newPin ->
          walletManager.setPin(newPin)
          walletManager.updateLastActiveTime()
          if (isImportMode) onStepChange(WalletStep.IMPORT_INPUT)
          else {
            onMnemonicChange(walletManager.createNewWallet(128, false))
            onStepChange(WalletStep.CREATE_SHOW)
          }
        },
        onCancel = { onStepChange(WalletStep.WELCOME) }
      )
      return@Box
    }

    Box(modifier = Modifier.fillMaxSize()) {
      when (step) {
        WalletStep.WELCOME -> NoWalletView(
          onCreateClick = { onSetImportMode(false); onStepChange(WalletStep.PIN_SETUP) },
          onImportClick = { onSetImportMode(true); onStepChange(WalletStep.PIN_SETUP) }
        )
        WalletStep.CREATE_SHOW -> MnemonicBackupView(mnemonic = tempMnemonic, onNext = { onStepChange(WalletStep.CREATE_VERIFY) })
        WalletStep.CREATE_VERIFY -> VerifySeedView(mnemonic = tempMnemonic, onVerified = { walletManager.importWalletFromMnemonic(tempMnemonic); onStepChange(WalletStep.DASHBOARD) })
        WalletStep.IMPORT_INPUT -> ImportWalletView(onImport = { seed -> if (walletManager.importWalletFromMnemonic(seed)) onStepChange(WalletStep.DASHBOARD) })
        else -> {}
      }
    }

    IconButton(
      onClick = {
        when (step) {
          WalletStep.CREATE_VERIFY -> onStepChange(WalletStep.CREATE_SHOW)
          WalletStep.CREATE_SHOW, WalletStep.IMPORT_INPUT -> {
            walletManager.setPin(null)
            onMnemonicChange("")
            onStepChange(WalletStep.WELCOME)
          }
          WalletStep.WELCOME -> closeWallet()
          else -> {
            walletManager.setPin(null)
            onStepChange(WalletStep.WELCOME)
          }
        }
      },
      modifier = Modifier.statusBarsPadding().padding(start = 8.dp, top = 8.dp).align(Alignment.TopStart)
    ) {
      Icon(painterResource(MR.images.ic_arrow_back_ios_new), stringResource(MR.strings.back), tint = MaterialTheme.colors.primary)
    }
  }
}