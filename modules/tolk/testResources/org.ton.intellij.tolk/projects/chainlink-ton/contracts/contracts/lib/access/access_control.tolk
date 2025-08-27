tolk 1.0

import "@stdlib/tvm-dicts";
import "../utils.tolk";

/**
 * @dev Contract module that allows children to implement role-based access
 * control mechanisms. This version is a port of OpenZeppelin {access/AccessControl.sol}
 * and {access/extensions/AccessControlEnumerable.sol}, merged to allow enumerating role
 * members as TVM dictionaries are iterable.
 *
 * Roles are referred to by their `int` identifier. These should be exposed
 * in the external API and be unique. The best way to achieve this is by
 * using `const` hash digests (256 bit):
 *
 * ```tolk
 * const DEFAULT_ADMIN_ROLE = 0x00;
 * ```
 *
 * Roles can be used to represent a set of permissions. To restrict access to a
 * function call, use {requireRole}:
 *
 * ```tolk
 * fun foo(rbac: AccessControl<T>, acc: address) {
 *     rbac.requireRole(MY_ROLE, acc));
 *     ...
 * }
 * ```
 *
 * Roles can be granted and revoked dynamically via the {AccessControl_GrantRole} and
 * {AccessControl_RevokeRole} messages. Each role has an associated admin role, and only
 * accounts that have a role's admin role can call {grantRole} and {revokeRole}.
 *
 * By default, the admin role for all roles is `DEFAULT_ADMIN_ROLE`, which means
 * that only accounts with this role will be able to grant or revoke other
 * roles. More complex role relationships can be created by using
 * {_setRoleAdmin}.
 *
 * WARNING: The `DEFAULT_ADMIN_ROLE` is also its own admin: it has permission to
 * grant and revoke this role. Extra precautions should be taken to secure
 * accounts that have been granted it.
 */

// --- Messages - incoming ---

/// @dev Grants `role` to `account`.
///
/// If `account` had not been already granted `role`, sends a {AccessControl_RoleGranted} message.
///
/// Requirements:
///
/// - the caller must have `role`'s admin role.
///
/// May send a {AccessControl_RoleGranted} message.
///
struct (0x95cd540f) AccessControl_GrantRole {
    /// Query ID of the change owner request.
    queryId: uint64;

    /// Role definition.
    role: uint256;
    /// New account to add.
    account: address;
}

/// @dev Revokes `role` from `account`.
///
/// If `account` had been granted `role`, sends a {AccessControl_RoleRevoked} message.
///
/// Requirements:
///
/// - the caller must have `role`'s admin role.
///
/// May send a {AccessControl_RoleRevoked} message.
///
struct (0x969b0db9) AccessControl_RevokeRole {
    /// Query ID of the change owner request.
    queryId: uint64;

    /// Role definition.
    role: uint256;
    /// Account to revoke.
    account: address;
}

/// @dev Revokes `role` from the calling account.
///
/// Roles are often managed via {AccessControl_GrantRole} and
/// {AccessControl_RevokeRole}: this function's purpose is to provide a
/// mechanism for accounts to lose their privileges if they are
/// compromised (such as when a trusted device is misplaced).
///
/// If `account` had been granted `role`, sends a {AccessControl_RoleRevoked} message.
///
/// Requirements:
///
/// - the caller must be `callerConfirmation`.
///
/// May send a {AccessControl_RoleRevoked} message.
///
struct (0x39452c46) AccessControl_RenounceRole {
    /// Query ID of the change owner request.
    queryId: uint64;

    /// Role definition.
    role: uint256;
    /// Account to revoke.
    callerConfirmation: address;
}

/// @dev Union of all incoming messages.
type AccessControl_InMessage = AccessControl_GrantRole
    | AccessControl_RevokeRole
    | AccessControl_RenounceRole;

// --- Messages - outgoing ---

