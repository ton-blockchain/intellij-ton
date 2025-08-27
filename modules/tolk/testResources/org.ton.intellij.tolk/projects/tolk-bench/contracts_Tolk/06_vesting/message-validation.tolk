// The vesting contract is low-level.
// For instance, when it proxies messages to masterchain, it validates those messages
// to ensure they have known opcodes.
// This file is dedicated to parsing and validating raw message cells.

import "storage"
import "errors"
import "messages"

// https://github.com/ton-blockchain/ton/blob/d2b418bb703ed6ccd89b7d40f9f1e44686012014/crypto/block/block.tlb#L605
const CONFIG_CONFIG_ID = 0
const ELECTOR_CONFIG_ID = 1

// contract "elector"
// https://github.com/ton-blockchain/ton/blob/master/crypto/smartcont/elector-code.fc

type AllowedOpToElectorContract =
    | AllowedOpElectorNewStake
    | AllowedOpElectorRecoverStake
    | AllowedOpElectorVoteForComplaint
    | AllowedOpElectorVoteForProposal

struct (0x4e73744b) AllowedOpElectorNewStake
struct (0x47657424) AllowedOpElectorRecoverStake
struct (0x56744370) AllowedOpElectorVoteForComplaint
struct (0x566f7465) AllowedOpElectorVoteForProposal

// contract "config"

type AllowedOpToConfigContract = AllowedOpElectorVoteForProposal

// other contracts, enumerate well-known opcodes

struct (0x00001000) AllowedOpSingleNominatorPoolWithdraw
struct (0x00001001) AllowedOpSingleChangeValidator
struct (0x47d54391) AllowedOpTonStakersDeposit
struct (0x595f07bc) AllowedOpJettonBurn
struct (0x69fb306c) AllowedOpTonStakersVote
struct (0x00000000) AllowedOpText { rest: RemainingBitsAndRefs }

type AllowedOpToOtherContract =
    | AllowedOpSingleNominatorPoolWithdraw
    | AllowedOpSingleChangeValidator
    | AllowedOpTonStakersDeposit
    | AllowedOpJettonBurn
    | AllowedOpTonStakersVote
    | AllowedOpElectorVoteForComplaint
    | AllowedOpElectorVoteForProposal
    | AllowedOpText

struct (0x64) AllowedOpTextAction_d  // single letter "d", etc
struct (0x77) AllowedOpTextAction_w
struct (0x44) AllowedOpTextAction_D
struct (0x57) AllowedOpTextAction_W

type AllowedOpTextAction =
    | AllowedOpTextAction_d
    | AllowedOpTextAction_w
    | AllowedOpTextAction_D
    | AllowedOpTextAction_W

fun address.matchesAddressFromConfig(self, configId: int): bool {
    var (addrWc, addrHash) = self.getWorkchainAndHash();
    var matched = false;
    if (addrWc == MASTERCHAIN) {
        // "elector" and "config" always exist in blockchain config params, it's 256-bit addr hash
        var configCell = blockchain.configParam(configId);
        matched = addrHash == configCell!.beginParse().preloadUint(256);
    }
    return matched;
}

// parsing message cells (TL/B int_msg_info)
// we split this into two parts: flags + addresses and all the rest (on demand),
// because "all the rest" is not needed always

struct MessageCellContentsStart {
    flags: uint4
    senderAddress: address
    destinationAddress: address
}

fun MessageCellContentsStart.isBounceable(self) {
    return (self.flags & 2) == 2
}

struct MessageCellContentsRest {
    valueCoins: coins
    maybeExtraCurrency: bool
    ihrFee: coins
    fwdFee: coins
    createdLt: uint64
    createdAt: uint32
    maybeStateInit: bool
    maybeBodyRef: bool
    refOrInline: RemainingBitsAndRefs
}

fun MessageCellContentsRest.hasStateInit(self) {
    return self.maybeStateInit
}

