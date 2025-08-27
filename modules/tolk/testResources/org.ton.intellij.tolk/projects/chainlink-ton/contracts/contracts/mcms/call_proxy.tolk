tolk 1.0

// --- Messages - incoming ---

/// @dev Top up contract with TON coins.
/// Contract might receive/hold TON as part of the maintenance process.
///
struct (0x3b3d63b8) CallProxy_TopUp {
    /// Query ID of the change owner request.
    queryId: uint64;
}

/// @dev Union of all incoming messages.
type CallProxy_InMessage = CallProxy_TopUp

// --- Storage ---

/// CallProxy contract storage, auto-serialized to/from cell.
struct CallProxy_Data {
    /// ID allows multiple independent instances, since contract address depends on initial state.
    id: uint32;

    /// Target address to which the contract forwards all messages.
    target: address;
}

/// Load from contract data using auto-serialization.
@inline
fun CallProxy_Data.fromContractData() {
    return CallProxy_Data.fromCell(contract.getData());
}

// --- Constants ---

const RENT_MAX_TON: int = ton("0.1");

const ERROR_CONTRACT_MAX_FUNDED = 101;
const ERROR_VALUE_OUT_OF_BOUNDS = 102;

// --- Message handlers ---

/// @notice a contract which acts as a forwarder that forwards the input from
/// any caller to a target contract.
fun onInternalMessage(in: InMessage) {
    val msg = lazy CallProxy_InMessage.fromSlice(in.body);
    match (msg) {
        CallProxy_TopUp => {
            // Top up the contract balance
            // Notice: we put a simple static limit on max rent this contract can hold
            //  - funds are only needed for rent
            //  - funds can't be withdrawn once deposited

            val current = contract.getOriginalBalance() - in.valueCoins;
            if (current > RENT_MAX_TON) {
                throw(ERROR_CONTRACT_MAX_FUNDED)
            }

            if (in.valueCoins > RENT_MAX_TON) {
                throw(ERROR_VALUE_OUT_OF_BOUNDS)
            }

            return;
        }
        else => {
             /// Load the contract storage as target address and proxy the message
            val data = lazy CallProxy_Data.fromCell(contract.getData());

            // Proxy a message to the target
            createMessage({
                bounce: false,
                value: 0,
                dest: data.target,
                body: in.body
            }).send(SEND_MODE_REGULAR | SEND_MODE_CARRY_ALL_REMAINING_MESSAGE_VALUE);
        }
    }
}

// --- Getters ---

get fun getID(): uint32 {
     return CallProxy_Data.fromContractData().id;
}

get fun getTarget(): address {
     return CallProxy_Data.fromContractData().target;
}
