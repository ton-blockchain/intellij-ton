import "types.tolk";
import "../lib/access/ownable_2step.tolk";
import "../lib/upgrades/type_and_version.tolk";
import "../deployable/types.tolk";
import "../lib/utils.tolk";

struct Storage {
    ownable: Ownable2Step;

    // TODO: expand this with versions support
    onRamp: address;
}

fun Storage.load(): Storage {
    return Storage.fromCell(contract.getData());
}

fun Storage.store(self) {
    return contract.setData(self.toCell());
}

type Msg = CCIPSend | SetRamp | JettonTransferNotification;

fun onInternalMessage(in: InMessage) {
    val msg = lazy Msg.fromSlice(in.body);
    match (msg) {
        SetRamp => { onSetRamp(msg, in.senderAddress) }
        CCIPSend => { onSend(msg, in.senderAddress) }
        JettonTransferNotification => { onTransferNotification(msg, in.senderAddress) }
        else => {
            // ignore empty messages, "wrong opcode" for others
            assert (in.body.isEmpty()) throw 0xFFFF
        }
    }
}

fun onBouncedMessage(in: InMessageBounced) {
}

fun onSetRamp(msg: SetRamp, sender: address) {
    var st = lazy Storage.load();
    st.ownable.requireOwner(sender);

    st.onRamp = msg.onRamp;
    st.store();

    // TODO: emit(OnRampSet)
}

fun onSend(msg: CCIPSend, sender: address) {
    ccipSend(msg, sender);
}

fun onTransferNotification(msg: JettonTransferNotification, sender: address) {
    val st = lazy Storage.load();

    // TODO: need to validate that the transfer notification came from our own wallet
    // assert(sender == st.owner) throw ERROR_NOT_OWNER;
    // TODO: need to validate the jetton (master) address, but to do this we need to
    // derive the address. We can probably fetch that from the ccip send payload's tokenAmounts

    // TODO: catch errors, if different message type than CCIPSend
    val sendMsg: CCIPSend = msg.forwardPayload!.load();

    // TODO: store msg.amount + value
    // make sure msg.amount == sendMsg.token amount

    ccipSend(sendMsg, msg.sender);

    // Forward excesses
    // self.forward(msg.sender, null, false, null);
}

fun ccipSend(msg: CCIPSend, sender: address) {
    val st = lazy Storage.load();

    // forward to onRamp
    // ccipSend(onRamp, msg, value/token amounts)
    val sendMsg = createMessage({
        bounce: true,
        value: ton("0.1"), // TODO:
        dest: st.onRamp,
        body: OnRampSend { msg: msg.toCell(), metadata: Metadata { sender } },
    });
    sendMsg.send(SEND_MODE_REGULAR);
}

get fun typeAndVersion(): (slice, slice) {
     return ("com.chainlink.ton.ccip.Router", "1.0.0");
 }
