import "@stdlib/tvm-lowlevel";
import "@stdlib/tvm-dicts.tolk";
import "../utils.tolk";

/// Upgradeable contract interface.
/// This interface defines the structure and methods required for a contract to
/// be upgradeable.
///
/// - `migrate`: A function that migrates the storage from the old
/// version to the new version. Its method_id must be set explicitly in the
/// contract, and be consistent across versions.
/// - `version`: A function that returns the version of the contract as a
/// string. e.g. "1.0.0".
/// - `requireUpgrade`: Enforces any requirements for upgrading the contract,
/// like ownership, and throws an error if not met. It is typed as a struct to
/// allow for more complex upgrade requirements. passing extra arguments to the
/// upgrade function, such as the contract owner address.
///
/// Example usage:
/// ```tolk
/// struct StorageV2 {
///     value: uint64;
///     id: uint32;
///     ownable2Step: Ownable2Step;
/// }
///
/// fun loadData() {
///     return StorageV2.fromCell(contract.getData());
/// }
///
/// fun saveData(data: StorageV2) {
///     contract.setData(data.toCell());
/// }
/// 
/// @method_id(1000)
/// fun migrate(c: cell): cell {
///     // Parse it as StorageV1
///     var oldStorage = StorageV1.fromCell(c);
///     // Create new StorageV2 with the old value and id
///     var newStorage = StorageV2{
///         value: oldStorage.value as uint64,
///         id: oldStorage.id,
///         ownable2Step: oldStorage.ownable2Step,
///     };
///     return newStorage.toCell();
/// }
/// @method_id(1001)
/// fun version(): slice { return "2.0.0"; }
///
/// struct requireUpgradeAutoArgs {
///     ownable2Step: Ownable2Step;
///     sender: address;
/// }
///
/// fun requireUpgrade(autoargs: requireUpgradeAutoArgs) {
///     autoargs.ownable2Step.requireOwner(autoargs.sender)
/// }
///
/// fun onInternalMessage(myBalance: int, msgValue: int, msgFull: cell, msgBody: slice) {
///     var cs: slice = msgFull.beginParse();
///     val flags = cs.loadMessageFlags();
///     var sender: address = cs.loadAddress();
///     var storage = loadData();
///     val handled = Upgradeable<requireUpgradeAutoArgs>{
///         version: version,
///         migrate: migrate,
///         requireUpgrade: Upgradeable_requireUpgrade<requireUpgradeAutoArgs> {
///             call: requireUpgrade,
///             autoArgs: requireUpgradeAutoArgs {
///                 ownable2Step: storage.ownable2Step,
///                 sender: sender,
///             },
///         },
///     }.onInternalMessage(myBalance, msgValue, msgFull, msgBody);
///     if (handled) {
///         return;
///     }
/// ```
struct Upgradeable {
    /// Abstract methods that must be implemented by the contract.
    migrate: (cell) -> cell;
    version: () -> slice;
}


/// Message for upgrading a contract.
struct (0x0aa811ed) Upgradeable_Upgrade {
    queryId: uint64;
    code: cell;
}

struct UpgradedEvent {
    /// The new code of the contract.
    code: cell;
    /// The SHA256 hash of the new code.
    hash: uint256;
    /// The version of the contract after the upgrade.
    version: UnsafeBodyNoRef<slice>;
}

const UPGRADED_EVENT = 0xa33b498e; // Event topic for the upgrade event

fun Upgradeable.upgrade(self, code: cell) {
    // Schedule the code to be changed after the compute phase
    contract.setCodePostponed(code);
    // Load the code dinamically
    var cont = transformSliceToContinuation(code.beginParse());
    setTvmRegisterC3(cont); // After this line, we are running the new code
    // Call the migrate function implemented by the new version
    var c = contract.getData();
    var newStorage = self.migrate(c);
    contract.setData(newStorage);
    emit(UPGRADED_EVENT, UpgradedEvent{
        code: code,
        hash: code.hash(),
        version: UnsafeBodyNoRef {
            forceInline: self.version(), // This is expected to be the version of the new code
            },
        });
}
