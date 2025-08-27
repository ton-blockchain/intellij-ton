tolk 1.0

import "@stdlib/tvm-dicts";

import "../lib/utils";
import "../lib/access/ownable_2step.tolk";
import "../lib/crypto/merkle_proof.tolk";

/// @notice This is a multi-sig contract that supports signing many transactions (called "ops" in
/// the context of this contract to prevent confusion with transactions on the underlying chain)
/// targeting many chains with a single set of signatures. Authorized ops along with some metadata
/// are stored in a Merkle tree, which is generated offchain. Each op has an associated chain id,
/// ManyChainMultiSig contract address and nonce. The nonce enables us to enforce the
/// (per-ManyChainMultiSig contract instance) ordering of ops.
///
/// At any time, this contract stores at most one Merkle root. In the typical case, all ops
/// in the Merkle tree are expected to be executed before another root is set. Since the Merkle root
/// itself reveals ~ no information about the tree's contents, we take two measures to improve
/// transparency. First, we attach an expiration time to each Merkle root after which it cannot
/// be used any more. Second, we embed metadata in the tree itself that has to be proven/revealed
/// to the contract when a new root is set; the metadata contains the range of nonces (and thus
/// number of ops) in the tree intended for the ManyChainMultiSig contract instance on which the
/// root is being set.
///
/// Once a root is registered, *anyone* is allowed to furnish proofs of op inclusion in the Merkle
/// tree and execute the corresponding op. The contract enforces that ops are executed in the
/// correct order and with the correct arguments. A notable exception to this is the gas limit of
/// the call, which can be freely determined by the executor. We expect (transitive) callees to
/// implement standard behavior of simply reverting if insufficient gas is provided. In particular,
/// this means callees should not have non-reverting gas-dependent branches.
///
/// Note: In the typical case, we expect the time from a root being set to all of the ops
/// therein having been executed to be on the order of a couple of minutes.

// --- Messages - incoming ---

/// @dev Top up contract with TON coins.
/// Contract might receive/hold TON as part of the maintenance process.
///
struct (0x5f427bb3) MCMS_TopUp {
    /// Query ID of the change owner request.
    queryId: uint64;
}

/// @dev Sets a new expiring root.
///
/// @param root is the new expiring root.
/// @param validUntil is the time by which root is valid
/// @param metadata is the authenticated metadata about the root, which is stored as one of
/// the leaves.
/// @param metadataProof is the MerkleProof of inclusion of the metadata in the Merkle tree.
/// @param signatures the ECDSA signatures on (root, validUntil).
///
/// @dev the message (root, validUntil) should be signed by a sufficient set of signers.
/// This signature authenticates also the metadata.
///
/// @dev this method can be executed by anyone who has the root and valid signatures.
/// as we validate the correctness of signatures, this imposes no risk.
struct (0xe7fabde3) MCMS_SetRoot {
    /// Query ID of the change owner request.
    queryId: uint64;

    /// The new expiring root.
    root: uint256;
    /// The time by which the root is valid.
    validUntil: uint32;

    /// The metadata about the root, which is stored as one of the leaves.
    metadata: RootMetadata;
    /// The MerkleProof of inclusion of the metadata in the Merkle tree.
    metadataProof: cell; // vec<uint256>;
    /// The ECDSA signatures on (root, validUntil).
    signatures: cell; // vec<Signature>
}

/// @notice Execute the received op after verifying the proof of its inclusion in the
/// current Merkle tree. The op should be the next op according to the order
/// enforced by the merkle tree whose root is stored in data.expiringRootAndOpCount, i.e., the
/// nonce of the op should be equal to data.expiringRootAndOpCount.opCount.
///
/// @param op is Op to be executed
/// @param proof is the MerkleProof for the op's inclusion in the MerkleTree which its
/// root is the data.expiringRootAndOpCount.root.
///
/// @dev ANYONE can call this function! That's intentional. Callers can only execute verified,
/// ordered ops in the Merkle tree.
///
/// @dev we perform a raw call to each target. Raw calls to targets that don't have associated
/// contract code will always succeed regardless of data.
///
/// @dev the gas limit of the call can be freely determined by the caller of this function.
/// We expect callees to revert if they run out of gas.
struct (0x9b9ce96a) MCMS_Execute {
    /// Query ID of the change owner request.
    queryId: uint64;

    /// The op to be executed.
    op: Cell<Op>; // Notice: split out as cell to avoid size limits
    /// The MerkleProof for the op's inclusion in the MerkleTree
    proof: cell; // vec<uint256>,
}

/// @notice sets a new data.config. If clearRoot is true, then it also invalidates
/// data.expiringRootAndOpCount.root.
///
/// @param signerKeys holds the public keys of the active signers. The keys must be in
/// ascending order.
/// @param signerGroups maps each signer to its group
/// @param groupQuorums holds the required number of valid signatures in each group.
/// A group i is called successful group if at least groupQuorum[i] distinct signers provide a
/// valid signature.
/// @param groupParents holds each group's parent. The groups must be arranged in a tree s.t.
/// group 0 is the root of the tree and the i-th group's parent has index j less than i.
/// Iff setRoot is called with a set of signatures that causes the root group to be successful,
/// setRoot allows a root to be set.
/// @param clearRoot, if set to true, invalidates the current root. This option is needed to
/// invalidate the current root, so to prevent further ops from being executed. This
/// might be used when the current root was signed under a loser group configuration or when
/// some previous signers aren't trusted any more.
struct (0x89277f4b) MCMS_SetConfig {
    /// Query ID of the change owner request.
    queryId: uint64;