/// @dev Sent when `account` is granted `role`.
///
/// `sender` is the account that originated the contract call. This account bears the admin role (for the granted role).
/// Expected in cases where the role was granted using the internal {AccessControl-_grantRole}.
///
struct (0xcf3ca837) AccessControl_RoleGranted {
    /// Query ID of the change owner request.
    queryId: uint64;

    /// Role definition.
    role: uint256;
    /// New account added.
    account: address;
    /// Account that requested the change.
    sender: address;
}

/// @dev Sent when `account` is revoked `role`.
///
/// `sender` is the account that originated the contract call:
///  - if using `revokeRole`, it is the admin role bearer
/// - if using `renounceRole`, it is the role bearer (i.e. `account`)
///
struct (0x990fe1c7) AccessControl_RoleRevoked {
    /// Query ID of the change owner request.
    queryId: uint64;

    /// Role definition.
    role: uint256;
    /// Account revoked.
    account: address;
    /// Account that requested the change.
    sender: address;
}

/// @dev Sent when `newAdminRole` is set as ``role``'s admin role, replacing `previousAdminRole`
///
/// `DEFAULT_ADMIN_ROLE` is the starting admin for all roles, despite
/// {AccessControl_RoleAdminChanged} not being sent to signal this.
///
struct (0xbd7e8bce) AccessControl_RoleAdminChanged {
    /// Query ID of the change owner request.
    queryId: uint64;

    /// Role definition.
    role: uint256;
    /// Previous admin role of the specific role.
    previousAdminRole: uint256;
    /// New admin role of the specific role.
    newAdminRole: uint256;
}

// --- Storage ---

/// AccessControl contract struct, host storage and runtime hooks (extensions).
struct AccessControl<T> {
    data: AccessControl_Data;

    /// Runtime hooks (extensions)
    context: T?;
    hooks: AccessControl_Hooks<T>?;
}

/// AccessControl data struct, auto-serialized to/from cell.
struct AccessControl_Data {
    roles: dict; // map<int, AccessControl_RoleData>
}

/// Internal storage struct for role data
///
/// Each role has a mapping of accounts that have been granted that role,
/// and an admin role that can manage that role. 
struct AccessControl_RoleData {
    adminRole: uint256;

    // Number of members in the role
    membersLen: uint64;
    // Members of the role, indexed by their address hash.
    hasRole: dict; // map<address, slice>
}

// --- Hooks struct (extensions) ---

/// @dev Hook extensions exposed by the AccessControl contract.
struct AccessControl_Hooks<T> {
    /// Hook to get the sender of the message.
    msgSender: ((T?, address, slice) -> address)?;

    /// Hook before granting a role.
    shouldGrantRole: ((T?, uint64, int, address) -> void)?;

    /// Hook before revoking a role.
    shouldRevokeRole: ((T?, uint64, int, address) -> void)?;
}

// --- Constants ---

const DEFAULT_ADMIN_ROLE = 0x00;

const ERROR_ACCESS_CONTROL_UNAUTHORIZED_ACCOUNT = 90;
const ERROR_ACCESS_CONTROL_BAD_CONFIRMATION = 91;

// --- Constants - storage ---

const LEN_ADDRESS = 267;  // '100' (std addr no anycast) + workchain 8 + hash LEN_ROLE
const LEN_ROLE = 256; // LEN_ROLE bits for role ID

// --- Methods ---

/// @dev Returns the sender of the message.
///
/// Override available via {AccessControl_Hooks<T>.msgSender} hook.
fun AccessControl<T>.msgSender(self, msgSender: address, msgBody: slice): address {
    if (self.hooks != null && self.hooks.msgSender != null) {
        return self.hooks.msgSender(self.context, msgSender, msgBody); // extension hook
    }

    // Default implementation: return the address from the message sender
    return msgSender;
}

/// @dev Modifier that checks that an account has a specific role. Reverts
/// with a standardized message including the required role.
///
/// Format of the revert message is described in {requireRole}.
@inline
fun AccessControl<T>.onlyRole(self, role: int, msgFull: cell): void {
    self.requireRoleSender(role, msgFull)
}

