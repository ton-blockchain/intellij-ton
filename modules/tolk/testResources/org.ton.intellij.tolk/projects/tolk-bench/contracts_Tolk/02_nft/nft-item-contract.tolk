import "@stdlib/gas-payments"
import "errors"
import "storage"
import "messages"
import "fees-management"

type AllowedMessageToNftItem =
    | AskToChangeOwnership
    | RequestStaticData

fun onInternalMessage(in: InMessage) {
    var loadingStorage = startLoadingNftItemStorage();
    if (!loadingStorage.isInitialized()) {
        val uninitedSt = loadingStorage.parseNotInitialized();
        assert (in.senderAddress == uninitedSt.collectionAddress) throw ERROR_NOT_FROM_COLLECTION;

        // using a message from collection, convert "uninitialized" to "initialized" state
        val initParams = NftItemInitAtDeployment.fromSlice(in.body);
        val storage: NftItemStorage = {
            itemIndex: uninitedSt.itemIndex,
            collectionAddress: uninitedSt.collectionAddress,
            ownerAddress: initParams.ownerAddress,
            content: initParams.content,
        };
        storage.save();
        return;
    }

    var storage = loadingStorage.parseInitialized();

    val msg = lazy AllowedMessageToNftItem.fromSlice(in.body);

    match (msg) {
        AskToChangeOwnership => {
            assert (in.senderAddress == storage.ownerAddress) throw ERROR_NOT_FROM_OWNER;

            assert (msg.forwardPayload.remainingBitsCount()) throw ERROR_INCORRECT_FORWARD_PAYLOAD;
            assert (msg.newOwnerAddress.getWorkchain() == BASECHAIN) throw ERROR_INVALID_WORKCHAIN;

            val fwdFee = in.originalForwardFee;
            var restAmount = contract.getOriginalBalance() - MIN_TONS_FOR_STORAGE;
            if (msg.forwardTonAmount) {
                restAmount -= (msg.forwardTonAmount + fwdFee);
            }
            var needResponse = msg.sendExcessesTo.isInternal();
            if (needResponse) {
                assert (msg.sendExcessesTo.getWorkchain() == BASECHAIN) throw ERROR_INVALID_WORKCHAIN;
                restAmount -= fwdFee;
            }

            assert (restAmount >= 0) throw ERROR_TOO_SMALL_REST_AMOUNT; // base nft spends fixed amount of gas, will not check for response

            if (msg.forwardTonAmount) {
                val ownershipMsg = createMessage({
                    bounce: false,
                    dest: msg.newOwnerAddress,
                    value: msg.forwardTonAmount,
                    body: NotificationForNewOwner {
                        queryId: msg.queryId,
                        oldOwnerAddress: storage.ownerAddress,
                        payload: msg.forwardPayload,
                    }
                });
                ownershipMsg.send(SEND_MODE_PAY_FEES_SEPARATELY);
            }
            if (needResponse) {
                val excessesMsg = createMessage({
                    bounce: false,
                    dest: msg.sendExcessesTo,
                    value: restAmount,
                    body: ReturnExcessesBack {
                        queryId: msg.queryId,
                    }
                });
                excessesMsg.send(SEND_MODE_PAY_FEES_SEPARATELY);
            }

            storage.ownerAddress = msg.newOwnerAddress;
            storage.save();
        }

        RequestStaticData => {
            val respondMsg = createMessage({
                bounce: false,
                dest: in.senderAddress,
                value: 0,
                // why ever, in FunC implementation, item index was encoded as 256-bit (not 64-bit),
                // do the same here to pass FunC tests;
                // but since a response becomes too long (64 + 256 + address), the compiler will create a ref;
                // force the compiler not to create a ref, because it's guaranteed that with
                // value (coins) = 0 it will nevertheless fit into a message cell directly
                body: UnsafeBodyNoRef {
                    forceInline: ResponseStaticData {
                        queryId: msg.queryId,
                        itemIndex: storage.itemIndex as uint256,
                        collectionAddress: storage.collectionAddress,
                    }
                }
            });
            respondMsg.send(SEND_MODE_CARRY_ALL_REMAINING_MESSAGE_VALUE);
        }

        else => {
            // ignore empty messages, "wrong opcode" for others
            assert (in.body.isEmpty()) throw 0xFFFF
        }
    }
}



struct NftDataReply {
    isInitialized: bool
    itemIndex: int
    collectionAddress: address
    ownerAddress: address? = null
    content: Cell<SnakeString>? = null
}

get fun get_nft_data(): NftDataReply {
    var loadingStorage = startLoadingNftItemStorage();
    if (!loadingStorage.isInitialized()) {
        val uninitedSt = loadingStorage.parseNotInitialized();
        return {
            isInitialized: false,
            itemIndex: uninitedSt.itemIndex,
            collectionAddress: uninitedSt.collectionAddress,
        }
    }

    val storage = loadingStorage.parseInitialized();
    return {
        isInitialized: true,
        itemIndex: storage.itemIndex,
        collectionAddress: storage.collectionAddress,
        ownerAddress: storage.ownerAddress,
        content: storage.content,
    }
}
