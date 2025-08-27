tolk 1.0

import "@stdlib/tvm-dicts";

import "../lib/utils";
import "../lib/access/access_control";

/**
 * @notice Contract module which acts as a timelocked controller with role-based
 * access control. When set as the owner of an `Ownable` smart contract, it
 * can enforce a timelock on `onlyOwner` maintenance operations and prevent
 * a list of blocked functions from being called. The timelock can be bypassed
 * by a bypasser or an admin in emergency situations that require quick action.
 *
 * Non-emergency actions are expected to follow the timelock.
 *
 * The contract has five roles. Each role can be inhabited by multiple
 * (potentially overlapping) addresses.
 *
 * 1) Admin: The admin manages membership for all roles (including the admin
 *    role itself). The admin automatically inhabits all other roles. The admin
 *    can call the bypasserExecuteBatch function to bypass any restrictions like
 *    the delay imposed by the timelock and the list of blocked functions. The
 *    admin can manage the list of blocked functions. In practice, the admin
 *    role is expected to (1) be inhabited by a contract requiring a secure
 *    quorum of votes before taking any action and (2) to be used rarely, namely
 *    only for emergency actions or configuration of the RBACTimelock.
 *
 * 2) Proposer: The proposer can schedule delayed operations that don't use any
 *    blocked function selector.
 *
 * 3) Executor: The executor can execute previously scheduled operations once
 *    their delay has expired. The contract enforces that the calls in an
 *    operation are executed with the correct args (target, data, value), but
 *    the executor can freely choose the gas limit. Since the executor is
 *    typically not particularly trusted, we recommend that (transitive) callees
 *    implement standard behavior of simply reverting if insufficient gas is
 *    provided. In particular, this means callees should not have non-reverting
 *    gas-dependent branches.
 *
 * 4) Canceller: The canceller can cancel operations that have been scheduled
 *    but not yet executed.
 *
 * 5) Bypasser: The bypasser can bypass any restrictions like the delay imposed
 *    by the timelock and the list of blocked functions to immediately execute
 *    operations, e.g. in case of emergencies.
 *
 * Note that this contract doesn't place any restrictions on the gas limit used
 * when executing operations. See the above comment on the executor role for
 * more details.
 *
 * @dev This contract is a modified version of OpenZeppelin's
 * contracts/governance/TimelockController.sol contract from v4.7.0, accessed in
 * commit 561d1061fc568f04c7a65853538e834a889751e8 of
 * github.com/OpenZeppelin/openzeppelin-contracts
 * Said contract is under "Copyright (c) 2016-2023 zOS Global Limited and
 * contributors" and its original MIT license can be found at
 * https://github.com/OpenZeppelin/openzeppelin-contracts/blob/561d1061fc568f04c7a65853538e834a889751e8/LICENSE
 */

// --- Messages - incoming ---

/// @dev Initializes the contract with the following parameters:
///
/// - `minDelay`: initial minimum delay for operations
/// - `admin`: account to be granted admin role
/// - `proposers`: accounts to be granted proposer role
/// - `executors`: accounts to be granted executor role
/// - `cancellers`: accounts to be granted canceller role
/// - `bypassers`: accounts to be granted bypasser role
///
struct (0x4982fcfd) Timelock_Init {
    /// Query ID of the change owner request.
    queryId: uint64;

    /// Minimum delay in seconds for future operations.
    minDelay: uint64;

    /// Address of the admin account.
    admin: address;

    /// Collection of addresses to be granted proposer, executor, canceller and bypasser roles.
    proposers: cell;  // vec<address>
    executors: cell;  // vec<address>
    cancellers: cell; // vec<address>
    bypassers: cell;  // vec<address>
}

/// @dev Top up contract with TON coins.
/// Contract might receive/hold TON as part of the maintenance process.
///
struct (0xfee62ba6) Timelock_TopUp {
    /// Query ID of the change owner request.
    queryId: uint64;
}

/// @dev Schedule an operation containing a batch of transactions.
///
/// Emits one {Timelock_CallScheduled} event per transaction in the batch.
///
/// Requirements:
///
/// - the caller must have the 'proposer' or 'admin' role.
/// - all payloads must not start with a blocked function selector.
///
struct (0x094718f4) Timelock_ScheduleBatch {
    /// Query ID of the change owner request.
    queryId: uint64;

    // Array of calls to be scheduled
    calls: cell; // vec<Timelock_Call>
    // Predecessor operation ID
    predecessor: uint256;
    // Salt used to derive the operation ID
    salt: uint256;
    // Delay in seconds before the operation can be executed
    delay: uint64;
}

/// @dev Cancel an operation.
///
/// Requirements:
///
/// - the caller must have the 'canceller' or 'admin' role.
///
struct (0xaf3bf1d0) Timelock_Cancel {
    /// Query ID of the change owner request.
    queryId: uint64;

    /// ID of the operation to cancel.
    id: uint256;
}