// @dev Returns `true` if `account` has been granted `role`.
fun AccessControl<T>.hasRole(self, role: int, account: address): bool {
    // Check if the role is valid
    val (data, exists) = self.data.roles.uDictGetRef(LEN_ROLE, role);
    if (!exists) {
        return false;
    }

    // Check if the account exists in the role data
    val d = lazy AccessControl_RoleData.fromCell(data!);
    val (_, accExists) = d.hasRole.sDictGet(LEN_ADDRESS, account as slice);
    return accExists;
}

/// @dev Revert with a standard message if `msgSender()` is missing `role`.
///
/// Format of the revert message is described in {requireRole}.
fun AccessControl<T>.requireRoleSender(self, role: int, msgFull: cell): void {
    val sender = self.msgSender(msgFull);
    self.requireRole(role, sender);
}

/// @dev Revert with a standard message if `account` is missing `role`.
///
/// The format of the revert reason is given by the following regular expression:
///
/// /^AccessControl: account (0x[0-9a-f]{40}) is missing role (0x[0-9a-f]{64})$/
///
fun AccessControl<T>.requireRole(self, role: int, account: address): void {
    if (!self.hasRole(role, account)) {
        throw(ERROR_ACCESS_CONTROL_UNAUTHORIZED_ACCOUNT); // TODO: attach error arguments
    }
}

/// @dev Returns the admin role that controls `role`. See {grantRole} and {revokeRole}.
///
/// To change a role's admin, use {setRoleAdmin}.
fun AccessControl<T>.getRoleAdmin(self, role: int): int {
    val (data, exists) = self.data.roles.uDictGetRef(LEN_ROLE, role);
    if (!exists) {
        return DEFAULT_ADMIN_ROLE;
    }

    val d = lazy AccessControl_RoleData.fromCell(data!);
    return d.adminRole;
}

// --- Methods - <AccessControl<T>> enumerable extension ---

/// @dev Returns one of the accounts that have `role`. `index` must be a
/// value between 0 and {getRoleMemberCount}, non-inclusive.
///
/// This function is useful for enumerating all accounts that have a role, but is expensive
/// in terms of gas. It is recommended to use {getRoleMemberFirst} and {getRoleMemberNext}
/// to iterate through the accounts in a more gas-efficient way.
///
/// Role bearers are not sorted in any particular way, and their ordering may change at any point.
@inline
fun AccessControl<T>.getRoleMember(self, role: int, index: uint32): address {
    // Check if the role is valid
    val (data, exists) = self.data.roles.uDictGetRef(LEN_ROLE, role);
    if (!exists) {
        return createAddressNone(); // role does not exist
    }

    // Check if the account exists in the role data
    var d = lazy AccessControl_RoleData.fromCell(data!);
    if (index >= d.membersLen) {
        return createAddressNone(); // index out of bounds
    }

    // Notice: we need to iterate through the dictionary to get the account at the specified index.
    // This is not efficient, but it is the only way to get an account at index without additional data structures.
    var i = 0;
    while (i < index) {
        d.hasRole.sDictDeleteFirstAndGet(LEN_ADDRESS);
        i += 1; // increment to next account
    }

    // Get the account at the specified index
    var (acc: slice?, _: slice?, _: bool) = d.hasRole.sDictDeleteFirstAndGet(LEN_ADDRESS);
    return acc! as address;
}

/// @dev Returns the number of accounts that have `role`. Can be used
/// together with {getRoleMember} to enumerate all bearers of a role.
@inline
fun AccessControl<T>.getRoleMemberCount(self, role: int): int {
    // Check if the role is valid
    val (data, exists) = self.data.roles.uDictGetRef(LEN_ROLE, role);
    if (!exists) {
        return 0; // role does not exist
    }

    val d = lazy AccessControl_RoleData.fromCell(data!);
    return d.membersLen;
}

