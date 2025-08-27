import "@stdlib/lisp-lists"
import "@stdlib/tvm-dicts"
import "types.tolk";
import "../lib/access/ownable_2step.tolk";
import "../deployable/types.tolk";
import "../lib/upgrades/type_and_version.tolk";
import "../lib/utils.tolk";

const VAL_1E5:  uint256 = 100000;
const VAL_1E14: uint256 = 100000000000000;
const VAL_1E16: uint256 = 10000000000000000;
const VAL_1E18: uint256 = 1000000000000000000;

// TODO:
const TON_2_EVM_MESSAGE_FIXED_BYTES = 1;
const TON_2_EVM_MESSAGE_FIXED_BYTES_PER_TOKEN = 1;

const CCIP_LOCK_OR_BURN_V1_RET_BYTES = 32;

const CHAIN_FAMILY_SELECTOR_EVM:   uint32 = 0x2812d52c;
const CHAIN_FAMILY_SELECTOR_SVM:   uint32 = 0x1e10bdc4;
const CHAIN_FAMILY_SELECTOR_APTOS: uint32 = 0xac77ffec;
const CHAIN_FAMILY_SELECTOR_SUI:   uint32 = 0xc4e05953;

struct Storage {
    ownable: Ownable2Step;

    maxFeeJuelsPerMsg: uint96;
    linkToken: address;
    tokenPriceStalenessThreshold: uint64;

    // TODO: this is on DestChain instead (same as Solana)
    // usdPerUnitGasByDestChain: UMap<uint64, TimestampedPrice>; // (idict len=64) chainSelector -> TimestampedPrice
    usdPerToken: Map<TimestampedPrice>;
    // TODO: use presence in this map to validate a token as feeToken
    premiumMultiplierWeiPerEth: Map<uint64>;

    destChainConfigs: UMap<uint64, DestChainConfig>
}

fun Storage.load(): Storage {
    return Storage.fromCell(contract.getData());
}

fun Storage.store(self) {
    return contract.setData(self.toCell());
}

// This represents FeeQuoter's DestChainConfig (not OnRamp)
struct DestChainConfig {
    config: FeeQuoterDestChainConfig;

    // TODO: split this out since updates are more frequent?
    usdPerUnitGas: Cell<GasPrice>; // TODO: it'd be nice if this wasn't split off

    tokenTransferFeeConfigs: Map<TokenTransferFeeConfig>; // (sdict) address -> TokenTransferFeeConfig
}

type Msg = UpdatePrices
    | UpdateFeeTokens
    | UpdateTokenTransferFeeConfigs
    | UpdateDestChainConfig
    | GetValidatedFee<cell>; // marked as cell since we never attempt to load the metadata

fun onInternalMessage(in: InMessage) {
    val msg = lazy Msg.fromSlice(in.body);
    match (msg) {
        UpdatePrices => {
            // TODO: validate that price updates came from the offramp
            var st = lazy Storage.load();
            updatePrices(mutate st, msg.updates);
            st.store();
        }
        UpdateFeeTokens => {
            var st = lazy Storage.load();
            st.ownable.requireOwner(in.senderAddress);
            updateFeeTokens(mutate st, msg);
            st.store();
        }
        UpdateTokenTransferFeeConfigs => {
            var st = lazy Storage.load();
            st.ownable.requireOwner(in.senderAddress);
            updateTokenTransferFeeConfigs(mutate st, msg);
            st.store();
        }
        UpdateDestChainConfig => {
            var st = lazy Storage.load();
            st.ownable.requireOwner(in.senderAddress);
            updateDestChainConfig(mutate st, msg); // TODO: pass in st.destChainConfigs instead
            st.store();
        }
        GetValidatedFee<cell> => { getValidatedFee(msg, in.senderAddress) }
        else => {
            // ignore empty messages, "wrong opcode" for others
            assert (in.body.isEmpty()) throw 0xFFFF
        }
    }
}

fun updateFeeTokens(mutate st: Storage, msg: UpdateFeeTokens) {
    var (token, config, exists) = msg.add.min();
    while (exists) {
        st.premiumMultiplierWeiPerEth.set(token!, config!.premiumMultiplierWeiPerEth);

        (token, config, exists) = msg.add.next(token!);
    }

    var iter = Iterator<address>.new(msg.remove);
    while (!iter.empty()) {
        val tokenAddr = iter.next();
        st.premiumMultiplierWeiPerEth.delete(tokenAddr);
    }
}