    signerKeys: cell;   // vec<uint256> (public keys)
    signerGroups: cell; // vec<uint8>
    groupQuorums: dict; // map<uint8, uint8> (indexed, iterable backwards)
    groupParents: dict; // map<uint8, uint8> (indexed, iterable backwards)
    clearRoot: bool;
}

/// @dev Union of all incoming messages.
type MCMS_InMessage = MCMS_TopUp
    | MCMS_SetRoot
    | MCMS_Execute
    | MCMS_SetConfig;

// --- Messages - outgoing ---

/// @notice Emitted when a new root is set.
struct (0xa6533a3d) MCMS_NewRoot {
    /// Query ID of the change owner request.
    queryId: uint64;

    /// The new expiring root.
    root: uint256;
    /// The time by which the root is valid.
    validUntil: uint32;
    /// The metadata about the root, which is stored as one of the leaves.
    metadata: RootMetadata;
}

/// @notice Emitted when a new config is set.
struct (0xd80be574) MCMS_ConfigSet {
    /// Query ID of the change owner request.
    queryId: uint64;

    /// The new config.
    config: Config;
    /// Whether the root was cleared.
    isRootCleared: bool;
}

/// @notice Emitted when an op gets successfully executed.
struct (0x7cf37cbf) MCMS_OpExecuted {
    /// Query ID of the change owner request.
    queryId: uint64;

    /// The nonce of the operation.
    nonce: uint40;
    /// The address to which the operation is directed.
    to: address;
    /// The data to be sent with the operation.
    data: cell; // body
    /// The value to be sent with the operation.
    value: coins;
}

/// @dev Union of all outgoing messages.
type MCMS_OutMessage = MCMS_NewRoot
    | MCMS_ConfigSet
    | MCMS_OpExecuted;

// --- Storage ---

/// MCMS contract struct, host storage and runtime hooks (extensions).
struct MCMS<T> {
    data: MCMS_Data;

    /// Runtime hooks (extensions)
    context: T? = null;
    hooks: MCMS_Hooks<T>? = null;
}

/// MCMS contract storage, auto-serialized to/from cell.
struct MCMS_Data {
    /// ID allows multiple independent instances, since contract address depends on initial state.
    id: uint32;

    /// Ownable trait data
    ownable: Ownable2Step;

    /// signers is used to easily validate the existence of the signer by its public key. We still
    /// have signers stored in config in order to easily deactivate them when a new config is set.
    signers: dict; // map<uint256, Signer> - exists if the public key is a signer

    /// The current configuration of the contract
    config: Cell<Config>; // Notice: split out as cell to avoid size limits

    /// Remember signedHashes that this contract has seen. Each signedHash can only be set once.
    seenSignedHashes: dict; // map<uint256, bool>
    /// The current expiring root and the number of ops in it.
    expiringRootAndOpCount: ExpiringRootAndOpCount;
    /// The current metadata about the root.
    rootMetadata: Cell<RootMetadata>; // Notice: split out as cell to avoid size limits
}

/// Load from contract data using auto-serialization.
@inline
fun MCMS_Data.fromContractData() {
    return MCMS_Data.fromCell(contract.getData());
}

/// Store as contract data into persistent storage using auto-serialization.
@inline
fun MCMS_Data.storeAsContractData(self) {
    contract.setData(self.toCell());
}

// --- Hooks struct (extensions) ---

/// @dev Hook extensions exposed by the MCMS contract.
struct MCMS_Hooks<T> {
    /// Hook to inject custom execution logic.
    execute: ((T?, address, coins, cell) -> void)?;
}

// --- Constants ---

/// Should be used as the first 32 bytes of the pre-image of the leaf that holds a
/// op. This value is for domain separation of the different values stored in the
/// Merkle tree.
const MANY_CHAIN_MULTI_SIG_DOMAIN_SEPARATOR_OP =
    stringSha256_32("MANY_CHAIN_MULTI_SIG_DOMAIN_SEPARATOR_OP");

/// Should be used as the first 32 bytes of the pre-image of the leaf that holds the
/// root metadata. This value is for domain separation of the different values stored in the
/// Merkle tree.
const MANY_CHAIN_MULTI_SIG_DOMAIN_SEPARATOR_METADATA =
    stringSha256_32("MANY_CHAIN_MULTI_SIG_DOMAIN_SEPARATOR_METADATA");

const NUM_GROUPS = 32;
const MAX_NUM_SIGNERS = 200;

/// @notice Thrown when number of signers is 0 or greater than MAX_NUM_SIGNERS.
const ERROR_OUT_OF_BOUNDS_NUM_SIGNERS = 100;
/// @notice Thrown when signerKeys and signerGroups have different lengths.
const ERROR_SIGNER_GROUPS_LENGTH_MISMATCH = 101;

/// @notice Thrown when number of some signer's group is greater than (NUM_GROUPS-1).
const ERROR_OUT_OF_BOUNDS_GROUP = 102;