@inline
fun AccessControl<T>.getRoleMemberFirst(self, role: int): address {
    // Check if the role is valid
    val (data, exists) = self.data.roles.uDictGetRef(LEN_ROLE, role);
    if (!exists) {
        return createAddressNone(); // role does not exist
    }

    val d = lazy AccessControl_RoleData.fromCell(data!);
    if (d.membersLen == 0) {
        return createAddressNone(); // no members in the role
    }

    // Get the first account in the role data
    var (acc, _, _) = d.hasRole.sDictGetFirst(LEN_ADDRESS);
    return acc! as address;
}

@inline
fun AccessControl<T>.getRoleMemberNext(self, role: int, pivot: address): address {
    // Check if the role is valid
    val (data, exists) = self.data.roles.uDictGetRef(LEN_ROLE, role);
    if (!exists) {
        return createAddressNone(); // role does not exist
    }

    // Get the next account in the role data after the pivot
    val d = lazy AccessControl_RoleData.fromCell(data!);
    var (acc, _, accExists) = d.hasRole.sDictGetNext(LEN_ADDRESS, pivot as slice);
    if (!accExists) {
        return createAddressNone(); // no next account
    }

    return acc! as address;
}

/// @dev Return all accounts that have `role`.
@inline
fun AccessControl<T>.getRoleMembers(self, role: int): dict {
    // Check if the role is valid
    val (data, exists) = self.data.roles.uDictGetRef(LEN_ROLE, role);
    if (!exists) {
        return createEmptyDict(); // role does not exist
    }

    val d = lazy AccessControl_RoleData.fromCell(data!);
    return d.hasRole;
}

// --- Methods - mutations ---

/// @dev Grants `role` to `account`.
///
/// Requirements:
/// - the `sender` must have `role`'s admin role.
///
/// Sends a {AccessControl_RoleGranted} event (to: sender, account) if `role` was granted.
fun AccessControl<T>.grantRole(
    mutate self,
    queryId: uint64,
    sender: address,
    role: int,
    account: address,
    returnExcess: bool = false
): bool {
    self.requireRole(self.getRoleAdmin(role), sender);
    return self._grantRole(queryId, sender, role, account, returnExcess);
}

/// @dev Revokes `role` from `account`.
///
/// Requirements:
/// - the `sender` must have `role`'s admin role.
///
/// Sends a {AccessControl_RoleRevoked} event (to: sender, account) if `role` was revoked.
fun AccessControl<T>.revokeRole(
    mutate self,
    queryId: uint64,
    sender: address,
    role: int,
    account: address,
    returnExcess: bool = false
): bool {
    self.requireRole(self.getRoleAdmin(role), sender);
    return self._revokeRole(queryId, sender, role, account, returnExcess);
}

/// @dev Revokes `role` from the calling account.
///
/// Roles are often managed via {grantRole} and {revokeRole}: this function's
/// purpose is to provide a mechanism for accounts to lose their privileges
/// if they are compromised (such as when a trusted device is misplaced).
///
/// If the calling account had been revoked `role`, sends a {AccessControl_RoleRevoked} message.
///
/// Requirements:
///
/// - the `sender` must be `callerConfirmation`.
///
/// Sends a {AccessControl_RoleRevoked} event (to: sender, account) if `role` was revoked.
fun AccessControl<T>.renounceRole(
    mutate self,
    queryId: uint64,
    sender: address,
    role: int,
    callerConfirmation: address,
    returnExcess: bool = false
): bool {
    if (callerConfirmation != sender) {
        throw(ERROR_ACCESS_CONTROL_BAD_CONFIRMATION); // TODO: attach error arguments
    }

    // Renouncing a role is equivalent to revoking it from the caller
    return self.revokeRole(queryId, sender, role, callerConfirmation, returnExcess);
}

