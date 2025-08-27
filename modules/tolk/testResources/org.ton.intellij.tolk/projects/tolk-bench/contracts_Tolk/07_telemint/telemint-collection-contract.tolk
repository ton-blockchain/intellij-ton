import "@stdlib/gas-payments"
import "errors"
import "storage"
import "messages"

fun checkRestrictions(restrictions: OverrideRestrictionsAtDeploy, senderAddress: address): address {
    if (restrictions.forceSenderAddress != null) {
        assert (senderAddress == restrictions.forceSenderAddress) throw ERR_INVALID_SENDER_ADDRESS;
    }
    if (restrictions.rewriteSenderAddress != null) {
        senderAddress = restrictions.rewriteSenderAddress;
    }
    return senderAddress;
}

@inline_ref
fun returnBidOnIncorrectConfig(returnToAddress: address) {
    val returnBidMsg = createMessage({
        bounce: false,
        dest: returnToAddress,
        value: 0,
        body: ReturnBidBackToBidder{}
    });
    returnBidMsg.send(SEND_MODE_CARRY_ALL_REMAINING_MESSAGE_VALUE);
}

type AllowedMessageToCollection =
    | TopUp
    | AskToDeployItem

fun onInternalMessage(in: InMessage) {
    val msg = lazy AllowedMessageToCollection.fromSlice(in.body);

    match (msg) {
        TopUp => {
            // NB: it is not possible to recover any money transferred to this account
            // so we return back all transfers except ones with comment #topup in it
            assert (msg.comment.bitsEqual("#topup") & (msg.comment.remainingRefsCount() == 0)) throw ERR_WRONG_TOPUP_COMMENT;
        }

        AskToDeployItem => {
            val storage = lazy CollectionStorage.load();
            val hash = msg.signedData.hash();
            assert (isSignatureValid(hash, msg.signature as slice, storage.publicKey)) throw ERR_INVALID_SIGNATURE;

            val input = lazy SignedDataAtDeploy.fromSlice(msg.signedData);
            assert (storage.subwalletId == input.subwalletId) throw ERR_WRONG_SUBWALLET_ID;
            var now = blockchain.now();
            assert (input.validSince < now) throw ERR_NOT_YET_VALID_SIGNATURE;
            assert (now < input.validTill) throw ERR_EXPIRED_SIGNATURE;

            var senderAddress = in.senderAddress;
            if (input.restrictions != null) {
                senderAddress = checkRestrictions(input.restrictions.load(), senderAddress);
            }

            val bidAmount = in.valueCoins;
            val auctionConfig = lazy input.auctionConfig.load();
            assert (bidAmount >= auctionConfig.minBid) throw ERR_NOT_ENOUGH_FUNDS;
            if (auctionConfig.isInvalid()) {
                returnBidOnIncorrectConfig(senderAddress);
                return;
            }

            var itemIndex = input.tokenName.bitsHash();

            val deployMsg = createMessage({
                bounce: true,
                dest: calcDeployedTeleItem(itemIndex, contract.getAddress(), storage.nftItemCode),
                value: 0,
                body: TelemintItemDeploy {
                    bidderAddress: senderAddress,
                    bidAmount: bidAmount,
                    tokenInfo: TokenNameAndDomain { tokenName: input.tokenName, domain: storage.fullDomain.load() }.toCell(),
                    nftContent: input.content,
                    auctionConfig: input.auctionConfig,
                    royaltyParams: input.royalty != null ? input.royalty : storage.royaltyParams,
                }
            });
            deployMsg.send(SEND_MODE_CARRY_ALL_REMAINING_MESSAGE_VALUE);
        }

        else => {
            // ignore empty messages, throw for others
            assert (in.body.isEmpty()) throw ERR_UNKNOWN_OP
        }
    }
}

fun onExternalMessage(inMsg: slice) {
    var storage = lazy CollectionStorage.load();
    assert (!storage.isCollectionInitialized) throw ERR_FORBIDDEN_TOUCH;
    acceptExternalMessage();
    storage.isCollectionInitialized = true;
    storage.save();
}



struct CollectionDataReply {
    nextItemIndex: int
    collectionMetadata: cell
    ownerAddress: address
}

get fun get_collection_data(): CollectionDataReply {
    val storage = lazy CollectionStorage.load();

    return {
        nextItemIndex: -1,
        collectionMetadata: storage.content,
        ownerAddress: createAddressNone(),
    }
}

get fun get_nft_address_by_index(index: int): address {
    val storage = lazy CollectionStorage.load();
    val teleitemDeployed = calcDeployedTeleItem(index, contract.getAddress(), storage.nftItemCode);
    return address.fromValidBuilder(teleitemDeployed.buildAddress());
}

get fun get_nft_content(index: int, individualNftContent: cell): cell {
    return individualNftContent;
}