/// @notice Thrown when the group tree isn't well-formed.
const ERROR_GROUP_TREE_NOT_WELL_FORMED = 103;

/// @notice Thrown when the quorum of some group is larger than the number of signers in it.
const ERROR_OUT_OF_BOUNDS_GROUP_QUORUM = 104;

/// @notice Thrown when a disabled group contains a signer.
const ERROR_SIGNER_IN_DISABLED_GROUP = 105;

/// @notice Thrown when the signers' public keys are not a strictly increasing monotone sequence.
/// Prevents signers from including more than one signature.
const ERROR_SIGNERS_KEYS_MUST_BE_STRICTLY_INCREASING = 106;

/// @notice Thrown when the signature corresponds to invalid signer.
const ERROR_INVALID_SIGNER = 107;

/// @notice Thrown when there is no sufficient set of valid signatures provided to make the
/// root group successful.
const ERROR_INSUFFICIENT_SIGNERS = 108;

/// @notice Thrown when attempt to set metadata or execute op for another chain.
const ERROR_WRONG_CHAIN_ID = 109;

/// @notice Thrown when the multiSig address in metadata or op is
/// incompatible with the address of this contract.
const ERROR_WRONG_MULTI_SIG = 110;

/// @notice Thrown when the preOpCount <= postOpCount invariant is violated.
const ERROR_WRONG_POST_OP_COUNT = 111;

/// @notice Thrown when attempting to set a new root while there are still pending ops
/// from the previous root without explicitly overriding it.
const ERROR_PENDING_OPS = 112;

/// @notice Thrown when preOpCount in metadata is incompatible with the current opCount.
const ERROR_WRONG_PRE_OP_COUNT = 113;

/// @notice Thrown when the provided merkle proof cannot be verified.
const ERROR_PROOF_CANNOT_BE_VERIFIED = 114;

/// @notice Thrown when attempt to execute an op after
/// data.expiringRootAndOpCount.validUntil has passed.
const ERROR_ROOT_EXPIRED = 115;

/// @notice Thrown when attempt to bypass the enforced ops' order in the merkle tree or
/// re-execute an op.
const ERROR_WRONG_NONCE = 116;

/// @notice Thrown when attempting to execute an op even though opCount equals
/// metadata.postOpCount.
const ERROR_POST_OP_COUNT_REACHED = 117;

/// @notice Thrown when the underlying call in _execute() reverts.
const ERROR_CALL_REVERTED = 118;

/// @notice Thrown when attempt to set past validUntil for the root.
const ERROR_VALID_UNTIL_HAS_ALREADY_PASSED = 119;

/// @notice Thrown when setRoot() is called before setting a config.
const ERROR_MISSING_CONFIG = 120;

/// @notice Thrown when attempt to set the same (root, validUntil) in setRoot().
const ERROR_SIGNED_HASH_ALREADY_SEEN = 121;

// --- Constants - storage ---

// --- Data structures ---

// Signer information
struct Signer {
    /// The public key of the signer.
    key: uint256;
    /// The index of the signer in data.config.signers
    index: uint8;
    /// 0 <= group < NUM_GROUPS. Each signer can only be in one group.
    group: uint8;
}

/// Signing groups are arranged in a tree. Each group is an interior node and has its own quorum.
/// Signers are the leaves of the tree. A signer/leaf node is successful iff it furnishes a valid
/// signature. A group/interior node is successful iff a quorum of its children are successful.
/// setRoot succeeds only if the root group is successful.
/// Here is an example:
///
///                    ┌──────┐
///                 ┌─►│2-of-3│◄───────┐
///                 │  └──────┘        │
///                 │        ▲         │
///                 │        │         │
///              ┌──┴───┐ ┌──┴───┐ ┌───┴────┐
///          ┌──►│1-of-2│ │2-of-2│ │signer A│
///          │   └──────┘ └──────┘ └────────┘
///          │       ▲      ▲  ▲
///          │       │      │  │     ┌──────┐
///          │       │      │  └─────┤1-of-2│◄─┐
///          │       │      │        └──────┘  │
///  ┌───────┴┐ ┌────┴───┐ ┌┴───────┐ ▲        │
///  │signer B│ │signer C│ │signer D│ │        │
///  └────────┘ └────────┘ └────────┘ │        │
///                                   │        │
///                            ┌──────┴─┐ ┌────┴───┐
///                            │signer E│ │signer F│
///                            └────────┘ └────────┘
///
/// - If signers [A, B] sign, they can set a root.
/// - If signers [B, D, E] sign, they can set a root.
/// - If signers [B, D, E, F] sign, they can set a root. (Either E's or F's signature was
///   superfluous.)
/// - If signers [B, C, D] sign, they cannot set a root, because the 2-of-2 group on the second
///   level isn't successful and therefore the root group isn't successful either.
///
/// To map this tree to a Config, we:
/// - create an entry in signers for each signer (sorted by address in ascending order)
/// - assign the root group to index 0 and have it be its own parent
/// - assign an index to each non-root group, such that each group's parent has a lower index
///   than the group itself
/// For example, we could transform the above tree structure into:
/// groupQuorums = [2, 1, 2, 1] + [0, 0, ...] (rightpad with 0s to NUM_GROUPS)
/// groupParents = [0, 0, 0, 2] + [0, 0, ...] (rightpad with 0s to NUM_GROUPS)
/// and assuming that address(A) < address(C) < address(E) < address(F) < address(D) < address(B)
/// signers = [
///    {addr: address(A), index: 0, group: 0}, {addr: address(C), index: 1, group: 1},
///    {addr: address(E), index: 2, group: 3}, {addr: address(F), index: 3, group: 3},
///    {addr: address(D), index: 4, group: 2}, {addr: address(B), index: 5, group: 1},
///  ]
struct Config {
    signers: dict; // map<uint8, Signer> - (indexed)
    /// groupQuorums[i] stores the quorum for the i-th signer group. Any group with
    /// groupQuorums[i] = 0 is considered disabled. The i-th group is successful if
    /// it is enabled and at least groupQuorums[i] of its children are successful.
    groupQuorums: dict; // map<uint8, uint8> (indexed, iterable backwards)
    /// groupParents[i] stores the parent group of the i-th signer group. We ensure that the
    /// groups form a tree structure (where the root/0-th signer group points to itself as
    /// parent) by enforcing
    /// - (i != 0) implies (groupParents[i] < i)
    /// - groupParents[0] == 0
    groupParents: dict; // map<uint8, uint8> (indexed, iterable backwards)
}

