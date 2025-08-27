import "./types.tolk";
import "./exit_codes.tolk";
import "./../utils.tolk";


//crc32("OCR3Base_ConfigSet")
const OCR3BASE_CONFIG_SET_TOPIC: int = stringCrc32("OCR3Base_ConfigSet");

//crc32("OCR3Base_Transmitted")
const OCR3BASE_TRANSMITTED: int = stringCrc32("OCR3Base_Transmitted");

/// @notice Event, triggers a new run of the offchain reporting protocol.
/// @param ocrPluginType OCR plugin type for which the config was set.
/// @param configDigest configDigest of this configuration.
/// @param signers ith element is address ith oracle uses to sign a report.
/// @param transmitters ith element is address ith oracle uses to transmit a report via the transmit method.
/// @param F maximum number of faulty/dishonest oracles the protocol can tolerate while still working correctly.
struct OCR3Base_ConfigSet {
    ocrPluginType: uint16;
    configDigest: uint256;
    signers: cell; //Vector<uint256>
    transmitters: cell; // Vector<address>
    bigF: uint8;
}


/// @notice Event, optionally emitted to indicate the latest configDigest and sequence number
/// for which a report was successfully transmitted. Alternatively, the contract may
/// use latestConfigDigestAndEpoch with scanLogs set to false.
struct OCR3Base_Transmitted {
   ocrPluginType: uint16;
   configDigest: uint256;
   sequenceNumber: uint64
}

/// @notice Sets offchain reporting protocol configuration incl. participating oracles.
/// NOTE: The OCR3 config must be sanity-checked against the home-chain registry configuration, to ensure home-chain
/// and remote-chain parity!
/// @param ocrConfigArgs OCR config update args.
/// @dev precondition number of transmitters should match the expected F/fChain relationship.
/// For transmitters, the function only validates that len(transmitters) > 0 && len(transmitters) <= MAX_NUM_ORACLES
/// && len(transmitters) <= len(signers) [if sig verification is enabled].
// 0x2B78359F = crc32('OCR3Base_SetOCR3Config')
struct (0x2B78359F) OCR3Base_SetOCR3Config {
    queryId: uint64;
    configDigest: uint256;
    ocrPluginType: uint16;
    bigF: uint8;
    isSignatureVerificationEnabled: bool;
    signers: cell; //Vector<uint256>;
    transmitters: cell; //Vector<address>;
}

struct OCR3Base {
	chainId: uint8; //TODO :Is this even necessary?
	commit: Cell<Config>?;
	execute: Cell<Config>?;
}

fun OCR3Base.getConfig(self, ocrPluginType: uint16): Config? {
	if (ocrPluginType == OCR_PLUGIN_TYPE_COMMIT) {
		if (self.commit == null) {
			return null;
		}
		return self.commit!.load();

	} else if (ocrPluginType == OCR_PLUGIN_TYPE_EXECUTE) {
		if (self.execute == null) {
			return null;
		}
		return self.execute!.load();
	} else {
		throw ERROR_NON_EXISTENT_OCR_PLUGIN_TYPE;
	}
}

