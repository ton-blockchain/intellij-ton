import "@stdlib/gas-payments"
import "errors"
import "storage"
import "messages"
import "c5-register-validation"

const SIZE_SIGNATURE = 512

@inline_ref
fun processExtraActions(extraActions: SnakedExtraActions, isExtension: bool) {
    while (true) {
        val action = lazy ExtraAction.fromSlice(extraActions);
        match (action) {
            AddExtensionExtraAction => {
                var (extensionWorkchain, extensionAddrHash) = action.addr.getWorkchainAndHash();
                var myWorkchain = contract.getAddress().getWorkchain();
                assert (myWorkchain == extensionWorkchain) throw ERROR_EXTENSION_WRONG_WORKCHAIN;

                var storage = lazy Storage.load();
                val inserted = storage.extensions.tryInsert(extensionAddrHash);
                assert (inserted) throw ERROR_ADD_EXTENSION;
                storage.save();
            }

            RemoveExtensionExtraAction => {
                var (extensionWorkchain, extensionAddrHash) = action.addr.getWorkchainAndHash();
                var myWorkchain = contract.getAddress().getWorkchain();
                assert (myWorkchain == extensionWorkchain) throw ERROR_EXTENSION_WRONG_WORKCHAIN;

                var storage = lazy Storage.load();
                val removed = storage.extensions.tryDelete(extensionAddrHash);
                assert (removed) throw ERROR_REMOVE_EXTENSION;
                if (!storage.isSignatureAllowed) {
                    assert (storage.extensions != null) throw ERROR_REMOVE_LAST_EXTENSION_WHEN_SIGNATURE_DISABLED;
                }
                storage.save();
            }

            SetSignatureAllowedExtraAction => {
                assert (isExtension) throw ERROR_ONLY_EXTENSION_CAN_CHANGE_SIGNATURE_MODE;

                var storage = lazy Storage.load();
                assert (storage.isSignatureAllowed != action.allowSignature) throw ERROR_THIS_SIGNATURE_MODE_ALREADY_SET;
                if (!action.allowSignature) {
                    assert (storage.extensions != null) throw ERROR_DISABLE_SIGNATURE_WHEN_EXTENSIONS_IS_EMPTY;
                }
                storage.isSignatureAllowed = action.allowSignature;
                storage.save();
            }

            else => throw ERROR_UNSUPPORTED_ACTION
        }
        if (!extraActions.hasNext()) {
            return;
        }
        extraActions = extraActions.getNext();
    }
}

fun processActions(outActions: OutActionsCell?, hasExtraActions: bool, extraActions: SnakedExtraActions, isExternal: bool, isExtension: bool) {
    if (outActions != null) {
        // Simply set the C5 register with all pre-computed actions after verification:
        setTvmRegisterC5(outActions.verifyC5Actions(isExternal));
    }
    if (hasExtraActions) {
        processExtraActions(extraActions, isExtension);
    }
}


type AllowedMessageToWalletV5 = ExtensionActionRequest | InternalSignedRequest

type AllowedExternalMessageToWalletV5 = ExternalSignedRequest


fun onExternalMessage(inMsgBody: slice) {
    var signature = inMsgBody.getLastBits(SIZE_SIGNATURE);
    var signedSlice = inMsgBody.removeLastBits(SIZE_SIGNATURE);

    var msg = AllowedExternalMessageToWalletV5.fromSlice(signedSlice, {
        throwIfOpcodeDoesNotMatch: ERROR_INVALID_MESSAGE_OPERATION,
    });

    var storage = lazy Storage.load();

    assert (isSignatureValid(signedSlice.hash(), signature, storage.publicKey)) throw ERROR_INVALID_SIGNATURE;
    assert (storage.isSignatureAllowed | (storage.extensions == null)) throw ERROR_SIGNATURE_DISABLED;
    assert (msg.seqno == storage.seqno) throw ERROR_INVALID_SEQNO;
    assert (msg.walletId == storage.subwalletId) throw ERROR_INVALID_WALLET_ID;
    assert (msg.validUntil > blockchain.now()) throw ERROR_EXPIRED;

    acceptExternalMessage();

    // Store and commit the seqno increment to prevent replays even if the subsequent requests fail.
    storage.isSignatureAllowed = true;
    storage.seqno += 1;
    storage.save();

    commitContractDataAndActions();
    processActions(msg.outActions, msg.hasExtraActions, msg.extraActions, true, false);
}


@on_bounced_policy("manual")        // tell the compiler "don't insert code to filter out bounced messages"
fun onInternalMessage(in: InMessage) {
    val msg = lazy AllowedMessageToWalletV5.fromSlice(in.body);

    match (msg) {
        InternalSignedRequest => {
            val bodyLongEnough = in.body.remainingBitsCount() >= SIZE_SIGNATURE + 128;
            if (!bodyLongEnough) {
                return;     // don't try to parse it (it will fail), just ignore
            }

            var signature = in.body.getLastBits(SIZE_SIGNATURE);
            var signedSlice = in.body.removeLastBits(SIZE_SIGNATURE);

            var storage = lazy Storage.load();
            if (!isSignatureValid(signedSlice.hash(), signature, storage.publicKey)) {
                return;
            }

            assert (storage.isSignatureAllowed | (storage.extensions == null)) throw ERROR_SIGNATURE_DISABLED;
            assert (msg.seqno == storage.seqno) throw ERROR_INVALID_SEQNO;
            assert (msg.walletId == storage.subwalletId) throw ERROR_INVALID_WALLET_ID;
            assert (msg.validUntil > blockchain.now()) throw ERROR_EXPIRED;

            // Store and commit the seqno increment to prevent replays even if the subsequent requests fail.
            storage.isSignatureAllowed = true;
            storage.seqno += 1;
            storage.save();

            processActions(msg.outActions, msg.hasExtraActions, msg.extraActions, false, false);
        }

        ExtensionActionRequest => {
            var (senderWorkchain, senderAddrHash) = in.senderAddress.getWorkchainAndHash();
            var myWorkchain = contract.getAddress().getWorkchain();
            if (myWorkchain != senderWorkchain) {
                return;
            }

            val storage = lazy Storage.load();
            if (!storage.extensions.exists(senderAddrHash)) {
                // Note that some random contract may have deposited funds with this prefix,
                // so we accept the funds silently instead of throwing an error (wallet v4 does the same).
                return;
            }

            processActions(msg.outActions, msg.hasExtraActions, msg.extraActions, false, true);
        }

        else => {
            // if opcode is strange or body is empty at all (for instance, bounced messages)
            // just ignore, do not throw
        }
    }
}



get fun is_signature_allowed(): bool {
    val storage = lazy Storage.load();
    return storage.isSignatureAllowed;
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

// Returns raw dictionary (or null if empty) where keys are address hashes. Workchains of extensions are same with wallet smart contract workchain
get fun get_extensions(): dict {
    val storage = lazy Storage.load();
    return storage.extensions;
}
