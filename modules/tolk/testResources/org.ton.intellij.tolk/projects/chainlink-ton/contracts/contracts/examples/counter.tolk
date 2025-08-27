tolk 1.0

import "@stdlib/common.tolk"
import "../lib/upgrades/type_and_version.tolk"
import "../lib/utils.tolk"
import "../lib/access/ownable_2step.tolk"

/// Counter contract + event emission (Tolk example)
/// Message to set the counter value.
struct (0x00000004) SetCount {
    queryId: uint64 // Standard query_id field
    newCount: uint32 // Argument for the operation
}

/// Message to increase the counter value.
struct (0x10000005) IncreaseCount {
    queryId: uint64 // Standard query_id field
}

/// Event Topics
const COUNT_SET_TOPIC = 0x0766fed0 // crc32("CountSet")
const COUNT_INCREASED_TOPIC = 0x1947b328 // crc32("CountIncreased")

/// Event emitted when the counter is set
struct CountSet {
    id: uint32
    value: uint32
    sender: address
}

/// Event emitted when the counter is increased
struct CountIncreased {
    id: uint32
    value: uint32
    sender: address
}

/// Reply back to sender when the counter is set
struct (0xf3a02426) CountSetMsg {
    id: uint32
    value: uint32
    sender: address
}

/// Reply back to sender when the counter is increased
struct (0x41c92746) CountIncreasedMsg {
    id: uint32
    value: uint32
    sender: address
}

struct Storage {
    id: uint32
    value: uint32
    ownable: Ownable2Step
}

fun Storage.load(): Storage {
    return Storage.fromCell(contract.getData());
}

fun Storage.store(self) {
    return contract.setData(self.toCell());
}

type Msg = SetCount | IncreaseCount

fun onInternalMessage(in: InMessage) {
    val msg = lazy Msg.fromSlice(in.body); // 63 error code is thrown if the message opcode is unknown
    match (msg) {
        SetCount => {
            /// Instructs the contract to set the counter.
            var st = lazy Storage.load();
            st.ownable.requireOwner(in.senderAddress);
            st.value = msg.newCount;
            st.store();

            // emit ExtMsgOut
            emit<CountSet>(COUNT_SET_TOPIC, { id: st.id, value: st.value, sender: in.senderAddress });

            // reply back to sender
            val reply = createMessage({
                bounce: false,
                value: 0,
                dest: in.senderAddress,
                body: CountSetMsg {
                    id: st.id,
                    value: st.value,
                    sender: in.senderAddress,
                },
            });
            reply.send(SEND_MODE_CARRY_ALL_REMAINING_MESSAGE_VALUE);
        }
        IncreaseCount => {
            /// Instructs the contract to increase the counter.
            var st = lazy Storage.load();
            st.ownable.requireOwner(in.senderAddress);
            st.value = st.value + 1;
            st.store();

            // emit ExtMsgOut
            emit<CountIncreased>(
                COUNT_INCREASED_TOPIC,
                {
                    id: st.id,
                    value: st.value,
                    sender: in.senderAddress,
                }
            );

            // reply back to sender
            val reply = createMessage({
                bounce: false,
                value: 0,
                dest: in.senderAddress,
                body: CountIncreasedMsg {
                    id: st.id,
                    value: st.value,
                    sender: in.senderAddress,
                },
            });
            reply.send(SEND_MODE_CARRY_ALL_REMAINING_MESSAGE_VALUE);
        }
        else => {
            var st = lazy Storage.load();
            val msgHandled = 0
                || st.ownable.onInternalMessage(in.senderAddress, in.body);
            st.store();

            if (!msgHandled) {
                // If the message was not handled
                // ignore empty messages, "wrong opcode" for others
                assert (in.body.isEmpty()) throw ERROR_WRONG_OP;
            }
        }
    }
}

get fun owner(): address {
    val s = lazy Storage.load();
    return s.ownable.get_owner();
}

/// Gets the current id of the contract.
get fun id(): int {
    val s = lazy Storage.load();
    return s.id;
}

/// Gets the current counter value.
get fun value(): int {
    val s = lazy Storage.load();
    return s.value;
}

/// Gets the current type and version of the contract.
get fun typeAndVersion(): (slice, slice) {
    return TypeAndVersion { typeStr: "com.chainlink.ton.examples.Counter", versionStr: "1.1.0" }
        .typeAndVersion();
}