/// MerkleRoots are a bit tricky since they reveal almost no information about the contents of
/// the tree they authenticate. To mitigate this, we enforce that this contract can only execute
/// ops from a single root at any given point in time. We further associate an expiry
/// with each root to ensure that messages are executed in a timely manner. setRoot and various
/// execute calls are expected to happen in quick succession. We put the expiring root and
/// opCount in same struct in order to reduce gas costs of reading and writing.
struct ExpiringRootAndOpCount {
    /// The expiring root.
    root: uint256;
    /// We prefer using block.timestamp instead of block.number, as a single
    /// root may target many chains. We assume that block.timestamp can
    /// be manipulated by block producers but only within relatively tight
    /// bounds (a few minutes at most).
    validUntil: uint32;
    /// each ManyChainMultiSig instance has it own independent opCount.
    opCount: uint40;
}

/// @notice Each root also authenticates metadata about itself (stored as one of the leaves)
/// which must be revealed when the root is set.
///
/// @dev We need to be careful that abi.encode(MANY_CHAIN_MULTI_SIG_DOMAIN_SEPARATOR_METADATA, RootMetadata)
/// is greater than 64 bytes to prevent collisions with internal nodes in the Merkle tree. See
/// openzeppelin-contracts/contracts/utils/cryptography/MerkleProof.sol:15 for details.
struct RootMetadata {
    /// chainId and multiSig uniquely identify a ManyChainMultiSig contract instance that the
    /// root is destined for.
    /// int256 since it is unclear if we can represent chainId as uint64 (and TON introduces negative chain IDs).
    /// There is a proposal (https://ethereum-magicians.org/t/eip-2294-explicit-bound-to-chain-id/11090) to
    /// bound chainid to 64 bits, but it is still unresolved.
    chainId: int256;
    multiSig: address;
    // opCount before adding this root
    preOpCount: uint40;
    // opCount after executing all ops in this root
    postOpCount: uint40;
    // override whatever root was already stored in this contract even if some of its
    // ops weren't executed.
    // Important: it is strongly recommended that offchain code set this to false by default.
    // Be careful setting this to true as it may break assumptions about what transactions from
    // the previous root have already been executed.
    overridePreviousRoot: bool;
}

/// @dev An ECDSA signature.
struct Signature {
    // Notice: no `v: uint8;` field, as public key recovery is not supported.

    r: uint256;
    s: uint256;

    // Instead of v attach the signer (public key hash)
    signer: uint256;
}

/// @notice an op to be executed by the ManyChainMultiSig contract
///
/// @dev We need to be careful that abi.encode(LEAF_OP_DOMAIN_SEPARATOR, RootMetadata)
/// is greater than 64 bytes to prevent collisions with internal nodes in the Merkle tree. See
/// openzeppelin-contracts/contracts/utils/cryptography/MerkleProof.sol:15 for details.
struct Op {
    chainId: int256;
    multiSig: address;
    nonce: uint40;
    to: address;
    value: coins;
    data: cell; // body of the operation
}

// --- Methods ---

/// @dev Loads the MCMS contract data from persistent storage.
fun MCMS<T>.load(context: T? = null, hooks: MCMS_Hooks<T>? = null): MCMS<T> {
    // Load the contract data from the persistent storage
    var data = MCMS_Data.fromContractData();

    // Return the MCMS instance with loaded data and hooks
    return MCMS<T> {
        data: data,
        context: context,
        hooks: hooks,
    };
}

