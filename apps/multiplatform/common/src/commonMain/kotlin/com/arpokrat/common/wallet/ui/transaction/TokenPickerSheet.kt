package com.arpokrat.common.wallet.ui.transaction

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.LocalPlatformContext
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.svg.SvgDecoder
import com.arpokrat.common.platform.LazyColumnWithScrollBar
import com.arpokrat.common.platform.Log
import com.arpokrat.common.platform.settings
import com.arpokrat.common.wallet.DEBUG_SWAP_POLLING
import com.arpokrat.common.views.chat.topPaddingToContent
import com.arpokrat.common.views.helpers.AppBarTitle
import com.arpokrat.common.views.helpers.ModernTextField
import com.arpokrat.common.wallet.SwapCatalog
import com.arpokrat.common.wallet.SwapToken
import com.arpokrat.common.wallet.SwapService
import com.arpokrat.common.wallet.SwapTokenMapping
import com.arpokrat.common.wallet.toSwapToken
import com.arpokrat.common.wallet.ui.components.DynamicCryptoIcon
import com.arpokrat.common.wallet.ui.components.WalletSpinner
import com.arpokrat.res.MR
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Composable
fun SwapTokenPickerScreen(
  title: String,
  walletTokens: List<SwapToken>,
  onPick: (SwapToken) -> Unit,
  onKeepAlive: () -> Unit = {},
  isWalletLocked: () -> Boolean = { false },
  closeFlow: () -> Unit = {}
) {
  var term by remember { mutableStateOf("") }
  var results by remember { mutableStateOf<List<SwapToken>>(emptyList()) }
  var isSearching by remember { mutableStateOf(false) }
  var networkFilter by remember { mutableStateOf<String?>(null) }
  var recent by remember { mutableStateOf(SwapRecent.load()) }
  var browseTab by remember { mutableStateOf(0) }
  var xstocksTried by remember { mutableStateOf(false) }

  LaunchedEffect(Unit) { while (isActive) { onKeepAlive(); delay(500) } }
  LaunchedEffect(Unit) { while (isActive) { if (isWalletLocked()) { closeFlow(); break }; delay(400) } }

  LaunchedEffect(Unit) { SwapCatalog.ensureLoaded() }
  val featured = SwapCatalog.featured
  LaunchedEffect(browseTab) {
    if (browseTab == 2 && SwapCatalog.xStocks.isEmpty()) { SwapCatalog.ensureXStocksLoaded(); xstocksTried = true }
  }

  val pick: (SwapToken) -> Unit = { t ->
    SwapRecent.add(t); recent = SwapRecent.load()
    onPick(t)
  }

  LaunchedEffect(term) {
    networkFilter = null
    if (term.isBlank()) { results = emptyList(); isSearching = false; return@LaunchedEffect }
    isSearching = true
    delay(400)
    val q = term.trim()
    val dtos = SwapService.searchToken(q)
    results = dtos.map { it.toSwapToken() }.sortedWith(swapResultOrder(q))
    isSearching = false
  }

  val q = term.trim().lowercase()
  val allWalletIds = remember(walletTokens) { walletTokens.map { it.tokenId }.toSet() }

  val recentDisplay = remember(recent, walletTokens) {
    recent.map { r -> walletTokens.firstOrNull { it.tokenId == r.tokenId } ?: r }
  }
  val matchingWallet: List<SwapToken> = remember(q, walletTokens) {
    if (q.isBlank()) walletTokens
    else walletTokens.filter {
      it.symbol.lowercase().contains(q) || it.name.lowercase().contains(q) || it.ticker.lowercase().contains(q)
    }
  }

  val networks: List<String> = remember(results) {
    results.map { it.network }.filter { it.isNotBlank() }.distinct().sorted()
  }
  val catalog: List<SwapToken> = results.filter {
    (networkFilter == null || it.network == networkFilter) && it.tokenId !in allWalletIds
  }
  val popular: List<SwapToken> = remember(featured, allWalletIds) {
    featured.filter { it.tokenId !in allWalletIds }
  }
  val xstocks: List<SwapToken> = SwapCatalog.xStocks.filter { it.tokenId !in allWalletIds }

  LazyColumnWithScrollBar(
    modifier = Modifier.fillMaxSize().background(MaterialTheme.colors.background),
    contentPadding = PaddingValues(top = topPaddingToContent(false), bottom = 32.dp)
  ) {
    item { AppBarTitle(title = title) }

    item {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colors.background).padding(horizontal = 20.dp, vertical = 4.dp)
      ) {
        ModernTextField(
          value = term,
          onValueChange = { term = it },
          modifier = Modifier.weight(1f),
          placeholder = { Text(stringResource(MR.strings.wallet_swap_search_hint)) },
          leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colors.primary) }
        )
        if (networks.size > 1) {
          Spacer(Modifier.width(8.dp))
          NetworkFilterPill(
            current = networkFilter,
            networks = networks,
            allLabel = stringResource(MR.strings.wallet_swap_all_networks),
            onSelect = { networkFilter = it }
          )
        }
      }
    }

    when {
      isSearching -> item {
        Box(Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
          WalletSpinner(size = 28, strokeWidth = 3f)
        }
      }

      q.isBlank() -> {
        if (recentDisplay.isNotEmpty()) {
          item {
            Column(Modifier.padding(horizontal = 20.dp)) {
              SectionHeader(stringResource(MR.strings.wallet_swap_recent))
              LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)) {
                items(recentDisplay) { token -> RecentChip(token = token, onClick = { pick(token) }) }
              }
            }
          }
        }
        item {
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            SwapSegmentChip(stringResource(MR.strings.wallet_swap_your_cryptos), selected = browseTab == 0, modifier = Modifier.weight(1f)) { browseTab = 0 }
            SwapSegmentChip(stringResource(MR.strings.wallet_swap_most_popular), selected = browseTab == 1, modifier = Modifier.weight(1f)) { browseTab = 1 }
            SwapSegmentChip(stringResource(MR.strings.wallet_swap_xstocks), selected = browseTab == 2, modifier = Modifier.weight(1f)) { browseTab = 2 }
          }
        }
        when (browseTab) {
          0 -> {
            if (walletTokens.isEmpty()) {
              item { Text(stringResource(MR.strings.wallet_swap_no_results), color = MaterialTheme.colors.onBackground.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp)) }
            } else {
              items(walletTokens, key = { "w_" + it.tokenId }) { token ->
                Box(Modifier.padding(horizontal = 20.dp)) { TokenRow(token = token, onClick = { pick(token) }) }
              }
            }
          }
          1 -> {
            if (popular.isEmpty()) {
              item { Box(Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) { WalletSpinner(size = 28, strokeWidth = 3f) } }
            } else {
              items(popular, key = { "p_" + it.tokenId }) { token ->
                Box(Modifier.padding(horizontal = 20.dp)) { TokenRow(token = token, onClick = { pick(token) }) }
              }
            }
          }
          else -> {
            when {
              xstocks.isNotEmpty() -> items(xstocks, key = { "x_" + it.tokenId }) { token ->
                Box(Modifier.padding(horizontal = 20.dp)) { TokenRow(token = token, onClick = { pick(token) }) }
              }
              xstocksTried -> item { Text(stringResource(MR.strings.wallet_swap_xstocks_empty), color = MaterialTheme.colors.onBackground.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp)) }
              else -> item { Box(Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) { WalletSpinner(size = 28, strokeWidth = 3f) } }
            }
          }
        }
      }

      else -> {
        if (matchingWallet.isNotEmpty()) {
          item { Box(Modifier.padding(horizontal = 20.dp)) { SectionHeader(stringResource(MR.strings.wallet_swap_your_cryptos)) } }
          items(matchingWallet, key = { "w_" + it.tokenId }) { token ->
            Box(Modifier.padding(horizontal = 20.dp)) { TokenRow(token = token, onClick = { pick(token) }) }
          }
        }
        if (catalog.isNotEmpty()) {
          item {
            Box(Modifier.padding(horizontal = 20.dp)) {
              if (matchingWallet.isNotEmpty()) Column { Spacer(Modifier.height(8.dp)); SectionHeader(stringResource(MR.strings.wallet_swap_all_tokens)) }
              else SectionHeader(stringResource(MR.strings.wallet_swap_all_tokens))
            }
          }
          items(catalog, key = { "c_" + it.tokenId }) { token ->
            Box(Modifier.padding(horizontal = 20.dp)) { TokenRow(token = token, onClick = { pick(token) }) }
          }
        } else if (matchingWallet.isEmpty()) {
          item { Text(stringResource(MR.strings.wallet_swap_no_results), color = MaterialTheme.colors.onBackground.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp)) }
        }
      }
    }
  }
}