fun OCR3Base.setOcr3Config(mutate self, msg: OCR3Base_SetOCR3Config) {
	assert(msg.bigF > 0, ERROR_BIG_F_MUST_BE_POSITIVE);

	var ocrConfig = self.getConfig(msg.ocrPluginType);
	if (ocrConfig == null) {
		ocrConfig = newOCRConfig();
	} 
	ocrConfig = ocrConfig!;

	// If F is 0, then the config is not yet set.
	if (ocrConfig.configInfo.bigF == 0) {
		ocrConfig.configInfo.isSignatureVerificationEnabled = msg.isSignatureVerificationEnabled;
	} else {
		assert(
			ocrConfig.configInfo.isSignatureVerificationEnabled == msg.isSignatureVerificationEnabled,
			ERROR_STATIC_CONFIG_CANNOT_BE_CHANGED
		);
	}
	val transmitters_length = countAddresses(msg.transmitters);

	assert(transmitters_length <= MAX_NUM_ORACLES, ERROR_TOO_MANY_TRANSMITTERS);
	assert(transmitters_length > 0, ERROR_NO_TRANSMITTERS);
	
	if (msg.isSignatureVerificationEnabled) {
		val signers_length = Iterator<uint256>.new(msg.signers).countBytes() / 32;
		assert(signers_length > 0, ERROR_NO_SIGNERS);
		assert(signers_length <= MAX_NUM_ORACLES, ERROR_TOO_MANY_SIGNERS);
		assert(signers_length > 3 * msg.bigF, ERROR_BIG_F_TOO_HIGH);
		assert(signers_length >= transmitters_length, ERROR_TOO_MANY_TRANSMITTERS);
		ocrConfig.configInfo.n = signers_length;
		ocrConfig.assignSignerOracles(msg.signers);
	}
	ocrConfig.assignTransmitterOracles(msg.transmitters);
	ocrConfig.configInfo.bigF = msg.bigF;
	ocrConfig.configInfo.configDigest = msg.configDigest;
	
	if (msg.ocrPluginType == OCR_PLUGIN_TYPE_COMMIT) {
		self.commit = ocrConfig.toCell();
	} else if (msg.ocrPluginType == OCR_PLUGIN_TYPE_EXECUTE) {
		self.execute = ocrConfig.toCell();
	}

	emit(OCR3BASE_CONFIG_SET_TOPIC, OCR3Base_ConfigSet {
			ocrPluginType: msg.ocrPluginType,
			configDigest: ocrConfig.configInfo.configDigest,
			signers: msg.signers,
			transmitters: msg.transmitters,
			bigF: ocrConfig.configInfo.bigF
		}
	);
}

fun OCR3Base.transmit(
    mutate self,
    transmitter: address,
    ocrPluginType: int,
    reportContext: ReportContext,
    report: cell,
    signatures: cell, //Vector<SignatureEd25519>
) {
    var ocrConfig = self.getConfig(ocrPluginType);
    assert(ocrConfig != null, ERROR_NON_EXISTENT_OCR_PLUGIN_TYPE);
    ocrConfig = ocrConfig!;
    var configInfo: ConfigInfo = ocrConfig.configInfo;
    
    // TODO: EVM checks transaction data length here, I think it's probably too expensive to do this in TON 
    
    assert(reportContext.configDigest == configInfo.configDigest, ERROR_CONFIG_DIGEST_MISMATCH);
    
    // TODO: EVM checks assert_chain_not_forked, not sure how to do this check or if it's even necessary in TON
    
    val pluginTransmitters = ocrConfig.transmitters;

    assert(pluginTransmitters.has(transmitter), ERROR_UNAUTHORIZED_TRANSMITTER);

    if (configInfo.isSignatureVerificationEnabled) {
    	var expectedSignatures = configInfo.bigF + 1;
	val signatures_length = Iterator<SignatureEd25519>.new(signatures).countBytes() / 96; //expensive
    	assert(signatures_length == expectedSignatures, ERROR_WRONG_NUMBER_OF_SIGNATURES);
    	var hashedReport = self.hashReport(report, reportContext);
    	var pluginSigners = ocrConfig.signers;

    	self.verifySignature(pluginSigners, hashedReport, signatures);
    }

    emit(OCR3BASE_TRANSMITTED, OCR3Base_Transmitted {
	    ocrPluginType: ocrPluginType,
	    configDigest: reportContext.configDigest,
	    sequenceNumber: reportContext.sequenceBytes
    });
}

fun OCR3Base.hashReport(self, report: cell, reportContext: ReportContext): int {
	var data = beginCell()
		.storeRef(report)
		.storeUint(reportContext.configDigest, 256)
		.storeUint(0, 192) // Padding to 256 bits
		.storeUint(reportContext.sequenceBytes, 64)
		.endCell();
	return data.hash();
}

fun OCR3Base.verifySignature(
	mutate self,
	signers: UMap<uint256, uint8>, // <signer -> index>
	hashedReport: uint256,
	signatures: cell // Vector<SignatureEd25519>
) {
	var seen = Bitmap256.new();
	var signaturesIt = Iterator<SignatureEd25519>.new(signatures); 
	while (!signaturesIt.empty()) {
		val signature = signaturesIt.next();
		val signer = signature.signer;

		val (signerIndex, authorized) = signers.get(signer);
		assert(authorized, ERROR_UNAUTHORIZED_SIGNER);
		assert(isSignatureValid(hashedReport,signature.toCell().beginParse(), signer),	ERROR_INVALID_SIGNATURE);

		assert(!seen.has(signerIndex!), ERROR_NON_UNIQUE_SIGNATURES);
		seen.set(signerIndex!);
	}
}