/// @notice setRoot Sets a new expiring root.
///
/// @param root is the new expiring root.
/// @param validUntil is the time by which root is valid
/// @param metadata is the authenticated metadata about the root, which is stored as one of
/// the leaves.
/// @param metadataProof is the MerkleProof of inclusion of the metadata in the Merkle tree.
/// @param signatures the ECDSA signatures on (root, validUntil).
///
/// @dev the message (root, validUntil) should be signed by a sufficient set of signers.
/// This signature authenticates also the metadata.
///
/// @dev this method can be executed by anyone who has the root and valid signatures.
/// as we validate the correctness of signatures, this imposes no risk.
fun MCMS<T>.setRoot(
    mutate self,
    sender: address,
    queryId: uint64,
    root: uint256,
    validUntil: uint32,
    metadata: RootMetadata,
    metadataProof: Iterator<uint256>,
    signatures: cell // vec<Signature>
) {
    /// Construct the message that should be signed by the signers.
    val signedHash = beginCell()
        .storeUint(root, 256)
        .storeUint(validUntil, 32)
        .endCell()
        .hash();

    // Each (root, validUntil) tuple can only bet set once. For example, this prevents a
    // scenario where there are two signed roots with overridePreviousRoot = true and
    // an adversary keeps alternatively calling setRoot(root1), setRoot(root2),
    // setRoot(root1), ...
    {
        var (_, exists) = self.data.seenSignedHashes.uDictGet(256, signedHash);
        if (exists) {
            throw ERROR_SIGNED_HASH_ALREADY_SEEN;
        }
    }

    // Upack config from the contract data
    var config = Config.fromCell(self.data.config);

    // the group at the root of the tree (with index 0) determines whether the vote passed,
    // we cannot proceed if it isn't configured with a valid (non-zero) quorum
    var rootQuorum: uint8;
    {
        var (_rootQuorum, exists) = config.groupQuorums.uDictGet(8, 0);
        if (!exists) {
            throw ERROR_MISSING_CONFIG;
        }
        rootQuorum = _rootQuorum!.loadUint(8);
        if (rootQuorum == 0) {
            throw ERROR_MISSING_CONFIG;
        }
    }

    {
        var i = 0;
        var signer: Signer;
        var prevSigner: uint256 = 0; // start with zero key
        var ss = Iterator<Signature>.new(signatures);
        var groupVoteCounts = createEmptyDict(); // number of votes per group, map<uint8, uint8> (indexed)
        while (!ss.empty()) {
            var s = ss.next();
            val valid = isSignatureValid(signedHash, s.toCell().beginParse(), s.signer);
            if (!valid) {
                throw ERROR_INVALID_SIGNER;
            }

            // the off-chain system is required to sort the signatures by the
            // signer key in an increasing order
            if (prevSigner >= s.signer) {
                throw ERROR_SIGNERS_KEYS_MUST_BE_STRICTLY_INCREASING;
            }
            prevSigner = s.signer;

            {
                val (_signer, exists) = self.data.signers.uDictGet(256, s.signer);
                if (!exists) {
                    throw ERROR_INVALID_SIGNER;
                }
                signer = Signer.fromSlice(_signer!);
            }

            if (signer.key != s.signer) {
                throw ERROR_INVALID_SIGNER;
            }

            // Notice: break/continue from loops are not supported yet in Tolk.
            // Instead, we use a flag to indicate whether we should break the loop.
            var _break = false;
            var group = signer.group;
            while (!_break) {
                var votes = 1;
                {
                    var (current, exists) = groupVoteCounts.uDictGet(8, group);
                    if (exists) {
                        votes = current!.loadUint(8) + 1;
                    }
                }
                // Store the incremented vote count for the group
                groupVoteCounts.uDictSet(8, group, beginCell().storeUint(votes, 8).endCell().beginParse());

                var quorum = 0;
                {
                    var (_quorum, exists) = config.groupQuorums.uDictGet(8, group);
                    if (exists) {
                        quorum = _quorum!.loadUint(8);
                    }
                }

                if (votes != quorum) {
                    // bail out unless we just hit the quorum. we only hit each quorum once,
                    // so we never move on to the parent of a group more than once.
                    _break = true;
                }

                if (group == 0) {
                    // reached root
                    _break = true;
                }

                if (!_break) {
                    var parent = 0;
                    {
                        var (_parent, exists) = config.groupParents.uDictGet(8, group);
                        if (exists) {
                            parent = _parent!.loadUint(8);
                        }
                    }

                    group = parent;
                }
            }
        }

        // did the root group reach its quorum?
        var (_rootVotes, exists) = groupVoteCounts.uDictGet(8, 0);
        if (!exists || _rootVotes!.loadUint(8) < rootQuorum) {
            throw ERROR_INSUFFICIENT_SIGNERS;
        }
    }

    if (validUntil < blockchain.now()) {
        throw ERROR_VALID_UNTIL_HAS_ALREADY_PASSED;
    }

    {
        // verify metadataProof
        // Notice: we use the standard sha256 hash function to hash the leaf.
        val hashedLeaf =
            beginCell()
                .storeUint(MANY_CHAIN_MULTI_SIG_DOMAIN_SEPARATOR_METADATA, 256)
                .storeSlice(metadata.toCell().beginParse())
                .endCell()
                .hash();
        // "../lib/crypto/merkle_proof.tolk"
        if (!verify(metadataProof, root, hashedLeaf)) {
            throw ERROR_PROOF_CANNOT_BE_VERIFIED;
        }
    }

    // Check if right environment via chainId
    if (globalId() as int256 != metadata.chainId) {
        throw ERROR_WRONG_CHAIN_ID;
    }

    if (contract.getAddress() != metadata.multiSig) {
        throw ERROR_WRONG_MULTI_SIG;
    }

    val opCount: uint40 = self.data.expiringRootAndOpCount.opCount;

    // don't allow a new root to be set if there are still outstanding ops that have not been
    // executed, unless overridePreviousRoot is set
    //
    // Unpack the root metadata from the contract data
    val rootMetadata = lazy RootMetadata.fromCell(self.data.rootMetadata);
    if (opCount != rootMetadata.postOpCount && !metadata.overridePreviousRoot) {
        throw ERROR_PENDING_OPS;
    }

    // the signers are responsible for tracking opCount offchain and ensuring that
    // preOpCount equals to opCount
    if (opCount != metadata.preOpCount) {
        throw ERROR_WRONG_PRE_OP_COUNT;
    }

    if (metadata.preOpCount > metadata.postOpCount) {
        throw ERROR_WRONG_POST_OP_COUNT;
    }

    // done with validation, persist in in contract state
    self.data.seenSignedHashes.uDictSet(256, signedHash, createEmptySlice());
    self.data.expiringRootAndOpCount = ExpiringRootAndOpCount{
        root: root,
        validUntil: validUntil,
        opCount: metadata.preOpCount
    };
    self.data.rootMetadata = metadata.toCell();

    // Reply to sender new root was set, return excess
    createMessage({
        bounce: false,
        value: 0,
        dest: sender,
        body: MCMS_NewRoot{queryId, root, validUntil, metadata}
    }).send(SEND_MODE_REGULAR | SEND_MODE_CARRY_ALL_REMAINING_MESSAGE_VALUE);
}