fun updateTokenTransferFeeConfigs(mutate st: Storage, msg: UpdateTokenTransferFeeConfigs) {
    var (destChainSelector, update, exists) = msg.updates.min();
    while (exists) {
        val (maybeDestChainConfig, configExists) = st.destChainConfigs.get(destChainSelector!);
        if (configExists) {
            var destChainConfig = maybeDestChainConfig!;

            var (tokenAddr, config, existsInner) = update!.add.min();
            while (existsInner) {
                destChainConfig.tokenTransferFeeConfigs.set(tokenAddr!, config!);

                (tokenAddr, config, existsInner) = update!.add.next(tokenAddr!);
            }

            var iter = Iterator<address>.new(update!.remove);
            while (!iter.empty()) {
                val tokenAddr = iter.next();
                destChainConfig.tokenTransferFeeConfigs.delete(tokenAddr);
            }

            st.destChainConfigs.replace(destChainSelector!, destChainConfig);
        }

        (destChainSelector, update, exists) = msg.updates.next(destChainSelector!);
    }
}

// TODO: allow providing an array of these msgs to feeQuoter
fun updateDestChainConfig(mutate st: Storage, msg: UpdateDestChainConfig) {
    val (maybeDestChainConfig, exists) = st.destChainConfigs.get(msg.destChainSelector);
    if (exists) {
        var destChainConfig = maybeDestChainConfig!;
        destChainConfig.config = msg.destChainConfig;
        st.destChainConfigs.replace(msg.destChainSelector, destChainConfig);
    } else {
        st.destChainConfigs.set(msg.destChainSelector, DestChainConfig {
            config: msg.destChainConfig,
            usdPerUnitGas: GasPrice {
                executionGasPrice: 0,
                dataAvailabilityGasPrice: 0,
                timestamp: 0,
            }.toCell(),
            tokenTransferFeeConfigs: Map<TokenTransferFeeConfig>.new()
        })
    }
}

fun updatePrices(mutate st: Storage, priceUpdates: PriceUpdates) {
    val timestamp = blockchain.now();

    // TODO: assert cells are well-formed
    var iter = Iterator<TokenPriceUpdate>.new(priceUpdates.tokenPriceUpdates);
    while (!iter.empty()) {
        val update = iter.next();

        st.usdPerToken.set(update.sourceToken, TimestampedPrice {
            value: update.usdPerToken,
            timestamp
        })
      //TODO: emit UsdPerTokenUpdated(update.sourceToken, update.usdPerToken, block.timestamp);
    }

    var iter2 = Iterator<GasPriceUpdate>.new(priceUpdates.gasPriceUpdates);
    while (!iter2.empty()) {
        val update = iter2.next();

        val (maybeDestChainConfig, exists) = st.destChainConfigs.get(update.destChainSelector);
        // NOTE: this only allows pushing gas updates to chains with a destChainConfig
        if (exists) {
            var destChainConfig = maybeDestChainConfig!;
            destChainConfig.usdPerUnitGas = GasPrice {
                executionGasPrice: update.executionGasPrice,
                dataAvailabilityGasPrice: update.dataAvailabilityGasPrice,
                timestamp
            }.toCell();
            st.destChainConfigs.replace(update.destChainSelector, destChainConfig);

          //TODO: emit UsdPerUnitGasUpdated(update.destChainSelector, update.usdPerUnitGas, block.timestamp);
        }
    }
}

// apply_premium_multiplier_wei_per_eth_updates??

fun DestChainConfig.getValidatedGasPrice(self): (uint112, uint112) {
    val price = self.usdPerUnitGas.load();
    val threshold = self.config.gasPriceStalenessThreshold as int64;
    val elapsedTime = blockchain.now() - price.timestamp;

    assert(threshold == 0 || threshold > elapsedTime, ERROR_STALE_GAS_PRICE);

    return (price.executionGasPrice, price.dataAvailabilityGasPrice)
}

