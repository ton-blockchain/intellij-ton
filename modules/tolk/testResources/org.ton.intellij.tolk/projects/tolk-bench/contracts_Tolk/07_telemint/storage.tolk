import "fees-management"

// encoded as TL/B `len: (## 8) data: (bits (len*8))`
type TelegramString = slice

fun TelegramString.packToBuilder(self, mutate b: builder) {
    val bytes = self.remainingBitsCount() / 8;      // assume it's divisible (since decoded from input)
    b.storeUint(bytes, 8);
    b.storeSlice(self);
}

fun TelegramString.unpackFromSlice(mutate s: slice) {
    val bytes = s.loadUint(8);
    return s.loadBits(bytes * 8);
}

struct CollectionStorage {
    isCollectionInitialized: bool
    subwalletId: uint32
    publicKey: uint256
    content: cell
    nftItemCode: cell
    fullDomain: Cell<TelegramString>
    royaltyParams: Cell<RoyaltyParams>
}

struct RoyaltyParams {
    royaltyFactor: uint16
    royaltyBase: uint16
    address: address
}

fun CollectionStorage.load() {
    return CollectionStorage.fromCell(contract.getData())
}

fun CollectionStorage.save(self) {
    contract.setData(self.toCell())
}


struct ItemStorageNotInitialized {
    config: Cell<ItemConfig>
}

struct ItemStorage {
    config: Cell<ItemConfig>
    ownerAddress: address
    content: Cell<ItemContent>
    auction: Cell<Auction>?
    royaltyParams: Cell<RoyaltyParams>
}

// actual storage of an NFT item is tricky: it's either initialized or not;
// after NFT has been inited, it's represented as `ItemStorage`;
// before initialization, it has only cell ItemConfig: `ItemStorageNotInitialized`;
// hence, we start parsing it, then we detect, whether it's inited or not
struct ItemStorageMaybeNotInitialized {
    contractData: slice
}

// how do we detect whether it's initialized or not?
// the answer: when "inited", we store `ownerAddress` and other fields,
// so, we have some data bits, and for uninited, we have only 1 ref (see ItemStorageNotInitialized)
fun ItemStorageMaybeNotInitialized.isInitialized(self) {
    val hasData = self.contractData.remainingBitsCount();
    return hasData
}

fun ItemStorageMaybeNotInitialized.parseNotInitialized(self) {
    return ItemStorageNotInitialized.fromSlice(self.contractData)
}

fun ItemStorageMaybeNotInitialized.parseInitialized(self) {
    return ItemStorage.fromSlice(self.contractData)
}

fun startLoadingItemStorage(): ItemStorageMaybeNotInitialized {
    return {
        contractData: contract.getData().beginParse()
    }
}

fun ItemStorage.load() {
    return ItemStorage.fromCell(contract.getData())
}

fun ItemStorage.save(self) {
    contract.setData(self.toCell())
}


struct ItemConfig {
    index: uint256
    collectionAddress: address
}

struct ItemContent {
    nftContent: cell
    dns: cell?
    tokenInfo: Cell<TokenNameAndDomain>
}

struct TokenNameAndDomain {
    tokenName: TelegramString
    domain: TelegramString
}

struct AuctionConfig {
    benificiaryAddress: address
    minBid: coins
    maxBid: coins
    minBidStep: uint8
    minExtendTime: uint32
    duration: uint32
}

struct Auction {
    config: Cell<AuctionConfig>
    lastBid: Cell<LastBidInfo>?
    minBid: coins
    endTime: uint32
}

struct LastBidInfo {
    bidderAddress: address
    bidAmount: coins
    bidTimestamp: uint32
}

fun calcDeployedTeleItem(index: int, collectionAddress: address, nftItemCode: cell): AutoDeployAddress {
    val emptyNftItemStorage: ItemStorageNotInitialized = {
        config: ItemConfig { index, collectionAddress }.toCell(),
    };

    return {
        workchain: MY_WORKCHAIN,
        stateInit: {
            code: nftItemCode,
            data: emptyNftItemStorage.toCell()
        }
    }
}

fun Auction.createStartingFromNow(config: Cell<AuctionConfig>, minBid: coins, duration: int): Auction {
    return {
        config: config,
        lastBid: null,
        minBid: minBid,
        endTime: blockchain.now() + duration
    }
}

fun AuctionConfig.isInvalid(self) {
    return
        (self.minBid < MIN_TONS_FOR_STORAGE + MINTING_PRICE_UPPER_BOUND) |
        ((self.maxBid != 0) & (self.maxBid < self.minBid)) |
        (self.minBidStep <= 0) |
        (self.minExtendTime > 60 * 60 * 24 * 7) |
        (self.duration > 60 * 60 * 24 * 365);
}
