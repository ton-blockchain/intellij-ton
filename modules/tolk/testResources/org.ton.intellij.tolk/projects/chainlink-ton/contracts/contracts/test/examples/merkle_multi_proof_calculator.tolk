import "../../lib/crypto/merkle_multi_proof.tolk"
import "../../lib/utils.tolk"

struct MerkleMultiProofCalculator {
    id: uint64,
    root: uint256
}

fun loadData(): MerkleMultiProofCalculator {
    return MerkleMultiProofCalculator.fromCell(contract.getData());
}
fun saveData(data: MerkleMultiProofCalculator) {
    contract.setData(data.toCell());
}


struct(0x00000001) CalculateRoot {
    queryId: uint64,
    leaves: cell,
    proofs: cell,
    proofFlagBits: uint256,
}

fun onInternalMessage(myBalance: int, msgValue: int, msgFull: cell, msgBody: slice) {
    if (msgBody.isEmpty()) { // ignore all empty messages
        return;
    }

    var storage = loadData();
    val msg = CalculateRoot.fromSlice(msgBody);

    var leavesIterator = Iterator<uint256>.new(msg.leaves);
    var proofsIterator = Iterator<uint256>.new(msg.proofs);
    val root = merkleRoot(
        leavesIterator,
        proofsIterator,
        msg.proofFlagBits,
    );

    storage.root = root;
    saveData(storage);
}

get fun root(): uint256 {
    var storage = loadData();
    return storage.root;
}