/// @notice Execute the received op after verifying the proof of its inclusion in the
/// current Merkle tree. The op should be the next op according to the order
/// enforced by the merkle tree whose root is stored in data.expiringRootAndOpCount, i.e., the
/// nonce of the op should be equal to data.expiringRootAndOpCount.opCount.
///
/// @param op is Op to be executed
/// @param proof is the MerkleProof for the op's inclusion in the MerkleTree which its
/// root is the data.expiringRootAndOpCount.root.
///
/// @dev ANYONE can call this function! That's intentional. Callers can only execute verified,
/// ordered ops in the Merkle tree.
///
/// @dev we perform a raw call to each target. Raw calls to targets that don't have associated
/// contract code will always succeed regardless of data.
///
/// @dev the gas limit of the call can be freely determined by the caller of this function.
/// We expect callees to revert if they run out of gas.
fun MCMS<T>.execute(
    mutate self,
    sender: address,
    queryId: uint64,
    op: Op,
    proof: Iterator<uint256>,
) {
    val currentExpiringRootAndOpCount = self.data.expiringRootAndOpCount;

    // Unpack the root metadata from the contract data
    val rootMetadata = lazy RootMetadata.fromCell(self.data.rootMetadata);

    if (rootMetadata.postOpCount <= currentExpiringRootAndOpCount.opCount) {
        throw ERROR_POST_OP_COUNT_REACHED;
    }

    // Check if right environment via chainId
    if (op.chainId != globalId() as int256) {
        throw ERROR_WRONG_CHAIN_ID;
    }

    if (op.multiSig != contract.getAddress()) {
        throw ERROR_WRONG_MULTI_SIG;
    }

    if (blockchain.now() > currentExpiringRootAndOpCount.validUntil) {
        throw ERROR_ROOT_EXPIRED;
    }

    if (op.nonce != currentExpiringRootAndOpCount.opCount) {
        throw ERROR_WRONG_NONCE;
    }

    // verify that the op exists in the merkle tree
    // Notice: we use the standard sha256 hash function to hash the leaf.
    val hashedLeaf =
        beginCell()
            .storeUint(MANY_CHAIN_MULTI_SIG_DOMAIN_SEPARATOR_OP, 256)
            .storeRef(op.toCell())
            .endCell()
            .hash();
    // "../lib/crypto/merkle_proof.tolk"
    if (!verify(proof, currentExpiringRootAndOpCount.root, hashedLeaf)) {
        throw ERROR_PROOF_CANNOT_BE_VERIFIED;
    }

    // increase the counter *before* execution to prevent reentrancy issues
    self.data.expiringRootAndOpCount.opCount = currentExpiringRootAndOpCount.opCount + 1;

    self._execute(op.to, op.value, op.data);

    // Reply back to sender
    createMessage({
        bounce: false,
        value: DEFAULT_MESSAGE_BUDGET_FEE, // send small value as gas budget, keep the rest
        dest: sender,
        body: MCMS_OpExecuted{queryId, nonce: op.nonce, to: op.to, data: op.data, value: op.value}
    }).send(SEND_MODE_REGULAR);
}

