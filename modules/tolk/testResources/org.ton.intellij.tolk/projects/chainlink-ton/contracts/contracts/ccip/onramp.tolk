import "@stdlib/lisp-lists";
import "types.tolk";
import "../lib/access/ownable_2step.tolk";
import "../lib/upgrades/type_and_version.tolk";
import "../lib/utils.tolk";
import "@stdlib/tvm-dicts";

const CCIP_MESSAGE_SENT_TOPIC: int = stringCrc32("CCIPMessageSent"); 

struct CCIPMessageSent {
    message: TVM2AnyRampMessage;
}

struct Storage {
    ownable: Ownable2Step;

    // static config
    chainSelector: uint64; // source chain selector
    // TODO: rmnRemote (only used to validate curse on getFee), nonceManager, tokenAdminRegistry
    // hmm, need to consider the design here.
    // probably like solana tokenAdminRegistry will be fused into onramp/router
    // but the problem is unbounded dict growth
    // it's only used to resolve getPoolBySourceToken when calling lockOrBurn
    // maybe onramp -> feeQuoter -> tokenAdminRegistry(token instance) -> tokenPool.LockAndBurn?
    // NOTE: fee quoter also processes pool return data

    // dynamic config
    config: Cell<OnRampDynamicConfig>;

    destChainConfigs: UMap<uint64, DestChainConfig>; // chainSelector -> DestChainConfig
}

struct DestChainConfig {
    router: address; // Local router address that is allowed to send messages to the destination chain.
    sequenceNumber: uint64;
    allowlistEnabled: bool;
    allowedSenders: Map<bool>; // address -> bool
}
// TODO: move allowlist into accounts
// user calls wallet -> account contract -> router, account verifies dest chain and if
// the user is allowed to send. This will also allow users to deposit funds ahead of time
// unless we intend to keep the allow list small?
// nonceManager: also seems to fit best in a sender subcontract with a dict destChainSelector -> nonce
// if we pass through account contract then we can grab a nonce and pass it through?

fun Storage.load(): Storage {
    return Storage.fromCell(contract.getData());
}

fun Storage.store(self) {
    return contract.setData(self.toCell());
}

type Msg = OnRampSend | MessageValidated<Metadata> | SetDynamicConfig | OnRampUpdateDestChainConfigs | UpdateAllowlists;

fun onInternalMessage(in: InMessage) {
    val msg = lazy Msg.fromSlice(in.body);
    match (msg) {
        OnRampSend => { send(msg, in.senderAddress) }
        MessageValidated<Metadata> => { onMessageValidated(msg, in.senderAddress) }
        SetDynamicConfig => {
            var st = lazy Storage.load();
            st.ownable.requireOwner(in.senderAddress);
            st.config = msg.config.toCell();
            st.store();
        }
        OnRampUpdateDestChainConfigs => {
            var st = lazy Storage.load();
            st.ownable.requireOwner(in.senderAddress);
            applyDestChainConfigUpdates(mutate st, msg.updates);
            st.store();
        }
        UpdateAllowlists => {
            var st = lazy Storage.load();
            st.ownable.requireOwner(in.senderAddress);
            applyAllowlistUpdates(mutate st, msg.updates);
            st.store();
        }
        else => {
            // ignore empty messages, "wrong opcode" for others
            assert (in.body.isEmpty()) throw 0xFFFF
        }
    }
}

fun send(payload: OnRampSend, sender: address) {
    val st = lazy Storage.load();

    val msg = payload.msg.load();

    // validate allowlist
    val destChainConfig = st.destChainConfigs.mustGet(msg.destChainSelector, ERROR_UNKNOWN_DEST_CHAIN_SELECTOR);

    // ccipSend must be forwarded from the router
    assert(destChainConfig.router == sender, ERROR_UNAUTHORIZED);

    if (destChainConfig.allowlistEnabled) {
        val (allowed, exists) = destChainConfig.allowedSenders.get(payload.metadata.sender);
        assert(exists && allowed!, ERROR_SENDER_NOT_ALLOWED);
    }

    // TODO: validate jetton amounts match msg.tokenAmounts

    // call into feeQuoter to validate and calculate fees
    val getValidatedFeeMsg = createMessage({
        bounce: true,
        value: ton("0.05"), // TODO:
        dest: st.config.load().feeQuoter,
        body: GetValidatedFee<Metadata> {
            msg: payload.msg,
            metadata: payload.metadata.toCell(), // TODO: must include nonce 
        }
    });
    getValidatedFeeMsg.send(SEND_MODE_REGULAR);
}