/// @dev Execute an (ready) operation containing a batch of transactions.
///
/// Emits one {Timelock_CallExecuted} event per transaction in the batch.
///
/// Requirements:
///
/// - the caller must have the 'executor' or 'admin' role.
///
struct (0x6e9Bf263) Timelock_ExecuteBatch {
    /// Query ID of the change owner request.
    queryId: uint64;

    // Array of calls to be scheduled
    calls: cell; // vec<Timelock_Call>
    // Predecessor operation ID
    predecessor: uint256;
    // Salt used to derive the operation ID
    salt: uint256;
}

/// @dev Changes the minimum timelock duration for future operations.
///
/// Emits a {Timelock_MinDelayChange} event.
///
/// Requirements:
///
/// - the caller must have the 'admin' role.
///
struct (0x7a57a45c) Timelock_UpdateDelay {
    /// Query ID of the change owner request.
    queryId: uint64;

    /// New minimum delay in seconds for future operations.
    newDelay: uint64;
}

/// @dev Blocks a function selector from being used, i.e. schedule
/// operations with this function selector will revert.
///
/// Note that blocked selectors are only checked when an operation is being
/// scheduled, not when it is executed. You may want to check any pending
/// operations for whether they contain the blocked selector and cancel them.
///
/// Requirements:
///
/// - the caller must have the 'admin' role.
///
struct (0x2637af77) Timelock_BlockFunctionSelector {
    /// Query ID of the change owner request.
    queryId: uint64;

    /// Function selector to block.
    selector: uint32;
}

/// @dev Unblocks a previously blocked function selector so it can be used again.
///
/// Requirements:
///
/// - the caller must have the 'admin' role.
///
struct (0x26f19f4e) Timelock_UnblockFunctionSelector {
    /// Query ID of the change owner request.
    queryId: uint64;

    /// Function selector to unblock.
    selector: uint32;
}

/// @dev Directly execute a batch of transactions, bypassing any other checks.
///
/// Emits one {Timelock_BypasserCallExecuted} event per transaction in the batch.
///
/// Requirements:
///
/// - the caller must have the 'bypasser' or 'admin' role.
///
struct (0xbb0e9f7d) Timelock_BypasserExecuteBatch {
    /// Query ID of the change owner request.
    queryId: uint64;

    /// Array of calls to be scheduled
    calls: cell; // vec<Timelock_Call>
}

/// @dev Union of all incoming messages.
type Timelock_InMessage = Timelock_Init
    | Timelock_TopUp
    | Timelock_ScheduleBatch
    | Timelock_Cancel
    | Timelock_ExecuteBatch
    | Timelock_UpdateDelay
    | Timelock_BlockFunctionSelector
    | Timelock_UnblockFunctionSelector
    | Timelock_BypasserExecuteBatch;

/// @dev Union of all outgoing messages.
type Timelock_OutMessage = Timelock_BatchScheduled
    | Timelock_CallScheduled
    | Timelock_BatchExecuted
    | Timelock_CallExecuted
    | Timelock_BypasserBatchExecuted
    | Timelock_BypasserCallExecuted
    | Timelock_Canceled
    | Timelock_MinDelayChange
    | Timelock_FunctionSelectorBlocked
    | Timelock_FunctionSelectorUnblocked

// --- Messages - outgoing ---

/// @dev Sent back to sender after a batch is scheduled.
struct (0xdf65b59e) Timelock_BatchScheduled {
    /// Query ID of the change owner request.
    queryId: uint64;

    /// ID of the batch that was scheduled.
    id: uint256;
    /// Delay in seconds before the operation can be executed
    delay: uint64;
}

/// @dev Emitted when a call is scheduled as part of operation `id`.
struct (0xc55fca54) Timelock_CallScheduled {
    /// Query ID of the change owner request.
    queryId: uint64;

    /// ID of the batch for the operation that was scheduled.
    id: uint256;
    index: uint64;
    /// Call to be executed as part of the operation.
    /// Notice: split out into a separate cell to avoid message size limit issues (vs. EVM inline).
    call: Cell<Timelock_Call>;
    predecessor: uint256;
    salt: uint256;
    delay: uint64;
}

/// @dev Sent back to sender after a batch is executed.
struct (0xa941ea1a) Timelock_BatchExecuted {
    /// Query ID of the change owner request.
    queryId: uint64;

    /// ID of the batch that was executed.
    id: uint256;
}

/// @dev Emitted when a call is performed as part of operation `id`.
struct (0x49ea5d0e) Timelock_CallExecuted {
    /// Query ID of the change owner request.
    queryId: uint64;

    /// ID of the batch for the operation that was executed.
    id: uint256;
    index: uint64;
    target: address;
    value: coins;
    data: cell;
}

/// @dev Sent back to sender after a batch is executed via bypasser escape hatch.
struct (0x539b4214) Timelock_BypasserBatchExecuted {
    /// Query ID of the change owner request.
    queryId: uint64;
}

/// @dev Emitted when a call is performed via bypasser.
struct (0x9c7f3010) Timelock_BypasserCallExecuted {
    /// Query ID of the change owner request.
    queryId: uint64;

    /// Index of the call in the batch.
    index: uint64;
    target: address;
    value: coins;
    data: cell;
}

