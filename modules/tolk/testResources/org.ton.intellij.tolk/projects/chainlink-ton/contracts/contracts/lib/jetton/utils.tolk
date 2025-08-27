import "@stdlib/common.tolk"

// Jetton wallet utilities for Tolk
const JETTON_TOPIC : int = 0x351 // for easier indexing

// Jetton Wallet state structure  
struct JettonWalletData {
    status: uint4
    balance: coins
    ownerAddress: address
    jettonMasterAddress: address
}

// Calculate jetton wallet state init
fun calculateJettonWalletStateInit(
    ownerAddress: address,
    jettonMasterAddress: address,
    jettonWalletCode: cell,
): cell {
    return StateInit {
        fixedPrefixLength: null,
        special: null,
        code: jettonWalletCode,
        data: JettonWalletData {
            status: 0,
            balance: 0,
            ownerAddress: ownerAddress,
            jettonMasterAddress: jettonMasterAddress,
        }
            .toCell(),
        library: null,
    }
        .toCell();
}

fun calculateUserJettonWalletAddress(
    ownerAddress: address,
    jettonMasterAddress: address,
    jettonWalletCode: cell,
): address {
    val stateInit = calculateJettonWalletStateInit(
        ownerAddress,
        jettonMasterAddress,
        jettonWalletCode
    );
    val addrBuilder = AutoDeployAddress { stateInit: stateInit, toShard: null }.buildAddress();
    return address.fromValidBuilder(addrBuilder);
}