get fun validatedFee(msg: CCIPSend): uint256 {
    var st = lazy Storage.load();

    val destChainConfig = st.destChainConfigs.mustGet(msg.destChainSelector, ERROR_DEST_CHAIN_NOT_ENABLED);
    assert(destChainConfig.config.isEnabled, ERROR_DEST_CHAIN_NOT_ENABLED);
    // validate the fee token is actually supported
    val premiumMultiplier = st.premiumMultiplierWeiPerEth.mustGet(msg.feeToken, ERROR_FEE_TOKEN_NOT_SUPPORTED) as uint256;
    val feeTokenPrice = st.usdPerToken.mustGet(msg.feeToken, ERROR_FEE_TOKEN_NOT_SUPPORTED);

    var gasLimit = 0;
    // NOTE: we could deploy distinct contracts to cut down on code
    // match (cfg.chainFamilySelector) {
    //     CHAIN_FAMILY_SELECTOR_EVM | CHAIN_FAMILY_SELECTOR_APTOS | CHAIN_FAMILY_SELECTOR_SUI => { } // resolveGenericGasLimit()
    //     CHAIN_FAMILY_SELECTOR_SVM => { } // resolveSVMGasLimit()
    // }
    // validateDestFamilyAddress(cfg.chainFamilySelector, receiver, gasLimit);

    // TODO: this can throw and bounce the message
    val (executionGasPrice, dataAvailabilityGasPrice) = destChainConfig.getValidatedGasPrice();

    // Network fee

    var tokensIter = Iterator<TokenAmount>.new(msg.tokenAmounts);
    var (tokenCount, premiumFeeUsdWei, tokenTransferGas, tokenTransferBytesOverhead) = (0, destChainConfig.config.networkFeeUsdCents as uint256, 0, 0);
    if (!tokensIter.empty()) {
        (tokenCount, premiumFeeUsdWei, tokenTransferGas, tokenTransferBytesOverhead) = getTokenTransferCost(st, destChainConfig, mutate tokensIter);
    };

    // Execution cost

    // Calculate calldata gas cost while accounting for EIP-7623 variable calldata gas pricing
    // This logic works for EVMs post Pectra upgrade, while being backwards compatible with pre-Pectra EVMs.
    // This calculation is not exact, the goal is to not lose money on large payloads.
    // The fixed OCR report calldata overhead gas is accounted for in `dest_gas_overhead`.
    // It is not included in the calculation below for simplicity.
    val (_cellsCount, dataBitsSize, _cellRefs, traversed) = msg.data.calculateSize(99); // TODO: use Iterator.lenBytes
    assert(traversed, ERROR_INVALID_MSG_DATA);
    assert(dataBitsSize % 8 == 0, ERROR_INVALID_MSG_DATA);
    val calldataLen = dataBitsSize / 8 as uint256 + tokenTransferBytesOverhead;
    var calldataGas = calldataLen * destChainConfig.config.destGasPerPayloadByteBase as uint256;
    val calldataThreshold = destChainConfig.config.destGasPerPayloadByteThreshold as uint256;
    if (calldataLen > calldataThreshold) {
        val baseCalldataGas = destChainConfig.config.destGasPerPayloadByteBase as uint256 * calldataThreshold;
        val extraBytes = calldataLen - calldataThreshold;
        val extraCalldataGas = extraBytes * destChainConfig.config.destGasPerPayloadByteHigh as uint256;
        calldataGas = baseCalldataGas + extraCalldataGas;
    }
    val executionGas = gasLimit + destChainConfig.config.destGasOverhead as uint256 + calldataGas + tokenTransferGas;

    val executionCost = executionGasPrice
        * executionGas
        * destChainConfig.config.gasMultiplierWeiPerEth as uint256;

    // Data availability cost

    // Sums up byte lengths of fixed message fields and dynamic message fields.
    // Fixed message fields do account for the offset and length slot of the dynamic fields.
    val dataAvailabilityLengthBytes = TON_2_EVM_MESSAGE_FIXED_BYTES
        + calldataLen
        + tokenCount as uint256 * TON_2_EVM_MESSAGE_FIXED_BYTES_PER_TOKEN
        + tokenTransferBytesOverhead;

    // dest_data_availability_overhead_gas is a separate config value for flexibility to be updated
    // independently of message codestChainConfig. Its value is determined by CCIP lane implementation, e.g.
    // the overhead data posted for OCR.
    val dataAvailabilityGas = dataAvailabilityLengthBytes
        * destChainConfig.config.destGasPerDataAvailabilityByte as uint256
        + destChainConfig.config.destDataAvailabilityOverheadGas as uint256;

    // data_availability_gas_price is in 18 decimals, dest_data_availability_multiplier_bps is in 4 decimals
    // We pad 14 decimals to bring the result to 36 decimals, in line with token bps and execution fee.
    val dataAvailabilityCost = dataAvailabilityGasPrice
        * dataAvailabilityGas
        * destChainConfig.config.destDataAvailabilityMultiplierBps as uint256
        * VAL_1E14;

    // At this step, every fee component has been raised to 36 decimals
    val feeTokenValue =
        (premiumFeeUsdWei * premiumMultiplier) + executionCost + dataAvailabilityCost;

    // Fee token value is in 36 decimals
    // Fee token price is in 18 decimals USD for 1e18 smallest token denominations.
    // The result is the fee in the fee tokens smallest denominations (e.g. lamport for Sol).
    val feeTokenAmount = (feeTokenValue / feeTokenPrice.value);

    return feeTokenAmount;
}