/// @dev Emitted when operation `id` is cancelled.
struct (0x580e80f2) Timelock_Canceled {
    /// Query ID of the change owner request.
    queryId: uint64;

    /// ID of the cancelled operation.
    id: uint256;
}

/// @dev Emitted when the minimum delay for future operations is modified.
struct (0x904b14e0) Timelock_MinDelayChange {
    /// Query ID of the change owner request.
    queryId: uint64;

    /// Duration of the old minimum delay in seconds.
    oldDuration: uint64;
    /// Duration of the new minimum delay in seconds.
    newDuration: uint64;
}

/// @dev Emitted when a function selector is blocked.
struct (0x9c4d6d94) Timelock_FunctionSelectorBlocked {
    /// Query ID of the change owner request.
    queryId: uint64;

    /// Function selector that was blocked.
    selector: uint32;
}

/// @dev Emitted when a function selector is unblocked.
struct (0xf410a31b) Timelock_FunctionSelectorUnblocked {
    /// Query ID of the change owner request.
    queryId: uint64;

    /// Function selector that was unblocked.
    selector: uint32;
}

// --- Storage ---

/// Timelock contract struct, host storage and runtime hooks (extensions).
struct Timelock<T> {
    data: Timelock_Data;

    /// Runtime hooks (extensions)
    context: T? = null,
    hooks: Timelock_Hooks<T>? = null;
}

/// RBACTimelock contract storage, auto-serialized to/from cell.
struct Timelock_Data {
    /// ID allows multiple independent instances, since contract address depends on initial state.
    id: uint32;

    /// Minimum delay for operations in seconds
    minDelay: uint64;
    /// Map of operation id to timestamp
    timestamps: dict; // map<int, uint64>

    /// Number of fn selectors blocked by the contract.
    blockedFnSelectorsLen: uint32;
    /// Map of blocked function selectors.
    blockedFnSelectors: dict; // map<uint32, slice>

    /// AccessControl trait data
    rbac: Cell<AccessControl_Data>;
}

/// Load from contract data using auto-serialization.
@inline
fun Timelock_Data.fromContractData() {
    return Timelock_Data.fromCell(contract.getData());
}

/// Store as contract data into persistent storage using auto-serialization.
@inline
fun Timelock_Data.storeAsContractData(self) {
    contract.setData(self.toCell());
}

/// @dev Represents a single call
struct Timelock_Call {
    /// Address of the target contract to call.
    target: address;
    /// Value in TONs to send with the call.
    value: coins;
    /// Data to send with the call - message body.
    data: cell;
}

/// @dev Batch of transactions represented as a operation, which can be scheduled and executed.
struct OperationBatch {
    // Array of calls to be scheduled
    calls: cell; // vec<Timelock_Call>
    // Predecessor operation ID
    predecessor: uint256;
    // Salt used to derive the operation ID
    salt: uint256;
}

// --- Hooks struct (extensions) ---

/// @dev Hook extensions exposed by the Timelock contract.
struct Timelock_Hooks<T> {
    /// @dev AccessControl hooks exposed by the Timelock contract.
    rbac: AccessControl_Hooks<T>;

    /// Hook to get the identifier of an operation containing a batch of transactions.
    hashOperationBatch: ((T?, OperationBatch) -> uint256)?;
}

// --- Constants ---

const TOPIC_BYPASSER_CALL_EXECUTED = stringCrc32("Timelock_BypasserCallExecuted");
const TOPIC_CALL_SCHEDULED = stringCrc32("Timelock_CallScheduled");
const TOPIC_CALL_EXECUTED = stringCrc32("Timelock_CallExecuted");

/// Notice: role constants are kept as original Ethereum implementation (keccak256)
const ADMIN_ROLE = 0xa49807205ce4d355092ef5a8a18f56e8913cf4a201fbe287825b095693c21775;     // keecak256('ADMIN_ROLE')
const PROPOSER_ROLE = 0xb09aa5aeb3702cfd50b6b62bc4532604938f21248a27a1d5ca736082b6819cc1;  // keecak256('PROPOSER_ROLE')
const CANCELLER_ROLE = 0xfd643c72710c63c0180259aba6b2d05451e3591a24e58b62239378085726f783; // keecak256('CANCELLER_ROLE')
const EXECUTOR_ROLE = 0xd8aa0f3194971a2a116679f7c2090f6939c8d4e01a2a8d7e41d55e5351469e63;  // keecak256('EXECUTOR_ROLE')
const BYPASSER_ROLE = 0xa1b2b8005de234c4b8ce8cd0be058239056e0d54f6097825b5117101469d5a8d;  // keecak256('BYPASSER_ROLE')

/// Timestamp value used to mark an operation as done
const DONE_TIMESTAMP = 1;

const ERROR_SELECTOR_IS_BLOCKED = 101;
const ERROR_OPERATION_NOT_READY = 102;
const ERROR_OPERATION_MISSING_DEPENDENCY = 103;
const ERROR_OPERATION_CAN_NOT_BE_CANCELLED = 104;
const ERROR_OPERATION_ALREADY_SCHEDULED = 105;
const ERROR_INSUFFICIENT_DELAY = 106;

