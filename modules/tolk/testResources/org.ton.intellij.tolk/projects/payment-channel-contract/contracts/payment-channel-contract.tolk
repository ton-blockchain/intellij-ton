tolk 1.0

import "@stdlib/common"
import "@stdlib/gas-payments"
import "@stdlib/tvm-dicts"
import "@stdlib/tvm-lowlevel"
import "storage"
import "utils"
import "fees"
import "errors"
import "messages"

fun Channel.topUpBalance(mutate self, amount: int, isA: bool) {
    assert(self.inited) throw ERROR_NOT_INITED;
    // we allow topup only when there is no disput
    assert(self.quarantine == null) throw ERROR_QUARANTINE_ALREADY_ACTIVE;

    var balanceUnpacked = lazy self.balance.load();

    if (isA) {
        balanceUnpacked.depositA += amount;
    } else {
        balanceUnpacked.depositB += amount;
    }
    self.balance = balanceUnpacked.toCell();

    contract.setData(self.serialize());
}

fun initChannel(channel: Channel, msg: InitChannelMessage) {
    assert(!channel.inited) throw ERROR_ALREADY_INITED;

    assert(isSignatureValid(msg.signedBody.hash(), msg.signature as slice, msg.isA ? channel.keyA : channel.keyB)) throw ERROR_NOT_AUTHORIZED;
    msg.signedBody.verifyMessageType(TAG_INIT_CHANNEL, channel.id);

    var balanceUnpacked = lazy channel.balance.load();
    var paymentConfigUnpacked = lazy channel.paymentConfig.load();

    // must be 0, we handle adjustments in topup and withdraw
    assert((balanceUnpacked.sentA | balanceUnpacked.sentB |
            balanceUnpacked.depositA | balanceUnpacked.depositB |
            balanceUnpacked.withdrawA | balanceUnpacked.withdrawB) == 0) throw ERROR_INCORRECT_INITIAL_BALANCE;

    var storageMin: int = FEE_MIN_BALANCE;
    if(paymentConfigUnpacked.customCurrency != null) {
        if (paymentConfigUnpacked.customCurrency is PaymentConfigJettonStorage) {
            // must keep fee for final withdrawal
            var jettonConfig = paymentConfigUnpacked.customCurrency.jettonCell.load();
            storageMin += (FEE_JETTON_PAYOUT * 2) + FEE_JETTON_WALLET_PROVIDE;
            if (jettonConfig.jettonWallet.isNone()) {
                // request our wallet address from jetton master
                requestJettonWallet(jettonConfig.jettonRoot);
            }
        } else {
            storageMin += FEE_EC_PAYOUT * 2;
        }
    }

    assert((paymentConfigUnpacked.storageFee > storageMin) &&
        (contract.getOriginalBalance() >= paymentConfigUnpacked.storageFee)) throw ERROR_NOT_ENOUGH_MONEY_FOR_INIT_STORAGE;

    channel.inited = true;
    return contract.setData(channel.serialize());
}

@inline_ref
fun sendPayout(config: PaymentConfig, sAddr: address, amount: int, payload: builder, flags: int) {

    var msg: OutMessage?;
    var fee = 0;

    match(config.customCurrency) {
        null =>  {
            msg = createMessage({
                bounce: false,
                dest: sAddr,
                value: amount,
                body: UnsafeBodyNoRef{
                    forceInline: payload
                }
            })
        }
        PaymentConfigJettonStorage => {
                var jettonConfig = lazy config.customCurrency.jettonCell.load();
                msg = createPayoutJetton(jettonConfig.jettonWallet, sAddr, amount, payload, flags);
                fee = FEE_JETTON_PAYOUT;
        }
        PaymentConfigExtra => {
                msg = createPayoutEC(config.customCurrency.ecId, sAddr, amount, payload, flags);
                fee = FEE_EC_PAYOUT;
        }
    };

    msg.send(flags);
    return fee;
}

