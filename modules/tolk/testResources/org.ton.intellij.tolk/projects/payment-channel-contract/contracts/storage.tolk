import "errors"

const TAG_STATE = 0x43685374

struct SemichannelInternal {
    data: SemichannelBody
    counterpartyData: Cell<SemichannelBody>?
}
struct Semichannel {
    data: SemichannelBody
    counterpartyData: SemichannelBody
}
struct SemichannelPacked {
    tag: uint32
    id: uint128
    state: SemichannelInternal
}

struct SemichannelSigned {
    signature: bits512
    channel: Cell<SemichannelPacked>
}

struct SemichannelBody {
    seqno: uint64
    sent: coins
    conditionalsHash: uint256
}

struct Quarantine {
    stateA: SemichannelBody
    stateB: SemichannelBody
    startedAt: uint32
    committedbyA: bool
    challenged: bool
}

struct ClosureConfig {
    quarantineDuration: uint32
    fine: coins
    closeDuration: uint32
}

struct PaymentConfigJetton {
    jettonRoot: address
    jettonWallet: address
}
struct PaymentConfigExtra {
    ecId: uint32
}
struct PaymentConfigJettonStorage {
    jettonCell: Cell<PaymentConfigJetton>
}
type CustomCurrency =
    | PaymentConfigExtra
    | PaymentConfigJettonStorage

struct PaymentConfig {
    storageFee: coins
    addressA: address
    addressB: address
    customCurrency: CustomCurrency?
}

struct Balance {
    depositA: coins
    depositB: coins
    withdrawA: coins
    withdrawB: coins
    sentA: coins
    sentB: coins
}

struct Channel {
    inited: bool
    balance: Cell<Balance>
    keyA: uint256
    keyB: uint256
    id: uint128
    closureConfig: Cell<ClosureConfig>
    commitedSeqnoA: uint64
    commitedSeqnoB: uint64

    quarantine: Cell<Quarantine>?
    paymentConfig: Cell<PaymentConfig>
}


@inline
fun ClosureConfig.serialize(self): cell {
    return self.toCell();
}

@inline
fun PaymentConfig.serialize(self): cell {
    return self.toCell();
}

@inline
fun Balance.serialize(self): cell {
    return Balance.toCell(self);
}

@inline
fun Channel.parse(c: cell): Channel {
    return Channel.fromCell(c);
}

@inline_ref
fun Channel.serialize(self): cell {
    return self.toCell();
}

@inline_ref
fun Semichannel.parse(c: cell, id: uint128, key: uint256): Semichannel {
    var signedState = SemichannelSigned.fromCell(c);
    assert(isSignatureValid(signedState.channel.hash(), signedState.signature as slice, key)) throw ERROR_NOT_AUTHORIZED;
    var unpackedChannel = SemichannelPacked.fromCell(signedState.channel);

    assert(unpackedChannel.tag == TAG_STATE) throw ERROR_WRONG_TAG;
    assert(unpackedChannel.id  == id) throw ERROR_WRONG_CHANNEL_ID;

    var data = unpackedChannel.state.data;

    var counterpartyData = SemichannelBody{
        seqno: 0,
        sent: 0,
        conditionalsHash: 0,
    };

    if (unpackedChannel.state.counterpartyData != null) {
        counterpartyData = SemichannelBody.fromCell(unpackedChannel.state.counterpartyData);
    }

    return Semichannel{
        data: data,
        counterpartyData: counterpartyData,
    };
};


@inline_ref
fun Quarantine.serialize(self): Cell<Quarantine>{
    return self.toCell();
}

@inline
fun slice.loadSemichannelBody(mutate self): SemichannelBody {
    return self.loadAny<SemichannelBody>();
}