// --- Constants - storage ---

/// Represents TON message op code size
const LEN_BLOCKED_FN_SELECTOR = 32;
/// Represents the amount of TON coins we reserve on the balance to pay for storage fees.
const MIN_TON_FOR_STORAGE = 1000000000; // 1 TON

// --- Methods ---

/// @dev Loads the Timelock contract data from persistent storage.
fun Timelock<T>.load(context: T? = null, hooks: Timelock_Hooks<T>? = null): Timelock<T> {
    // Load the contract data from the persistent storage
    var data = Timelock_Data.fromContractData();

    // Return the Timelock instance with loaded data and hooks
    return Timelock<T> {
        data: data,
        context: context,
        hooks: hooks,
    };
}

/// @dev Requires that the sender has the given role or the admin role.
fun Timelock<T>.onlyRoleOrAdminRole(self, role: int, sender: address): void {
    val rbac = self.hookTrait__AccessControl();
    if (!rbac.hasRole(ADMIN_ROLE, sender)) {
        rbac.requireRole(role, sender);
    }
}

/// @dev Returns whether an id correspond to a registered operation. This
/// includes both Pending, Ready and Done operations.
fun Timelock<T>.isOperation(self, id: int): bool {
    return self.getTimestamp(id) > 0;
}

/// @dev Returns whether an operation is pending or not.
fun Timelock<T>.isOperationPending(self, id: int): bool {
    return self.getTimestamp(id) > DONE_TIMESTAMP;
}

/// @dev Returns whether an operation is ready or not.
fun Timelock<T>.isOperationReady(self, id: int): bool {
    val t = self.getTimestamp(id);
    return t > DONE_TIMESTAMP && t <= blockchain.now();
}

/// @dev Returns whether an operation is done or not.
fun Timelock<T>.isOperationDone(self, id: int): bool {
    return self.getTimestamp(id) == DONE_TIMESTAMP;
}

/// @dev Returns the timestamp at with an operation becomes ready (0 for
/// unset operations, 1 for done operations).
fun Timelock<T>.getTimestamp(self, id: int): uint64 {
    // Get the timestamp for the given id from the timestamps dictionary
    var (timestampSlice, exists) = self.data.timestamps.uDictGet(256, id);
    if (!exists) {
        return 0; // operation not exists
    }

    // Load the timestamp from the slice
    return timestampSlice!.loadUint(64);
}

/// @dev Returns the minimum delay for an operation to become valid.
///
/// This value can be changed by executing an operation that calls `updateDelay`.
fun Timelock<T>.getMinDelay(self): uint64 {
    return self.data.minDelay;
}

/// @dev Returns the operation batch hashe as its ID.
///
/// Override available via {Timelock_Hooks<T>.hashOperationBatch} hook.
fun Timelock<T>.hashOperationBatch(self, op: OperationBatch): uint256 {
    if (self.hooks != null && self.hooks.hashOperationBatch != null) {
        return self.hooks.hashOperationBatch(self.context, op); // extension hook
    }

    // Fallback to taking the standard cell hash of the operation
    return op.toCell().hash();
}

/// @dev Initializes the contract
fun Timelock<T>.init(
    mutate self,
    sender: address,
    queryId: uint64,
    minDelay: uint64,
    admin: address,
    proposers: cell,  // vec<address>
    executors: cell,  // vec<address>
    cancellers: cell, // vec<address>
    bypassers: cell,  // vec<address>
): void {
    // TODO: add a check to secure contract initialization
    //   - check that the contract is not already initialized
    //   - check that the deployer is the Timelock_Init sender

    var rbac = self.hookTrait__AccessControl();

    rbac._setRoleAdmin(queryId, sender, ADMIN_ROLE, ADMIN_ROLE);
    rbac._setRoleAdmin(queryId, sender, PROPOSER_ROLE, ADMIN_ROLE);
    rbac._setRoleAdmin(queryId, sender, EXECUTOR_ROLE, ADMIN_ROLE);
    rbac._setRoleAdmin(queryId, sender, CANCELLER_ROLE, ADMIN_ROLE);
    rbac._setRoleAdmin(queryId, sender, BYPASSER_ROLE, ADMIN_ROLE);

    rbac._grantRole(queryId, sender, ADMIN_ROLE, admin);

    // Grant roles to proposers, executors, cancellers and bypassers
    var pp = Iterator<address>.new(proposers);
    while (!pp.empty()) {
        rbac._grantRole(queryId, sender, PROPOSER_ROLE, pp.next());
    }

    var ee = Iterator<address>.new(executors);
    while (!ee.empty()) {
        rbac._grantRole(queryId, sender, EXECUTOR_ROLE, ee.next());
    }

    var cc = Iterator<address>.new(cancellers);
    while (!cc.empty()) {
        rbac._grantRole(queryId, sender, CANCELLER_ROLE, cc.next());
    }

    var bb = Iterator<address>.new(bypassers);
    while (!bb.empty()) {
        rbac._grantRole(queryId, sender, BYPASSER_ROLE, bb.next());
    }

    self.data.minDelay = minDelay;

    // Reply back to sender
    createMessage({
        bounce: false,
        value: DEFAULT_MESSAGE_BUDGET_FEE, // send small value as gas budget, keep the rest
        dest: sender,
        body: Timelock_MinDelayChange{queryId, oldDuration: 0, newDuration: minDelay}
    }).send(SEND_MODE_REGULAR);
}

