import "@stdlib/gas-payments"
import "sharding"
import "errors"

// Storage costs
// these constants are used to estimate storage fee (how much we should pay for storing a wallet contract);
// actual storage state (desired value of these constants) are printed to console in StateInit.spec.ts

const STORAGE_SIZE_MaxWallet_bits         = 1026
const STORAGE_SIZE_MaxWallet_cells        = 3
const STORAGE_SIZE_InitStateWallet_bits   = 812
const STORAGE_SIZE_InitStateWallet_cells  = 3

const MESSAGE_SIZE_BurnNotification_bits  = 754   // body = 32+64+124+(3+8+256)+(3+8+256)
const MESSAGE_SIZE_BurnNotification_cells = 1     // body always in ref

const MIN_STORAGE_DURATION = 5 * 365 * 24 * 3600  // 5 years


// Gas costs
// these constants are used to estimate gas fee (how much we should remain on balance for a swap to succeed);
// they must be absolutely equal to consumed gas; if not, tests fail;
// actual consumed gas (desired value of these constants) are printed to console after tests run

const GAS_CONSUMPTION_JettonTransfer    = 6649
const GAS_CONSUMPTION_JettonReceive     = 7855
const GAS_CONSUMPTION_BurnRequest       = 4230
const GAS_CONSUMPTION_BurnNotification  = 4530


fun getPrecompiledGasConsumption(): int?
    asm "GETPRECOMPILEDGAS"

fun calculateJettonWalletMinStorageFee() {
    return calculateStorageFee(MY_WORKCHAIN, MIN_STORAGE_DURATION, STORAGE_SIZE_MaxWallet_bits, STORAGE_SIZE_MaxWallet_cells);
}

fun forwardInitStateOverhead() {
    return calculateForwardFeeWithoutLumpPrice(MY_WORKCHAIN, STORAGE_SIZE_InitStateWallet_bits, STORAGE_SIZE_InitStateWallet_cells);
}

fun checkAmountIsEnoughToTransfer(msgValue: int, forwardTonAmount: int, fwdFee: int) {
    var fwdCount = forwardTonAmount ? 2 : 1; // second sending (forward) will be cheaper that first

    var jettonWalletGasConsumption = getPrecompiledGasConsumption();
    var sendTransferGasConsumption = (jettonWalletGasConsumption == null) ? GAS_CONSUMPTION_JettonTransfer : jettonWalletGasConsumption;
    var receiveTransferGasConsumption = (jettonWalletGasConsumption == null) ? GAS_CONSUMPTION_JettonReceive : jettonWalletGasConsumption;

    assert (msgValue >
        forwardTonAmount +
        // 3 messages: wal1->wal2,  wal2->owner, wal2->response
        // but last one is optional (it is ok if it fails)
        fwdCount * fwdFee +
        forwardInitStateOverhead() + // additional fwd fees related to initstate in iternal_transfer
        calculateGasFee(MY_WORKCHAIN, sendTransferGasConsumption) +
        calculateGasFee(MY_WORKCHAIN, receiveTransferGasConsumption) +
        calculateJettonWalletMinStorageFee()
    ) throw ERROR_NOT_ENOUGH_GAS;
}

fun checkAmountIsEnoughToBurn(msgValue: int) {
    var jettonWalletGasConsumption = getPrecompiledGasConsumption();
    var sendBurnGasConsumption = (jettonWalletGasConsumption == null) ? GAS_CONSUMPTION_BurnRequest : jettonWalletGasConsumption;

    assert (msgValue >
        calculateForwardFee(MY_WORKCHAIN, MESSAGE_SIZE_BurnNotification_bits, MESSAGE_SIZE_BurnNotification_cells) +
        calculateGasFee(MY_WORKCHAIN, sendBurnGasConsumption) +
        calculateGasFee(MY_WORKCHAIN, GAS_CONSUMPTION_BurnNotification)
    ) throw ERROR_NOT_ENOUGH_GAS;
}