/// @dev Sets `adminRole` as `role`'s admin role.
///
/// Internal function without access restriction.
///
/// Sends a {AccessControl_RoleAdminChanged} reply to sender.
fun AccessControl<T>._setRoleAdmin(
    mutate self,
    queryId: uint64,
    sender: address,
    role: int,
    adminRole: int,
    returnExcess: bool = false
): void {
    var d: AccessControl_RoleData = self.data.getRoleDataOrSetDefault(role);
    val previousAdminRole = d.adminRole;
    // Update the admin role for the role
    d.adminRole = adminRole;

    // Save the updated role data
    self.data.roles.uDictSetRef(LEN_ROLE, role, d.toCell());

    val body = AccessControl_RoleAdminChanged{queryId, role, previousAdminRole, newAdminRole: adminRole};
    self.sendReply(sender, body, DEFAULT_MESSAGE_BUDGET_FEE, returnExcess);
}

/// @dev Attempts to grant `role` to `account` and returns a boolean indicating if `role` was granted.
///
/// Internal function without access restriction.
/// Sends a {AccessControl_RoleGranted} event (to: sender, account) if `role` was granted.
fun AccessControl<T>._grantRole(
    mutate self,
    queryId: uint64,
    sender: address,
    role: int,
    account: address,
    returnExcess: bool = false
): bool {
    if (self.hooks != null && self.hooks.shouldGrantRole != null) {
        self.hooks.shouldGrantRole(self.context, queryId, role, account); // extension hook
    }

    // Default implementation of granting a role
    var d: AccessControl_RoleData = self.data.getRoleDataOrSetDefault(role);

    // Check if the account exists in the role data
    val (_, exists) = d.hasRole.sDictGet(LEN_ADDRESS, account as slice);

    if (exists) {
        return false; // nothing to do
    }

    // Add the account to the role
    d.hasRole.sDictSet(LEN_ADDRESS, account as slice, createEmptySlice());
    d.membersLen += 1; // increment the number of members in the role

    // Save the updated role data
    self.data.roles.uDictSetRef(LEN_ROLE, role, d.toCell());

    val body = AccessControl_RoleGranted{queryId, role, account, sender};
    self.sendNotification(sender, body, DEFAULT_MESSAGE_BUDGET_FEE, account);
    self.sendReply(sender, body, DEFAULT_MESSAGE_BUDGET_FEE, returnExcess);

    return true; // role granted successfully
}

/// @dev Attempts to revoke `role` from `account` and returns a boolean indicating if `role` was revoked.
///
/// Internal function without access restriction.
/// Sends a {AccessControl_RoleRevoked} event (to: sender, account) if `role` was revoked.
fun AccessControl<T>._revokeRole(
    mutate self,
    queryId: uint64,
    sender: address,
    role: int,
    account: address,
    returnExcess: bool = false
): bool {
    if (self.hooks != null && self.hooks.shouldRevokeRole != null) {
        self.hooks.shouldRevokeRole(self.context, queryId, role, account); // extension hook
    }

    // Default implementation of revoking a role
    var d: AccessControl_RoleData = self.data.getRoleDataOrSetDefault(role);

    // Check if the account exists in the role data
    val (_, exists) = d.hasRole.sDictGet(LEN_ADDRESS, account as slice);

    if (!exists) {
        return false; // nothing to do
    }

    // Remove the account from the role
    d.hasRole.sDictDelete(LEN_ADDRESS, account as slice);
    d.membersLen -= 1; // decrement the number of members in the role

    // Save the updated role data
    self.data.roles.uDictSetRef(LEN_ROLE, role, d.toCell());

    val body = AccessControl_RoleRevoked{queryId, role, account, sender};
    self.sendNotification(sender, body, DEFAULT_MESSAGE_BUDGET_FEE, account);
    self.sendReply(sender, body, DEFAULT_MESSAGE_BUDGET_FEE, returnExcess);

    return true; // role revoked successfully
}

/// --- Helpers ---