/// @dev Schedule an operation containing a batch of transactions.
///
/// Emits one {Timelock_CallScheduled} event per transaction in the batch.
///
/// Requirements:
///
/// - the caller must have the 'proposer' or 'admin' role.
/// - all payloads must not start with a blocked function selector.
///
fun Timelock<T>.scheduleBatch(
    mutate self,
    sender: address,
    queryId: uint64,
    calls: cell, // vec<Timelock_Call>
    predecessor: uint256,
    salt: uint256,
    delay: uint64
): void {
    self.onlyRoleOrAdminRole(PROPOSER_ROLE, sender);

    val id = self.hashOperationBatch(OperationBatch{calls, predecessor, salt});
    self.schedule(id, delay);

    var index = 0;
    var cc = Iterator<Timelock_Call>.new(calls);
    while (!cc.empty()) {
        val call = cc.next();
        self.checkFunctionSelectorNotBlocked(call.data.beginParse());

        val body = Timelock_CallScheduled{
            queryId,
            id,
            index,
            call: call.toCell(),
            predecessor,
            salt,
            delay
        };
        emit(TOPIC_CALL_SCHEDULED, body);

        index += 1;
    }

    // Reply back to sender
    createMessage({
        bounce: false,
        value: DEFAULT_MESSAGE_BUDGET_FEE, // send small value as gas budget, keep the rest
        dest: sender,
        body: Timelock_BatchScheduled{queryId, id, delay}
    }).send(SEND_MODE_REGULAR);
}

/// @dev Schedule an operation that becomes valid after a given delay.
fun Timelock<T>.schedule(mutate self, id: uint256, delay: uint64): void {
    assert(!self.isOperation(id)) throw ERROR_OPERATION_ALREADY_SCHEDULED;
    assert(delay >= self.getMinDelay()) throw ERROR_INSUFFICIENT_DELAY;

    val value = beginCell().storeUint(blockchain.now() + delay, 64).endCell().beginParse();
    self.data.timestamps.uDictSet(256, id, value); // seconds since epoch
}

/// @dev Cancel an operation.
///
/// Requirements:
///
/// - the caller must have the 'canceller' or 'admin' role.
///
fun Timelock<T>.cancel(mutate self, sender: address, queryId: uint64, id: uint256): void {
    self.onlyRoleOrAdminRole(CANCELLER_ROLE, sender);
    assert(self.isOperationPending(id)) throw ERROR_OPERATION_CAN_NOT_BE_CANCELLED;

    // Remove the timestamp for the operation
    self.data.timestamps.uDictDelete(256, id);

    // Reply back to sender
    createMessage({
        bounce: false,
        value: 0,
        dest: sender,
        body: Timelock_Canceled{queryId, id}
    }).send(SEND_MODE_REGULAR | SEND_MODE_CARRY_ALL_REMAINING_MESSAGE_VALUE);
}

/// @dev Execute an (ready) operation containing a batch of transactions.
/// Note that we perform a raw call to each target. Raw calls to targets that
/// don't have associated contract code will always succeed regardless of
/// payload.
///
/// Emits one {Timelock_CallExecuted} event per transaction in the batch.
///
/// Requirements:
///
/// - the caller must have the 'executor' or 'admin' role.
///
fun Timelock<T>.executeBatch(
    mutate self,
    sender: address,
    queryId: uint64,
    calls: cell, // vec<Timelock_Call>
    predecessor: uint256,
    salt: uint256
): void {
    self.onlyRoleOrAdminRole(EXECUTOR_ROLE, sender);

    val id = self.hashOperationBatch(OperationBatch{calls, predecessor, salt});
    self.beforeCall(id, predecessor);

    var index = 0;
    var cc = Iterator<Timelock_Call>.new(calls);
    while (!cc.empty()) {
        val call = cc.next();
        self.execute(call);

        val body = Timelock_CallExecuted{
            queryId,
            id,
            index,
            target: call.target,
            value: call.value,
            data: call.data
        };
        emit(TOPIC_CALL_EXECUTED, body);

        index += 1;
    }

    self.afterCall(id);

    // Reply back to sender
    createMessage({
        bounce: false,
        value: DEFAULT_MESSAGE_BUDGET_FEE, // send small value as gas budget, keep the rest
        dest: sender,
        body: Timelock_BatchExecuted{queryId, id}
    }).send(SEND_MODE_REGULAR);
}

/// @dev Execute an operation's call.
fun Timelock<T>.execute(self, call: Timelock_Call): void {
    // TODO: notice, we can not require success before continuing with the next call (async execution model)
    createMessage({
        bounce: true, // TODO: how to handle bounced messages (errors)?
        value: call.value,
        dest: call.target,
        body: call.data
    }).send(SEND_MODE_PAY_FEES_SEPARATELY);
}

