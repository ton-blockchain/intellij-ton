tolk 1.0

import "../utils.tolk"

/// @dev Returns true if a `leaf` can be proved to be a part of a Merkle tree
/// defined by `root`. For this, a `proof` must be provided, containing
/// sibling hashes on the branch from the leaf to the root of the tree. Each
/// pair of leaves and each pair of pre-images are assumed to be sorted.
///
/// This version handles proofs in memory with the default hashing function.
///
fun verify(proof: Iterator<uint256>, root: uint256, leaf: uint256): bool {
    return processProof(proof, leaf) == root;
}

/// @dev Returns the rebuilt hash obtained by traversing a Merkle tree up
/// from `leaf` using `proof`. A `proof` is valid if and only if the rebuilt
/// hash matches the root of the tree. When processing the proof, the pairs
/// of leaves & pre-images are assumed to be sorted.
///
/// This version handles proofs in memory with the default hashing function.
fun processProof(proof: Iterator<uint256>, leaf: uint256): uint256 {
    var computedHash = leaf;
    while (!proof.empty()) {
        computedHash = hashPair(computedHash, proof.next());
    }
    return computedHash;
}

fun hashPair(a: uint256, b: uint256): uint256 {
    if (a < b) {
        return hashInternalNode(a, b);
    } else {
        return hashInternalNode(b, a);
    }
}

/// Notice: use keccak256 hash function to match Ethereum (+tooling) implementation
fun hashInternalNode(a: uint256, b: uint256): uint256 {
    var data = beginCell()
        .storeUint(a, 256)
        .storeUint(b, 256)
        .endCell()
        .beginParse();
    return keccak256(data);
}
