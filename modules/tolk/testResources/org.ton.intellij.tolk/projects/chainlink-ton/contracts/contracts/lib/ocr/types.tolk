import "./../utils.tolk";
import "./exit_codes.tolk";

// Maximum number of oracles the offchain reporting protocol is designed for
const MAX_NUM_ORACLES: int = 255;

const OCR_PLUGIN_TYPE_COMMIT: int = 0;
const OCR_PLUGIN_TYPE_EXECUTE: int = 1;


struct Config {
	configInfo: ConfigInfo;
	signers: UMap<uint256, uint8>;
	transmitters: Map<uint8>;
}

fun newOCRConfig(): Config {
	return Config {
		configInfo: ConfigInfo{},
		signers: UMap<uint256, uint8>.new(256),
		transmitters: Map<uint8>.new()
	}
}


fun Config.assignSignerOracles(
	mutate self,
	signers: cell /*Vector<uint256>*/
) {
	// TODO: Right now we cannot do ed25519 public key validation on chain. For now we assume valid keys.
	var signersDict = UMap<uint256, uint8>.new(256);
	var signersIt = Iterator<uint256>.new(signers);
	var i = 0;
	while (!signersIt.empty()) {
		val signer = signersIt.next();
		assert(!signersDict.has(signer), ERROR_REPEATED_SIGNERS);
		i = i + 1;
		signersDict.set(
			signer,
			i
		);
	}
	self.signers = signersDict;
}

fun Config.assignTransmitterOracles(
	mutate self,
	transmitters: cell, //Vector<address>
) {
	var transmittersDict = Map<uint8>.new(); 
	var transmittersIt = Iterator<address>.new(transmitters);
	var i = 0;
	while (!transmittersIt.empty()) {
		val transmitter = transmittersIt.next();
		assert(!transmittersDict.has(transmitter), ERROR_REPEATED_TRANSMITTERS);
		i = i + 1;
		transmittersDict.set(
			transmitter,
			i
		);
	}
	self.transmitters = transmittersDict;
}

struct ConfigInfo {
	configDigest: uint256 = 0;
	bigF: uint8 = 0;
	n: uint8 = 0;
	isSignatureVerificationEnabled: bool = false;
}

struct SignatureEd25519 {
	r: uint256;
	s: uint256;
	signer: uint256
}

struct ReportContext {
	configDigest: uint256;
	_padding: bytes24;
	sequenceBytes: uint64
}