@inline
fun createPayoutEC(ecId: int, sAddr: address, amount: int, payoutBody: builder, flags: int) {
    var currenciesToSend: dict = createEmptyDict();
    if(amount > 0) {
        currenciesToSend.uDictSetBuilder(32, ecId, beginCell().storeVarUInt32(amount));
    }
    return createMessage({
        bounce: false,
        dest: sAddr,
        value: (FEE_EC_PAYOUT, currenciesToSend),
        body: UnsafeBodyNoRef {
            forceInline: payoutBody
        }
    });

}

@inline
fun createPayoutJetton(wallet: address, sAddr: address, amount: int, payload: builder, flags: int) {
    /*
      transfer query_id:uint64 amount:(VarUInteger 16) destination:MsgAddress
               response_destination:MsgAddress custom_payload:(Maybe ^Cell)
               forward_ton_amount:(VarUInteger 16) forward_payload:(Either Cell ^Cell)
               = InternalMsgBody; 32 + 64 + 124 + 267 + 267 + 1 + 5  + 1
    */
    return createMessage({
        bounce: false,
        dest: wallet,
        value: FEE_JETTON_PAYOUT,
        body: AskToTransfer {
                queryId: 0,
                jettonAmount: amount,
                customPayload: null,
                transferRecipient: sAddr,
                sendExcessesTo: sAddr,
                forwardTonAmount: FEE_JETTON_FORWARD,
                forwardPayload: payload
        }
    });
}

fun Channel.closeChannel(mutate self, unpackedBalance: Balance) {
    var closeMsg = beginCell().storeAny(ChannelClosedNotification {
        channelId: self.id
    });

    var unpackedConfig  = lazy self.paymentConfig.load();
    sendPayout(unpackedConfig, unpackedConfig.addressB, unpackedBalance.calcB(), closeMsg, SEND_MODE_IGNORE_ERRORS);
    // side A is considered to be deployer, so he will get rest contract's ton balance
    sendPayout(unpackedConfig, unpackedConfig.addressA, unpackedBalance.calcA(), closeMsg, SEND_MODE_CARRY_ALL_BALANCE | SEND_MODE_IGNORE_ERRORS);

    // while self contract will be emptied it will survive for some time
    // by clearing storage we allow immediately reopen self with the same configs
    self.inited = false;
    self.balance = Balance {
        depositA: 0,
        depositB: 0,
        withdrawA: 0,
        withdrawB: 0,
        sentA: 0,
        sentB: 0,
    }.toCell();
    self.quarantine = null;

    return contract.setData(self.serialize());
}

@inline
fun Channel.cooperativeClose(mutate self, msg: CooperativeCloseMessage) {
    var hash: int = msg.signedBody.hash();

    msg.signedBody.verifyMessageType(TAG_COOPERATIVE_CLOSE, self.id);

    assert(isSignatureValid(hash, msg.signatureA.beginParse(), self.keyA) &&
        isSignatureValid(hash, msg.signatureB.beginParse(), self.keyB)) throw ERROR_NOT_AUTHORIZED;

    var closeBody = msg.signedBody.loadAny<CooperativeCloseBody>();

    var unpackedBalance = lazy self.balance.load();

    unpackedBalance.sentA = closeBody.sentA;
    unpackedBalance.sentB = closeBody.sentB;

    var newSeqnoA: int = closeBody.seqnoA;
    var newSeqnoB: int = closeBody.seqnoB;
    assert((self.commitedSeqnoA < newSeqnoA) &&
        (self.commitedSeqnoB < newSeqnoB)) throw ERROR_SEQNO_REGRESS;

    self.commitedSeqnoA = newSeqnoA;
    self.commitedSeqnoB = newSeqnoB;

    self.closeChannel(unpackedBalance);
}