fun getValidatedFee(payload: GetValidatedFee<cell>, sender: address) {
    val msg = payload.msg.load();
    val feeTokenAmount = validatedFee(msg);

    val getValidatedFeeMsg = createMessage({
        bounce: true,
        value: ton("0.05"), // TODO:
        dest: sender,
        body: MessageValidated<cell> {
            msg: payload.msg,
            metadata: payload.metadata,
            fee: feeTokenAmount
        }
    });
    getValidatedFeeMsg.send(SEND_MODE_REGULAR);
}

// TODO: a lot of the as uint256 annotations seem to be unnecessary in this function
fun getTokenTransferCost(st: Storage, destChainConfig: DestChainConfig, mutate tokens: Iterator<TokenAmount>): (int, uint256, uint32, uint32) {
    var tokenTransferFeeWei: uint256 = 0;
    var tokenTransferGas: uint32 = 0;
    var tokenTransferBytesOverhead: uint32 = 0;

    var count = 0;

    while (!tokens.empty()) {
        val tokenAmount = tokens.next();
        count += 1;

        val transferFeeConfig = destChainConfig.tokenTransferFeeConfigs.mustGet(tokenAmount.token, ERROR_TOKEN_NOT_SUPPORTED);
        if (!transferFeeConfig.isEnabled) {
            // If the token has no specific overrides configured, we use the global defaults.
            tokenTransferFeeWei +=((destChainConfig.config.defaultTokenFeeUsdCents as uint256) * VAL_1E16);
            tokenTransferGas += destChainConfig.config.defaultTokenDestGasOverhead;
            tokenTransferBytesOverhead += CCIP_LOCK_OR_BURN_V1_RET_BYTES;
        } else {
            var bpsFeeUsdWei = 0;
            if (transferFeeConfig.deciBps > 0) {
                val tokenPrice = st.usdPerToken.mustGet(tokenAmount.token, ERROR_TOKEN_NOT_SUPPORTED);

                // Calculate token transfer value, then apply fee ratio
                // ratio represents multiples of 0.1bps, or 1e-5
                val tokenUsdValue = tokenAmount.amount as uint256 * tokenPrice.value / VAL_1E18;
                bpsFeeUsdWei = (tokenUsdValue * (transferFeeConfig.deciBps as uint256)) / VAL_1E5;
            };

            tokenTransferGas += transferFeeConfig.destGasOverhead;
            tokenTransferBytesOverhead += transferFeeConfig.destBytesOverhead;

            var minFeeUsdWei = (transferFeeConfig.minFeeUsdCents as uint256) * VAL_1E16;
            var maxFeeUsdWei = (transferFeeConfig.maxFeeUsdCents as uint256) * VAL_1E16;
            tokenTransferFeeWei += min(maxFeeUsdWei, max(minFeeUsdWei, bpsFeeUsdWei));
        }
    }
    return (count, tokenTransferFeeWei, tokenTransferGas, tokenTransferBytesOverhead);
}

get fun tokenPrice(token: address): TimestampedPrice {
    val st = lazy Storage.load();
    return st.usdPerToken.mustGet(token, ERROR_TOKEN_NOT_SUPPORTED);
}

// vec<TimestampedPrice>
get fun tokenPrices(tokens: tuple): tuple {
    val st = lazy Storage.load();
    
    var list: tuple? = null;

    // TODO: are we able to get dynamic tuple inputs in getters?
    var iter = TupleIterator<address>.new(tokens);

    while(!iter.empty()) {
        val token = iter.next();
        list = listPrepend(token, list);
    }
    return list!;
}

get fun destinationChainGasPrice(destChainSelector: uint64) {
    
}

get fun tokenAndGasPrices(token: address, destChainSelector: uint64) {
    
}

// vec<address>
get fun feeTokens(): tuple {
    val st = lazy Storage.load();
    var d = st.premiumMultiplierWeiPerEth.value;
    
    var list: tuple? = null;
    do {
        var (key: slice?, _value: slice?, found: bool) = d.sDictDeleteFirstAndGet(ADDR_KEY_LEN);
        if (found) {
            var address = key! as address;
            list = listPrepend(address, list);
        }
    } while (found);
    return list!;
}

get fun tokenTransferFeeConfig() {}

// TODO: processMessageArgs(), processPoolReturnData()?

get fun destChainConfig(destChainSelector: uint64): DestChainConfig {
    val st = lazy Storage.load();
    return st.destChainConfigs.mustGet(destChainSelector, ERROR_DEST_CHAIN_NOT_ENABLED);
}

get fun staticConfig(): (uint96, address, uint64) {
    val st = lazy Storage.load();
    return (st.maxFeeJuelsPerMsg, st.linkToken, st.tokenPriceStalenessThreshold);
}

get fun typeAndVersion(): (slice, slice) {
     return ("com.chainlink.ton.ccip.FeeQuoter", "1.0.0");
 }