/// @dev Hook before execution of an operation's calls.
fun Timelock<T>.beforeCall(self, id: uint256, predecessor: uint256): void {
    assert(self.isOperationReady(id)) throw ERROR_OPERATION_NOT_READY;
    assert(predecessor == 0 || self.isOperationDone(predecessor)) throw ERROR_OPERATION_MISSING_DEPENDENCY;

    // We need to reserve TON coins on the balance to pay for storage fees
    reserveToncoinsOnBalance(MIN_TON_FOR_STORAGE, RESERVE_MODE_EXACT_AMOUNT);
}

/// @dev Hook after execution of an operation's calls.
fun Timelock<T>.afterCall(mutate self, id: uint256): void {
    assert(self.isOperationReady(id)) throw ERROR_OPERATION_NOT_READY;

    val value = beginCell().storeUint(DONE_TIMESTAMP, 64).endCell().beginParse();
    self.data.timestamps.uDictSet(256, id, value); // Mark the operation as done
}

/// @dev Changes the minimum timelock duration for future operations.
///
/// Emits a {Timelock_MinDelayChange} event.
///
/// Requirements:
///
/// - the caller must have the 'admin' role.
///
fun Timelock<T>.updateDelay(mutate self, sender: address, queryId: uint64, newDelay: uint64): void {
    val rbac = self.hookTrait__AccessControl();
    rbac.requireRole(ADMIN_ROLE, sender);

    // Send a message to the sender to notify about the change
    createMessage({
        bounce: false,
        value: 0,
        dest: sender,
        body: Timelock_MinDelayChange{
            queryId,
            oldDuration: self.data.minDelay,
            newDuration: newDelay
        }
    }).send(SEND_MODE_REGULAR | SEND_MODE_CARRY_ALL_REMAINING_MESSAGE_VALUE);

    // Update the minimum delay
    self.data.minDelay = newDelay;
}

/// @dev Blocks a function selector from being used, i.e. schedule
/// operations with this function selector will revert.
///
/// Note that blocked selectors are only checked when an operation is being
/// scheduled, not when it is executed. You may want to check any pending
/// operations for whether they contain the blocked selector and cancel them.
///
/// Requirements:
///
/// - the caller must have the 'admin' role.
///
fun Timelock<T>.blockFunctionSelector(mutate self, sender: address, queryId: uint64, selector: uint32): bool {
    val rbac = self.hookTrait__AccessControl();
    rbac.requireRole(ADMIN_ROLE, sender);

    val updated = self.data.blockedFnSelectors.uDictSetIfNotExists(LEN_BLOCKED_FN_SELECTOR, selector, createEmptySlice());
    if (updated) {
        self.data.blockedFnSelectorsLen += 1;

        createMessage({
            bounce: false,
            value: 0,
            dest: sender,
            body: Timelock_FunctionSelectorBlocked{queryId, selector}
        }).send(SEND_MODE_REGULAR | SEND_MODE_CARRY_ALL_REMAINING_MESSAGE_VALUE);

        return true;
    }

    return false;
}

/// @dev Unblocks a previously blocked function selector so it can be used again.
///
/// Requirements:
///
/// - the caller must have the 'admin' role.
///
fun Timelock<T>.unblockFunctionSelector(mutate self, sender: address, queryId: uint64, selector: uint32): bool {
    val rbac = self.hookTrait__AccessControl();
    rbac.requireRole(ADMIN_ROLE, sender);

    val updated = self.data.blockedFnSelectors.uDictDelete(LEN_BLOCKED_FN_SELECTOR, selector);
    if (updated) {
        self.data.blockedFnSelectorsLen -= 1;

        // Reply to sender
        createMessage({
            bounce: false,
            value: 0,
            dest: sender,
            body: Timelock_FunctionSelectorUnblocked{queryId, selector}
        }).send(SEND_MODE_REGULAR | SEND_MODE_CARRY_ALL_REMAINING_MESSAGE_VALUE);

        return true;
    }

    return false;
}

/// @dev Directly execute a batch of transactions, bypassing any other checks.
///
/// Emits one {Timelock_BypasserCallExecuted} event per transaction in the batch.
///
/// Requirements:
///
/// - the caller must have the 'bypasser' or 'admin' role.
///
fun Timelock<T>.bypasserExecuteBatch(
    mutate self,
    sender: address,
    queryId: uint64,
    calls: cell, // vec<Timelock_Call>
): void {
    self.onlyRoleOrAdminRole(BYPASSER_ROLE, sender);

    var index = 0;
    var cc = Iterator<Timelock_Call>.new(calls);
    while (!cc.empty()) {
        val call = cc.next();
        self.execute(call);

        val body = Timelock_BypasserCallExecuted{
            queryId,
            index,
            target: call.target,
            value: call.value,
            data: call.data
        };
        emit(TOPIC_BYPASSER_CALL_EXECUTED, body);
        index += 1;
    }

    // Reply back to sender
    createMessage({
        bounce: false,
        value: DEFAULT_MESSAGE_BUDGET_FEE, // send small value as gas budget, keep the rest
        dest: sender,
        body: Timelock_BypasserBatchExecuted{queryId}
    }).send(SEND_MODE_REGULAR);
}

