// SnakeString describes a (potentially long) string inside a cell;
// short strings are stored as-is, like "my-picture.png";
// long strings are nested refs, like "xxxx".ref("yyyy".ref("zzzz"))
type SnakeString = slice

fun SnakeString.unpackFromSlice(mutate s: slice) {
    // obviously, SnakeString can be only the last: it's just "the remainder";
    // for correctness, it's better to validate it has no more refs:
    // assert (s.remainingRefsCount() <= 1) throw 5;
    // but since here we're matching the original FunC implementation, leave no checks
    val snakeRemainder = s;
    s = createEmptySlice();     // no more left to read
    return snakeRemainder
}

fun SnakeString.packToBuilder(self, mutate b: builder) {
    b.storeSlice(self)
}


struct RoyaltyParams {
    numerator: uint16
    denominator: uint16
    royaltyAddress: address
}

struct NftCollectionStorage {
    adminAddress: address
    nextItemIndex: uint64
    content: Cell<CollectionContent>
    nftItemCode: cell
    royaltyParams: Cell<RoyaltyParams>
}

struct CollectionContent {
    collectionMetadata: cell
    commonContent: Cell<SnakeString>
}

struct NftItemStorage {
    itemIndex: uint64
    collectionAddress: address
    ownerAddress: address
    content: Cell<SnakeString>
}

struct NftItemStorageNotInitialized {
    itemIndex: uint64
    collectionAddress: address
}


fun NftCollectionStorage.load() {
    return NftCollectionStorage.fromCell(contract.getData())
}

fun NftCollectionStorage.save(self) {
    contract.setData(self.toCell())
}


// actual storage of an NFT item is tricky: it's either initialized or not;
// after NFT has been inited, it's represented as `NftItemStorage`;
// before initialization, it has only itemIndex and collectionAddress: `NftItemStorageNotInitialized`;
// hence, we start parsing it, that then we detect, whether it's inited or not
struct NftItemStorageMaybeNotInitialized {
    contractData: slice
}

// how do we detect whether it's initialized or not?
// the answer: when "inited", we store `content` (cell),
// so, we have a ref, and for uninited, we don't have a ref
fun NftItemStorageMaybeNotInitialized.isInitialized(self) {
    val hasContent = self.contractData.remainingRefsCount();
    return hasContent
}

fun NftItemStorageMaybeNotInitialized.parseNotInitialized(self) {
    return NftItemStorageNotInitialized.fromSlice(self.contractData)
}

fun NftItemStorageMaybeNotInitialized.parseInitialized(self) {
    return NftItemStorage.fromSlice(self.contractData)
}

fun startLoadingNftItemStorage(): NftItemStorageMaybeNotInitialized {
    return {
        contractData: contract.getData().beginParse()
    }
}

fun NftItemStorage.save(self) {
    contract.setData(self.toCell())
}


fun calcDeployedNftItem(itemIndex: uint64, collectionAddress: address, nftItemCode: cell): AutoDeployAddress {
    val emptyNftItemStorage: NftItemStorageNotInitialized = {
        itemIndex,
        collectionAddress,
    };

    return {
        stateInit: {
            code: nftItemCode,
            data: emptyNftItemStorage.toCell()
        }
    }
}
