import "@stdlib/tvm-dicts"
import "@stdlib/gas-payments"
import "@stdlib/lisp-lists"
import "storage"
import "message-validation"
import "errors"
import "messages"

type AllowedMessageToVestingWallet = RequestAddToWhitelist | AskToSendMessage

fun onInternalMessage(in: InMessage) {
    val msg = lazy AllowedMessageToVestingWallet.fromSlice(in.body);

    match (msg) {
        AskToSendMessage => {
            var storage = lazy Storage.load();
            val vesting = lazy storage.vestingParameters.load();
            assert (in.senderAddress == vesting.ownerAddress, 0);

            val attachedMsg = msg.attachedMsg;
            var lockedAmount = vesting.getLockedAmount(blockchain.now());
            if (lockedAmount > 0) {
                // if the vesting has expired, you can send any messages
                attachedMsg.validateIfLockedAmount(storage.whitelist, lockedAmount, vesting.senderAddress);
            }
            sendRawMessage(attachedMsg.msgCell, attachedMsg.sendMode);

            val excessesMsg = createMessage({
                bounce: true,
                dest: in.senderAddress,
                value: 0,
                body: SendExcesses {
                    queryId: msg.queryId
                }
            });
            excessesMsg.send(SEND_MODE_CARRY_ALL_REMAINING_MESSAGE_VALUE);
        }

        RequestAddToWhitelist => {
            var storage = lazy Storage.load();
            val vesting = lazy storage.vestingParameters.load();
            assert (in.senderAddress == vesting.senderAddress, 0);

            var addrToWhitelist = msg.addrToWhitelist;
            var nextRef = msg.restRefs;
            do {
                storage.whitelist.addWhitelisted(addrToWhitelist);

                var moreRefs = nextRef.remainingRefsCount();
                if (moreRefs) {
                    nextRef = nextRef.loadRef().beginParse();
                    addrToWhitelist = nextRef.loadAddress();
                }
            } while (moreRefs);
            storage.save();

            val excessesMsg = createMessage({
                bounce: true,
                dest: in.senderAddress,
                value: 0,
                body: ResponseAddedToWhitelist {
                    queryId: msg.queryId
                }
            });
            excessesMsg.send(SEND_MODE_CARRY_ALL_REMAINING_MESSAGE_VALUE);
        }

        else => {
            // on unknown opcode, just accept coins from anyone, do not throw
        }
    }
}

fun onExternalMessage(extBody: slice) {
    // extBody is: signature + signed data
    var signature = extBody.loadBits(512);
    val hashOfAllAfter = extBody.hash();
    val msg = ExternalMsgAfterSignature.fromSlice(extBody);
    assert (msg.validUntil > blockchain.now()) throw ERROR_EXPIRED;

    var storage = lazy Storage.load();
    assert (msg.seqno == storage.seqno) throw ERROR_INVALID_SEQNO;
    assert (msg.subwalletId == storage.subwalletId) throw ERROR_INVALID_SUBWALLET_ID;
    assert (isSignatureValid(hashOfAllAfter, signature, storage.publicKey)) throw ERROR_INVALID_SIGNATURE;

    acceptExternalMessage();

    try {
        val attachedMsg = AttachedMessage.fromSlice(msg.rest);
        val vesting = storage.vestingParameters.load();
        var lockedAmount = vesting.getLockedAmount(blockchain.now());
        if (lockedAmount > 0) {
            // if the vesting is in progress, validate a message before sending it
            attachedMsg.validateIfLockedAmount(storage.whitelist, lockedAmount, vesting.senderAddress);
        } else {
            // if not (the vesting has expired), you can send any messages
        }
        sendRawMessage(attachedMsg.msgCell, attachedMsg.sendMode);
    } catch {
    }

    storage.seqno += 1;
    storage.save();
}



struct VestingDataReply {
    vestingParameters: VestingParameters
    whitelist: dict
}

get fun seqno(): int {
    val storage = lazy Storage.load();
    return storage.seqno;
}

get fun get_subwallet_id(): int {
    val storage = lazy Storage.load();
    return storage.subwalletId;
}

get fun get_public_key(): int {
    val storage = lazy Storage.load();
    return storage.publicKey;
}

get fun get_vesting_data(): VestingDataReply {
    val storage = lazy Storage.load();

    return {
        vestingParameters: storage.vestingParameters.load(),
        whitelist: storage.whitelist,
    }
}

get fun is_whitelisted(address: address): bool {
    val storage = lazy Storage.load();
    return storage.whitelist.isWhitelisted(address);
}

get fun get_whitelist(): tuple {
    val storage = lazy Storage.load();
    var d: dict = storage.whitelist;

    var list: tuple? = null;
    do {
        var (key: slice?, value: slice?, found: bool) = d.sDictDeleteFirstAndGet(ADDRESS_SIZE);
        if (found) {
            var (wc, addressHash) = (key! as address).getWorkchainAndHash();
            list = listPrepend([wc, addressHash], list);
        }
    } while (found);

    return list!;
}

get fun get_locked_amount(atTime: int): coins {
    val storage = lazy Storage.load();
    return storage.vestingParameters.load().getLockedAmount(atTime);
}
