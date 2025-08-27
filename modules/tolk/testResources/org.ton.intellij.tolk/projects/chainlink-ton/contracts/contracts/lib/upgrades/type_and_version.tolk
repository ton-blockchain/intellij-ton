/// Implements getters for contract type and version, current code and code hash.

/// The implementor is expected to implement a `typeAndVersion(): (slice, slice)` getter, wrapping a call to `TypeAndVersion.typeAndVersion;`
///
/// Example:
///
/// ```tolk
/// get typeAndVersion(): (slice, slice) {
///     return TypeAndVersion {
///         typeStr: "com.chainlink.ton.examples.Counter",
///         versionStr: "1.0.0",
///     }.typeAndVersion();
/// }
/// ```
///
/// `typeStr` must be a Reverse Domain Name Notation string that is unique to the contract and should not change between versions.
/// Example: "com.chainlink.project.package.ContractName"
/// Read more about Reverse DNS Notation at https://en.wikipedia.org/wiki/Reverse_domain_name_notation
///
/// `versionStr` must be a semantic version string (e.g. "1.0.0").
struct TypeAndVersion {
    typeStr: slice,
    versionStr: slice,
}

/// A getter returning the current type and version of the contract.
fun TypeAndVersion.typeAndVersion(self): (slice, slice) {
    return (self.typeStr, self.versionStr);
}

// Returns the current code of the contract.
get fun code(): cell {
    return contract.getCode();
}

// Returns the sha256 hash of the current code of the contract.
get fun codeHash(): int {
    return contract.getCode().hash();
}