/// @notice sets a new data.config. If clearRoot is true, then it also invalidates
/// data.expiringRootAndOpCount.root.
///
/// @param signerKeys holds the public keys of the active signers. The keys must be in
/// ascending order.
/// @param signerGroups maps each signer to its group
/// @param groupQuorums holds the required number of valid signatures in each group.
/// A group i is called successful group if at least groupQuorum[i] distinct signers provide a
/// valid signature.
/// @param groupParents holds each group's parent. The groups must be arranged in a tree s.t.
/// group 0 is the root of the tree and the i-th group's parent has index j less than i.
/// Iff setRoot is called with a set of signatures that causes the root group to be successful,
/// setRoot allows a root to be set.
/// @param clearRoot, if set to true, invalidates the current root. This option is needed to
/// invalidate the current root, so to prevent further ops from being executed. This
/// might be used when the current root was signed under a loser group configuration or when
/// some previous signers aren't trusted any more.
fun MCMS<T>.setConfig(
    mutate self,
    sender: address,
    queryId: uint64,
    signerKeys: cell,   // vec<uint256> (public keys)
    signerGroups: cell, // vec<uint8>
    groupQuorums: dict, // map<uint8, uint8> (indexed, iterable backwards)
    groupParents: dict, // map<uint8, uint8> (indexed, iterable backwards)
    clearRoot: bool,
) {
    self.data.ownable.requireOwner(sender);

    // Validate input lengths
    var signerKeyIterator = Iterator<uint256>.new(signerKeys);
    val numSigners = signerKeyIterator.size(32); // notice size in bytes
    if (numSigners == 0 || numSigners > MAX_NUM_SIGNERS) {
        throw ERROR_OUT_OF_BOUNDS_NUM_SIGNERS;
    }

    var signerGroupIterator = Iterator<uint8>.new(signerGroups);
    if (numSigners != signerGroupIterator.size(1)) { // notice size in bytes
        throw ERROR_SIGNER_GROUPS_LENGTH_MISMATCH;
    }

    {
        // Validate group structure
        // counts the number of children of each group
        var groupChildrenCounts = createEmptyDict();
        // first, we count the signers as children
        while (!signerGroupIterator.empty()) {
            var g = signerGroupIterator.next();
            if (g >= NUM_GROUPS) {
                throw ERROR_OUT_OF_BOUNDS_GROUP;
            }

            var (count, exists) = groupChildrenCounts.uDictGet(8, g);
            var value = beginCell()
                .storeUint(1, 8)
                .endCell()
                .beginParse();
            if (exists) {
                value = beginCell()
                    .storeUint(count!.loadUint(8) + 1, 8)
                    .endCell()
                    .beginParse();
            }
            groupChildrenCounts.uDictSet(8, g, value);
        }

        // second, we iterate backwards so as to check each group and propagate counts from
        // child group to parent groups up the tree to the root

        var j = 0;
        repeat(NUM_GROUPS) {
            val i = NUM_GROUPS - 1 - j;
            // ensure we have a well-formed group tree. the root should have itself as parent
            var gp: int = 0; // default parent for root group
            {
                var (value, exists) = groupParents.uDictGet(8, i);
                if (exists) {
                    gp = value!.loadUint(8);
                }
            }
            if ((i != 0 && gp >= i) || (i == 0 && gp != 0)) {
                throw ERROR_GROUP_TREE_NOT_WELL_FORMED;
            }

            var gq = 0; // default quorum for groups without a set quorum
            {
                var (value, exists) = groupQuorums.uDictGet(8, i);
                if (exists) {
                    gq = value!.loadUint(8);
                }
            }

            val disabled = gq == 0;

            var count = 0; // default count
            {
                // get the number of children for this group
                var (value, exists) = groupChildrenCounts.uDictGet(8, i);
                if (exists) {
                    count = value!.loadUint(8);
                }
            }

            if (disabled) {
                // a disabled group shouldn't have any children
                if (count > 0) {
                    throw ERROR_SIGNER_IN_DISABLED_GROUP;
                }
            } else {
                // ensure that the group quorum can be met
                if (count < gq) {
                    throw ERROR_OUT_OF_BOUNDS_GROUP_QUORUM;
                }

                var current = 0;
                {
                   var (value, exists)  = groupChildrenCounts.uDictGet(8, gp);
                   if (exists) {
                       current = value!.loadUint(8);
                   }
                }
                groupChildrenCounts.uDictSet(8, gp, beginCell()
                    .storeUint(current + 1, 8)
                    .endCell()
                    .beginParse());
                // the above line clobbers groupChildrenCounts[0] in last iteration, don't use it after the loop ends
            }

            // Increment j until we reach the last group
            j += 1;
        }
    }

    // Upack config from the contract data
    var config = Config.fromCell(self.data.config);
    // Iterate over and remove old signers
    var (_index, oldSigner, exists) = config.signers.uDictDeleteFirstAndGet(8);
    while (exists) {
        val s = lazy Signer.fromSlice(oldSigner!);
        // Remove any old signer addresses
        self.data.signers.uDictDelete(256, s.key);
        (_index, oldSigner, exists) = config.signers.uDictDeleteFirstAndGet(8);
    }
    config.signers = createEmptyDict(); // reset signers

    config.groupQuorums = groupQuorums;
    config.groupParents = groupParents;

    // add new signers' addresses, we require that the signers' list be a strictly monotone
    // increasing sequence
    var i = 0;
    var prevSigner: uint256 = 0; // start with a zero key
    // Reset the signers group iterator
    signerGroupIterator = Iterator<uint8>.new(signerGroups);
    while (!signerKeyIterator.empty()) {
        var key = signerKeyIterator.next();
        if (prevSigner >= key) {
            throw ERROR_SIGNERS_KEYS_MUST_BE_STRICTLY_INCREASING;
        }

        val group = signerGroupIterator.next();
        var signer = Signer{key, index: i as uint8, group: group}.toCell().beginParse();
        self.data.signers.uDictSet(256, key, signer);
        config.signers.uDictSet(8, i, signer);

        prevSigner = key;

        // Increment i for the next signer
        i += 1;
    }

    // Pack the config back into a data cell
    self.data.config = config.toCell();

    // Optionally clear root
    if (clearRoot) {
        // clearRoot is equivalent to overriding with a completely empty root
        val opCount = self.data.expiringRootAndOpCount.opCount;
        self.data.expiringRootAndOpCount = ExpiringRootAndOpCount{
            root: 0, // empty root
            validUntil: 0,
            opCount: opCount
        };
        self.data.rootMetadata = RootMetadata{
            chainId: globalId() as int256,
            multiSig: contract.getAddress(),
            preOpCount: opCount,
            postOpCount: opCount,
            overridePreviousRoot: true
        }.toCell();
    }

    // Reply to sender config is set, return excess
    createMessage({
        bounce: false,
        value: 0,
        dest: sender,
        body: MCMS_ConfigSet{
            queryId: queryId,
            config: Config.fromCell(self.data.config),
            isRootCleared: clearRoot
        }
    }).send(SEND_MODE_REGULAR | SEND_MODE_CARRY_ALL_REMAINING_MESSAGE_VALUE);
}