/// @dev Returns the number of blocked function selectors.
fun Timelock<T>.getBlockedFunctionSelectorCount(self): uint32 {
    return self.data.blockedFnSelectorsLen;
}

/// @dev Returns the blocked function selector with the given index.
///
/// This function is useful for enumerating all accounts that have a role, but is expensive
/// in terms of gas. It is recommended to use {getBlockedFunctionSelectorFirst} and {getBlockedFunctionSelectorNext}
/// to iterate through the accounts in a more gas-efficient way.
///
/// Function selectors are not sorted in any particular way, and their ordering may
/// change at any point.
///
/// WARNING: When using {getBlockedFunctionSelectorCount} and
/// {getBlockedFunctionSelectorAt} via RPC, make sure you perform all queries
/// on the same block. When using these functions within an onchain
/// transaction, make sure that the state of this contract hasn't changed in
/// between invocations to avoid time-of-check time-of-use bugs.
///
/// See the following
/// https://forum.openzeppelin.com/t/iterating-over-elements-on-enumerableset-in-openzeppelin-contracts/2296[forum
/// post] for more information.
///
@inline
fun Timelock<T>.getBlockedFunctionSelectorAt(self, index: uint32): uint32 {
    if (index >= self.data.blockedFnSelectorsLen) {
        return 0; // index out of bounds
    }

    // Get a mutable reference to the blocked function selectors map (used for iteration)
    var blockedFnSelectors = self.data.blockedFnSelectors;

    // Notice: we need to iterate through the dictionary to get the item at the specified index.
    // This is not efficient, but it is the only way to get an item at index without additional data structures.
    var i = 0;
    while (i < index) {
        blockedFnSelectors.uDictDeleteFirstAndGet(LEN_BLOCKED_FN_SELECTOR);
        i += 1; // increment to next account
    }

    // Get the item at the specified index
    var (selector: int?, _: slice?, _: bool) = blockedFnSelectors.uDictDeleteFirstAndGet(LEN_BLOCKED_FN_SELECTOR);
    return selector! as uint32;
}

@inline
fun Timelock<T>.getBlockedFunctionSelectorFirst(self): uint32 {
    if (self.data.blockedFnSelectorsLen == 0) {
        return 0; // no blocked function selectors
    }

    // Get the first item in the map
    var (selector, _, _) = self.data.blockedFnSelectors.uDictGetFirst(LEN_BLOCKED_FN_SELECTOR);
    return selector! as uint32;
}

@inline
fun Timelock<T>.getBlockedFunctionSelectorNext(self, pivot: uint32): uint32 {
    // Get the next item in the map, after the pivot
    var (selector, _, exists) = self.data.blockedFnSelectors.uDictGetNext(LEN_BLOCKED_FN_SELECTOR, pivot);
    if (!exists) {
        return 0; // no next item
    }

    return selector! as uint32;
}

/// @dev Checks to see if the function being scheduled is blocked. 
@inline
fun Timelock<T>.checkFunctionSelectorNotBlocked(self, data: slice): void {
    if (data.isEndOfBits() || data.remainingBitsCount() < 32) { // ignore all empty messages
        return;
    }

    // Load the function selector from the message body
    val selector = data.loadMessageOp(); // 32-bit integer
    var (_, exists) = self.data.blockedFnSelectors.uDictGet(LEN_BLOCKED_FN_SELECTOR, selector);

    if (exists) {
        throw(ERROR_SELECTOR_IS_BLOCKED); // TODO: attach error arguments
    }
}

// --- Message handlers ---

@inline
fun Timelock<T>.onInternalMessage(mutate self, msgSender: address, msgValue: coins, msgBody: slice) {
    val msg = lazy Timelock_InMessage.fromSlice(msgBody);
    match (msg) {
        Timelock_Init => {
            self.init(msgSender, msg.queryId, msg.minDelay, msg.admin, msg.proposers, msg.executors, msg.cancellers, msg.bypassers);
        }
        Timelock_TopUp => {
            // Top up the contract balance, no action needed, return early - no need to update storage
            return;
        }
        Timelock_ScheduleBatch => {
            self.scheduleBatch(msgSender, msg.queryId, msg.calls, msg.predecessor, msg.salt, msg.delay);
        }
        Timelock_Cancel => {
            self.cancel(msgSender, msg.queryId, msg.id);
        }
        Timelock_ExecuteBatch => {
            self.executeBatch(msgSender, msg.queryId, msg.calls, msg.predecessor, msg.salt);
        }
        Timelock_UpdateDelay => {
            self.updateDelay(msgSender, msg.queryId, msg.newDelay);
        }
        Timelock_BlockFunctionSelector => {
            self.blockFunctionSelector(msgSender, msg.queryId, msg.selector);
        }
        Timelock_UnblockFunctionSelector => {
            self.unblockFunctionSelector(msgSender, msg.queryId, msg.selector);
        }
        Timelock_BypasserExecuteBatch => {
            self.bypasserExecuteBatch(msgSender, msg.queryId, msg.calls);
        }
        else => {
            var rbac = self.hookTrait__AccessControl();

            val msgHandled = 0
                || rbac.onInternalMessage(msgSender, msgValue, msgBody);

            if (!msgHandled) {
                // If the message was not handled, throw an error
                throw ERROR_WRONG_OP;
            }

            // Map from underlying trait storage to contract storage
            self.data.rbac = rbac.data.toCell();
        }
    }
}

