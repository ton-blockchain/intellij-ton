
struct AttachedMessage {
    sendMode: uint8
    msgCell: cell
}

struct (0x7258a69b) RequestAddToWhitelist {
    queryId: uint64
    addrToWhitelist: address
    restRefs: RemainingBitsAndRefs
}

struct (0xf258a69b) ResponseAddedToWhitelist {
    queryId: uint64
}

struct (0xa7733acd) AskToSendMessage {
    queryId: uint64
    attachedMsg: AttachedMessage
}

struct (0xf7733acd) SendExcesses {
    queryId: uint64
}

struct ExternalMsgAfterSignature {
    subwalletId: uint32
    validUntil: uint32
    seqno: uint32
    rest: RemainingBitsAndRefs
}

