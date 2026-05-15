package com.arpokrat.common.wallet

import com.arpokrat.common.BuildConfigCommon

/**
 * Factory object responsible for instantiating the appropriate blockchain service clients.
 * Routes RPC and Explorer API requests based on the provided network chain ID.
 */
object NetworkFactory {

  fun getEvmService(chainId: Int): EvmChainService {
    val alchemyKey = BuildConfigCommon.ALCHEMY_API_KEY.replace("\"", "").trim()
    val etherscanV2Key = BuildConfigCommon.ETHERSCAN_API_KEY.replace("\"", "").trim()

    // The raw chainId (e.g., 60 for ETH, 137 for Polygon) is used directly to resolve the RPC URL.
    // It is passed as-is to avoid ID collisions (e.g., transforming 60 to 1 would conflict with Bitcoin Testnet)
    // when generating addresses via TrustWalletCore in the WalletManager.
    val rpcUrl = when (chainId) {
      60, 1 -> "https://eth-mainnet.g.alchemy.com/v2/$alchemyKey"
      137, 966 -> "https://polygon-mainnet.g.alchemy.com/v2/$alchemyKey"
      11155111 -> "https://eth-sepolia.g.alchemy.com/v2/$alchemyKey"
      80002 -> "https://polygon-amoy.g.alchemy.com/v2/$alchemyKey"
      else -> "https://polygon-rpc.com"
    }
    val explorerApiUrl = "https://api.etherscan.io/v2/api"

    return EvmChainService(rpcUrl, chainId, explorerApiUrl, etherscanV2Key)
  }

  fun getTronService(chainId: Int): TronChainService {
    val alchemyKey = BuildConfigCommon.ALCHEMY_API_KEY.replace("\"", "").trim()

    // Alchemy's REST proxy is used for Tron RPC calls.
    // Ensure the chainId strictly matches 195 to route to the Mainnet.
    val rpc = if (chainId == 195) "https://tron-mainnet.g.alchemy.com/v2/$alchemyKey"
    else "https://tron-testnet.g.alchemy.com/v2/$alchemyKey"

    val explorer = if (chainId == 195) "https://apilist.tronscanapi.com"
    else "https://nileapi.tronscan.org"

    return TronChainService(rpcUrl = rpc, explorerUrl = explorer)
  }

  fun getSolanaService(chainId: Int): SolanaChainService {
    val alchemyKey = BuildConfigCommon.ALCHEMY_API_KEY.replace("\"", "").trim()

    val rpc = if (chainId == 501) "https://solana-mainnet.g.alchemy.com/v2/$alchemyKey"
    else "https://solana-devnet.g.alchemy.com/v2/$alchemyKey"

    return SolanaChainService(rpcUrl = rpc)
  }

  fun getBitcoinService(chainId: Int): BitcoinService {
    // Uses Mempool.space open-source API for UTXO and transaction history
    val api = if (chainId == 0) "https://mempool.space/api" else "https://mempool.space/testnet/api"

    return BitcoinService(apiUrl = api)
  }
}