// TODO: handle bounced messages
fun onInternalMessage(in: InMessage) {
    // Load the contract storage as Timelock and handle the message
    var timelock = Timelock<bool>.load();
    timelock.onInternalMessage(in.senderAddress, in.valueCoins, in.body);

    // Store the updated contract data
    timelock.data.storeAsContractData();
}

// --- Extension hooks ---

@inline
fun Timelock<T>.hookTrait__AccessControl(self): AccessControl<T>{
    // Construct trait instance (with AccessControl<Timelock>)
    return {
        data: AccessControl_Data.fromCell(self.data.rbac),
        // Set up runtime hooks (extensions)
        context: self.context,
        hooks: self.hooks != null ? self.hooks!.rbac : null,
    };
}

// --- Getters ---

get fun typeAndVersion(): (slice, slice) {
     return ("com.chainlink.ton.mcms.Timelock", "1.0.0");
}

/// @see <Timelock<T>.isOperation>
get fun isOperation(id: int): bool {
    return Timelock<bool>.load().isOperation(id);
}

/// @see <Timelock<T>.isOperationPending>
get fun isOperationPending(id: int): bool {
    return Timelock<bool>.load().isOperationPending(id);
}

/// @see <Timelock<T>.isOperationReady>
get fun isOperationReady(id: int): bool {
    return Timelock<bool>.load().isOperationReady(id);
}

/// @see <Timelock<T>.isOperationDone>
get fun isOperationDone(id: int): bool {
    return Timelock<bool>.load().isOperationDone(id);
}

/// @see <Timelock<T>.getTimestamp>
get fun getTimestamp(id: int): uint64 {
    return Timelock<bool>.load().getTimestamp(id);
}

/// @see <Timelock<T>.getMinDelay>
get fun getMinDelay(): uint64 {
    return Timelock<bool>.load().getMinDelay();
}

/// @see <Timelock<T>.hashOperationBatch>
get fun hashOperationBatch(op: OperationBatch): uint256 {
    return Timelock<bool>.load().hashOperationBatch(op);
}

/// @see <Timelock<T>.getBlockedFunctionSelectorCount>
get fun getBlockedFunctionSelectorCount(): uint32 {
    return Timelock<bool>.load().getBlockedFunctionSelectorCount();
}

/// @see <Timelock<T>.getBlockedFunctionSelectorAt>
get fun getBlockedFunctionSelectorAt(index: uint32): uint32 {
    return Timelock<bool>.load().getBlockedFunctionSelectorAt(index);
}

// TODO: should we also expose <Timelock<T>._checkFunctionSelectorNotBlocked (not in EVM version)?

// --- Getters - <AccessControl<T>> extension ---

/// @see <AccessControl<T>.hasRole>
get fun hasRole(role: int, account: address): bool {
    val timelock = Timelock<bool>.load();
    val rbac = timelock.hookTrait__AccessControl();
    return rbac.hasRole(role, account);
}

/// @see <AccessControl<T>.getRoleAdmin>
get fun getRoleAdmin(role: int): int {
    val timelock = Timelock<bool>.load();
    val rbac = timelock.hookTrait__AccessControl();
    return rbac.getRoleAdmin(role);
}

// --- Getters - <AccessControl<T>> enumerable extension ---

/// @see <AccessControl<T>.getRoleMember>
get fun getRoleMember(role: int, index: uint32): address {
    val rbac = Timelock<bool>.load().hookTrait__AccessControl();
    return rbac.getRoleMember(role, index);
}

/// @see <AccessControl<T>.getRoleMemberFirst>
get fun getRoleMemberFirst(role: int): address {
    val rbac = Timelock<bool>.load().hookTrait__AccessControl();
    return rbac.getRoleMemberFirst(role);
}

/// @see <AccessControl<T>.getRoleMemberNext>
get fun getRoleMemberNext(role: int, pivot: address): address {
    val rbac = Timelock<bool>.load().hookTrait__AccessControl();
    return rbac.getRoleMemberNext(role, pivot);
}

/// @see <AccessControl<T>.getRoleMemberCount>
get fun getRoleMemberCount(role: int): int {
    val rbac = Timelock<bool>.load().hookTrait__AccessControl();
    return rbac.getRoleMemberCount(role);
}

/// @see <AccessControl<T>.getRoleMembers>
get fun getRoleMembers(role: int): dict {
    val rbac = Timelock<bool>.load().hookTrait__AccessControl();
    return rbac.getRoleMembers(role);
}