@inline
fun Channel.cooperativeCommit(mutate self, msg: CooperativeCommitMessage, msgValue: int) {
    var hash: int = msg.signedBody.hash();

    msg.signedBody.verifyMessageType(TAG_COOPERATIVE_COMMIT, self.id);

    assert(isSignatureValid(hash, msg.signatureA.beginParse(), self.keyA) &&
        isSignatureValid(hash, msg.signatureB.beginParse(), self.keyB)) throw ERROR_NOT_AUTHORIZED;

    var commitBody = msg.signedBody.loadAny<CooperativeCommitBody>();

    var unpackedBalance = lazy self.balance.load();
    unpackedBalance.sentA = commitBody.sentA;
    unpackedBalance.sentB = commitBody.sentB;

    assert((self.commitedSeqnoA < commitBody.seqnoA) &
        (self.commitedSeqnoB < commitBody.seqnoB)) throw ERROR_SEQNO_REGRESS;

    self.commitedSeqnoA = commitBody.seqnoA;
    self.commitedSeqnoB = commitBody.seqnoB;

    if (self.quarantine != null) {
        var qua = lazy self.quarantine.load();
        if ((commitBody.seqnoA > qua.stateA.seqno) || (commitBody.seqnoB > qua.stateB.seqno)) {
            // quarantinened state is older than newly commited one, drop it
            self.quarantine = null;
        }
    }

    var withdrawAmount = commitBody.withdrawB - unpackedBalance.withdrawB;
    assert(withdrawAmount >= 0) throw ERROR_WITHDRAW_REGRESS;

    var unpackedPaymentConfig = lazy self.paymentConfig.load();
    var withdrawBody: builder? = null;
    if (withdrawAmount > 0) {
        unpackedBalance.withdrawB = commitBody.withdrawB;
        withdrawBody = beginCell().storeAny(WithdrawNotification {
            channelId: self.id
        });
        msgValue -= sendPayout(unpackedPaymentConfig, unpackedPaymentConfig.addressB, withdrawAmount, withdrawBody, 0);
    }

    withdrawAmount = commitBody.withdrawA - unpackedBalance.withdrawA;
    assert(withdrawAmount >= 0) throw ERROR_WITHDRAW_REGRESS;
    if (withdrawAmount > 0) {
        unpackedBalance.withdrawA = commitBody.withdrawA;
        if(withdrawBody == null) {
            withdrawBody = beginCell().storeAny(WithdrawNotification {
                channelId: self.id
            });
        }
        msgValue -= sendPayout(unpackedPaymentConfig,unpackedPaymentConfig.addressA, withdrawAmount, withdrawBody, 0);
    }

    assert(msgValue >= 0) throw ERROR_AMOUNT_NOT_COVERS_FEE;
    assert(unpackedBalance.calcA() >= 0 && unpackedBalance.calcB() >= 0) throw ERROR_NOT_ENOUGH_BALANCE;

    self.balance = unpackedBalance.toCell();

    contract.setData(self.serialize());
}

@inline
fun startUncooperativeClose(channel: Channel, msg: StartUncooperativeCloseMessage) {
    assert(channel.quarantine == null) throw ERROR_QUARANTINE_ALREADY_ACTIVE;
    assert(isSignatureValid(msg.signedBody.hash(), msg.signature as slice, msg.signedByA ? channel.keyA : channel.keyB)) throw ERROR_NOT_AUTHORIZED;

    msg.signedBody.verifyMessageType(TAG_START_UNCOOPERATIVE_CLOSE, channel.id);
    var commitBody = msg.signedBody.loadAny<StartUncooperativeCloseBody>();

    var a = Semichannel.parse(commitBody.channelA, channel.id, channel.keyA);
    var b = Semichannel.parse(commitBody.channelB, channel.id, channel.keyB);

    verifyStates(channel.commitedSeqnoA, channel.commitedSeqnoB,a, b);

    var qua = Quarantine{
        stateA: a.data,
        stateB: b.data,
        startedAt: blockchain.now(),
        committedbyA: msg.signedByA,
        challenged: false,
    };

    channel.quarantine = qua.serialize();
    return contract.setData(channel.serialize());
}