fun MessageCellContentsRest.getBodySlice(self): slice {
    var refOrInline = self.refOrInline;
    return self.maybeBodyRef ? refOrInline.loadRef().beginParse() : refOrInline
}

fun validateMessageToElectorContract(body: slice) {
    val msg = lazy AllowedOpToElectorContract.fromSlice(body);
    match (msg) {
        AllowedOpElectorNewStake => { }
        AllowedOpElectorRecoverStake => { }
        AllowedOpElectorVoteForComplaint => { }
        AllowedOpElectorVoteForProposal => { }
        else => throw ERROR_COMMENT_NOT_ALLOWED
    }
}

fun validateMessageToConfigContract(body: slice) {
    val msg = lazy AllowedOpToConfigContract.fromSlice(body);
    match (msg) {
        AllowedOpElectorVoteForProposal => { }
        else => throw ERROR_COMMENT_NOT_ALLOWED
    }
}

@inline_ref
fun AttachedMessage.validateIfLockedAmount(self, whitelist: WhitelistDict, lockedAmount: coins, vestingSenderAddress: address) {
    assert (self.sendMode == SEND_MODE_IGNORE_ERRORS + SEND_MODE_PAY_FEES_SEPARATELY) throw ERROR_SEND_MODE_NOT_ALLOWED;

    // start parsing raw msgCell
    var msgCs = self.msgCell.beginParse();
    val cellHeader: MessageCellContentsStart = msgCs.loadAny();
    val destinationAddress = cellHeader.destinationAddress;

    // can send any message to a vesting sender
    if (destinationAddress == vestingSenderAddress) {
        return;
    }

    // can send only to whitelisted
    if (!whitelist.isWhitelisted(destinationAddress)) {
        reserveToncoinsOnBalance(lockedAmount, RESERVE_MODE_AT_MOST);
        return;
    }

    // a message must be bounceable
    assert (cellHeader.isBounceable()) throw ERROR_NON_BOUNCEABLE_NOT_ALLOWED;

    // parse raw msg cell up to the end
    val restOfCell = lazy MessageCellContentsRest.fromSlice(msgCs);

    // a message can not contain StateInit
    assert (!restOfCell.hasStateInit()) throw ERROR_STATE_INIT_NOT_ALLOWED;

    var body = restOfCell.getBodySlice();

    if (destinationAddress.matchesAddressFromConfig(ELECTOR_CONFIG_ID)) {
        validateMessageToElectorContract(body);
        return;
    }

    if (destinationAddress.matchesAddressFromConfig(CONFIG_CONFIG_ID)) {
        validateMessageToConfigContract(body);
        return;
    }

    // okay, it's a message for some other destination (not elector, not config);
    // empty — allowed

    if (body.isEmpty()) {
        return;
    }

    // not empty — check by known opcodes

    val msg = lazy AllowedOpToOtherContract.fromSlice(body);
    match (msg) {
        AllowedOpText => {
            if (msg.rest.remainingBitsCount()) {
                // allowed "d", "w", and some others
                val action = lazy AllowedOpTextAction.fromSlice(msg.rest);
                match (action) {
                    AllowedOpTextAction_d => { }
                    AllowedOpTextAction_w => { }
                    AllowedOpTextAction_D => { }
                    AllowedOpTextAction_W => { }
                    else => throw ERROR_SYMBOLS_NOT_ALLOWED
                }
                return;
            }
            // empty text comment allowed
        }
        AllowedOpSingleNominatorPoolWithdraw => { }
        AllowedOpSingleChangeValidator => { }
        AllowedOpTonStakersDeposit => { }
        AllowedOpJettonBurn => { }
        AllowedOpTonStakersVote => { }
        AllowedOpElectorVoteForComplaint => { }
        AllowedOpElectorVoteForProposal => { }
        else => throw ERROR_COMMENT_NOT_ALLOWED
    }
}

