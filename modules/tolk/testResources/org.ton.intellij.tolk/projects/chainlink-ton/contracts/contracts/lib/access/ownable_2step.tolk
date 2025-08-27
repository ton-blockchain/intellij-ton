import "./../utils.tolk";

// Exit codes

/// Matches the exit code for `TactExitCodeAccessDenied` from `@stdlib`.
const ERROR_ONLY_CALLABLE_BY_OWNER = 132;

/// Custom exit code indicating that the new owner is the same as the current one.
const ERROR_CANNOT_TRANSFER_TO_SELF = 1001;

/// Custom exit code indicating that only the proposed (pending) owner may accept ownership.
const ERROR_MUST_BE_PROPOSED_OWNER = 1002;

/// Ownable2Step trait provides ownership two-step transfer functionality.
struct Ownable2Step {
    owner: address;
    pendingOwner: address?;
}

// --- Message Structs ---

/// Message sent by the owner to transfer ownership of a contract.
struct (0xF21B7DA1) Ownable2Step_TransferOwnership {
    queryId: uint64;
    newOwner: address;
}

/// Message sent by the pending owner to accept ownership of a contract.
struct (0xF9E29E4A) Ownable2Step_AcceptOwnership {
    queryId: uint64;
}

/// Event emitted by the contract confirming that ownership transfer has been requested.
struct Ownable2Step_OwnershipTransferRequested {
    queryId: uint64;
    newOwner: address;
}

/// Event emitted by the contract to confirm acceptance of contract ownership.
struct Ownable2Step_OwnershipTransferred {
    queryId: uint64;
    oldOwner: address;
    newOwner: address;
}

// --- Message Handler ---

/// Union type representing messages this module can handle.
type Ownable2Step_Messages =
    Ownable2Step_TransferOwnership
    | Ownable2Step_AcceptOwnership;

/// Handles `Ownable2Step` messages for ownership management.
fun Ownable2Step.onInternalMessage(mutate self, sender: address, body: slice): bool {
    //This will fail with exitcode 63 if the message is not an Ownable2Step message
    var msg = lazy Ownable2Step_Messages.fromSlice(body);

    match (msg) {
        Ownable2Step_TransferOwnership => {
            self.transferOwnership(sender, msg.newOwner);
            val emitMsg = createExternalLogMessage({
                dest: createAddressNone(),
                body: Ownable2Step_OwnershipTransferRequested {
                    queryId: msg.queryId,
                    newOwner: msg.newOwner
                }
            });
            emitMsg.send(SEND_MODE_REGULAR);
        },

        Ownable2Step_AcceptOwnership => {
            var oldOwner = self.owner;
            self.acceptOwnership(sender);
            val emitMsg = createExternalLogMessage({
                dest: createAddressNone(),
                body: Ownable2Step_OwnershipTransferred {
                    queryId: msg.queryId,
                    oldOwner: oldOwner,
                    newOwner: sender
                }
            });
            emitMsg.send(SEND_MODE_REGULAR);
        }
        else => {
            return false; // Not an Ownable2Step message
        }
    }
    return true;
}



// --- Internal Functions ---

/// Requires that the message sender is the current contract owner.
///
/// #### Exit codes
/// * 132: ERROR_ONLY_CALLABLE_BY_OWNER
fun Ownable2Step.requireOwner(self, sender: address) {
    assert(sender == self.owner, ERROR_ONLY_CALLABLE_BY_OWNER);
}

/// Initiates ownership transfer to a new address.
///
/// #### Exit codes
/// * 132: ERROR_ONLY_CALLABLE_BY_OWNER
/// * 1001: ERROR_CANNOT_TRANSFER_TO_SELF
fun Ownable2Step.transferOwnership(mutate self, sender: address, to: address) {
    self.requireOwner(sender);
    assert(to != self.owner, ERROR_CANNOT_TRANSFER_TO_SELF);

    self.pendingOwner = to;
}

/// Finalizes ownership transfer by replacing the current owner with the pending owner.
///
/// #### Exit codes
/// * 1002: ERROR_MUST_BE_PROPOSED_OWNER
fun Ownable2Step.acceptOwnership(mutate self, sender: address) {
    assert(self.pendingOwner != null, ERROR_MUST_BE_PROPOSED_OWNER);
    assert(sender == self.pendingOwner, ERROR_MUST_BE_PROPOSED_OWNER);

    var previousOwner = self.owner;
    self.owner = sender;
    self.pendingOwner = null;
}

// --- Getter Functions ---

/// Returns the current owner of the contract.
fun Ownable2Step.get_owner(self): address {
    return self.owner;
}

/// Returns the pending owner of the contract, if any.
fun Ownable2Step.get_pendingOwner(self): address? {
    return self.pendingOwner;
}
