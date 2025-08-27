import "@stdlib/tvm-dicts"
import "errors"
import "storage"
import "messages"

fun deployNftItem(itemIndex: int, nftItemCode: cell, attachTonAmount: coins, initParams: Cell<NftItemInitAtDeployment>) {
    val deployMsg = createMessage({
        bounce: true,
        dest: calcDeployedNftItem(itemIndex, contract.getAddress(), nftItemCode),
        value: attachTonAmount,
        body: initParams,
    });
    deployMsg.send(SEND_MODE_PAY_FEES_SEPARATELY);
}

type AllowedMessageToNftCollection =
    | RequestRoyaltyParams
    | DeployNft
    | BatchDeployNfts
    | ChangeCollectionAdmin

fun onInternalMessage(in: InMessage) {
    val msg = lazy AllowedMessageToNftCollection.fromSlice(in.body);

    match (msg) {
        DeployNft => {
            var storage = lazy NftCollectionStorage.load();
            assert (in.senderAddress == storage.adminAddress) throw ERROR_NOT_FROM_ADMIN;
            assert (msg.itemIndex <= storage.nextItemIndex) throw ERROR_INVALID_ITEM_INDEX;

            var isLast = msg.itemIndex == storage.nextItemIndex;
            deployNftItem(msg.itemIndex, storage.nftItemCode, msg.attachTonAmount, msg.initParams);
            if (isLast) {
                storage.nextItemIndex += 1;
                storage.save();
            }
        }

        RequestRoyaltyParams => {
            val storage = lazy NftCollectionStorage.load();
            val respondMsg = createMessage({
                bounce: false,
                dest: in.senderAddress,
                value: 0,
                body: ResponseRoyaltyParams {
                    queryId: msg.queryId,
                    royaltyParams: storage.royaltyParams.load(),
                }
            });
            respondMsg.send(SEND_MODE_CARRY_ALL_REMAINING_MESSAGE_VALUE);
        }

        BatchDeployNfts => {
            var storage = lazy NftCollectionStorage.load();
            assert (in.senderAddress == storage.adminAddress) throw ERROR_NOT_FROM_ADMIN;

            var counter = 0;
            var deployList = msg.deployList;
            while (true) {
                var (itemIndex, valueSlice, found) = deployList.uDictDeleteFirstAndGet(64);
                if (!found) {
                    storage.save();
                    return;
                }
                itemIndex = itemIndex!;     // tell the compiler that it's not-null (because found)

                counter += 1;
                assert (counter < 250) throw ERROR_BATCH_LIMIT_EXCEEDED;   // due to limits of action list size

                assert (itemIndex <= storage.nextItemIndex) throw ERROR_NOT_FROM_ADMIN + counter;
                val dictItem = BatchDeployDictItem.fromSlice(valueSlice!);
                deployNftItem(itemIndex, storage.nftItemCode, dictItem.attachTonAmount, dictItem.initParams);
                if (itemIndex == storage.nextItemIndex) {
                    storage.nextItemIndex += 1;
                }
            }
        }

        ChangeCollectionAdmin => {
            var storage = lazy NftCollectionStorage.load();
            assert (in.senderAddress == storage.adminAddress) throw ERROR_NOT_FROM_ADMIN;
            storage.adminAddress = msg.newAdminAddress;
            storage.save();
        }

        else => {
            // ignore empty messages, "wrong opcode" for others
            assert (in.body.isEmpty()) throw 0xFFFF
        }
    }
}



struct CollectionDataReply {
    nextItemIndex: int
    collectionMetadata: cell
    adminAddress: address
}

struct (0x01) OffchainMetadataReply {
    string: SnakeString
}

get fun get_collection_data(): CollectionDataReply {
    val storage = lazy NftCollectionStorage.load();
    val content = lazy storage.content.load();

    return {
        nextItemIndex: storage.nextItemIndex,
        collectionMetadata: content.collectionMetadata,
        adminAddress: storage.adminAddress,
    }
}

get fun get_nft_address_by_index(itemIndex: int): address {
    val storage = lazy NftCollectionStorage.load();
    val nftDeployed = calcDeployedNftItem(itemIndex, contract.getAddress(), storage.nftItemCode);
    return address.fromValidBuilder(nftDeployed.buildAddress());
}

get fun royalty_params(): RoyaltyParams {
    val storage = lazy NftCollectionStorage.load();
    return storage.royaltyParams.load();
}

get fun get_nft_content(itemIndex: int, individualNftContent: Cell<SnakeString>): Cell<OffchainMetadataReply> {
    val storage = lazy NftCollectionStorage.load();
    val content = lazy storage.content.load();

    // construct a responce from "common content" and "individual content";
    // for example:
    // common content = "https://site.org/my-collection/"
    // individual nft = "my-picture-123.png"  (probably, a long string, snake-encoded)
    // result         = concatenation
    return OffchainMetadataReply {
        string: beginCell()
            .storeSlice(content.commonContent.load())   // assume it's short (no refs)
            .storeRef(individualNftContent)             // so, it's the first ref (snake encoding)
            .endCell().beginParse()
    }.toCell()
}