@inline
fun challengeQuarantinedState(channel: Channel, msg: ChallengeQuarantineMessage) {
    assert(channel.quarantine != null) throw ERROR_NO_QUARANTINED_STATE;

    // Start with the same checks
    assert(isSignatureValid(msg.signedBody.hash(), msg.signature as slice, msg.challengedByA ? channel.keyA : channel.keyB)) throw ERROR_NOT_AUTHORIZED;

    msg.signedBody.verifyMessageType(TAG_CHALLENGE_QUARANTINEED_STATE, channel.id);

    var challengeBody = msg.signedBody.loadAny<ChallengeQuarantineBody>();
    var a = Semichannel.parse(challengeBody.stateA, channel.id, channel.keyA);
    var b = Semichannel.parse(challengeBody.stateB, channel.id, channel.keyB);

    verifyStates(channel.commitedSeqnoA, channel.commitedSeqnoB, a, b);

    var qua = lazy channel.quarantine.load();
    var closure = lazy channel.closureConfig.load();

    assert(qua.startedAt + closure.quarantineDuration > blockchain.now()) throw ERROR_TOO_LATE_FOR_QUARANTINE_CHALLENGE;
    assert(!qua.challenged) throw ERROR_QUARANTINEE_ALREADY_CHALLENGED;

    // misbehvaior is when party which committed outdated state
    // or regenerate latest state with lower sent
    var misbehaviorDetected: bool = false;
    if (qua.committedbyA) {
        misbehaviorDetected = (a.data.seqno > qua.stateA.seqno) || (a.data.sent > qua.stateA.sent);
        qua.stateA = a.data;
    } else {
        misbehaviorDetected = (b.data.seqno > qua.stateB.seqno) || (b.data.sent > qua.stateB.sent);
        qua.stateB = b.data;
    }
    // only counterparty can report misbehavior
    assert(qua.committedbyA != msg.challengedByA) throw ERROR_UNAUTHORIZED_CHALLENGE;

    if (misbehaviorDetected) {
        if (qua.committedbyA) {
            qua.stateA.sent += closure.fine;
        } else {
            qua.stateB.sent += closure.fine;
        }
    }

    qua.challenged = true;
    qua.committedbyA = msg.challengedByA;
    channel.quarantine = qua.serialize();

    return contract.setData(channel.serialize());
}

@inline
fun settleConditionals(channel: Channel, msg: SettleConditionalsMessage) {
    assert(channel.quarantine != null) throw ERROR_NO_QUARANTINED_STATE;

    assert(isSignatureValid(msg.signedBody.hash(), msg.signature as slice, msg.fromA ? channel.keyA : channel.keyB)) throw ERROR_NOT_AUTHORIZED;

    msg.signedBody.verifyMessageType(TAG_SETTLE_CONDITIONALS, channel.id);

    var conditionals = msg.signedBody.loadAny<SettleConditionalsBody>();

    var qua = lazy channel.quarantine.load();
    var closure = lazy channel.closureConfig.load();

    var quarantineeFinished: int = qua.startedAt + closure.quarantineDuration;
    assert(quarantineeFinished < blockchain.now()) throw ERROR_QUARANTINE_NOT_FINISHED;
    assert(quarantineeFinished + closure.closeDuration > blockchain.now()) throw ERROR_TOO_LATE_TO_SETTLE_CONDITIONALS;

    var targetState = msg.fromA ? qua.stateB : qua.stateA;

    var (csProof: slice, special: int) = conditionals.proof.beginParseSpecial();
    assert(special) throw ERROR_INCORRECT_CONDITIONALS_PROOF;
    assert(csProof.loadUint(8) == 3) throw ERROR_INCORRECT_CONDITIONALS_PROOF;
    assert(csProof.preloadUint(256) == targetState.conditionalsHash) throw ERROR_INCORRECT_CONDITIONALS_PROOF;

    var target: cell = csProof.preloadRef();
    var more: bool = true;
    do {
        var (cid: int?, input: slice?, more redef) = conditionals.toSettle.uDictDeleteFirstAndGet(32);
        if (more) {
            var empty: slice = createEmptySlice();
            var (target redef, condition: slice, ok: int) = target.isUdictReplaceGet(32, cid!, empty);
            assert(ok) throw ERROR_INCORRECT_CONDITIONALS_PROOF;
            var executableCondition = castToS2I(transformSliceToContinuation(condition));
            // NOTE! It seems dangerous to run unknown code and it really is!
            // However, runned code is signed by one party and executed by another
            // That way both party authorized it.
            // Obviously, B should not sign incorrect code which (for instance) sends all money to A
            // The same way A should not run incorrect code which sends all money to B.
            targetState.sent += executableCondition(input!);
        }
    } while (more);

    targetState.conditionalsHash = proofHash(target);
    if (msg.fromA) {
        qua.stateB = targetState;
    } else {
        qua.stateA = targetState;
    }

    channel.quarantine = qua.serialize();
    return contract.setData(channel.serialize());
}

