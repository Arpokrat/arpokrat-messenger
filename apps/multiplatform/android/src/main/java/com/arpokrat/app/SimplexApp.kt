package com.arpokrat.app

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.*
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.ViewCompat
import androidx.lifecycle.*
import androidx.work.*
import com.arpokrat.app.MainActivity.Companion.OLD_ANDROID_UI_FLAGS
import com.arpokrat.app.model.NtfManager
import com.arpokrat.app.model.NtfManager.AcceptCallAction
import com.arpokrat.app.views.call.CallActivity
import com.arpokrat.common.helpers.*
import com.arpokrat.common.model.*
import com.arpokrat.common.model.ChatController.appPrefs
import com.arpokrat.common.platform.*
import com.arpokrat.common.ui.theme.*
import com.arpokrat.common.views.call.*
import com.arpokrat.common.views.database.deleteOldChatArchive
import com.arpokrat.common.views.helpers.*
import com.arpokrat.common.views.onboarding.OnboardingStage
import com.jakewharton.processphoenix.ProcessPhoenix
import kotlinx.coroutines.*
import java.io.*
import java.util.*
import java.util.concurrent.TimeUnit
import java.math.BigDecimal
import java.math.BigInteger

import com.arpokrat.app.wallet.*
import com.arpokrat.common.wallet.*

const val TAG = "SIMPLEX"

class SimplexApp: Application(), LifecycleEventObserver {
  val chatModel: ChatModel
    get() = chatController.chatModel

  val chatController: ChatController = ChatController

  override fun onCreate() {
    super.onCreate()

    com.arpokrat.common.wallet.WalletContext.appContext = this

    System.loadLibrary("TrustWalletCore")

    AppContextProvider.initialize(this)
    if (ProcessPhoenix.isPhoenixProcess(this)) {
      return
    } else {
      registerGlobalErrorHandler()
      Handler(Looper.getMainLooper()).post {
        while (true) {
          try {
            Looper.loop()
          } catch (e: Throwable) {
            if (e is UnsatisfiedLinkError || e.message?.startsWith("Unable to start activity") == true) {
              Process.killProcess(Process.myPid())
              break
            } else {
              Thread.getDefaultUncaughtExceptionHandler()?.uncaughtException(Looper.getMainLooper().thread, e)
            }
          }
        }
      }
    }
    context = this
    initHaskell(packageName)
    initMultiplatform()
    reconfigureBroadcastReceivers()
    runMigrations()
    tmpDir.deleteRecursively()
    tmpDir.mkdir()
    deleteOldChatArchive()

    if (chatModel.migrationState.value != null) {
      appPrefs.onboardingStage.set(OnboardingStage.Step1_SimpleXInfo)
    } else if (DatabaseUtils.ksAppPassword.get() == null || DatabaseUtils.ksSelfDestructPassword.get() == null) {
      initChatControllerOnStart()
    }
    ProcessLifecycleOwner.get().lifecycle.addObserver(this@SimplexApp)
  }