// TODO: do we follow solana model and merge router + onramp? aptos also does this
// does onramp even do anything apart from forwarding to fee quoter?
fun onMessageValidated(payload: MessageValidated<Metadata>, sender: address) {
    var st = lazy Storage.load();
    val config = st.config.load();
    // notification must come from fee quoter
    assert(config.feeQuoter == sender, ERROR_UNAUTHORIZED);

    val msg: CCIPSend = payload.msg.load();
    val metadata = payload.metadata.load();

    // validate user transferred enough value
    // else refund and notify

    // assign sequence number, we can't do this sooner since the message might fail validation/getFee
    var destChainConfig = st.destChainConfigs.mustGet(msg.destChainSelector, ERROR_UNKNOWN_DEST_CHAIN_SELECTOR);
    val seqNr = destChainConfig.sequenceNumber;
    destChainConfig.sequenceNumber = destChainConfig.sequenceNumber + 1;
    st.destChainConfigs.set(msg.destChainSelector, destChainConfig);
    st.store();

    var rampMessage = TVM2AnyRampMessage {
        header: RampMessageHeader {
            messageId: 0, // populated after hashing
            sourceChainSelector: st.chainSelector,
            destChainSelector: msg.destChainSelector,
            sequenceNumber: seqNr,
            // Only bump nonce for messages that specify allowOutOfOrderExecution == false. Otherwise, we may block ordered
            // message nonces, which is not what we want.
            nonce: 0,
        },
        sender: metadata.sender,
        body: TVM2AnyRampMessageBody {
            receiver: msg.receiver.toCell(),
            data: msg.data,
            extraArgs: msg.extraArgs,
            tokenAmounts: createEmptyCell(), // TODO:
            feeToken: msg.feeToken,
            feeTokenAmount: payload.fee,
        }.toCell(),
        feeValueJuels: 0, // TODO:
    };

    // TODO: support for ordered execution
    // rampMessage.header.nonce = isOutOfOrderExecution
    //   ? 0
    //   : nonceManager.getIncrementedOutboundNonce(destChainSelector, metadata.sender);

    // Metadata hash preimage to ensure global uniqueness, ensuring 2 identical messages sent to 2 different lanes
    // will have a distinct hash.
    val metadataHash = beginCell()
        .storeUint(stringSha256("TVM2AnyMessageHashV1"), 256)
        .storeUint(st.chainSelector, 64)
        .storeUint(msg.destChainSelector, 64)
        .storeAddress(contract.getAddress())
        .endCell()
        .hash();

    rampMessage.header.messageId = rampMessage.generateMessageId(metadataHash);
    
    emit(CCIP_MESSAGE_SENT_TOPIC, CCIPMessageSent { message: rampMessage });

    // TODO: notify user
}

get fun isChainSupported(destChainSelector: uint64): bool {
    val st = lazy Storage.load();
    val (_, exists) = st.destChainConfigs.get(destChainSelector);
    return exists;
}

get fun expectedNextSequenceNumber(destChainSelector: uint64): uint64 {
    val st = lazy Storage.load();
    val config = st.destChainConfigs.mustGet(destChainSelector,ERROR_UNKNOWN_DEST_CHAIN_SELECTOR);
    return config.sequenceNumber + 1;
}

// TODO: getFee forwarding? users should just call the fee quoter

get fun staticConfig() {
    // chainSelector, rmnRemote, nonceManager, tokenAdminRegistry
}

get fun dynamicConfig(): OnRampDynamicConfig {
    val st = lazy Storage.load();
    return st.config.load()
}

fun applyDestChainConfigUpdates(mutate st: Storage, updates: cell) {
    var iter = Iterator<OnRampUpdateDestChainConfig>.new(updates);

    while (!iter.empty()) {
        val update = iter.next();
        // create or update entries
        val (maybeConfig, exists) = st.destChainConfigs.get(update.destChainSelector);
        if (exists) {
            var config = maybeConfig!;
            config.router = update.router;
            config.allowlistEnabled = update.allowlistEnabled;
            st.destChainConfigs.replace(update.destChainSelector, config);
        } else {
            st.destChainConfigs.set(update.destChainSelector, DestChainConfig {
                router: update.router,
                sequenceNumber: 0,
                allowlistEnabled: update.allowlistEnabled,
                allowedSenders: Map<bool>.new(),
            })
        }
    }
}

get fun destChainConfig(destChainSelector: uint64): DestChainConfig {
    val st = lazy Storage.load();
    return st.destChainConfigs.mustGet(destChainSelector,ERROR_UNKNOWN_DEST_CHAIN_SELECTOR);
}

// vec<address>
get fun allowedSendersList(destChainSelector: uint64): tuple {
    val st = lazy Storage.load();
    val config = st.destChainConfigs.mustGet(destChainSelector,ERROR_UNKNOWN_DEST_CHAIN_SELECTOR);
    var d = config.allowedSenders.value;

    var list: tuple? = null;
    do {
        var (key: slice?, _value: slice?, found: bool) = d.sDictDeleteFirstAndGet(ADDR_KEY_LEN);
        if (found) {
            // var (wc, addressHash) = (key! as address).getWorkchainAndHash();
            // list = listPrepend([wc, addressHash], list);
            var address = key! as address;
            list = listPrepend(address, list);
        }
    } while (found);

    return list!;
}

fun applyAllowlistUpdates(mutate st: Storage, updates: cell) {
    var iter = Iterator<UpdateAllowlist>.new(updates);

    while (!iter.empty()) {
        val update = iter.next();

        var config = st.destChainConfigs.mustGet(update.destChainSelector, ERROR_UNKNOWN_DEST_CHAIN_SELECTOR);

        var iterAdd = Iterator<address>.new(update.add);
        while (!iterAdd.empty()) {
            val sender = iterAdd.next();
            config.allowedSenders.set(sender, true);
        }

        var iterRemove = Iterator<address>.new(update.remove);
        while (!iterRemove.empty()) {
            val sender = iterRemove.next();
            config.allowedSenders.delete(sender);
        }

        st.destChainConfigs.set(update.destChainSelector, config);
    }
}

// outboundNonce() TODO: nonceManager?
// calculateMetadataHash/calculateMessageHash

get fun supportedTokens() {
    // TODO?
}


fun withdrawFeeTokens() {
    
}

get fun typeAndVersion(): (slice, slice) {
     return ("com.chainlink.ton.ccip.OnRamp", "1.0.0");
 }