@inline
fun Channel.finishUncooperativeClose(mutate self) {
    assert(self.quarantine != null) throw ERROR_NO_QUARANTINED_STATE;

    var qua = lazy self.quarantine.load();
    var closure = lazy self.closureConfig.load();

    assert(qua.startedAt +
        closure.quarantineDuration +
        closure.closeDuration < blockchain.now()) throw ERROR_TOO_EARLY_TO_CLOSE;

    var unpackedBalance = lazy self.balance.load();

    unpackedBalance.sentB = qua.stateB.sent;
    unpackedBalance.sentA = qua.stateA.sent;

    var balanceB = unpackedBalance.calcB();
    var balanceA = unpackedBalance.calcA();

    if (balanceB < 0) {
        unpackedBalance.sentA -= balanceB;
    }
    if (balanceA < 0) {
        unpackedBalance.sentB -= balanceA;
    }

    self.commitedSeqnoA = qua.stateA.seqno + 1;
    self.commitedSeqnoB = qua.stateB.seqno + 1;
    self.closeChannel(unpackedBalance);
}

type AllowedMessages =
    | TopUpMessage
    | JettonTransferNotification
    | TakeWalletAddressMessage
    | InitChannelMessage
    | TopUpMessage
    | CooperativeCommitMessage
    | CooperativeCloseMessage
    | StartUncooperativeCloseMessage
    | FinishUncooperativeCloseMessage
    | ChallengeQuarantineMessage
    | SettleConditionalsMessage;

fun onInternalMessage(in: InMessage) {
    var channel = Channel.parse(contract.getData());

    // Bounce is ignored unless onBouncedMessage

    // Note, operators of channels are always off-chain parties, not contracts
    // thus no responses and query_ids
    var msg = lazy AllowedMessages.fromSlice(in.body);

    match(msg) {
        TopUpMessage => {
            var unpackedPaymentConfig = lazy channel.paymentConfig.load();
            if (unpackedPaymentConfig.customCurrency is PaymentConfigExtra) {
                assert(in.valueCoins >= FEE_EC_ACCEPT) throw ERROR_AMOUNT_NOT_COVERS_FEE;

                var ec: dict = in.valueExtra;
                var (v: slice?, ok: bool) = ec.uDictDeleteAndGet(32, unpackedPaymentConfig.customCurrency.ecId);
                // If dictionary contains more than one EC - reject
                assert(ok && ec == null) throw ERROR_INVALID_EC_ID;

                var ecAmount: int = v!.loadVarUInt32();
                channel.topUpBalance(ecAmount, msg.isA);
            } else {
                assert(unpackedPaymentConfig.customCurrency == null) throw ERROR_INVALID_CURRENCY_TYPE;
                assert(in.valueCoins >= FEE_TON_ACCEPT) throw ERROR_AMOUNT_NOT_COVERS_FEE;
                channel.topUpBalance(in.valueCoins - FEE_TON_ACCEPT, msg.isA);
            }
        }
        JettonTransferNotification => {
            // return because custom excess handled inside
            return acceptJettons(channel, in.senderAddress, in.valueCoins, msg);
        }
        TakeWalletAddressMessage =>  {
            // no excess
            return assignJettonWallet(channel, in.senderAddress, msg.wallet_address);
        }
        CooperativeCommitMessage => {
            channel.cooperativeCommit(msg, in.valueCoins - FEE_COMMIT);
        }
        InitChannelMessage => {
            initChannel(channel, msg);
        }
        CooperativeCloseMessage => {
            // excess hadled in channelClose()
            return channel.cooperativeClose(msg);
        }
        StartUncooperativeCloseMessage => {
            startUncooperativeClose(channel, msg);
        }
        ChallengeQuarantineMessage => {
            challengeQuarantinedState(channel, msg);
        }
        SettleConditionalsMessage => {
            settleConditionals(channel, msg);
        }
        FinishUncooperativeCloseMessage => {
            // excess hadled in channelClose()
            return channel.finishUncooperativeClose();
        }
    }

    sendExcess(channel, in.senderAddress);
}