@Composable
private fun SectionHeader(text: String) {
  Text(
    text,
    color = MaterialTheme.colors.onBackground.copy(alpha = 0.5f),
    fontSize = 13.sp,
    fontWeight = FontWeight.Medium,
    modifier = Modifier.padding(vertical = 6.dp)
  )
}

private fun swapResultOrder(query: String): Comparator<SwapToken> {
  val q = query.lowercase()
  return compareBy(
    { if (it.ticker.equals(q, true) || it.symbol.equals(q, true)) 0 else 1 },
    { if (it.network.equals("Mainnet", true)) 0 else 1 }
  )
}

@Composable
private fun NetworkFilterPill(current: String?, networks: List<String>, allLabel: String, onSelect: (String?) -> Unit) {
  var open by remember { mutableStateOf(false) }
  val active = current != null
  Box {
    Surface(
      shape = RoundedCornerShape(20.dp),
      color = if (active) MaterialTheme.colors.primary.copy(alpha = 0.12f) else MaterialTheme.colors.onBackground.copy(alpha = 0.05f),
      border = if (active) BorderStroke(1.dp, MaterialTheme.colors.primary) else null,
      modifier = Modifier.clickable { open = true }
    ) {
      Row(modifier = Modifier.padding(start = 12.dp, end = 6.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
          current ?: allLabel,
          color = if (active) MaterialTheme.colors.primary else MaterialTheme.colors.onBackground.copy(alpha = 0.7f),
          fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1
        )
        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = if (active) MaterialTheme.colors.primary else MaterialTheme.colors.onBackground.copy(alpha = 0.7f))
      }
    }
    DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
      DropdownMenuItem(onClick = { onSelect(null); open = false }) { Text(allLabel) }
      networks.forEach { net -> DropdownMenuItem(onClick = { onSelect(net); open = false }) { Text(net) } }
    }
  }
}

