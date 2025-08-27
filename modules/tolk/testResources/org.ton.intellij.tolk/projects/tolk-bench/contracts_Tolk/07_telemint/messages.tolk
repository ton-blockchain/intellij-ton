import "storage"

struct (0x00000000) TopUp {
    comment: RemainingBitsAndRefs   // should be "#topup"
}

struct (0x4637289b) AskToDeployItem {
    signature: bits512
    signedData: RemainingBitsAndRefs
}

struct SignedDataAtDeploy {
    subwalletId: uint32
    validSince: uint32
    validTill: uint32
    tokenName: TelegramString
    content: cell
    auctionConfig: Cell<AuctionConfig>
    royalty: Cell<RoyaltyParams>?
    restrictions: Cell<OverrideRestrictionsAtDeploy>?
}

struct OverrideRestrictionsAtDeploy {
    forceSenderAddress: address?
    rewriteSenderAddress: address?
}

struct (0x299a3e15) TelemintItemDeploy {
    bidderAddress: address
    bidAmount: coins
    tokenInfo: Cell<TokenNameAndDomain>
    nftContent: cell
    auctionConfig: Cell<AuctionConfig>
    royaltyParams: Cell<RoyaltyParams>
}

struct (0x00000000) MakeNewStakeOrTopUp {
    comment: RemainingBitsAndRefs      // if empty — a new stake; if "#topup" — just top-up contract balance
}

struct (0x693d3950) RequestRoyaltyParams {
    queryId: uint64
}

struct (0xa8cb00ad) ResponseRoyaltyParams {
    queryId: uint64
    royaltyParams: RoyaltyParams
}

struct (0x2fcb26a2) RequestStaticData {
    queryId: uint64
}

struct (0x8b771735) ResponseStaticData {
    queryId: uint64
    index: uint256
    collectionAddress: address
}

struct (0x371638ae) AskToCancelAuction {
    queryId: uint64
}

struct (0x487a8e81) AskToStartAuction {
    queryId: uint64
    newAuctionConfig: Cell<AuctionConfig>
}

struct (0x5fcc3d14) AskToTransfer {
    queryId: uint64
    newOwnerAddress: address
    sendExcessesTo: address
    customPayload: cell?
    forwardTonAmount: coins
    forwardPayload: RemainingBitsAndRefs
}

struct (0x370fec51) SendMoneyToSomebody {
    queryId: uint64
}

struct (0x557cea20) OutbidNotificationToBidder {
    queryId: uint64
}

struct (0xa37a0983) RespondTeleitemOK

struct (0xa43227e1) ReturnBidBackToBidder

struct (0xd53276db) ReturnExcessesBack {
    queryId: uint64
}

struct (0x05138d91) NotificationOwnershipAssigned<TPayload> {
    queryId: uint64
    oldOwnerAddress: address
    payload: TPayload
}

struct ForwardPayloadInlineWrapper<T> {
    eitherBitIsRef: bool = false
    contents: T
}

struct (0x38127de1) ProvidedTeleitemBidInfo {
    bidAmount: coins
    bidTimestamp: uint32
}