@inline
fun acceptJettons(channel: Channel, sender: address, tonAmount: int, msg: JettonTransferNotification) {
    /* TEP-74
      transfer_notification#7362d09c query_id:uint64 amount:(VarUInteger 16)
                                     sender:MsgAddress forward_payload:(Either Cell ^Cell)
                                     = InternalMsgBody;
    */
    var unpackedConfig = lazy channel.paymentConfig.load();
    var customCurrency = unpackedConfig.customCurrency;
    assert(customCurrency is PaymentConfigJettonStorage) throw ERROR_INVALID_CURRENCY_TYPE;
    var jettonConfig = lazy customCurrency.jettonCell.load();

    var jettonAmount: int = msg.jettonAmount;

    var jettonSender: address = msg.jettonSender;
    var eitherForwardPayload: bool = msg.forwardPayload.loadBool();
    var forwardPayload: slice = eitherForwardPayload ? msg.forwardPayload.loadRef().beginParse() : msg.forwardPayload;

    if ((tonAmount < FEE_JETTON_ACCEPT) || (jettonConfig.jettonWallet != sender)) {
        return sendJettonsBack(jettonSender, jettonAmount, sender);
    }

    try {
        var parsedPayload = lazy TopUpMessage.fromSlice(forwardPayload);
        // Will throw on invalid payload
        channel.topUpBalance(jettonAmount, parsedPayload.isA);
        return sendExcess(channel,jettonSender);
    } catch {
        sendJettonsBack(jettonSender, jettonAmount, sender);
    }
}

@inline_ref
fun sendJettonsBack(dest: address, amount: int, jettonWallet: address) {
    /*
      transfer query_id:uint64 amount:(VarUInteger 16) destination:MsgAddress
               response_destination:MsgAddress custom_payload:(Maybe ^Cell)
               forward_ton_amount:(VarUInteger 16) forward_payload:(Either Cell ^Cell)
               = InternalMsgBody; 32 + 64 + 124 + 267 + 267 + 1 + 5  + 1
    */
    var msg = createMessage({
        bounce: false,
        dest: jettonWallet,
        value: 0,
        body: AskToTransfer {
                queryId: 0,
                jettonAmount: amount,
                transferRecipient: dest,
                sendExcessesTo: dest,
                customPayload: null,
                forwardTonAmount: 1,
                forwardPayload: beginCell()
            }
    });

    msg.send(SEND_MODE_CARRY_ALL_REMAINING_MESSAGE_VALUE | SEND_MODE_IGNORE_ERRORS);
}

@inline_ref
fun sendExcess(channel: Channel, dest: address) {
    var paymentConfig = lazy channel.paymentConfig.load();
    var reserve: int = paymentConfig.storageFee;

    if (paymentConfig.customCurrency == null) {
        var channelBalance = lazy channel.balance.load();
        reserve += channelBalance.calcA() + channelBalance.calcB();
    }
    reserveToncoinsOnBalance(reserve, RESERVE_MODE_AT_MOST);

    var msg = createMessage({
       bounce: false,
       dest: dest,
       value: 0,
       body: (OP_EXCESSES as uint32, 0 as uint64)
    });

    msg.send(SEND_MODE_CARRY_ALL_BALANCE | SEND_MODE_IGNORE_ERRORS);
}

