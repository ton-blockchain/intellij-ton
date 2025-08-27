import "@stdlib/tvm-dicts"

type WhitelistDict = dict     // a map of [address => nothing]

struct Storage {
    seqno: uint32
    subwalletId: uint32
    publicKey: uint256
    whitelist: WhitelistDict
    vestingParameters: Cell<VestingParameters>
}

fun Storage.load() {
    return Storage.fromCell(contract.getData());
}

fun Storage.save(self) {
    contract.setData(self.toCell());
}


struct VestingParameters {
    startTime: uint64
    totalDuration: uint32     // > 0 and <= 135 years (2^32 seconds) ; mod unlockPeriod == 0
    unlockPeriod: uint32      // > 0 and <= totalDuration
    cliffDuration: uint32     // >= 0 and < totalDuration ; mod unlockPeriod == 0
    totalAmount: coins
    senderAddress: address
    ownerAddress: address
}

@inline_ref
fun VestingParameters.getLockedAmount(self, nowTime: int): coins {
    if (nowTime > self.startTime + self.totalDuration) {
        return 0;
    }

    if (nowTime < self.startTime + self.cliffDuration) {
        return self.totalAmount;
    }

    return self.totalAmount -
        mulDivFloor(
            self.totalAmount,
            (nowTime - self.startTime) / self.unlockPeriod,
            self.totalDuration / self.unlockPeriod
        );
}


const ADDRESS_SIZE = 267  // '100' (std addr no anycast) + workchain 8 + hash 256

fun WhitelistDict.addWhitelisted(mutate self, address: address) {
    self.sDictSetBuilder(ADDRESS_SIZE, address as slice, beginCell());
}

fun WhitelistDict.isWhitelisted(self, address: address): bool {
    var (_, found: bool) = self.sDictGet(ADDRESS_SIZE, address as slice);
    return found;
}