  override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
    withLongRunningApi {
      when (event) {
        Lifecycle.Event.ON_START -> {
          isAppOnForeground = true
          if (chatModel.chatRunning.value == true) {
            withContext(Dispatchers.Main) {
              kotlin.runCatching {
                val currentUserId = chatModel.currentUser.value?.userId
                val chats = ArrayList(chatController.apiGetChats(chatModel.remoteHostId()))
                if (chatModel.currentUser.value?.userId == currentUserId) {
                  val currentChatId = chatModel.chatId.value
                  val oldStats = if (currentChatId != null) chatModel.getChat(currentChatId)?.chatStats else null
                  if (oldStats != null) {
                    val indexOfCurrentChat = chats.indexOfFirst { it.id == currentChatId }
                    if (indexOfCurrentChat >= 0) chats[indexOfCurrentChat] = chats[indexOfCurrentChat].copy(chatStats = oldStats)
                  }
                  chatModel.chatsContext.updateChats(chats)
                }
              }.onFailure { }
            }
          }
        }
        Lifecycle.Event.ON_RESUME -> {
          isAppOnForeground = true
          if (chatModel.controller.appPrefs.onboardingStage.get() == OnboardingStage.OnboardingComplete && chatModel.currentUser.value != null) {
            SimplexService.showBackgroundServiceNoticeIfNeeded()
          }
          if (chatModel.chatRunning.value != false &&
            chatModel.controller.appPrefs.onboardingStage.get() == OnboardingStage.OnboardingComplete &&
            appPrefs.notificationsMode.get() == NotificationsMode.SERVICE &&
            appPrefs.newDatabaseInitialized.get()
          ) {
            SimplexService.start()
          }
        }
        else -> isAppOnForeground = false
      }
    }
  }

  fun allowToStartServiceAfterAppExit() = with(chatModel.controller) {
    appPrefs.notificationsMode.get() == NotificationsMode.SERVICE &&
        (!NotificationsMode.SERVICE.requiresIgnoringBattery || SimplexService.isBackgroundAllowed())
  }

  private fun allowToStartPeriodically() = with(chatModel.controller) {
    appPrefs.notificationsMode.get() == NotificationsMode.PERIODIC &&
        (!NotificationsMode.PERIODIC.requiresIgnoringBattery || SimplexService.isBackgroundAllowed())
  }

  fun schedulePeriodicServiceRestartWorker() = CoroutineScope(Dispatchers.Default).launch {
    if (!allowToStartServiceAfterAppExit()) {
      getWorkManagerInstance().cancelUniqueWork(SimplexService.SERVICE_START_WORKER_WORK_NAME_PERIODIC)
      return@launch
    }
    val workerVersion = chatController.appPrefs.autoRestartWorkerVersion.get()
    val workPolicy = if (workerVersion == SimplexService.SERVICE_START_WORKER_VERSION) {
      ExistingPeriodicWorkPolicy.KEEP
    } else {
      chatController.appPrefs.autoRestartWorkerVersion.set(SimplexService.SERVICE_START_WORKER_VERSION)
      ExistingPeriodicWorkPolicy.REPLACE
    }
    val work = PeriodicWorkRequestBuilder<SimplexService.ServiceStartWorker>(SimplexService.SERVICE_START_WORKER_INTERVAL_MINUTES, TimeUnit.MINUTES)
      .addTag(SimplexService.TAG)
      .addTag(SimplexService.SERVICE_START_WORKER_WORK_NAME_PERIODIC)
      .build()
    getWorkManagerInstance().enqueueUniquePeriodicWork(SimplexService.SERVICE_START_WORKER_WORK_NAME_PERIODIC, workPolicy, work)
  }

  fun schedulePeriodicWakeUp() = CoroutineScope(Dispatchers.Default).launch {
    if (!allowToStartPeriodically()) {
      MessagesFetcherWorker.cancelAll(withLog = false)
      return@launch
    }
    MessagesFetcherWorker.scheduleWork()
  }

  companion object {
    lateinit var context: SimplexApp private set
  }

  private fun initMultiplatform() {
    androidAppContext = this
    APPLICATION_ID = BuildConfig.APPLICATION_ID

    try {
      val walletManager = WalletManager()

      CryptoManager.wallet = object : CryptoWalletInterface {

        override fun getAddress(coinType: Int): String {
          return walletManager.getAddressForNetwork(coinType)
        }

        override fun getAvailableAssets(onResult: (List<CryptoAsset>) -> Unit) {
          CoroutineScope(Dispatchers.IO).launch {
            val balances = supervisorScope {
              DefaultAssets.getActiveAssets().map { tokenDef ->
                async {
                  try {
                    val myAddress = getAddress(tokenDef.network.id)
                    if (myAddress.isNotEmpty()) {

                      var balanceStr = "0.0"

                      when(tokenDef.network.id) {
                        137, 80002, 60, 11155111 -> {
                          val service = NetworkFactory.getEvmService(tokenDef.network.id)
                          balanceStr = if (tokenDef.contractAddress == null) {
                            service.getBalance(myAddress)
                          } else {
                            service.getERC20Balance(tokenDef.contractAddress!!, myAddress, tokenDef.decimals)
                          }
                        }
                        195, 10000 -> {
                          val service = NetworkFactory.getTronService(tokenDef.network.id)
                          balanceStr = if (tokenDef.contractAddress == null) {
                            service.getTrxBalance(myAddress)
                          } else {
                            service.getTrc20Balance(tokenDef.contractAddress!!, myAddress)
                          }
                        }
                        0, 1 -> {
                          balanceStr = NetworkFactory.getBitcoinService(tokenDef.network.id).getBalance(myAddress)
                        }
                        501, 9000 -> {
                          balanceStr = NetworkFactory.getSolanaService(tokenDef.network.id).getBalance(myAddress)
                        }
                      }

                      if ((balanceStr.toBigDecimalOrNull() ?: BigDecimal.ZERO) > BigDecimal.ZERO) {
                        CryptoAsset(
                          symbol = tokenDef.symbol,
                          name = tokenDef.name,
                          balance = balanceStr,
                          decimals = tokenDef.decimals,
                          coinType = tokenDef.network.id,
                          contractAddress = tokenDef.contractAddress
                        )
                      } else null
                    } else null
                  } catch (e: Exception) {
                    null
                  }
                }
              }.awaitAll()
            }
            withContext(Dispatchers.Main) { onResult(balances.filterNotNull()) }
          }
        }

        override fun estimateGas(amount: String, asset: CryptoAsset, toAddress: String, onResult: (String) -> Unit) {
          CoroutineScope(Dispatchers.IO).launch {
            try {
              if (asset.coinType == 137 || asset.coinType == 80002 || asset.coinType == 60 || asset.coinType == 11155111) {
                val service = NetworkFactory.getEvmService(asset.coinType)
                val gasPriceStr = service.getGasPrice()
                val gasLimit = if (asset.contractAddress == null) 21000L else 65000L
                val feeWei = BigDecimal(gasPriceStr).multiply(BigDecimal.valueOf(gasLimit))
                val feeDecimal = feeWei.divide(BigDecimal.TEN.pow(18))

                val feeSymbol = if (asset.coinType == 60 || asset.coinType == 11155111) "ETH" else "POL"
                withContext(Dispatchers.Main) { onResult("${feeDecimal.toPlainString().take(8)} $feeSymbol") }
              } else {
                withContext(Dispatchers.Main) { onResult("N/A") }
              }
            } catch (e: Exception) {
              withContext(Dispatchers.Main) { onResult("Err") }
            }
          }
        }

        override fun sendPayment(amount: String, asset: CryptoAsset, toAddress: String, onResult: (String?) -> Unit) {
          CoroutineScope(Dispatchers.IO).launch {
            try {
              val txHash: String? = when (asset.coinType) {
                137, 80002, 60, 11155111 -> {
                  NetworkFactory.getEvmService(asset.coinType)
                    .sendTransaction(walletManager, toAddress, amount, asset.contractAddress, asset.decimals)
                }
                501, 9000 -> {
                  NetworkFactory.getSolanaService(asset.coinType)
                    .sendTransaction(walletManager, toAddress, amount)
                }
                0, 1 -> {
                  NetworkFactory.getBitcoinService(asset.coinType)
                    .sendTransaction(walletManager, toAddress, amount, asset.coinType)
                }
                195, 10000 -> {
                  NetworkFactory.getTronService(asset.coinType)
                    .sendTransaction(walletManager, asset.coinType, toAddress, amount, asset.contractAddress, asset.decimals)
                }
                else -> null
              }

              withContext(Dispatchers.Main) { onResult(txHash) }

            } catch (e: Exception) {
              withContext(Dispatchers.Main) { onResult(null) }
            }
          }
        }

        override fun isPinSet(): Boolean = walletManager.isPinSet()
        override fun validatePin(inputPin: String): Boolean = walletManager.checkPin(inputPin)
      }

    } catch (e: Exception) {

    }

    ntfManager = object : com.arpokrat.common.platform.NtfManager() {
      override fun notifyCallInvitation(invitation: RcvCallInvitation): Boolean = NtfManager.notifyCallInvitation(invitation)
      override fun hasNotificationsForChat(chatId: String): Boolean = NtfManager.hasNotificationsForChat(chatId)
      override fun cancelNotificationsForChat(chatId: String) = NtfManager.cancelNotificationsForChat(chatId)
      override fun cancelNotificationsForUser(userId: Long) = NtfManager.cancelNotificationsForUser(userId)
      override fun displayNotification(user: UserLike, chatId: String, displayName: String, msgText: String, image: String?, actions: List<Pair<NotificationAction, () -> Unit>>) = NtfManager.displayNotification(user, chatId, displayName, msgText, image, actions.map { it.first })
      override fun androidCreateNtfChannelsMaybeShowAlert() = NtfManager.createNtfChannelsMaybeShowAlert()
      override fun cancelCallNotification() = NtfManager.cancelCallNotification()
      override fun cancelAllNotifications() = NtfManager.cancelAllNotifications()
      override fun showMessage(title: String, text: String) = NtfManager.showMessage(title, text)
    }
    platform = object : PlatformInterface {
      override suspend fun androidServiceStart() {
        SimplexService.start()
      }

      override fun androidServiceSafeStop() {
        SimplexService.safeStopService()
      }

      override fun androidCallServiceSafeStop() {
        CallService.stopService()
      }

      override fun androidNotificationsModeChanged(mode: NotificationsMode) {
        if (mode.requiresIgnoringBattery && !SimplexService.isBackgroundAllowed()) {
          appPrefs.backgroundServiceNoticeShown.set(false)
        }
        SimplexService.StartReceiver.toggleReceiver(mode == NotificationsMode.SERVICE)
        SimplexService.AppUpdateReceiver.toggleReceiver(mode == NotificationsMode.SERVICE)
        CoroutineScope(Dispatchers.Default).launch {
          if (mode == NotificationsMode.SERVICE) {
            SimplexService.start()
            delay(2000)
            if (!SimplexService.isServiceStarted && appPrefs.notificationsMode.get() == NotificationsMode.SERVICE) {
              SimplexService.start()
            }
          } else {
            SimplexService.safeStopService()
          }
        }
        if (mode != NotificationsMode.SERVICE) {
          getWorkManagerInstance().cancelUniqueWork(SimplexService.SERVICE_START_WORKER_WORK_NAME_PERIODIC)
        }
        if (mode != NotificationsMode.PERIODIC) {
          MessagesFetcherWorker.cancelAll()
        }
        SimplexService.showBackgroundServiceNoticeIfNeeded(showOffAlert = false)
      }

      override fun androidChatStartedAfterBeingOff() {
        SimplexService.cancelPassphraseNotification()
        when (appPrefs.notificationsMode.get()) {
          NotificationsMode.SERVICE -> CoroutineScope(Dispatchers.Default).launch { platform.androidServiceStart() }
          NotificationsMode.PERIODIC -> SimplexApp.context.schedulePeriodicWakeUp()
          NotificationsMode.OFF -> {}
        }
      }

      override fun androidChatStopped() {
        getWorkManagerInstance().cancelUniqueWork(SimplexService.SERVICE_START_WORKER_WORK_NAME_PERIODIC)
        SimplexService.safeStopService()
        MessagesFetcherWorker.cancelAll()
      }

      override fun androidChatInitializedAndStarted() {
        if (chatModel.controller.appPrefs.onboardingStage.get() == OnboardingStage.OnboardingComplete) {
          SimplexService.showBackgroundServiceNoticeIfNeeded()
          if (appPrefs.notificationsMode.get() == NotificationsMode.SERVICE)
            withBGApi {
              platform.androidServiceStart()
            }
        }
      }

      override fun androidIsBackgroundCallAllowed(): Boolean = !SimplexService.isBackgroundRestricted()

      override fun androidSetNightModeIfSupported() {
        if (Build.VERSION.SDK_INT < 31) return
        val light = if (CurrentColors.value.name == DefaultTheme.SYSTEM_THEME_NAME) {
          null
        } else {
          CurrentColors.value.colors.isLight
        }
        val mode = when (light) {
          null -> UiModeManager.MODE_NIGHT_AUTO
          true -> UiModeManager.MODE_NIGHT_NO
          false -> UiModeManager.MODE_NIGHT_YES
        }
        val uiModeManager = androidAppContext.getSystemService(UI_MODE_SERVICE) as UiModeManager
        uiModeManager.setApplicationNightMode(mode)
      }

      override fun androidSetStatusAndNavigationBarAppearance(isLightStatusBar: Boolean, isLightNavBar: Boolean, blackNavBar: Boolean, themeBackgroundColor: Color) {
        val window = mainActivity.get()?.window ?: return
        @Suppress("DEPRECATION")
        val statusLight = isLightStatusBar && chatModel.activeCall.value == null
        val navBarLight = isLightNavBar || windowOrientation() == WindowOrientation.LANDSCAPE
        val windowInsetController = ViewCompat.getWindowInsetsController(window.decorView)
        if (windowInsetController?.isAppearanceLightStatusBars != statusLight) {
          windowInsetController?.isAppearanceLightStatusBars = statusLight
        }
        window.navigationBarColor = Color.Transparent.toArgb()
        if (windowInsetController?.isAppearanceLightNavigationBars != navBarLight) {
          windowInsetController?.isAppearanceLightNavigationBars = navBarLight
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
          window.decorView.systemUiVisibility = if (statusLight && navBarLight) {
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR or OLD_ANDROID_UI_FLAGS
          } else if (statusLight) {
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or OLD_ANDROID_UI_FLAGS
          } else if (navBarLight) {
            View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR or OLD_ANDROID_UI_FLAGS
          } else {
            OLD_ANDROID_UI_FLAGS
          }
          window.navigationBarColor = if (blackNavBar) Color.Black.toArgb() else themeBackgroundColor.toArgb()
        } else {
          window.navigationBarColor = Color.Transparent.toArgb()
        }
      }

      override fun androidStartCallActivity(acceptCall: Boolean, remoteHostId: Long?, chatId: ChatId?) {
        val context = mainActivity.get() ?: return
        val intent = Intent(context, CallActivity::class.java)
          .addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        if (acceptCall) {
          intent.setAction(AcceptCallAction)
            .putExtra("remoteHostId", remoteHostId)
            .putExtra("chatId", chatId)
        }
        intent.flags += Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT
        context.startActivity(intent)
      }

      override fun androidPictureInPictureAllowed(): Boolean {
        val appOps = androidAppContext.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        return appOps.checkOpNoThrow(AppOpsManager.OPSTR_PICTURE_IN_PICTURE, Process.myUid(), packageName) == AppOpsManager.MODE_ALLOWED
      }

      override fun androidCallEnded() {
        activeCallDestroyWebView()
      }

      override fun androidRestartNetworkObserver() {
        NetworkObserver.shared.restartNetworkObserver()
      }

      override fun androidIsXiaomiDevice(): Boolean = setOf("xiaomi", "redmi", "poco").contains(Build.BRAND.lowercase())

      @SuppressLint("SourceLockedOrientationActivity")
      @Composable
      override fun androidLockPortraitOrientation() {
        val context = LocalContext.current
        DisposableEffect(Unit) {
          val activity = context as? Activity ?: return@DisposableEffect onDispose {}
          activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
          onDispose {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
          }
        }
      }

      override suspend fun androidAskToAllowBackgroundCalls(): Boolean {
        if (SimplexService.isBackgroundRestricted()) {
          val userChoice: CompletableDeferred<Boolean> = CompletableDeferred()
          SimplexService.showBGRestrictedInCall {
            userChoice.complete(it)
          }
          return userChoice.await()
        }
        return true
      }

      override fun androidCreateActiveCallState(): Closeable = ActiveCallState()

      override val androidApiLevel: Int get() = Build.VERSION.SDK_INT
    }
  }

  private fun reconfigureBroadcastReceivers() {
    val mode = appPrefs.notificationsMode.get()
    SimplexService.StartReceiver.toggleReceiver(mode == NotificationsMode.SERVICE)
    SimplexService.AppUpdateReceiver.toggleReceiver(mode == NotificationsMode.SERVICE)
  }
}