@inline
fun AccessControl_Data.getRoleDataOrSetDefault(mutate self, role: int): AccessControl_RoleData {
    // Check if the role is valid
    val (data, exists) = self.roles.uDictGetRef(LEN_ROLE, role);
    if (exists) {
        return AccessControl_RoleData.fromCell(data!);
    }

    // Create a new role data if it doesn't exist
    var _data: AccessControl_RoleData = {
        adminRole: DEFAULT_ADMIN_ROLE,
        // Initialize hasRole as an empty dictionary
        hasRole: createEmptyDict(),
        membersLen: 0,
    };
    self.roles.uDictSetRef(LEN_ROLE, role, _data.toCell());
    return _data;
}

/// @dev Sends a (notification) message to the the updated account.
@inline
fun AccessControl<T>.sendNotification<B>(
    self,
    sender: address,
    body: B,
    value: coins = DEFAULT_MESSAGE_BUDGET_FEE,
    account: address,
): void {
    // Skip notification if account == sender
    if (account != sender) {
        // Create and send the message (notification) to the account that was updated
        createMessage({ bounce: false, value, dest: account, body })
            .send(SEND_MODE_REGULAR);
    }
}

/// @dev Sends a (reply) message to the sender.
@inline
fun AccessControl<T>.sendReply<B>(
    self,
    sender: address,
    body: B,
    value: coins = DEFAULT_MESSAGE_BUDGET_FEE,
    returnExcess: bool = true
): void {
    // Create and send the message (reply) to the sender
    value = returnExcess ? ton("0") : value; // return all, or fixed compute budget

    createMessage({bounce: false, value, dest: sender, body })
        .send(returnExcess
            ? SEND_MODE_REGULAR | SEND_MODE_CARRY_ALL_REMAINING_MESSAGE_VALUE
            : SEND_MODE_REGULAR);
}

// --- Getters ---

/// @dev getter to export by the contract
@inline
fun AccessControl<T>.get_hasRole(self, role: int, account: address): bool {
   return self.hasRole(role, account)
}

/// @dev getter to export by the contract
@inline
fun AccessControl<T>.get_getRoleAdmin(self, role: int): int {
   return self.getRoleAdmin(role)
}

// --- Getters - <AccessControl<T>> enumerable extension ---

/// @dev getter to export downstream
@inline
fun AccessControl<T>.get_getRoleMember(self, role: int, index: uint32): address {
   return self.getRoleMember(role, index)
}

/// @dev getter to export downstream
@inline
fun AccessControl<T>.get_getRoleMemberCount(self, role: int): int {
   return self.getRoleMemberCount(role)
}

// @dev getter to export downstream
@inline
fun AccessControl<T>.get_getRoleMemberFirst(self, role: int): address {
   return self.getRoleMemberFirst(role)
}

/// @dev getter to export downstream
@inline
fun AccessControl<T>.get_getRoleMemberNext(self, role: int, pivot: address): address {
   return self.getRoleMemberNext(role, pivot)
}

/// @dev getter to export downstream
@inline
fun AccessControl<T>.get_getRoleMembers(self, role: int): dict {
   return self.getRoleMembers(role)
}

// --- Message handlers ---

@inline
fun AccessControl<T>.onInternalMessage(mutate self, msgSender: address, _msgValue: coins, msgBody: slice) {
    // We return excess to sender by default via message handling,
    // but users can change this behavior by setting `returnExcess`
    // to false using AccessControl methods directly.
    val returnExcess = true;
    val sender = self.msgSender(msgSender, msgBody);
    val msg = lazy AccessControl_InMessage.fromSlice(msgBody);
    match (msg) {
        AccessControl_GrantRole => {
            self.grantRole(msg.queryId, sender, msg.role, msg.account, returnExcess);
        },
        AccessControl_RevokeRole => {
            self.revokeRole(msg.queryId, sender, msg.role, msg.account, returnExcess);
        },
        AccessControl_RenounceRole => {
            self.renounceRole(msg.queryId, sender, msg.role, msg.callerConfirmation, returnExcess);
        },
        else => {
            return 0; // unknown message, not handled
        }
    };

    return -1; // handled
}
