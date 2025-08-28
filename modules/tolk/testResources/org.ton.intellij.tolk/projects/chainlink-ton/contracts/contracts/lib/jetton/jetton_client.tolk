import "./utils.tolk"
import "messages"
import "../../lib/jetton/messages.tolk"

struct JettonClient {
    masterAddress: address
    jettonWalletCode: cell
}

@inline
fun JettonClient.walletAddress(self): address {
    return calculateUserJettonWalletAddress(
        contract.getAddress(),
        self.masterAddress,
        self.jettonWalletCode
    );
}

// removed in Tolk 1.1
type ExtraCurrenciesDict = dict;

struct JettonMessageOptions {
    /// whether a message will bounce back on error
    bounce: bool
    /// message value: attached tons (or tons + extra currencies)
    value: coins | (coins, ExtraCurrenciesDict)
}

@inline
fun JettonClient.sendSimple(
    self,
    sendOptions: JettonMessageOptions,
    sendMode: int,
    queryId: uint64,
    amount: coins,
    destination: address,
    responseDestination: address,
) {
    createMessage({
        bounce: sendOptions.bounce,
        value: sendOptions.value,
        dest: self.walletAddress(),
        body: AskToTransfer {
            queryId: queryId,
            jettonAmount: amount,
            transferRecipient: destination,
            sendExcessesTo: contract.getAddress(),
            customPayload: null,
            forwardTonAmount: 0,
            forwardPayload: beginCell().storeBool(false).endCell().beginParse(),
        },
    })
        .send(sendMode);
}

@inline
fun JettonClient.sendExtended(
    self,
    sendOptions: JettonMessageOptions,
    sendMode: int,
    queryId: uint64,
    amount: coins,
    destination: address,
    responseDestination: address,
    customPayload: cell?,
    forwardTonAmount: coins,
    forwardPayload: slice | cell,
) {
    var body = beginCell().storeAny(AskToTransfer {
        queryId: queryId,
        jettonAmount: amount,
        transferRecipient: destination,
        sendExcessesTo: responseDestination,
        customPayload: customPayload,
        forwardTonAmount: forwardTonAmount,
        forwardPayload: beginCell().endCell().beginParse(),
    });
    match (forwardPayload) {
        cell => {
            body.storeMaybeRef(forwardPayload);
        }
        slice => {
            body.storeBool(false).storeSlice(forwardPayload);
        }
    }
    createMessage({
        bounce: sendOptions.bounce,
        value: sendOptions.value,
        dest: self.walletAddress(),
        body: body,
    })
        .send(sendMode);
}

// Assert that the sender is the jetton wallet address. Used for validating incoming notifications.
@inline
fun JettonClient.isWallet(self, sender: address): bool {
    return sender == self.walletAddress();
}

@inline
fun loadMaybeForwardPayloadAsCell(forwardPayload: ForwardPayloadRemainder): cell? {
    val forwardPayloadCell = forwardPayload.loadMaybeRef();
    if (forwardPayloadCell != null) {
        forwardPayload.assertEnd();
        return forwardPayloadCell;
    } else {
        if (forwardPayload.isEmpty()) {
            return null;
        }
        return beginCell().storeSlice(forwardPayload).endCell();
    }
}

@inline
fun loadMaybeForwardPayloadAsSlice(forwardPayload: ForwardPayloadRemainder): slice? {
    val forwardPayloadCell = forwardPayload.loadMaybeRef();
    if (forwardPayloadCell != null) {
        forwardPayload.assertEnd();
        return forwardPayloadCell!.beginParse();
    } else {
        if (forwardPayload.isEmpty()) {
            return null;
        }
        return forwardPayload;
    }
}

@inline
fun loadForwardPayloadAsCell(forwardPayload: ForwardPayloadRemainder): cell? {
    val forwardPayloadCell = forwardPayload.loadMaybeRef();
    if (forwardPayloadCell != null) {
        forwardPayload.assertEnd();
        return forwardPayloadCell;
    }
    return beginCell().storeSlice(forwardPayload).endCell();
}

@inline
fun loadForwardPayloadAsSlice(forwardPayload: ForwardPayloadRemainder): slice? {
    val forwardPayloadCell = forwardPayload.loadMaybeRef();
    if (forwardPayloadCell != null) {
        forwardPayload.assertEnd();
        return forwardPayloadCell!.beginParse();
    }
    return forwardPayload;
}
