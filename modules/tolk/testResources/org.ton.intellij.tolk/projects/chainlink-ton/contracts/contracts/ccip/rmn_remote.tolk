import "types.tolk";
import "../lib/access/ownable_2step.tolk";
import "../lib/upgrades/type_and_version.tolk";

struct Storage {
    ownable: Ownable2Step;
}

fun Storage.load() {
    return Storage.fromCell(contract.getData());
}

fun Storage.store(self) {
    return contract.setData(self.toCell());
}

fun onInternalMessage(msgCell: cell, msgBody: slice) {
}


get fun typeAndVersion(): (slice, slice) {
     return ("com.chainlink.ton.ccip.RMNRemote", "1.0.0");
 }