/// @notice Execute an op's call. Performs a raw message send that can bounce on errors.
fun MCMS<T>._execute(self, target: address, value: coins, data: cell): void {
    if (self.hooks != null && self.hooks.execute != null) {
        return self.hooks.execute(self.context, target, value, data); // extension hook
    }

    // Send a message to the target with the provided value and data
    createMessage({
        bounce: true, // TODO: how to handle bounced messages (errors)?
        value: value,
        dest: target,
        body: data
    }).send(SEND_MODE_PAY_FEES_SEPARATELY);
}

/// Returns the current configuration of the contract
fun MCMS<T>.getConfig(self): Config {
    return Config.fromCell(self.data.config);
}

/// Returns the current op count, i.e., the number of ops in the current expiring root.
fun MCMS<T>.getOpCount(self): uint40 {
    return self.data.expiringRootAndOpCount.opCount;
}

/// Returns the current expiring root and its validUntil time.
fun MCMS<T>.getRoot(self): (uint256, uint32) {
    val expiringRootAndOpCount = self.data.expiringRootAndOpCount;
    return (expiringRootAndOpCount.root, expiringRootAndOpCount.validUntil);
}

/// Returns the current metadata about the root, which is stored as one of the leaves.
fun MCMS<T>.getRootMetadata(self): RootMetadata {
    return RootMetadata.fromCell(self.data.rootMetadata);
}

// --- Message handlers ---

@inline
fun MCMS<T>.onInternalMessage(mutate self, msgSender: address, msgValue: coins, msgBody: slice) {
    val msg = lazy MCMS_InMessage.fromSlice(msgBody);
    match (msg) {
        MCMS_TopUp => {
            // Top up the contract balance, no action needed, return early - no need to update storage
            return;
        }
        MCMS_SetRoot => {
            self.setRoot(msgSender, msg.queryId, msg.root, msg.validUntil, msg.metadata,
                Iterator<uint256>.new(msg.metadataProof), msg.signatures);
        }
        MCMS_Execute => {
            self.execute(msgSender, msg.queryId, Op.fromCell(msg.op),
                Iterator<uint256>.new(msg.proof));
        }
        MCMS_SetConfig => {
            self.setConfig(msgSender, msg.queryId, msg.signerKeys, msg.signerGroups,
                msg.groupQuorums, msg.groupParents, msg.clearRoot);
        }
        else => {
            val msgHandled = 0
                || self.data.ownable.onInternalMessage(msgSender, msgBody);

            if (!msgHandled) {
                // If the message was not handled, throw an error
                throw ERROR_WRONG_OP;
            }
        }
    }
}

// TODO: handle bounced messages
fun onInternalMessage(in: InMessage) {
    /// Load the contract storage as MCMS and handle the message
    var mcms = MCMS<bool>.load();
    mcms.onInternalMessage(in.senderAddress, in.valueCoins, in.body);

    /// Store the updated contract data
    mcms.data.storeAsContractData();
}

// --- Getters ---

get fun typeAndVersion(): (slice, slice) {
     return ("com.chainlink.ton.mcms.MCMS", "1.0.0");
}

/// @see <MCMS<T>.getConfig>
get fun getConfig(): Config {
    return MCMS<bool>.load().getConfig();
}

/// @see <MCMS<T>.getOpCount>
get fun getOpCount(): uint40 {
    return MCMS<bool>.load().getOpCount();
}

/// @see <MCMS<T>.getRoot>
get fun getRoot(): (uint256, uint32) {
    return MCMS<bool>.load().getRoot();
}

/// @see <MCMS<T>.getRootMetadata>
get fun getRootMetadata(): RootMetadata {
    return MCMS<bool>.load().getRootMetadata();
}

// --- Getters - Ownable2Step ---

/// @see <Ownable2Step.get_owner>
get fun owner(): address {
    return MCMS<bool>.load().data.ownable.get_owner();
}

/// @see <Ownable2Step.get_pendingOwner>
get fun pendingOwner(): address? {
    return MCMS<bool>.load().data.ownable.get_pendingOwner();
}