@inline
fun assignJettonWallet(channel: Channel, sender: address, newWallet: address) {
    /* TEP-89
      take_wallet_address#d1735400 query_id:uint64 wallet_address:MsgAddress
                                   owner_address:(Maybe ^MsgAddress)
                                   = InternalMsgBody;
    */

    var paymentConfig = lazy channel.paymentConfig.load();
    assert(paymentConfig.customCurrency is PaymentConfigJettonStorage) throw ERROR_INVALID_CURRENCY_TYPE;
    var jettonConfig  = lazy paymentConfig.customCurrency.jettonCell.load();

    assert(jettonConfig.jettonWallet.isNone()) throw ERROR_JETTON_WALLET_ALREADY_INSTALLED;
    assert(jettonConfig.jettonRoot == sender) throw ERROR_NOT_AUTHORIZED;

    jettonConfig.jettonWallet = newWallet;

    paymentConfig.customCurrency = PaymentConfigJettonStorage {
        jettonCell: jettonConfig.toCell()
    };

    channel.paymentConfig = paymentConfig.toCell();

    return contract.setData(channel.serialize());
}

@inline
fun verifyStates(commitedSeqnoA: int, commitedSeqnoB: int, a: Semichannel, b: Semichannel): void {
    assert((a.data.seqno >= commitedSeqnoA) &
        (b.data.seqno >= commitedSeqnoB) &
        (b.data.seqno >= a.counterpartyData.seqno) &
        (a.data.seqno >= b.counterpartyData.seqno) &
        (b.data.sent >= a.counterpartyData.sent) &
        (a.data.sent >= b.counterpartyData.sent)) throw ERROR_OUTDATED_STATE;

        if(a.counterpartyData.seqno > 0) {
            assert(a.counterpartyData.seqno >= commitedSeqnoB) throw ERROR_OUTDATED_STATE;
        }
        if(b.counterpartyData.seqno > 0) {
            assert(b.counterpartyData.seqno >= commitedSeqnoA) throw ERROR_OUTDATED_STATE;
        }
}

@inline_ref
fun slice.verifyMessageType(mutate self, tag: uint32, id: int): void {
    var messageId = self.loadAny<ChannelMessageId>();
    assert(messageId.tag == tag) throw ERROR_WRONG_TAG;
    assert(messageId.id == id) throw ERROR_WRONG_CHANNEL_ID;
}

@inline
fun Balance.calcA(self): int {
    return (self.depositA + self.sentB) - (self.sentA + self.withdrawA);
}

@inline
fun Balance.calcB(self): int {
    return (self.depositB + self.sentA) - (self.sentB + self.withdrawB);
}

@inline
fun requestJettonWallet(jettonRoot: address) {
    /* TEP-89
      provide_wallet_address#2c76b973 query_id:uint64 owner_address:MsgAddress
                                      include_address:Bool
                                      = InternalMsgBody;
    */
    var request = createMessage({
        bounce: false,
        dest: jettonRoot,
        value: FEE_JETTON_WALLET_PROVIDE,
        body: ProvideWalletAddress {
            queryId: 0,
            ownerAddress: contract.getAddress(),
            include_address: false
        }
    });
    return request.send(0);
}

get fun getChannelState(): int {
    var channel = Channel.parse(contract.getData());

    if (!channel.inited) {
        return STATE_UNINITED;
    }
    if (channel.quarantine == null) {
        return STATE_OPEN;
    }

    var qua = lazy channel.quarantine.load();
    var closure = lazy channel.closureConfig.load();

    if (qua.startedAt + closure.quarantineDuration > blockchain.now()) {
        return STATE_CLOSURE_STARTED;
    }

    if (qua.startedAt +
        closure.quarantineDuration +
        closure.closeDuration > blockchain.now()) {
        return STATE_SETTLING_CONDITIONALS;
    }

    return STATE_AWAITING_FINALIZATION;
}

get fun getChannelData() {
    var channel = Channel.parse(contract.getData());

    var channelBalance = lazy channel.balance.load();
    var paymentConfig  = lazy channel.paymentConfig.load();

    return (getChannelState(),
        [channelBalance.calcA(), channelBalance.calcB(), channelBalance.depositA, channelBalance.depositB, channelBalance.withdrawA, channelBalance.withdrawB],
        [channel.keyA, channel.keyB],
        channel.id,
        channel.closureConfig,
        [channel.commitedSeqnoA, channel.commitedSeqnoB],
        channel.quarantine,
        [paymentConfig.storageFee, paymentConfig.addressA, paymentConfig.addressB]);
}