@Composable
private fun RecentChip(token: SwapToken, onClick: () -> Unit) {
  Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colors.onBackground.copy(alpha = 0.05f), modifier = Modifier.clickable(onClick = onClick)) {
    Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
      SwapTokenIcon(token = token, size = 22.dp)
      Spacer(Modifier.width(6.dp))
      Text(token.symbol.uppercase(), color = MaterialTheme.colors.onBackground, fontWeight = FontWeight.Medium, fontSize = 13.sp)
    }
  }
}

@Composable
private fun TokenRow(token: SwapToken, onClick: () -> Unit) {
  Row(
    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(vertical = 10.dp, horizontal = 6.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    SwapTokenIcon(token = token, size = 40.dp)
    Spacer(Modifier.width(14.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(token.symbol.uppercase(), color = MaterialTheme.colors.onBackground, fontWeight = FontWeight.Bold, fontSize = 15.sp)
      Text(token.name, color = MaterialTheme.colors.onBackground.copy(alpha = 0.6f), fontSize = 13.sp, maxLines = 1)
    }
    if (token.network.isNotBlank()) {
      Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colors.onBackground.copy(alpha = 0.05f)) {
        Text(token.network, color = MaterialTheme.colors.onBackground.copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
      }
    }
  }
}

@Composable
fun SwapTokenIcon(token: SwapToken, size: Dp) {
  Box(modifier = Modifier.size(size)) {
    val fallback = @Composable {
      Box(
        modifier = Modifier.fillMaxSize()
          .background(MaterialTheme.colors.primary.copy(alpha = 0.1f), CircleShape)
          .border(1.dp, MaterialTheme.colors.primary.copy(alpha = 0.2f), CircleShape),
        contentAlignment = Alignment.Center
      ) { Text(token.symbol.take(1).uppercase(), color = MaterialTheme.colors.primary, fontWeight = FontWeight.Bold, fontSize = (size.value / 2.2).sp) }
    }
    val walletAsset = token.walletAsset
    when {
      walletAsset != null -> DynamicCryptoIcon(asset = walletAsset, size = size, showNetworkBadge = false)
      token.iconUrl != null -> SubcomposeAsyncImage(
        model = ImageRequest.Builder(LocalPlatformContext.current)
          .data(token.iconUrl)
          .decoderFactory(SvgDecoder.Factory())
          .memoryCacheKey(token.iconUrl)
          .diskCacheKey(token.iconUrl)
          .crossfade(true)
          .build(),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = Modifier.fillMaxSize().clip(CircleShape),
        loading = { fallback() },
        error = {
          if (DEBUG_SWAP_POLLING) Log.e("SwapPolling", "icon failed ${token.symbol} url=${token.iconUrl}: ${it.result.throwable.message}")
          fallback()
        }
      )
      else -> fallback()
    }

    if (SwapTokenMapping.isNonNativeToken(token)) {
      val chainIcon = SwapCatalog.chainIconUrl(token.network)
      if (chainIcon != null) {
        val badgeSize = size * 0.44f
        Box(
          modifier = Modifier.align(Alignment.BottomEnd).size(badgeSize)
            .background(MaterialTheme.colors.surface, CircleShape)
            .padding(badgeSize * 0.08f),
          contentAlignment = Alignment.Center
        ) {
          SubcomposeAsyncImage(
            model = ImageRequest.Builder(LocalPlatformContext.current)
              .data(chainIcon)
              .decoderFactory(SvgDecoder.Factory())
              .crossfade(true)
              .build(),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize().clip(CircleShape),
            loading = {},
            error = {}
          )
        }
      }
    }
  }
}

@Serializable
private data class RecentToken(val tokenId: String, val symbol: String, val name: String, val network: String, val iconUrl: String?)

private object SwapRecent {
  private const val KEY = "swap_recent_targets"
  private const val MAX = 8
  private val json = Json { ignoreUnknownKeys = true }

  fun load(): List<SwapToken> = try {
    settings.getStringOrNull(KEY)
      ?.let { json.decodeFromString<List<RecentToken>>(it) }
      ?.map { SwapToken(ticker = it.tokenId.substringBefore("|"), network = it.network, symbol = it.symbol, name = it.name, iconUrl = it.iconUrl) }
      ?: emptyList()
  } catch (e: Exception) {
    emptyList()
  }

  fun add(token: SwapToken) {
    val current = load().filter { it.tokenId != token.tokenId }
    val updated = (listOf(token) + current).take(MAX)
      .map { RecentToken(it.tokenId, it.symbol, it.name, it.network, it.iconUrl) }
    try { settings.putString(KEY, json.encodeToString(updated)) } catch (e: Exception) {}
  }
}
